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
//import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import org.bluemedialabs.util.MutableInteger;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Delimiters {
	static public final int INITIAL_CAPACITY = 100;

	private HashMap<Character, MutableInteger> delimMap;
	private char[] delims;
	private int[] delimCounts;
	private int uniqueCount = 0;
	private int totalCount = 0;


	public Delimiters() {
		delims = new char[INITIAL_CAPACITY + 1];
		delimCounts = new int[INITIAL_CAPACITY + 1];
		delimMap = new HashMap<Character, MutableInteger>(
		        (int) (INITIAL_CAPACITY / 0.7));
	}

	public void addAndCount(char c) {
		Character ch = new Character(c);
		MutableInteger pos = null;

		pos = (MutableInteger) delimMap.get(ch);
		if (pos == null) {
			// Have not yet seen this delimiter
			if (uniqueCount >= delims.length - 1) {
				// need to increase array sizes
				increaseCapacity();
			}
			pos = new MutableInteger(++uniqueCount);
			delimMap.put(ch, pos);
			delims[pos.getValue()] = c;
			delimCounts[pos.getValue()] = 0;
		}
		delimCounts[pos.getValue()]++;
		totalCount++;
	}

	private void increaseCapacity() {
		int len = (int) (delims.length * 1.5);
		char[] newDelims = new char[len];
		int[] newCounts = new int[len];

		// Copy old contents
		for (int i = 0; i < delims.length; i++) {
			newDelims[i] = delims[i];
			newCounts[i] = delimCounts[i];
		}
		delims = newDelims;
		delimCounts = newCounts;
	}


	public char getDelimiter(int dno) {
		return delims[dno];
	}
	public int getDelimCount(int dno) {
		return delimCounts[dno];
	}

	public int getUniqueCount() {
		return uniqueCount;
	}
	public int getTotalCount() {
		return totalCount;
	}

	protected void sort() {
		// As we can assume that there is only a very limited number of
		// delimiters, the following selection sort should be fine.
		int minCount, minPos;
		char ch;
//		Character ch;
		MutableInteger pos;
		for (int i = 1; i < uniqueCount; i++) {
			minCount = delimCounts[i];
			minPos = i;
			for (int j = i + 1; j <= uniqueCount; j++) {
				if (delimCounts[j] > minCount) {
					minCount = delimCounts[j];
					minPos = j;
				}
			}
			ch = delims[minPos];
			delimCounts[minPos] = delimCounts[i];
			delims[minPos] = delims[i];
			delimCounts[i] = minCount;
			delims[i] = ch;
			// Update hash table
			pos = (MutableInteger) delimMap.get(new Character(ch) /* ;-( */);
			pos.setValue(i);
		}
	}

	public Iterator<Delimiter> iterator() {
		return new DelimIterator(this);
	}

/*+**************************************************************************
 * Serializable & Storable implementation
 ****************************************************************************/

	public void store(DataOutput out) throws IOException {
		out.writeInt(uniqueCount);
		for (int i = 1; i <= uniqueCount; i++) {
			out.writeChar(delims[i]);
			out.writeInt(delimCounts[i]);
		}
		out.writeInt(totalCount);   // this is redundant (see below)
	}

	public void load(DataInput in) throws IOException {
		int total = 0;
		uniqueCount = in.readInt();
		// make sure the arrays are large enough
		if (delims.length <= uniqueCount) {
			delims = new char[uniqueCount + 1];
			delimCounts = new int[uniqueCount + 1];
		}
		for (int i = 1; i <= uniqueCount; i++) {
			delims[i] = in.readChar();
			delimCounts[i] = in.readChar();
			total += delimCounts[i];
		}
		totalCount = in.readInt();
		if (total != totalCount) {
			throw new IllegalStateException("Loading of delimiters successful. "
				+ "However, the stored total delimiter count " + totalCount
				+ " does not match the sum of loaded counters which is "
				+ total + ". The inconsistence must be somewhere before "
				+ "the delimiters");
		}
	}

	public int byteSize() {
		return -1;
	}


	private void writeObject(ObjectOutputStream out) throws IOException {
		store(out);
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		load(in);
	}



	/************************************************************************
	 * Iterator-related classes
	 ************************************************************************/

	static public class Delimiter {
		public char ch;
		public int count;
		public Delimiter(char c, int cnt) {
			ch = c;
			count = cnt;
		}
	}

	static private class DelimIterator implements Iterator<Delimiter> {
		Delimiters delims;
		int currentPos = 1;

		private DelimIterator(Delimiters delims) {
			this.delims = delims;
		}

		public boolean hasNext() {
			return (currentPos <= delims.uniqueCount);
		}

		public Delimiter next() {
			if (!hasNext()) {
				return null;
			} else {
				return new Delimiter(delims.delims[currentPos],
						delims.delimCounts[currentPos++]);
			}
		}

		public void remove() {
			throw new UnsupportedOperationException("Delimiter iterators are "
				+ "strictly read-only, cannot remove element");
		}
	} // DelimIterator

}