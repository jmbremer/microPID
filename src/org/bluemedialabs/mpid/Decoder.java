/*
 * Copyright 2008 blue media labs ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bluemedialabs.mpid;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import org.bluemedialabs.io.BitInput;
import org.bluemedialabs.io.BufferedInputChannel;
import org.bluemedialabs.io.BufferedOutputChannel;
import org.bluemedialabs.mpid.HuffmanCoder.StartCode;
import org.bluemedialabs.util.Configuration;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Decoder {
	// For later use... :
	static private final int IMIN             = Integer.MIN_VALUE;

	/*
	 * Codes returned by decode(). A positive number denotes a term number,
	 * zero some text that is neither special in XML nor a term nor a stop word.
	 * A low negative number is the negative value of a node number indicating
	 * an element or attribute start. High negative number are reserved for
	 * special codes, i.e., element tag end, all attributes end, stop words,
	 * and possibly others in the future.
	 */
	static public final int NAT               = 0; // Not a term
	static public final int STOP_WORD         = IMIN + 1; // A stop word (term)
	static public final int ELEMENT_END       = IMIN + 2;
	static public final int ALL_ATTRIBUTE_END = IMIN + 3;
	static public final int EOF               = IMIN;

	static public final int MAX_PSEUDO_CODE   = IMIN + 3;

	private StartCode[] startCodes;
	private int minCodeLen;
	// PROTECTED ONLY FOR DEBUGGING:
	protected int mappings[];   // Mapping from token#'s to term#'s etc.
