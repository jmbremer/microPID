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
import org.bluemedialabs.util.CloneableObject;
import org.bluemedialabs.util.*;


/**
 * <p>Do merge sort for long lists and something simpler for short lists!</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class InvertedList implements CloneableObject, Storable, BitStorable {
	// Not final because subclasses may use different setting...: (!???!?! :-( )
	static public boolean STORE_ENCODED = true;
	static public boolean LOAD_ENCODED  = true;
	// Final things:
	static public final int DEFAULT_INITIAL_CAPACITY = 1000;
	static public final double INCREASE_FACTOR       = 0.5;

	/**
	 * The maximum number of elements that should be included when this list
	 * is converted to a string. This parameter allows for a limit of a print
	 * out length in order to maintain readability. A value of -1 indicates
	 * that all elements should be printed regardless of the list length.
	 */
	static protected final int MAX_PRINT_ELEMENTS       = 100;
	static private final PrintStream out = System.out;

	// Regular properties:
	protected CloneableObject[] elements;
	private CloneableObject sample;
	private int mylength = 0;
	private int no = -1;   // # of the object this list belongs to
	private boolean sort;  // Sort the list before storage?
	// For encoded storage:
	protected BitDataOutput bitOut = new BitDataOutput(null);
	protected BitDataInput bitIn = new BitDataInput(null);
	protected Base128 b128 = new Base128();


	/**
	 * <p>Constructs a new list of elements of the supplied sample type. The
	 * sample not only has to be Cloneable but also Storable!</p>
	 *
	 * @param sample
	 * @param initialCapacity
	 */
	public InvertedList(CloneableObject sample, int initialCapacity) {
		if (!(sample instanceof Storable)) {
			throw new IllegalArgumentException("Cannot create inverted list of "
					+ "objects that are not Storable, but sample object is not");
		}
		this.sample = sample;
//		if ((STORE_ENCODED || LOAD_ENCODED) && guide == null) {
//			throw new NullPointerException("Need a valid document guide to "
//					+ "support compressed storage but guide is null");
//		}
//		this.guide = guide;
		elements = new CloneableObject[initialCapacity];
		for (int i = 0; i < initialCapacity; i++) {
			elements[i] = (CloneableObject) sample.clone();
		}
	}

	public InvertedList(CloneableObject sample) {
		this(sample, DEFAULT_INITIAL_CAPACITY);
	}


	public Object clone() {
		return new InvertedList(sample, capacity());
	}

	public void copy(Object obj) {
		throw new UnsupportedOperationException("InvertedList.copy() is not "
				+ "implemented yet");
	}

	/**
	 * Sets the number of this inverted list's related record. This might become
	 * necessary in case list should be merged into an initially empty list.
	 * Finding a non-matching record number, the merging will fail throwing
	 * an exception otherwise.
	 */
	protected void setNo(int no) {
		this.no = no;
	}

	public int getNo() {
		return no;
	}

	/**
	 * Adjusts the capacity to a new value that must be at least the current
	 * length, i.e., the number of used objects. Increasing the capacity also
	 * increases the number of pre-allocated objects.
	 *
	 * @param length The new capacity.
	 * @throws IllegalArgumentException If the new capacity is smaller than the
	 *    current length of the list.
	 */
	public void adjustCapacity(int length) {
		CloneableObject[] objs;
		if (length < mylength) {
			throw new IllegalArgumentException("Cannot decrease inverted list "
				+ "capacity from " + mylength + " to " + length + " while "
				+ "data is in use");
		}
		objs = new CloneableObject[length];
		// The copying is necessary in order to preserve the counter space!
		System.arraycopy(elements, 0, objs, 0, elements.length);
		// Pre-allocate the elements for the added elements
		for (int i = elements.length; i < length; i++) {
			objs[i] = (CloneableObject) sample.clone();
		}
		elements = objs;
	}

	/**
	 * Returns the current capacity of the list that is the number of elements
	 * in the internal array. Each element refers to a pre-allocated object!
	 *
	 * @return The current list capacity.
	 */
	public int capacity() {	return elements.length; }

	/**
	 * This is a somewhat dangerous operation as it increases the internal
	 * list length without appropriately setting the referenced objects. It is
	 * assumed that the objects have been or will be adjusted by direct
	 * reference to the internal array.
	 *
	 * @param len The new length (not capacity!).
	 */
	public void setLength(int len) {
		if (len > capacity()) {
			throw new IllegalArgumentException("Cannot adjust inverted list "
					+ "length beyond its capacity (" + len + " > "
					+ capacity() + ")");
		}
		mylength = len;
	}

	/**
	 * Returns the current length of the list, i.e., the number of actually
	 * used elements.
	 *
	 * @return The current list length.
	 */
	public int length() { return mylength; }

	/**
	 * Effectively reduces the list length to zero thereby making it empty.
	 */
	public void clear() { mylength = 0; }

	/**
	 * Well...
	 *
	 * @return true or false...
	 */
	public boolean isEmpty() {
		return (length() == 0);
		// Length = 0 means, there was no more record, because only records
		// with at least some counter data are actually stored.
	}


	/**
	 * Adds the supplied object to this list.
	 *
	 * @param obj
	 */
	public void add(CloneableObject obj) {
		if (length() >= capacity()) {
			// Need to create more room
			adjustCapacity((int) (capacity() * (1 + INCREASE_FACTOR)));
		}
		obj.copy(elements[mylength++]);
	}

	/**
	 * Like setLength() this method is for direct manipulation of the list-
	 * underlying array. Here the list length is just increased by one
	 * indicating that a new (already pre-allocated!) object has been
	 * initialized.
	 */
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
	public Object get(int index) {
		return elements[index];
	}

	/**
	 * Merges the given list into this list, assuming the lists are disjunct
	 * for now! Thus, merging is equivalent to appending.
	 */
	public void append(InvertedList list) {
		int combinedLen;

		combinedLen = length() + list.length();
		// Make sure our list has enough capacity to hold the combined list
		if (capacity() < combinedLen) {
			adjustCapacity(combinedLen);
		}
		int j = 0;  // position in source array
		int i;      // position in target array
		for (i = length(); i < combinedLen; i++) {
			list.elements[j].copy(elements[i]);
			j++;
		}
		mylength = combinedLen;
	}

	/**
	 * ...assuming both lists are <em>sorted</em>!!!
	 * NEEDS TO BE CHECKED! Quality assurance team.......!?!?!!!!
	 */
