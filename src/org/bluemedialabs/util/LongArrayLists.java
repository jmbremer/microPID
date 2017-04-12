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

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * <p>See ArrayLists.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class LongArrayLists {
	protected int[] start;    // start index of inverted list, -1 if invalid
	protected int[] end;      // current end of node#-related inverted list
	protected int listCount;
	protected long[] elem;
	protected int[] next;
	protected int usedElems;     // number of currently used elements


	public LongArrayLists(int capacity, int listCount) {
		elem = new long[capacity];
		next = new int[capacity];
		this.listCount = listCount;
		for (int i = 0; i < capacity; i++) {
			elem[i] = -1;
		}
		start = new int[listCount];
		end = new int[listCount];
		clear();
	}


	public void clear() {
		for (int i = 0; i < listCount; i++) {
			start[i] = -1;
			end[i] = -1;
		}
		for (int i = 0; i < elem.length; i++) {
//			elem[i] = -1;
			next[i] = -1;
		}
		usedElems = 0;
	}


	public void add(int listNo, long value) {
		// No more counter elements left?
		if (usedElems >= elem.length) {
			onFullArray();
		}
		elem[usedElems] = value;

		// Check for first element
		if (start[listNo] == -1) {
			// First element for this list#
			start[listNo] = usedElems;
			// Note: we dont' have to set next here yet!
		} else {
			// Adjust related "list"
			// (Here we know that end has a valid element for nodeNo!)
			next[end[listNo]] = usedElems;
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
		throw new IllegalStateException("The element array is full, but this "
										+ "case isn't handled properly");
	}

	public boolean isFull() { return (usedElems >= elem.length); }

	public boolean isEmpty(int listNo) { return (start[listNo] < 0); }

	public int getListCount() {	return listCount; }

	public int getElemCount(int listNo) {
		int count = 0;
		int pos = start[listNo];

		while (pos >= 0) {
			count++;
			pos = next[pos];
		}
		return count;
	}

	public int getCapacity() { return elem.length; }

	public int getUsedElems() {	return usedElems; }


	public Iterator listIterator() {
		return listIterator(-1);
	}

	public Iterator listIterator(int listNo) {
		assert (listNo >= -1 && listNo < listCount);
		return new LongArrayListsIterator(this, listNo);
	}



	/*+**********************************************************************
	 * Iterator class
	 ************************************************************************/

	static class LongArrayListsIterator implements Iterator {
		private LongArrayLists lists;
		private int currentNo = 0;
		private int currentPos;
		private boolean onlyThisNo = true;
		private MutableLong l = new MutableLong();


		LongArrayListsIterator(LongArrayLists lists, int no) {
			this.lists = lists;
			if (no == -1) {
				currentNo = 0;
				onlyThisNo = false;
			} else {
				currentNo = no;
				onlyThisNo = true;
			}
			currentPos = lists.start[currentNo];
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
				while (no < lists.getListCount() && !moreElems) {
					if (lists.start[no] >= 0) {
						moreElems = true;
					}
				}
				return moreElems;
			}
		}


		public Object next() {
			int nextPos;


			if (currentPos < 0) {
				currentPos = findNextPos();
				if (currentPos < 0) {
					throw new NoSuchElementException("There are no more "
							+ "elements in this array lists");
				}
			}
			l.setValue(lists.elem[currentPos]);
			currentPos = lists.next[currentPos];
			return l;
		}

		int findNextPos() {
			int no;

			if (onlyThisNo) {
				return -1;
			}
			no = currentNo + 1;
			while (no < lists.getListCount() && lists.start[no] == -1) {
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
//		int[][] test = {{1, 3}, {1, 5}, {1, 7}, {2, 2}, {2, 4}, {2, 6}, {2,8},
//						{3, 13}, {3, 42}};
//		MutableInteger m = new MutableInteger();
//		LongArrayLists lists = new LongArrayLists(10, 4);
//		Iterator it;
//
//		// Add test data
//		for (int i = 0; i < test.length; i++) {
//			m.setValue(test[i][1]);
//			lists.add(test[i][0], m);
//		}
//		// Look at list 0
//		printList(lists, 0);
//		// Print list 2
//		printList(lists, 2);
//		// Print all lists
//		printList(lists, -1);
//	}
//
//	static protected void printList(LongArrayLists lists, int listNo) {
//		Iterator it;
//
//		if (listNo >= 0) {
//			System.out.println("Contents of list " + listNo + ":");
//			it = lists.listIterator(listNo);
//		} else {
//			System.out.println("Contents of all lists:");
//			it = lists.iterator();
//		}
//		if (it.hasNext()) {
//			while (it.hasNext()) {
//				System.out.print("  " + it.next());
//				System.out.flush();
//			}
//			System.out.println();
//		} else {
//			System.out.println("<no elements>");
//		}
	}
}