//	private byte flags[];     // Flags for each mapping
	private int termCount;
	private int tokenCount;


	/*+**********************************************************************
	 * Class functions
	 ************************************************************************/

	static public Decoder load(String fileName) throws IOException {
		Decoder decoder = new Decoder();
		System.out.print("Loading decoder...");
		BufferedInputChannel bic = BufferedInputChannel.create(fileName);
		decoder.load(bic);
		bic.close();
		System.out.println("done.");
		return decoder;
	}


	static boolean isNode(int code) {
		return (code < 0 && code > MAX_PSEUDO_CODE);
	}

	static public Decoder load(Configuration config, String cfgName)
			throws IOException {
		return load(config.getProperty(cfgName, "SourceHome") + "/"
					+ config.getProperty(cfgName, "DecoderFileName"));

	}


	static public void generate(Configuration config, String cfgName)
			throws IOException {
		System.out.println("Loading tokens, terms, and DataGuide and creating "
						   + "Huffman coder...");
		Tokens tokens = Tokens.load(config, cfgName);
		Terms terms = Terms.load(config, cfgName);
		DataGuide guide = DataGuide.load(config, cfgName);
		HuffmanCoder coder = new HuffmanCoder(tokens);
		String fileName = config.getProperty(cfgName, "SourceHome") + "/"
				  + config.getProperty(cfgName, "DecoderFileName");
		System.out.println("Generating decoder...");
		Decoder decoder = new Decoder();
		decoder.init(tokens, terms, guide, coder.getStartCodes());
		System.out.print("Storing decoder...");
		BufferedOutputChannel out =
				BufferedOutputChannel.create(fileName);
		decoder.store(out);
		out.close();
		System.out.println("done.");
	}



	/*+**********************************************************************
	 * Object functions
	 ************************************************************************/

	protected Decoder() {}


	public void init(Tokens tokens, Terms terms, DataGuide guide, StartCode[] sc) {
		// Duplicate start codes
		startCodes = new StartCode[sc.length];
		for (int i = 0; i < sc.length; i++) {
			startCodes[i] = (StartCode) sc[i].clone();
		}
		minCodeLen = tokens.get(1).getCodeLen();

		// Prepare node name to number mappings...
		int nodeCount = guide.getNodeCount();
		HashMap<String, Integer> tagMap = new HashMap<String, Integer>(
		        (int) (nodeCount * 1.3));
		GuideNode node;
		for (int i = 1; i <= nodeCount; i++) {
			node = guide.getNode(i);
			tagMap.put(node.getName(), new Integer(i));
		}
		// Generate mapping table from tokens to terms
		tokenCount = tokens.getUniqueCount();
		termCount = terms.getTermCount();
		mappings = new int[tokenCount + 1];
		mappings[0] = -1;
		for (int i = 1; i <= tokenCount; i++) {
			Token token = tokens.get(i);
			String str = token.getName();
			Term term = terms.get(str.toLowerCase());
			if (term != null) {
				mappings[i] = term.getNo();
			} else {
				if (str.charAt(0) == '<' && str.length() > 1) {
					// Some (special) element name
					if (str.charAt(1) == '/') {
						// Element end
						mappings[i] = ELEMENT_END;
					} else if (str.charAt(1) == '@') {
						mappings[i] = ALL_ATTRIBUTE_END;
					} else {
						// An element name
						try {
						mappings[i] =
						  - ((Integer) tagMap.get(str.substring(1))).intValue();
						} catch (NullPointerException e) {
							System.out.println("Problems finding mapping for '"
									+ str.substring(1) + "'");
							throw e;
						}
					}
				} else if (str.charAt(0) == '@' && str.length() > 1) {
					// An attribute name
					mappings[i] =
						  - ((Integer) tagMap.get(str.substring(1))).intValue();
				} else {
					// Not a term but nothing special either...
					mappings[i] = NAT;
				}
			}
		}
	}


	/**
	 * Returns the code related to the next token found in the given
	 * bit input stream or null if no more tokens are available.
	 */
	public int decode(BitInput bis) throws IOException {
		int b;
		StartCode sc;
		try {
			int len = minCodeLen;
			int code = bis.read(len);
//			System.out.print("*" + Integer.toBinaryString(code));
			sc = startCodes[len];
			while (code < sc.bits) {
				b = bis.read();
				code = (code << 1) | b;  // changed from + b on 2003-02-04
				sc = startCodes[++len];
			}

			code = sc.termNo + (code - sc.bits);

//			System.out.print(" ##" + code);
//			if (mappings[code] != code) {
//				System.out.print("(" + mappings[code] + ")");
//			}

			return (mappings[code]);
			//			System.out.println("Code is " + tmp + " (len=" + len + ")  ");

			// How about the end condition? Can anything happen here that
			// would not result in an EOF exception but not be a valid code???
		} catch (EOFException e) {
			// No more data left. Note that the case that we are right
			// in the middle of decoding a token when this happens may
			// not occur unless there is an invalid code right at the end
			// (or something screwed up somewhere in the middle).
			// Even if the stream contains some additinal 0-bytes at the end
			// (the standard way of padding the end) this should not
			// break things!
			return EOF;
		}
	}

	public int getTokenCount() { return tokenCount; }
	public int getTermCount() { return termCount; }

	public String toString() {
		return toString(null, null, null);
	}

	public String toString(Tokens tokens, Terms terms, DataGuide guide) {
		StringBuffer buf = new StringBuffer(1000);
		Term term;
		Token token;

		buf.append("(");
		for (int i = 1; i <= tokenCount; i++) {
			buf.append("\n");
			buf.append(i);
			buf.append(") ");
			if (tokens != null && terms != null && guide != null) {
				token = tokens.get(i);
				buf.append(token.getName());
				buf.append("\t - ");
				if (mappings[i] > 0) {
					term = terms.get(mappings[i]);
					buf.append(term.getName());
//					buf.append("(");
//					buf.append(term.getNo());
//					buf.append(")");
				} else if (mappings[i] < 0
						   && mappings[i] > Decoder.MAX_PSEUDO_CODE) {
					buf.append(guide.getNode(- mappings[i]).getName());
					buf.append(" -node- ");
				}
			}
			buf.append(" (");
			buf.append(mappings[i]);
			buf.append(")");
		}
		buf.append("\n");
		buf.append(")");
		return buf.toString();
	}


	/*+******************************************************************
	 * Storable implementation
	 ********************************************************************/

	public void store(DataOutput out) throws IOException {
		// Store start codes...
		out.writeInt(startCodes.length);
		for (int i = 0; i < startCodes.length; i++) {
			startCodes[i].store(out);
		}
		out.writeInt(minCodeLen);
		// Store mappings (flags unused yet!)
		out.writeInt(tokenCount);
		out.writeInt(termCount);
		for (int i = 0; i < mappings.length; i++) {
			out.writeInt(mappings[i]);
		}
	}

	public void load(DataInput in) throws IOException {
		int count;
		// Load start codes...
		count = in.readInt();
		startCodes = new StartCode[count];
		for (int i = 0; i < startCodes.length; i++) {
			startCodes[i] = new StartCode();
			startCodes[i].load(in);
		}
		minCodeLen = in.readInt();
		// Load mappings
		tokenCount = in.readInt();
		termCount = in.readInt();
		mappings = new int[tokenCount + 1];
		for (int i = 0; i < mappings.length; i++) {
			mappings[i] = in.readInt();
		}
	}

	public int byteSize() {
		return -1;
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		// Check validity of input
		if (args.length != 2) {
			printUsage();
		}
		// Load configuration
		IdxrConfig config = (IdxrConfig) IdxrConfig.load(args[1]);

//		dumpDecoderFile(config, args[0]);
		generate(config, args[0]);
	}

	static private void printUsage() {
		System.out.println();
		System.out.println("Expecting exactly 2 arguments for idxr.Decoder:");
		System.out.println("(1) The configuration name");
		System.out.println("(2) The configuration file name");
	}

	static public void dumpDecoderFile(IdxrConfig config, String cfgName)
			throws IOException {
		Tokens to = Tokens.load(config, cfgName);
		Terms te = Terms.load(config, cfgName);
		DataGuide g = DataGuide.load(config, cfgName);
		Decoder d = Decoder.load(config, cfgName);

		System.out.println(d.toString(to, te, g));
	}

}