/*
	public void merge(InvertedList list) {
		int len = length();          // Length of this list
		int listLen = list.length(); // Length of supplied list
		int newLen = len + listLen;
		int pos;
		int listPos = 0;

//		System.out.println("Merging this list of length " + length()
//			+ " with given list of length " + list.length());
		// Make sure there's enough space left
		if (capacity() < newLen) {
//			System.out.println("Need to increase capacity from " + capacity()
//				+ " to " + newLen);
			adjustCapacity(newLen); // this is the maximum theoretical length
		}
		// copy current list to end
		pos = newLen - len;
		System.arraycopy(elements, 0, elements, pos, len);
//		System.out.println("Copied old contents to position " + pos );
		int i = -1;
		while (++i < newLen && pos < len + listLen && listPos < listLen) {
			if (elements[pos].no < list.elements[listPos].no) {
				elements[i] = elements[pos++];
			} else if (elements[pos].no > list.elements[listPos].no) {
				elements[i] = list.elements[listPos++];
			} else {
				// actually merge some values
				elements[i] = elements[pos++];
				elements[i].value += list.elements[listPos++].value;
				newLen--;   // ..because that was just the theoretical maximum
			}
		}
		// copy rest of whichever list was longer
		if (i < newLen) {
			// still elements left
			if (pos < newLen) {
				System.arraycopy(elements, pos, elements, i, len + listLen - pos);
			} else {
				System.arraycopy(list.elements, listPos, elements, i, listLen - listPos);
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
//			if (elements[pos].no < list.elements[listPos].no) {
//				pos++;
//			} else if (elements[pos].no = list.elements[listPos].no) {
//				elements[pos].value += list.elements[listPos].value;
//				pos++;
//				listPos++;
//			} else {
//				// actually merge some values
//				elements[i] = elements[pos++];
//				elements[i].value += list.elements[listPos++].value;
//				newLen--;   // ..because that was just the theoretical maximum
//			}
//
//	}

	public Iterator iterator() {
		return new InvListIterator(this);
	}


	/**
	 * Sorts the list, iff its elements are Comparable!
	 *
	 * @throws ClassCastException If elements do not implement Comparable.
	 */
	public void sort() {
		if (mylength > 1) {
			if (!(sample instanceof Comparable)) {
				throw new ClassCastException("Cannot sort lists as elements "
						+ "are not Comparable");
			}
			if (mylength <= 100) {
				quickSort();
			} else {
				Arrays.sort(elements, 0, mylength);
			}
		}
	}

	/**
	 * Not a quicksort but a quick sort for small lists.
	 */
	private void quickSort() {
		CloneableObject obj;
		for (int i = 0; i < mylength - 1; i++) {
			for (int j = i + 1; j < mylength; j++) {
				if (((Comparable) elements[i]).compareTo(elements[j]) > 0) {
					// Swap elements
					obj = elements[i];
					elements[i] = elements[j];
					elements[j] = obj;
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
			if (elements[f].compareTo(elements[t]) > 0) {
				c = elements[f];
				elements[f] = elements[t];
				elements[t] = c;
				t++;
			} else {
				f++;
			}
		}
	}
	*/


	/**
	 * Should be overwritten for subclassses whose to string conversion
	 * requires the data guide for producing a meaningful string representation.
	 *
	 * @return A string representation of this list.
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer(Math.max(1, mylength) * 10);
		int limit = (MAX_PRINT_ELEMENTS >= 0?
					 Math.min(MAX_PRINT_ELEMENTS, mylength): mylength);

		buf.append("(");
		if (mylength == 0) {
			buf.append("<empty>");
		} else {
			buf.append("length=");
			buf.append(mylength);
			buf.append(", list=(");
			buf.append("" + elements[0]);
			for (int i = 1; i < limit; i++) {
				buf.append(" ");
				buf.append("" + elements[i]);
			}
		}
		if (MAX_PRINT_ELEMENTS < mylength && MAX_PRINT_ELEMENTS > 0) {
			buf.append(" <");
			buf.append(mylength - MAX_PRINT_ELEMENTS);
			buf.append(" more entries>");
		} else if (MAX_PRINT_ELEMENTS == 0) {
			buf.append("<");
			buf.append(mylength);
			buf.append(" entries>");
		}
		buf.append("))");
		return buf.toString();
	}


	/*+******************************************************************
	 * Storable implementation
	 ********************************************************************/

//	public void store(DataOutput out) throws IOException {
//		if (STORE_ENCODED) {
//			storeEncoded(out);
//		} else {
//			storeRaw(out);
//		}
//	}

	public void store(DataOutput out) throws IOException {
		// Write header length
		out.writeInt(mylength);
		// Write counter elements
		for (int i = 0; i < mylength; i++) {
			((Storable) elements[i]).store(out);
		}
	}

	/**
	 * Stores the counters in the PID-encoded way returning the number
	 * of bits that were required (making it possible for the caller
	 * to fill the last byte completely to byte allign multiple records
	 * of data.)
	 */
	public void store(BitOutput out) throws IOException {
		throw new IllegalStateException("Base InvertedList class does not "
		   + "support encoded (bit-aligned) storage!");
	}

//	public void load(DataInput in) throws IOException {
//		if (LOAD_ENCODED) {
//			loadEncoded(in);
//		} else {
//			loadRaw(in);
//		}
//	}

	public void load(DataInput in) throws IOException {
		int i = 0;

		try {
//			System.out.print("..loading inv. list..");
			mylength = -1;  // To allow check whether loading succeeded
			mylength = in.readInt();
//			System.out.print(" ..list len. is " + mylength + "..");
			if (mylength > elements.length) {
				adjustCapacity(mylength);
			}
			for (i = 0; i < mylength; i++) {
				((Storable) elements[i]).load(in);
			}
		} catch (EOFException e) {
			if (mylength != 0) {
				// Seems there was no data left but no inconsistencies
				throw new IOException("Loading of partial inverted list; "
					+ "length is " + mylength + ", but "
					+ "only " + (i - 1) + " elements available");
			}
			// else: everything is fine
		} catch (IOException e) {
			// Something wrong with the data!
			throw new IOException("IO exception while trying to load inverted "
				+ "list processing element " + (i + 1) + "of " + mylength
				+ " elements (" + e + ")");
		}
	}

	/**
	 * Overwrite in subclass to enable encoded loading!
	 *
	 * @param in
	 * @throws IOException
	 */
	public void load(BitInput in) throws IOException {
		throw new IllegalStateException("Base InvertedList class does not "
		   + "support encoded (bit-aligned) loading!");
	}


	public int byteSize() {
		return 4 + ((Storable) sample).byteSize() * mylength;
		// 4 bytes for length, single object's bytes per element
	}

	public long bitSize() {
		return -1;
	}



	/************************************************************************
	 * InvListIterator
	 ************************************************************************/
	static private class InvListIterator implements Iterator {
		InvertedList list;
		int currentPos = 0;

		private InvListIterator(InvertedList list) {
			this.list = list;
		}

		public boolean hasNext() {
			return (currentPos < list.length());
		}

		public Object next() {
			if (!hasNext()) {
				return null;
			} else {
				return list.elements[currentPos++];
			}
		}

		public void remove() {
			throw new UnsupportedOperationException("Inverted list iterators are "
				+ "strictly read-only, cannot remove element");
		}
	} // InvListIterator



	/************************************************************************
	 * TEST
	 *
	 * Some general test cases for all kinds of inverted lists.
	 ************************************************************************/

	static public void inMemTest(CloneableObject sample) throws IOException {
		InvertedList a, b;

		a = new InvertedList(sample, 100000);
		initList(a, 100);
		b = new InvertedList(sample);
		initList(b, 20);
//		System.out.println("\nList a is " + a);
//		System.out.println("\nList b is " + b);
		a.append(b);
		System.out.println("\na.append(b) is " + a);
//		a.clear();
//		initList(a, A);
//		a.merge(b);
//		System.out.println("a.merge(b) is " + a);
//		System.in.read();
	}

	static private void initListWithCheck(InvertedList li, int elemCount) {
		if (li.capacity() < elemCount) {
			li.adjustCapacity(elemCount);
		}
		initList(li, elemCount);
		li.setLength(elemCount);
	}

	static protected void initList(InvertedList li, int elemCount) {
		for (int i = 0; i < elemCount; i++) {
			li.elements[i] = new MutableInteger(10 - (i % 10));
		}
	}


	static public void loadStoreTest(IdxrConfig config, String cfgName)
			throws IOException {
		final String fileName = config.getIndexHome(cfgName) + "/invlist.test";
		MutableInteger m = new MutableInteger(-1);
		InvertedList li = new InvertedList(m);
		initList(li, 100);
		BufferedOutputChannel out = BufferedOutputChannel.create(fileName);
		pl("Storing list...\n" + li);
		pl("...to file '" + fileName + "'...");
		li.store(out);
		out.close();
		InvertedList li2 = new InvertedList(m, 10);
		BufferedInputChannel in = BufferedInputChannel.create(fileName);
		li2.load(in);
		in.close();
		pl("\nLoaded list:  " + li2);
	}


	public static void main(String[] args) throws Exception {
		StopWatch watch = new StopWatch();

		watch.start();

//		MutableInteger i = new MutableInteger(42);
//		pl(i.toString());
		inMemTest(new MutableInteger(42));
		IdxrConfig config = (IdxrConfig) IdxrConfig.load(args[1]);
		loadStoreTest(config, args[0]);

		watch.stop();
		System.out.println("\nTime for operation(s): " + watch);
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