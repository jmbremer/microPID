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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p>Do merge sort for long lists and something simpler for short lists!</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class InvertedDfList implements Storable {
	static public final String REP_HOME = "somewhere"; // To be replaced...

	static public final boolean STORE_ENCODED  = true;
	static public final boolean LOAD_ENCODED  = true;
	static public int DEFAULT_INITIAL_CAPACITY = 1000;
	static public double INCREASE_FACTOR = 0.5;
	static private PrintStream out = System.out;

	protected Counter[] counts;
	private int mylength = 0;
	private DataGuide guide;
	private BitDataOutput bitOut = new BitDataOutput(null);
	private BitDataInput bitIn = new BitDataInput(null);
	private Base128 b128 = new Base128();


	public InvertedDfList(int initialCapacity, DataGuide guide) {
		if ((STORE_ENCODED || LOAD_ENCODED) && guide == null) {
			throw new NullPointerException("Need a valid document guide to "
					+ "support compressed storage but guide is null");
		}
		this.guide = guide;
		counts = new Counter[initialCapacity];
		for (int i = 0; i < initialCapacity; i++) {
			counts[i] = new Counter();
		}
	}

	public InvertedDfList(DataGuide guide) {
		this(DEFAULT_INITIAL_CAPACITY, guide);
	}

	public InvertedDfList() {
		this(DEFAULT_INITIAL_CAPACITY, null);
	}

//		protected void reuse(int termNo, Counter[] docCounts, int start) {
//			this.termNo = termNo;
//			this.docCounts = docCounts;
//			this.start = start;
//			// Determine list length
//			elemCount = 1;
//			int i = docCounts[start].next;
//			while (i > 0) {
//				i = docCounts[i].next;
//				elemCount++;
//			}
//		}

	/**
	 * Sets the number of this inverted list's related record. This might become
	 * necessary in case list should be merged into an initially empty list.
	 * Finding a non-matching record number, the merging will fail throwing
	 * an exception otherwise.
	 */
//	protected void setRecNo(int no) {
//		recNo = no;
//	}

//	public int getRecNo() {
//		return recNo;
//	}

	public void adjustCapacity(int length) {
		Counter[] c;
		if (length < mylength) {
			throw new IllegalArgumentException("Cannot decrease inverted list "
				+ "capacity from " + mylength + " to " + length + " while "
				+ "data is in use");
		}
		c = new Counter[length];
		// The copying is necessary in order to preserve the counter space!
//		if (preserve) {
			System.arraycopy(counts, 0, c, 0, counts.length);
//		}
		for (int i = counts.length; i < length; i++) {
			c[i] = new Counter();
		}
		counts = c;
	}

	public int capacity() {	return counts.length; }

	public void setLength(int len) {
		if (len > capacity()) {
			throw new IllegalArgumentException("Cannot adjust inverted list "
					+ "length beyond its capacity (" + len + " > "
					+ capacity() + ")");
		}
		mylength = len;
	}
	public int length() { return mylength; }
	public void clear() { mylength = 0; }

	public boolean isEmpty() {
		return (length() == 0);
		// Length = 0 means, there was no more record, because only records
		// with at least some counter data are actually stored.
	}


	public void add(Counter c) {
		if (length() >= capacity()) {
			// Need to create more room
			adjustCapacity((int) (capacity() * (1 + INCREASE_FACTOR)));
		}
		c.copyInto(counts[mylength++]);
	}

	public void add() {
		if (length() >= capacity()) {
			// Somebody added another element through a reference, but
			// the capacity wasn't large enough!?
			throw new IllegalStateException("Add() called though length has "
					+ "reached capacity " + capacity());
		}
		mylength++;

	}

	/**
	 * Returns a reference to the counter at the given index.
	 *
	 * @param index
	 * @return
	 */
	public Counter get(int index) {
		return counts[index];
	}

	/**
	 * Merges the given list into this list, assuming the lists are disjunct
	 * for now! Thus, merging is equivalent to appending.
	 */
	public void append(InvertedDfList list) {
		int combinedLen;

		combinedLen = length() + list.length();
		// Make sure our list has enough capacity to hold the combined list
		if (capacity() < combinedLen) {
			adjustCapacity(combinedLen);
		}
		int j = 0;
		try {

		int i;
		for (i = length(); i < combinedLen; i++) {
			list.counts[j].copyInto(counts[i]);
			j++;
		}

		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println(";;;LJLJL");
		}
		mylength = combinedLen;
	}

	/**
	 * ...assuming both lists are <em>sorted</em>!!!
	 */
/*
	public void merge(InvertedList list) {
		int len = length();             // length of this list
		int listLen = list.length(); // length of supplied list
		int newLen = len + listLen;
		int pos;
		int listPos = 0;

//		System.out.println("Merging this list of length " + length()
//			+ " with given list of length " + list.length());
		// make sure there's enough space left
		if (capacity() < newLen) {
//			System.out.println("Need to increase capacity from " + capacity()
//				+ " to " + newLen);
			adjustCapacity(newLen); // this is the maximum theoretical length
		}
		// copy current list to end
		pos = newLen - len;
		System.arraycopy(counts, 0, counts, pos, len);
//		System.out.println("Copied old contents to position " + pos );
		int i = -1;
		while (++i < newLen && pos < len + listLen && listPos < listLen) {
			if (counts[pos].no < list.counts[listPos].no) {
				counts[i] = counts[pos++];
			} else if (counts[pos].no > list.counts[listPos].no) {
				counts[i] = list.counts[listPos++];
			} else {
				// actually merge some values
				counts[i] = counts[pos++];
				counts[i].value += list.counts[listPos++].value;
				newLen--;   // ..because that was just the theoretical maximum
			}
		}
		// copy rest of whichever list was longer
		if (i < newLen) {
			// still elements left
			if (pos < newLen) {
				System.arraycopy(counts, pos, counts, i, len + listLen - pos);
			} else {
				System.arraycopy(list.counts, listPos, counts, i, listLen - listPos);
			}
		}
		mylength = newLen;
		// that should do it!?...
	}
*/

//	private void mergeLists(int pos, int listPos, Queue q) {
//		if (!q.isEmpty()) {
//			// need to compare new list with queue
//
//
//		} else {
//			if (counts[pos].no < list.counts[listPos].no) {
//				pos++;
//			} else if (counts[pos].no = list.counts[listPos].no) {
//				counts[pos].value += list.counts[listPos].value;
//				pos++;
//				listPos++;
//			} else {
//				// actually merge some values
//				counts[i] = counts[pos++];
//				counts[i].value += list.counts[listPos++].value;
//				newLen--;   // ..because that was just the theoretical maximum
//			}
//
//	}

	public Iterator iterator() {
		return new InvListIterator(this);
	}


	public void sort() {
		if (mylength > 1) {
			if (mylength <= 100) {
				quickSort();
			} else {
				Arrays.sort(counts, 0, mylength);
			}
		}
	}

	/**
	 * Not a quicksort but a quick sort for small lists.
	 */
	private void quickSort() {
		Counter c;
		for (int i = 0; i < mylength - 1; i++) {
			for (int j = i + 1; j < mylength; j++) {
				if (counts[i].compareTo(counts[j]) > 0) {
					// Swap counts
					c = counts[i];
					counts[i] = counts[j];
					counts[j] = c;
				}
			}
		}
	}

/*	private void mergeSort(int from, int to) {
		int middle;
		if (to == from + 1) {
			return;
		} else {
			// Split range in half
			middle = to + ((to - from + 1) / 2);
			mergeSort(from, middle);
			mergeSort(middle, to);
			merge(from, middle, to);
		}
	}

	private void merge(int from, int middle, int to) {
		int f = from, t = middle;
		while (f < middle && t < to) {
			if (counts[f].compareTo(counts[t]) > 0) {
				c = counts[f];
				counts[f] = counts[t];
				counts[t] = c;
				t++;
			} else {
				f++;
			}
		}
	}
	*/


	public String toString() {
		StringBuffer buf = new StringBuffer(Math.max(1, mylength) * 13);

		buf.append("(");
		if (mylength == 0) {
			buf.append("<empty>");
		} else {
			buf.append("length=");
			buf.append(mylength);
			buf.append(", list=(");
			buf.append(counts[0].getPid().toString(guide));
			buf.append(",");
			buf.append(counts[0].getCount());
			for (int i = 1; i < mylength /*&& i < 100*/; i++) {
//				if (i == 16)
//				pl(i + ",");
//				buf.append("\n" + i + ") ");
				buf.append(" ");
				buf.append(counts[i].getPid().toString(guide));
				buf.append(",");
				buf.append(counts[i].getCount());
			}
		}
//		if (mylength > 100) {
//			buf.append(" <");
//			buf.append(mylength - 100);
//			buf.append(" more entries>");
//		}
		buf.append("))");
		return buf.toString();
	}


	/*+******************************************************************
	 * Storable implementation
	 ********************************************************************/

	public void store(DataOutput out) throws IOException {
		if (STORE_ENCODED) {
			storeEncoded(out);
		} else {
			storeRaw(out);
		}
	}

	public void storeRaw(DataOutput out) throws IOException {
		// Write header length
		out.writeInt(mylength);
		// Write counter elements
		for (int i = 0; i < mylength; i++) {
			counts[i].store(out);
		}
	}

	/**
	 * Stores the counters in the PID-encoded way returning the number
	 * of bits that were required (making it possible for the caller
	 * to fill the last byte completely to byte allign multiple records
	 * of data.)
	 */
	public long storeEncoded(DataOutput out) throws IOException {
		PathId pid;
		long len = 0;
		int nodeNo, lastNodeNo = -1;
		int nodeNoBits = guide.getNodeNoBits() + 1;
		long numBits;
		int totalNumLen = 0;
		int count;

		out.writeInt(mylength);

		bitOut.reattach(out);

		// Write header length in base 128
//		b128.writeInt(mylength, bitOut);
//		bitOut.flush();
		// Here, the flush is ok, because we know that base128 numbers are
		// byte-aligned.
//		len += b128.getCodeLen();
		len += 32;

		// Write counter elements
		for (int i = 0; i < mylength; i++) {
			pid = counts[i].getPid();

			// Write PID's node#
			nodeNo = pid.getNodeNo();
			if (nodeNo != lastNodeNo) {
				// Need to write the full number (with one extension bit!)
				bitOut.write(nodeNo, nodeNoBits);
				lastNodeNo = nodeNo;
				len += nodeNoBits;
			} else {
				bitOut.write(1, 1); // Just a single flag bit
				len++;
			}

			// Write PID's number bits
			numBits = pid.getPosBits();
//			try {
				totalNumLen = guide.getNode(nodeNo).getTotalPosBitLen();
//			} catch (ArrayIndexOutOfBoundsException e) {
//				System.out.println("Node# = " + nodeNo);
//				e.printStackTrace();
//			}
			bitOut.write(numBits, totalNumLen);
			len += totalNumLen;

			// Write counter
//			codeLen = GammaCoder.codeLength(counts[i].getCount());
//			out.write(dummy, codeLen);
			count = counts[i].getCount();
			int debugLen = GammaCoder.encode(count, bitOut);
			len += debugLen;

			// FOR DEBUGGING:
			if (nodeNo == 24) {
				System.out.print(""//"term#=" + (i + 1)
				   + "\nnode#=24, " + "\tnumBits=" + numBits
				   + "\tnumLen=" + totalNumLen + "\tcount=" + count + "\tcountLen="
				   + debugLen + "    ");
			}
		}
		// Who is supposed to do this where? (-> We, here!)
		bitOut.flush();
		return len;
	}

	public void load(DataInput in) throws IOException {
		if (LOAD_ENCODED) {
			loadEncoded(in);
		} else {
			loadRaw(in);
		}
	}

	public void loadRaw(DataInput in) throws IOException {
//		mylength = 0;  !!!! May not be defined again!?!?!!!!
		int i = 0;

		try {
//			System.out.print("..loading inv. list..");
			mylength = in.readInt();
//			System.out.print(" ..list len. is " + mylength + "..");
			if (mylength > counts.length) {
				adjustCapacity(mylength);
			}
			for (i = 0; i < mylength; i++) {
				counts[i].getPid().load(in);
				counts[i].setCount(in.readByte());
			}
		} catch (EOFException e) {
			if (mylength != 0) {
				// Seems there was no data left but no inconsistencies
				throw new IOException("Loading of partial inverted list; "
					+ "length is " + mylength + ", but "
					+ "only " + (i - 1) + " counters available");
			}
			// else: everything is fine
		} catch (IOException e) {
			// Something wrong with the data!
			throw new IOException("IO exception while trying to load inverted "
				+ "list. ... (" + e + ")");
		}
	}

	public void loadEncoded(DataInput in) throws IOException {
//		mylength = 0;   -- see loadRaw() !!!
		int i = -1;
		long len = 0;
		int bit;
		int nodeNo, lastNodeNo = -1;
		int nodeNoBits = guide.getNodeNoBits();
		int numBits, totalNumLen = -1;
		int count;
		PathId pid;

		try {
			bitIn.reattach(in);
			System.out.print("..loading inv. list..");
//			mylength = b128.readInt(bitIn);
			mylength = in.readInt();
			System.out.print(" ..list len. is " + mylength + "..");
			if (mylength > counts.length) {
				adjustCapacity(mylength);
			}
			for (i = 0; i < mylength; i++) {
				// Determine node#
				bit = bitIn.read();
				if (bit > 0) {
					// Same node# as before
					nodeNo = lastNodeNo;
					System.out.print("..same node " + nodeNo + " as before..");
				} else {
					// Different node# than before
					nodeNo = bitIn.read(nodeNoBits);
					System.out.print("..new node " + nodeNo + " (before: "
									  + lastNodeNo + ")..");

					/*
					 * The following test does only make sense when the
					 * temporary data is stored sorted, too!
					 *
					if (nodeNo <= lastNodeNo) {
						String str = "New node# " + nodeNo + " is smaller than "
							  + "previous node# " + lastNodeNo + " which cannot "
							  + "be in a correct encoding (error in " + i
							  + "th element--first index is 0)";
						throw new IllegalStateException(str);
//						System.out.println(str);
					}
					 */

					lastNodeNo = nodeNo;
//					try {
						totalNumLen = guide.getNode(nodeNo).getTotalPosBitLen();
//						System.out.print("..totalNumLen = " + totalNumLen + "..");
//					} catch (IllegalArgumentException e) {
//						e.printStackTrace();
//						throw e;
//					}
					if (totalNumLen > 32) {
						throw new IllegalStateException("PID number may not be "
							+ "longer than 32 Bits as of yet but is "
							+ totalNumLen);
					}
				}

				// Fetch number bits
				numBits = bitIn.read(totalNumLen);
				System.out.print("..numBits = " + numBits + " (len = "
								  + totalNumLen + ")..");
				// Get counter
				count = GammaCoder.decode(bitIn);
				System.out.print("..count = " + count + "..");

				// Now we have everything we need so, just assign and return it
				pid = counts[i].getPid();
				pid.setNodeNo(nodeNo);
				pid.setPosBits(numBits);
//				pid.setNumLen(totalNumLen); -- HOPEFULLY NOT NECCESSARY ANYMORE
				counts[i].setCount(count);
				// That's it. It only remains to keep track of the total list
				// length in bits (len) and do something useful with it.
				System.out.println("  [" + counts[i] + "]   ");
			}

		} catch (EOFException e) {
			if (mylength != 0) {
				// Seems there was no data left but no inconsistencies
				throw e;
//				throw new IOException("Loading of partial inverted list; "
//					+ "length is " + mylength + ", but "
//					+ "only " + (i - 1) + " counters available (" + e + ")");
			}
			// else: everything is fine
		} catch (IOException e) {
			// Something wrong with the data!
			throw new IOException("IO exception while trying to load inverted "
				+ "list. ... (" + e + ")");
		}
	}

	public int byteSize() {
		return 4 + Counter.BYTE_SIZE * mylength;
		// 4 bytes for length, BS bytes per element
	}



	/************************************************************************
	 * InvListIterator
	 ************************************************************************/
	static private class InvListIterator implements Iterator {
		InvertedDfList list;
		int currentPos = 0;

		private InvListIterator(InvertedDfList list) {
			this.list = list;
		}

		public boolean hasNext() {
			return (currentPos < list.length());
		}

		public Object next() {
			if (!hasNext()) {
				return null;
			} else {
				return list.counts[currentPos++];
			}
		}

		public void remove() {
			throw new UnsupportedOperationException("Inverted list iterators are "
				+ "strictly read-only, cannot remove element");
		}
	} // InvListIterator


	/************************************************************************
	 * TEST
	 ************************************************************************/

//	static public void encodedStorage() throws IOException {
//		BufferedOutputChannel boc =
//				BufferedOutputChannel.create("F:/Java/invlist.test");
//		DataGuide guide = DataGuide.load("F:)
//		InvertedDfList li = new InvertedDfList(
//	}

	static public void inMemTest() throws IOException {
		final int[][] A = {{1,1}, {2,1}, {3,1}, {8,1}, {9,1}, {10,1}};
		final int[][] B = {{2,2}, {3,2}, {4,2}, {7,2}, {8,2}, {11,2}, {12,2}};
		InvertedDfList a, b;
		Counter count = new Counter();

		a = new InvertedDfList(1000000, null);
		initList(a, A);
		b = new InvertedDfList(null);
		initList(b, B);
		System.out.println("\nList a is " + a);
		System.out.println("\nList b is " + b);
		a.append(b);
		System.out.println("\na.append(b) is " + a);
//		a.clear();
//		initList(a, A);
//		a.merge(b);
//		System.out.println("a.merge(b) is " + a);
		System.in.read();
	}

	static private void initList(InvertedDfList li, int[][] a) {
		PathId pid = new PathId();
		pid.setPosBits(0x3A);
//		pid.setNumLen(8); -- HOPEFULLY NOT NECCESSARY ANYMORE
		for (int i = 0; i < a.length; i++) {
//			System.out.println("  " + a[i][0] + "/" + a[i][1]);
			pid.setNodeNo(a[i][0]);
			li.counts[i] = new Counter((PathId) pid.clone(), (byte) a[i][1]);
		}
		li.mylength = a.length;
	}


	static private InvertedDfList createRandom(DataGuide guide, int size) {
		InvertedDfList li = new InvertedDfList((int) (size * 1.1), guide);
		Counter c = new Counter();
		PathId pid = new PathId();

		for (int i = 0; i < size; i++) {
			guide.getRandomNode().getRandomPid(pid);
			c.setPid(pid);
			c.setCount(MyMath.random(1, 127));
			li.add((Counter) c.clone());
		}
		return li;
	}

	static private void loadStoreTest() throws IOException {
		MyMath.setSeed(13);
		DataGuide guide = DataGuide.load("F:/Data/XMLstandard/guide.tree");
		InvertedDfList li = createRandom(guide, 6);
		BufferedOutputChannel out = BufferedOutputChannel.create(
				"F:/Data/XMLstandard/invlist.test");
		pl("Storing list: " + li);
		li.store(out);
		out.close();
		InvertedDfList li2 = new InvertedDfList(3, guide);
		BufferedInputChannel in = BufferedInputChannel.create(
				"F:/Data/XMLstandard/invlist.test");
		li2.load(in);
		pl("Loaded list:  " + li2);
		in.close();
	}


	static private void dumpIndex() throws IOException {
//		final String REP_DIR = Config.REP_HOME;
		IndexSeqChannel ch = new IndexSeqChannel(REP_HOME + "/termdf");
		int termCount = ch.getRecordCount();
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		InvertedDfList li = new InvertedDfList(guide);


		// ... needs update
		for (int i = 1; i <= termCount; i++) {
			ch.get(i, li);
//			term = terms.get(i);
//			pl(i + ")" + term.getName() + "\t" + term.getCount() + "\t" + li);
		}
		ch.close();
	}


	static private void dumpListFile(int no) throws IOException {
//		final String REP_DIR = Config.DATA_HOME + "/XMLmini";
		BufferedInputChannel ch = BufferedInputChannel.create(
				REP_HOME + "/termdf.data" + no);
		MutableInteger termNo = new MutableInteger(-1);
//		int termNo = 0;
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		InvertedDfList li = new InvertedDfList(guide);

		try {
			while (true) {
				termNo.load(ch);
				p("\n_________" + termNo + "_________\t"); System.out.flush();
				li.load(ch);
//				if (termNo.getValue() >= 142) {
//					pl("There.");
//				}
				pl(li.length() + "\t" + li);
			}
		} catch (EOFException e) {
			pl("End of input channel reached after term# " + termNo);
			pl("(" + e + ")");
		}
		ch.close();
	}


	static private void dumpIdxSeqFile() throws IOException {
		IndexSeqSource source = new IndexSeqChannel(REP_HOME + "/termdf");
		MutableInteger termNo = new MutableInteger(-1);
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		InvertedDfList li = new InvertedDfList(guide);
		int i = -1;
		Terms terms = Terms.load(REP_HOME + "/terms");
		Term term;

		try {
			for (i = 1; i <= source.getRecordCount(); i++) {
				//				termNo.load(ch);
				source.get(i, li);
				term = terms.get(i);
				pl("\n" + i + ") " /*+ (term.getCount() != li.length()? "***": "")*/
				   + term.getName() + "\t" + term.getCount() + "\t" + li);
			}
		} catch (EOFException e) {
			pl("End of input channel reached after term# " + i);
			pl("(" + e + ")");
		}
		source.close();
	}


	static private void statIdxSeqFile() throws IOException {
		IndexSeqSource source = new IndexSeqChannel(REP_HOME + "/termdf");
		MutableInteger termNo = new MutableInteger(-1);
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		InvertedDfList li = new InvertedDfList(guide);
		int i = -1;

		int[] countCount = new int[500];
		int totalCount = 0;
		int maxLiLen = -1;

		try {

			for (i = 1; i <= source.getRecordCount(); i++) {
				//				termNo.load(ch);
				source.get(i, li);

				// Extract statistics
				for (int j = 0; j < li.length(); j++) {
					countCount[li.get(j).getCount()]++;
				}
				maxLiLen = Math.max(maxLiLen, li.length());
				totalCount += li.length();
			}

			pl("Total # of counters...... " + totalCount);
			pl("Counters per size........ ");
			int k = 500;
			while (countCount[--k] == 0);
			while (k > 0) {
				System.out.println("\t.." + k + "\t" + countCount[k]);
				k--;
			}
			pl("Maximum list length...... " + maxLiLen);
			pl("Avg. list length......... " + (totalCount / source.getRecordCount()));


		} catch (EOFException e) {
			pl("End of input channel reached after term# " + i);
			pl("(" + e + ")");
		}
		source.close();
	}


	public static void main(String[] args) throws Exception {
//		IndexSeqFile index = new IndexSeqFile(DATA_HOME + "/XMLstandard/termdf");
//		int termCount = index.getRecordCount();
//		InvertedDfList li = new InvertedDfList(null);
		StopWatch watch = new StopWatch();

//		for (int i = 1000; i > 990; i--) {
//			index.get(i, li);
//			out.println(li + "\n\n");
//		}

		watch.start();

//		loadStoreTest();

//		dumpIndex();

//		dumpListFile(1);
//		dumpIdxSeqFile();
//		statIdxSeqFile();
		watch.stop();
		System.out.println("\nTime for operation: " + watch);
	}


	static public void p(String s) {
		System.out.print(s);
	}
	static public void pl(String s) {
		System.out.println(s);
	}
	static public void pl() {
		System.out.println();
	}
}