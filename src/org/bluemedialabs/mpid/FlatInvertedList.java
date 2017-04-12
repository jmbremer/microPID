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
import java.util.Iterator;
import java.util.LinkedList;
import org.bluemedialabs.io.Storable;
import org.bluemedialabs.util.Queue;


/**
 * <p>Do merge sort for long lists and something simpler for short lists!</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class FlatInvertedList implements Storable {
	static public int DEFAULT_INITIAL_CAPACITY = 1000;
	protected Counter[] counts;
//	private int recNo;
	private int mylength = 0;

	public FlatInvertedList(int initialCapacity) {
		counts = new Counter[initialCapacity];
		for (int i = 0; i < initialCapacity; i++) {
			counts[i] = new Counter();
		}
	}

	public FlatInvertedList() {
		this(DEFAULT_INITIAL_CAPACITY);
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

	public int capacity() {
		return counts.length;
	}

	public int length() {
		return mylength;
	}

	public void clear() {
//		recNo = 0;
		mylength = 0;
	}

	public boolean isEmpty() {
		return (length() == 0);
		// Length = 0 means, there was no more record, because only records
		// with at least some counter data are actually stored.
	}

	/**
	 * Merges the given list into this list, assuming the lists are disjunct
	 * for now! Thus, merging is equivalent to appending.
	 */
	public void append(FlatInvertedList list) {
		int combinedLen;

//		if (list.getRecNo() != recNo) {
//			throw new IllegalArgumentException("Inverted list may only be "
//				+ "merged with records with the same number, but own number is "
//				+ getRecNo() + ", supplied number is " + list.getRecNo());
//		}
		combinedLen = length() + list.length();
		// Make sure our list has enough capacity to hold the combined list
		if (capacity() < combinedLen) {
			adjustCapacity(combinedLen);
		}
		int j = 0;
		for (int i = length(); i < combinedLen; i++) {
			counts[i].no = list.counts[j].no;
			counts[i].value = list.counts[j++].value;
		}
		mylength = combinedLen;
	}

	/**
	 * ...assuming both lists are <em>sorted</em>!!!
	 */
	public void merge(FlatInvertedList list) {
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
		// that should fo it!?...
	}

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


	public String toString() {
		StringBuffer buf = new StringBuffer(mylength * 13);

		buf.append("(");
		if (mylength == 0) {
			buf.append("<empty>");
		} else {
			buf.append(counts[0]);
			for (int i = 1; i < mylength && i < 100; i++) {
				buf.append(" ");
				buf.append(counts[i]);
			}
		}
		if (mylength > 100) {
			buf.append(" <");
			buf.append(mylength - 100);
			buf.append(" more entries>");
		}
		buf.append(")");
		return buf.toString();
	}


	/*+******************************************************************
	 * Storable implementation
	 ********************************************************************/

	public void store(DataOutput out) throws IOException {
		// Write header (#, length)
//		out.writeInt(recNo);
		out.writeInt(mylength);
		// Write counter elements
		for (int i = 0; i < mylength; i++) {
			out.writeInt(counts[i].no);
			out.writeShort(counts[i].value);
		}
	}

	public void load(DataInput in) throws IOException {
		mylength = 0;
		int i = 0;
		try {
//			recNo = in.readInt();
			mylength = in.readInt();
			if (mylength > counts.length) {
				adjustCapacity(mylength);
			}
			for (i = 0; i < mylength; i++) {
				counts[i].no = in.readInt();
				counts[i].value = in.readShort();
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

	public int byteSize() {
		return 4 + 6 * mylength;
		// 4 bytes for length, 6 bytes per element
	}


	/************************************************************************
	 * A single number-counter pair within the list.
	 ************************************************************************/
	static protected class Counter {
		int no;
		short value;

		Counter(int no, int value) {
			this.no = no;
			this.value = (short) value;
		}

		Counter() {}

		public Object clone() {
			Counter c = new Counter();
			c.no = no;
			c.value = value;
			return c;
		}

		public int getNo() { return no; }
		public int getValue() { return (int) value; }

		public String toString() {
			StringBuffer buf = new StringBuffer(20);
//			buf.append("(");
			buf.append(no);
			buf.append("/");
			buf.append(value);
//			buf.append(")");
			return buf.toString();
		}
	}


	/************************************************************************
	 * InvListIterator
	 ************************************************************************/
	static private class InvListIterator implements Iterator {
		FlatInvertedList list;
		int currentPos = 0;

		private InvListIterator(FlatInvertedList list) {
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



	public static void main(String[] args) throws Exception {
		final int[][] A = {{1,1}, {2,1}, {3,1}, {8,1}, {9,1}, {10,1}};
		final int[][] B = {{2,2}, {3,2}, {4,2}, {7,2}, {8,2}, {11,2}, {12,2}};
		FlatInvertedList a, b;
		FlatInvertedList.Counter count = new Counter();

		a = new FlatInvertedList();
		initList(a, A);
		b = new FlatInvertedList();
		initList(b, B);
		System.out.println("List a is " + a);
		System.out.println("List b is " + b);
		a.append(b);
		System.out.println("a.append(b) is " + a);
		a.clear();
		initList(a, A);
		a.merge(b);
		System.out.println("a.merge(b) is " + a);
	}

	static private void initList(FlatInvertedList li, int[][] a) {
		for (int i = 0; i < a.length; i++) {
//			System.out.println("  " + a[i][0] + "/" + a[i][1]);
			li.counts[i] = new Counter(a[i][0], a[i][1]);
		}
		li.mylength = a.length;
	}

}