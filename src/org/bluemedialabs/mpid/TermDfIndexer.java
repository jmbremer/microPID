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

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TermDfIndexer extends Indexer {
	private TermDfCounters termDfCounters;
	private PathId pid = new PathId();

	protected int totalTokenCount;
	protected int currentTokenCount;



	public TermDfIndexer(XmlDecoderStream in, DataGuide guide,
			TermDfCounters tdfCounters) {
		super(in, guide);
		this.termDfCounters = tdfCounters;
	}


	public void onAttribute(GuideNode node, HashMap termCounts) {
		handleCounts(node, termCounts);
	}

	protected void handleCounts(GuideNode node, HashMap termCounts) {
		node.getPid(pid);
		try {
			addCounters(termCounts, pid);
		} catch (IOException e) {
			throw new IllegalStateException("IO exception while processing "
					+ "attribute/element-related counter (" + e + "), "
					+ "current node is " + node);
		}
	}

	public void onElementStart(GuideNode node) {
		// Nothing to do for term-df indexing here
	}

	public void onElementEnd(GuideNode node, HashMap termCounts) {
		// Same procedure as for attributes
		handleCounts(node, termCounts);
	}


	// For debugging...
	private void printHash(HashMap map) {
		Iterator it = map.entrySet().iterator();
		Map.Entry entry;

		System.out.print("(");
		while (it.hasNext()) {
			entry = (Map.Entry) it.next();
			System.out.print(((Term) entry.getKey()).getName() + "/"
					+ ((MutableInteger) entry.getValue()).getValue() + "  ");
		}
		System.out.println();
	}

	static protected int counterCount = 0;
	static protected int oneCount = 0;

	private void addCounters(HashMap map, PathId pid) throws IOException {
		if (map.size() == 0)  return;
		Iterator it = map.entrySet().iterator();
		Map.Entry entry;
		MutableInteger termNo;
		MutableInteger counter;

		while (it.hasNext()) {
			entry = (Map.Entry) it.next();
			termNo = (MutableInteger) entry.getKey();
			counter = (MutableInteger) entry.getValue();
			counterCount++;
			if (counter.getValue() == 1) {
				oneCount++;
			}
			termDfCounters.addCount(termNo.getValue(), pid, (short) counter.getValue(), this);
		}
	}

	/*
	 * OLD version
	 *
	private void addCounters(HashMap map, PathId pid) throws IOException {
	if (map.size() == 0)  return;
	Iterator it = map.entrySet().iterator();
	Map.Entry entry;
	Term term;
	MutableInteger counter;

	while (it.hasNext()) {
		entry = (Map.Entry) it.next();
		term = (Term) entry.getKey();
		counter = (MutableInteger) entry.getValue();
		counterCount++;
		if (counter.getValue() == 1) {
			oneCount++;
		}
		termDfCounters.addCount(term.getNo(), pid, (short) counter.getValue(), this);
	}
	}
	*/

	static public void passTwoB(String repDir, int inMemCounts) throws IOException {
		String FILE_NAME = repDir + "/xml.data";
		Tokens tokens = new Tokens();
		Token token, nextToken;
		String name, nextName;
		StringBuffer strBuf = new StringBuffer(256);
		String str;

		// Create a stream for the input
		// FOR STREAMS...
//		BufferedInputStream bis = new BufferedInputStream(
//				new FileInputStream(FILE_NAME));
//		BitInputStream is = new BitInputStream(bis);
		// FOR CHANNELS...
		BufferedInputChannel bic = new BufferedInputChannel(
				(new FileInputStream(FILE_NAME)).getChannel(), 245760);
		BitDataInput is = new BitDataInput(bic);

		// Load tokens and construct huffman coder, etc.
		DataGuide guide = DataGuide.load(repDir + "/guide.tree");
		System.out.println("The maximum bit length for any PathId in this "
			+ "data guide is " + guide.computeMaxPidLength());

		// Tokens and Terms are replaced by single Decoder now!

//		System.out.print("Loading tokens...");
//		IndexSeqFile tokenFile = new IndexSeqFile(repDir + "/tokens");
//		tokens.load(tokenFile);
//		System.out.print(tokens.getUniqueCount() + "..");
//		System.out.println("done.");
		/*
		System.out.print("Deriving terms...");
		Terms terms = new Terms(tokens);
		System.out.println("done.");
		*/
//		Terms terms = new Terms();
//		System.out.print("Loading terms...");
//		tokenFile = new IndexSeqFile(repDir + "/terms");
//		terms.load(tokenFile);
//		System.out.print(terms.getTermCount() + "..");
//		System.out.println("done.");

		Decoder decoder = Decoder.load(repDir + "/decoder.data");

		//Print terms
//		System.out.println("Terms are...");
//		System.out.println(terms.toString() + "...");
//		for (int i = 1; i <= terms.getTermCount(); i++) {
//			System.out.println(terms.get(i).toString());
//		}
//		System.out.println();

		System.out.print("Constructing decoder input stream...");
		XmlDecoderStream xds = new XmlDecoderStream(is, decoder/*tokens*/);
		System.out.println("done.");

		System.out.print("Removing old temporary indexing files...");
		int r = DfMerger.removeOldFiles(repDir + "/termdf.data");
		System.out.println(r + "..done.");

		System.out.print("Constructing term-df counter class...");
		// FOR STREAMS...
		DataOutputSequence dos = new DataFileOutputStreamSeq(
				repDir + "/termdf.data", 245760);
		// FOR CHANNELS...
//		DataOutputSequence dos = new DataFileOutputChannelSeq(
//				repDir + "/termdf.data", 245760);
		TermDfCounters counters = new TermDfCounters(inMemCounts,
				decoder.getTermCount(), dos, guide);
		System.out.println("done.");

		TermDfIndexer indexer = new TermDfIndexer(xds, guide, /*terms,*/ counters);
		indexer.totalTokenCount = tokens.getTotalCount();
		indexer.start();
		counters.finalize();

//		for (int i = 1; i <= terms.getTermCount(); i++) {
//			int c = terms.terms[i].getCount();
//			int cc = terms.terms[i].indexCount;
//			if (c != cc) {
//				System.out.println("Term " + i + "\t" + cc + "/" + c);
//			}
//		}

		System.out.println("Storage space for index: "
			+ (counters.overallSpace + decoder.getTermCount() * 8) + " bytes");
		System.out.println("\nTotal # of counters stored: " + indexer.counterCount);
		System.out.println("Counters with value 1:      " + indexer.oneCount);
		System.out.println("Detailed statistics: ");
		int i = 140;
		while (counters.countStats[--i] == 0);
		while (i >= 0) {
			System.out.println(i + ",\t" + counters.countStats[i]);
			i--;
		}
		long space = (counters.overallSpace >>> 3) + decoder.getTermCount() * 8;
		System.out.println("Storage space for index: "
			+ counters.overallSpace + "bits + " + (decoder.getTermCount() * 8)
			+ " bytes (=" + space + " bytes =" + ((int) Math.round(space / 1024))
			+ " KB)");

		// Store some of the meta data to a file
		BufferedWriter bw = new BufferedWriter(
				new FileWriter(repDir + "/idxstats.txt"));
		bw.write("Total # of counters.......... " + indexer.counterCount);
		bw.newLine();
		bw.write("Counters per counter size.... ");
		i = 140;
		while (counters.countStats[--i] == 0);
		while (i >= 0) {
			bw.newLine();
			bw.write(i + ",\t" + counters.countStats[i]);
			i--;
		}
		bw.flush();
		bw.close();
	}





	public static void main(String[] args) throws Exception {
		final int IN_MEMORY_COUNTS = 20000;
//		String REP_DIR = DATA_HOME + "/XMLmini";
		StopWatch watch = new StopWatch();
		IdxrConfig config= (IdxrConfig) IdxrConfig.load(args);

		watch.start();
		passTwoB(config.getSourceHome(args[0]), IN_MEMORY_COUNTS);
		watch.stop();
		System.out.println("Complete elapsed time: " + watch);
	}





















	/*
	 * OLD version
	 *
	public void start() throws IOException {
	int state;
	String str;
	Token token;
	Term term;
	GuideNode currentNode = guide.getRoot();
	String prefix = new String();
	HashMap currentHash = null;    //
	Pool currentPool = null;       // Pool of mutable integers for a certain depth
	MutableInteger count;
	boolean inAttrib = false;
	PathId pid = new PathId();
	int depth = 0;

	while ((token = in.nextToken()) != null) {
		if (++currentTokenCount % 1000000 == 0) {
			System.out.print(".." + ((int) currentTokenCount / 1000000)
					+ "mio");
		}
		str = token.getName();
//			if (str.compareToIgnoreCase("streets") == 0) {
//				System.out.print(str + "..");
//			}
//			System.out.print(str + "..");
//			System.out.flush();
//			if (c > 302290) {
//				System.out.print("  '" + str + "' (" + currentNode.getName()
//					+ "/" + currentNode.getParent().getName() + ")");
//			}

		// Do we have any special token here?
		if (str.charAt(0) == '<') {
			if (str.charAt(1) == '/') {
				state = ELEM_END;
			} else if (str.charAt(1) == '@') {
				state = ALL_ATTRIB_END;
			} else {
				state = ELEM_START;
			}
		} else if (str.charAt(0) == '@' && str.length() > 1) {
			state = ATTRIB_START;
		} else {
			state = REGULAR;
		}
		// Now, we know what state we are getting into...


		if (state == ELEM_END || (inAttrib && state == ATTRIB_START)
				|| state == ALL_ATTRIB_END) {
			// It's a tag's end or an attribute's end
			depth--;
			// Store term info from current hash table
//					System.out.print("\nWriting back hash...");
			currentNode.getPid(pid);

			addCounters(currentHash, pid);

			if (state == ELEM_END) {
				// Increment child counter not before generating PID!
				currentNode.incCurrentChildCount();

				// When we leave this element all its children's child
				// counts should be reset!
				// (There's no need to do the same for attributes as we
				//  know the never have children an can only occur
				//  once within an element.)
				// ..reset all children's child counters to zero!..

				currentNode.resetChildCounts();
			}

//					printHash(currentHash);
			// Release resources and move on level up in the data guide
			unusedTables.push(currentHash);
			currentHash = (HashMap) hashTables.pop();
			currentPool.releaseAll();
			unusedPools.push(currentPool);
			currentPool = (Pool) mutIntPools.pop();
			currentNode = currentNode.getParent();
			inAttrib = false;
//					prefix = prefix.substring(2);
//					System.out.println(prefix + str);
		}

		if (state == ELEM_START || state == ATTRIB_START) {
			// It's and element's/attribute's start
			depth++;
			// Keep track of whether we are in an attibute
			// (because attrib's ends are hard to detect otherwise)
			if (state == ATTRIB_START) {
				inAttrib = true;
			}


//				try {
				currentNode = currentNode.getChild(str.substring(1));

//				} catch (NullPointerException e) {
//					System.out.println("Null pointer exception while getting "
//						+ "node " + currentNode + "'s child with name '"
//						+ str + "' (depth=" + depth + ", c=" + currentTokenCount + ")...");
//					System.out.println("Current node stack:");
//                    System.out.flush();
//				}
//                if (currentNode == null) {
//                    System.out.println("Have a null child '" + str.substring(1)
//                            + "' ('" + str + "')");
//                    printHash(currentHash);
//                }

//                    System.out.print(prefix + str);
//                    currentNode.getPid(pid);
//                    System.out.println("\t" + pid);
//					prefix += "  ";


			// Replace current hash table
			if (currentHash != null) hashTables.push(currentHash);
			currentHash = (HashMap) unusedTables.pop();
			currentHash.clear();
			// Replace current pool
			if (currentPool != null) mutIntPools.push(currentPool);
			currentPool = (Pool) unusedPools.pop();
			currentPool.releaseAll();
		}

		if (state == REGULAR && Terms.acceptAsTerm(str)) {
			term = terms.get(str.toLowerCase());
//				System.out.println(prefix + term);
			// Have we seen this term before?...
			count = (MutableInteger) currentHash.get(term);
			if (count == null) {
				// ..no counter for this term there as of now
				count = (MutableInteger) currentPool.claim();
				count.setValue(1);
				currentHash.put(term, count);
			} else {
				count.inc();
			}
		}

	}

	}
	*/
}