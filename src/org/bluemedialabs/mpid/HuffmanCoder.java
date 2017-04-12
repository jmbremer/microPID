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
import java.text.DecimalFormat;
import org.bluemedialabs.io.BitInput;
import org.bluemedialabs.io.IndexSeqFile;
import org.bluemedialabs.io.IndexSeqOutputChannel;
import org.bluemedialabs.io.Storable;
import org.bluemedialabs.util.StopWatch;
import org.bluemedialabs.mpid.Codeables.Codeable;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class HuffmanCoder {
	private Codeables codeables;
//	private Delimiters delims;
	private int termCount;
//	private int delimCount;
	private int maxFrequence;   // A value larger than anything for build. heap
//	private byte[] codeLen;
	private int minCodeLen;     // The minimum # of bits of any code word
	private int maxCodeLen;     // The maximum # of bits of any code word
	private StartCode[] startCodes;   // Codes of the 1st elem. for each code length

	// JUST FOR DEBUGGING:
//	public Decoder decoder = null;


	public HuffmanCoder(Codeables codeables/*, Delimiters delims*/) {
		this.codeables = codeables;
//		this.delims = delims;
		termCount = codeables.getUniqueCount();
		if (termCount <= 0) {
			throw new IllegalStateException("Huffman codes can only be "
				+ "determined if there is at least one term, but there were "
				+ " no codeables at all");
		}
//		delimCount = delims.getUniqueCount();
		maxFrequence = codeables.getTotalCount() + 1;
//		codeLen = new byte[termCount /*+ delimCount*/ + 1];
		if (codeables.getCodeable(1).getCodeLen() == 0) {
			// Codes have not yet been determined, thus do this now
			calcCodeLengths();
			// Reorder codeables accordingly
//			System.out.println("The first 40 codeables are:");
//			for (int i = 1; i <= 40; i++) {
//				System.out.println(codeables.get(i));
//			}
			codeables.sort(/*Token.CODE_LENGTH_COMPARATOR*/);
//			System.out.println("The first 40 codeables AFTER SORTING are:");
//			for (int i = 1; i <= 40; i++) {
//				System.out.println(codeables.get(i));
//			}
//			for (int i = 1; i <= termCount - 1; i++) {
//				if (codeables.get(i).huffCodeLen < codeables.get(i+1).huffCodeLen) {
//					System.out.println("Term " + i + " has shorter code length "
//						+ "than term " + (i + 1) + " (" + codeables.get(i).huffCodeLen
//						+ " vs " + codeables.get(i+1).huffCodeLen + ")");
//				}
//			}
			// NOTE: we have to assume here that there is no other storage
			// structure relying on the current term numbers!
		}
		// Now, in any case (previously existing or non-existing code lengths)
		// determine the start codes table etc.
		determStartCodes();


		// JUST FOR DEBUGGING:
		/*
		try {
			decoder = Decoder.load(Config.REP_HOME + "/decoder.data");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		*/
	}


	private void determStartCodes() {
		int tokensPerLen;
		int currentLen, nextLen;

		minCodeLen = codeables.getCodeable(1).getCodeLen();
		maxCodeLen = codeables.getCodeable(termCount).getCodeLen();
		startCodes = new StartCode[33];
		for (int i = 0; i < 33; i++) {
			startCodes[i] = new StartCode();
			startCodes[i].termNo = -1;
			startCodes[i].bits = Integer.MAX_VALUE;
			// The max value makes sure the comparison done during decoding
			// will always fail on none-existing code length!
		}
		currentLen = maxCodeLen;
		tokensPerLen = 1;
		for (int i = termCount - 1; i > 0; i--) {
			nextLen = codeables.getCodeable(i).getCodeLen();
			if (nextLen < currentLen) {
				// Found a transition to a smaller code length
				startCodes[currentLen].termNo = i + 1;
				startCodes[currentLen].count = tokensPerLen;
				if (currentLen == maxCodeLen) {
					startCodes[currentLen].bits = 0;
				} else {
					startCodes[currentLen].bits =
						(startCodes[currentLen + 1].bits
							+ startCodes[currentLen + 1].count) >>> 1;
				}
				// Make sure there are no holes in the start codes array
				// when there are lengths with no codeables.
				for (int j = currentLen - 1; j > nextLen; j--) {
					startCodes[j].termNo = -1;  // invalid!
					startCodes[j].count = 0;
					startCodes[j].bits =
						(startCodes[j + 1].bits + startCodes[j + 1].count) >>> 1;
				}
				currentLen = nextLen;
				tokensPerLen = 0;
			}
			tokensPerLen++;
		}
		// Now, add the still missing information for the tokens of least length
		startCodes[minCodeLen].termNo = 1;
		startCodes[minCodeLen].count = tokensPerLen;
		startCodes[minCodeLen].bits =
			(startCodes[minCodeLen + 1].bits
					+ startCodes[minCodeLen + 1].count) >>> 1;;
	}

	protected StartCode[] getStartCodes() {
		return startCodes;
	}


	private void calcCodeLengths() {
		int n = termCount /*+ delimCount*/;
		int[] A = new int[2 * n + 1];
		int h = n;
		int m1, m2;
		int node, depth;

		// Initialize array...first the term part
		for (int i = 1; i <= termCount; i++) {
			A[n + i] = codeables.getCodeable(i).getCount();
			A[i] = n + i;
		}
		// ...then the delimiter part
//		for (int i = termCount + 1; i <= termCount + delimCount; i++) {
//			A[n + i] = delims.getDelimCount(i - termCount);
//			A[i] = n + i;
//		}
		// Build heap
		init(A, 1, n);
		// Do the actual length calculation by repeatedly removing
		// the two smallest frequences at the top of the heap
		while (h > 1) {
			m1 = A[1];  // smallest frequence
			A[1] = A[h];
			h--;
			sift(A, 1, A[1], h);
			m2 = A[1];  // 2nd smallest frequence
			A[h+1] = A[m1] + A[m2]; // ..the combined frequence
			A[1] = h + 1;   // adjust forward and backward pointers
			A[m1] = h + 1;
			A[m2] = A[m1];
			sift(A, 1, A[1], h);    // sift in the combined frequence
		}
		// Compute the term (and delim.) lengths by following pointers backwards
		// (but implemented the other way around)
//		StopWatch w = new StopWatch();
//		w.start();
		A[2] = 0;
		for (int i = 3; i <= 2 *n; i++) {
			A[i] = A[A[i]] + 1;
		}
		for (int i = n + 1; i <= 2 * n; i++) {
			codeables.getCodeable(i - n).setCodeLen((byte) A[i]);
		}
//		for (int i = n + 1; i <= 2 * n; i++) {
//			depth = 0;
//			node = i;
//			while (node > 2) {
//				depth++;
//				node = A[node];
//			}
//			codeLen[i - n] = depth;
//		}
//		w.stop();
//		System.out.println(w);
	}


	private void sift(int[] A, int root, int rootVal, int last) {
		int leftChild, rightChild;
		int r, a, b;    // values of root, and left and right children
		if (root > last) {
			// Bottom reached, nothing more to do
			return;
		} else {
			r = A[rootVal];
			a = ((leftChild = 2 * root) <= last? A[A[leftChild]]: maxFrequence);
			b = ((rightChild = leftChild + 1) <= last?
												A[A[rightChild]]: maxFrequence);
			if (r > a) {
				// Need to sift into left child (or right child)
				if (a < b) {
					// a has to move up
					A[root] = A[leftChild];
//					A[leftChild] = A[root];
					// Let root think further into left sub-heap...
					sift(A, leftChild, rootVal, last);
				} else {
					// b moves up
					A[root] = A[rightChild];
//					A[rightChild] = A[root];
					// Let root think further into right sub-heap...
					sift(A, rightChild, rootVal, last);
				}
			} else if (r > b) {
				// Need to swap with right child (for sure)
				A[root] = A[rightChild];
//				A[rightChild] = A[root];
				// Let root think further into right sub-heap...
				sift(A, rightChild, rootVal, last);
			} else {
				// Found the position the root has to sink into
				A[root] = rootVal;
			}
		}
	}

	private void init(int[] A, int root, int last) {
		if (root > last) {
			return;
		} else {
			// Init left child heap
			init(A, 2 * root, last);
			// Init right child heap
			init(A, 2 * root + 1, last);
			// Init ourselfes by sifting our root
			sift(A, root, A[root], last);
		}
	}


	/**
	 * Returns the code for the given token.
	 */
	public int encode(Codeable c, int no) {
		StartCode code = startCodes[c.getCodeLen()];
//		System.out.print("'" + Indexer.tokens.get(no).getName() + "'");
		return code.bits + (no - code.termNo);
	}

	/**
	 * Returns the token related to the next code found in the given
	 * bit input stream or null if no more tokens are available.
	 */
	public Codeable decode(BitInput bis) throws IOException {
		int code = getCode(bis);
//		System.out.print(" ##" + code);
//		System.out.flush();
//		if (code >= 0) {
			// ..otherwise there is no related code mapping!
//			try {
//				int c = decoder.mappings[code];
//				System.out.print("(" + c  + ")");
//			} catch (ArrayIndexOutOfBoundsException e) {
//				System.out.print("(n/a)");
//			}
//		}
		return (code > 0? codeables.getCodeable(code): null);
	}

	public int getCode(BitInput bis) throws IOException {
		try {
			int len = minCodeLen;
			int code = bis.read(len);
//			System.out.print("*" + Integer.toBinaryString(code));
			while (code < startCodes[len].bits) {
				int b = bis.read();

//				System.out.print("*" + b);
//				System.out.flush();

				code = (code << 1) + b;
				len++;
			}
//			System.out.print("  ");
			StartCode sc = startCodes[len];

			return (sc.termNo + (code - sc.bits));
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
			return -1;
		}
	}

	public StartCode getStartCode(int len) {
		return startCodes[len];
	}

	// Should be eliminated later!!
//	public int getTermCodeLength(int tno) {
//		return codeables.get(tno).huffCodeLen;
//	}

//	public int getDelimCodeLength(int dno) {
//		return codeLen[termCount + dno];
//	}


//	public Decoder createDecoder() {
//	}

	public String toString() {
		StringBuffer buf = new StringBuffer(2024);

		buf.append("(minCodeLen=");
		buf.append(minCodeLen);
		buf.append(", maxCodeLen=");
		buf.append(maxCodeLen);
		buf.append(", startCodes=(\n");
		buf.append("\t(len=");
		buf.append(minCodeLen);
		buf.append(", code=");
		buf.append(startCodes[minCodeLen].toString(minCodeLen));
		buf.append(")");
		for (int i = minCodeLen + 1; i <= maxCodeLen; i++) {
			buf.append(",\n\t(len=");
			buf.append(i);
			buf.append(", code=");
			buf.append(startCodes[i].toString(i));
			buf.append(")");
		}
		buf.append("))");
		return buf.toString();
	}


	/************************************************************************
	 * Simple collection of code-related information.
	 ************************************************************************/
	static protected class StartCode implements Storable {
		static private final String empty
							= new String("                                ");
		static private final String format
							= new String("00000000000000000000000000000000");
		int termNo; // smallest term# with current bit length
		int bits;   // bits used for this code (rightmost bits!??)
		int count;  // # of tokens with this code's length

		public Object clone() {
			StartCode sc = new StartCode();
			sc.termNo = termNo;
			sc.bits = bits;
			sc.count = count;
			return sc;
		}

		public String toString(int len) {
			StringBuffer buf = new StringBuffer(100);
			String s1, s2;
			buf.append("(termNo=");
			buf.append(termNo);
			buf.append(", \tbits=");
			s1 = Integer.toBinaryString(bits);
			s2 = format.substring(0, len - s1.length());
			buf.append(empty.substring(0, 32 - len));
			buf.append(s2);
			buf.append(s1);
			buf.append(", count=");
			buf.append(count);
			buf.append(")");
			return buf.toString();
		}
		public String toString() {
			return toString(32);
		}


		/*+******************************************************************
		 * Storable implementation
		 ********************************************************************/

		public void store(DataOutput out) throws IOException {
			out.writeInt(termNo);
			out.writeInt(bits);
			out.writeInt(count);
		}

		public void load(DataInput in) throws IOException {
			termNo = in.readInt();
			bits = in.readInt();
			count = in.readInt();
		}

		public int byteSize() {
			return 9;
		}

	}



/****************************************************************************
 * Various TESTs
 ****************************************************************************/

	static public void main(String[] args) throws Exception {
//		System.out.println("Number 1234 has binary representation " +
//			new Integer(1234).toBinaryString(32));
		lengthTest();

	}


	static private void lengthTest() throws Exception {
		final String REP_DIR= "F:/Data/tmp";
		Tokens tokens = new Tokens();
		Token token;
//		Delimiters delims = new Delimiters();

		// Load tokens
		IndexSeqFile termFile = new IndexSeqFile(REP_DIR + "/tokens");
		tokens.load(termFile);
		// Construct dummy delimiters (for now)
//		adjustDelimCount(delims, ' ', (int) (codeables.getTotalCount() * 0.1)); // 10% spaces
//		adjustDelimCount(delims, ',', (int) (codeables.getTotalCount() * 0.005));
//		adjustDelimCount(delims, '.', (int) (codeables.getTotalCount() * 0.008));
		// Create coder
		HuffmanCoder test = new HuffmanCoder(tokens/*, delims*/);
		IndexSeqOutputChannel tokenOut = IndexSeqOutputChannel.create(
					"F:/Data/tmp/tokens", false);
		tokens.store(tokenOut);
		System.out.println("Tokens:\n" +  tokens);
		// Now, generate some test output
		System.out.println("Some term code lengths...");
		for (int i = 1; i <= 40; i++) {
			token = tokens.get(i);
			System.out.println(token + "\t" + token.huffCodeLen);
		}
//		System.out.println("All delimiter code lengths...");
//		for (int i = 1; i <= delims.getUniqueCount(); i++) {
//			System.out.println("'" + delims.getDelimiter(i) + "' ("
//				+ delims.getDelimCount(i) + " times)\t" +
//				test.getDelimCodeLength(i));
//		}
		System.out.println("\nNumber of tokens per code length...");
		int[] len = new int[33];
		for (int i = 0; i <= 32; i++) len[i] = 0;
		long x = 0;
		int oneCount = 0, moreCount = 0;
		for (int i = 1; i <= tokens.getUniqueCount(); i++) {
			int c = tokens.get(i).huffCodeLen;
			len[c]++;
			x += c * tokens.get(i).getCount();
			if (c == 26) {
				if (tokens.get(i).getCount() == 1) oneCount++;
				else moreCount++;
			}
		}
		for (int i = 1; i <= 32; i++) {
			System.out.println(i + "\t" + len[i]);
		}
		System.out.println("\nIdeal compression (only based on tokens)...");
		System.out.println("\t" + (x / (double) (tokens.getTotalCount() * (long) 32))+ "%");
		System.out.println("\nNumber of 26-bit tokens occuring only once...");
		System.out.println("\t" + oneCount + " out of " + (oneCount + moreCount));

		System.out.println("\n=====================================\n");
//		System.out.println(test);
	}

	static private void adjustDelimCount(Delimiters delims, char ch, int count) {
		for (int i = 0; i < count; i++) {
			delims.addAndCount(ch);
		}
	}


	static private void heapTest() {
		HuffmanCoder test = new HuffmanCoder(null);
		// Some adjustments in the constructor are necessary in order for
		// the following to run properly without tokens and delims supplied!
		int A[] = {-17, 11, 12, 13,14, 15, 16, 17, 18, 19, 20,
					   11, 20, 8, 2, 12, 23, 8, 21, 17, 10};
		System.out.println();
		printA(A, 1, 10);
		System.out.println();
		test.sift(A, 1, A[1], 10);
		printA(A, 1, 10);
		System.out.println();
		test.sift(A, 2, A[2], 10);
		printA(A, 1, 10);
		System.out.println();
		test.init(A, 1, 10);
		printA(A, 1, 10);
		System.out.println();
	}

	static private void printA(int[] A, int root, int last) {
		if (root <= last) {
			System.out.print("  " + A[A[root]] + " (");
			printA(A, 2 * root, last);
			System.out.print(",");
			printA(A, 2 * root + 1, last);
			System.out.print(")");
		}
	}
}