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
package org.bluemedialabs.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * <p>Efficiently maintains a list of lists. <em>Clearly, this is Java 1.1 code
 * that needs an update.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class ArrayLists {
	protected int[] start;    // start index of inverted list, -1 if invalid
	protected int[] end;      // current end of node#-related inverted list
	protected int listCount;
	protected Element[] elements;
	protected int usedElems;     // number of currently used elements
	protected CloneableObject sample;


	public ArrayLists(CloneableObject sample, int capacity, int listCount) {
		elements = new Element[capacity];
		this.listCount = listCount;
		for (int i = 0; i < capacity; i++) {
			elements[i] = new Element(sample.clone());
		}
		start = new int[listCount];
		end = new int[listCount];
		usedElems = capacity;  // ..to make reset, reset everything correctly!!
		reset();
	}

//	public void finalize() throws IOException {
//		this.store(out);
//		out.close();
//		reset();
//	}

	protected void reset() {
		for (int i = 0; i < listCount; i++) {
			start[i] = -1;
			end[i] = -1;
		}
		for (int i = 0; i < usedElems; i++) {
			elements[i].next = -1;
		}
		usedElems = 0;
	}


	public void add(int listNo, CloneableObject obj) throws IOException {
		// No more counter elements left?
		if (usedElems >= elements.length) {
			onFullArray();
		}
		Element elem = elements[usedElems];

		// Store new counter information into next available counter element
		elem.copyObject(obj);
//		elem.setNext(-1);  -- unneccessary!?! (s. reset)
		// Check for first element
		if (start[listNo] == -1) {
			// first counter for this term#
			start[listNo] = usedElems;
		} else {
			// Adjust related "list"
			// (Here we know that end has a valid element for nodeNo!)
			elements[end[listNo]].next = usedElems;
		}
		// In any case, the end is now here:
		end[listNo] = usedElems;
		usedElems++;
	}

	/**
	 * Overwrite this for a specific action when the underlying element
	 * array is full.
	 */
	protected void onFullArray() {
//		System.out.println("\nIvalNodeDfLists capacity of " + elements.length
//				  + " reached, storing elements to file " + out.getSeqNumber()
//				  + "...");
//		// Capacity reached, so write everything back
//		this.store(out);
//		// ..and prepare next DataOuput
//		out = out.next();
//		reset();
//		System.out.println("\nDone storing elements.");
	}

	public int getListCount() {
		return listCount;
	}

	public int getUsedElems() {
		return usedElems;
	}


	public Iterator iterator() {
		return new ArrayListsIterator(this);
	}

	public Iterator listIterator(int listNo) {
		assert (listNo >= 0 && listNo < listCount);
		return new ArrayListsIterator(this, listNo);
	}



/*+**************************************************************************
 * Element class
 ****************************************************************************/

	 /**
	  * Simple element of this list consisting of an IvalId and a next pointer.
	  */
	static protected class Element {
		protected Object obj;
		protected int next = 0;   // index of next counter, -1 if last element

		public Element(Object o) {
			obj = o;
		}

		public Element() {
			this(null);
		}

		// Copy!!! Not set reference!!!
		public void copyObject(CloneableObject o) { o.copy(obj); }
		public Object getObject() { return obj; }

		public void setNext(int n) { next = n; }
		public int getNext() { return next; }
	} // Element



/*+**************************************************************************
 * Iterator class
 ****************************************************************************/
	static private class ArrayListsIterator implements Iterator {
		private ArrayLists lists;
		private int currentNo = 0;
		private int currentPos;
		private boolean onlyThisNo = true;


		ArrayListsIterator(ArrayLists lists, int no) {
			this.lists = lists;
			currentNo = no;
			currentPos = lists.start[currentNo];
		}

		ArrayListsIterator(ArrayLists lists) {
			this(lists, 0);
			onlyThisNo = false;
		}


		public boolean hasNext() {
			boolean thisListHasMore = (currentPos >= 0);
			boolean moreElems = false;
			int no;

			if (thisListHasMore) {
				return true;
			} else if (onlyThisNo) {
				return false;
			} else {
				no = currentNo + 1;
				while (no < lists.listCount && !moreElems) {
					if (lists.start[no] >= 0) {
						moreElems = true;
					}
				}
				return moreElems;
			}
		}


		public Object next() {
			int pos;

			if (currentPos >= 0) {
				pos = currentPos;
				currentPos = lists.elements[currentPos].getNext();
			} else {
				pos = findNextPos();
				if (pos < 0) {
					throw new NoSuchElementException("There are no more "
							+ "elements in this array lists");
				}
				currentPos = lists.elements[pos].getNext();
			}
			return lists.elements[pos].getObject();
		}

		protected int findNextPos() {
			int no;

			if (onlyThisNo) {
				return -1;
			}
			no = currentNo + 1;
			while (no < lists.listCount && lists.start[no] == -1) {
				no++;
			}
			if (no < lists.listCount) {
				currentNo = no;
				return lists.start[no];
			} else {
				return -1;
			}
		}


		public void remove() {
			throw new UnsupportedOperationException("Removing of elements from "
				+ "array lists is not supported at the current time");
		}
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		int[][] test = {{1, 3}, {1, 5}, {1, 7}, {2, 2}, {2, 4}, {2, 6}, {2,8},
						{3, 13}, {3, 42}};
		MutableInteger m = new MutableInteger();
		ArrayLists lists = new ArrayLists(m, 10, 4);
		Iterator it;

		// Add test data
		for (int i = 0; i < test.length; i++) {
			m.setValue(test[i][1]);
			lists.add(test[i][0], m);
		}
		// Look at list 0
		printList(lists, 0);
		// Print list 2
		printList(lists, 2);
		// Print all lists
		printList(lists, -1);
	}

	static protected void printList(ArrayLists lists, int listNo) {
		Iterator it;

		if (listNo >= 0) {
			System.out.println("Contents of list " + listNo + ":");
			it = lists.listIterator(listNo);
		} else {
			System.out.println("Contents of all lists:");
			it = lists.iterator();
		}
		if (it.hasNext()) {
			while (it.hasNext()) {
				System.out.print("  " + it.next());
				System.out.flush();
			}
			System.out.println();
		} else {
			System.out.println("<no elements>");
		}
	}
}
