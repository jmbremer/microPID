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
//import java.util.Iterator;
//import java.util.LinkedList;
//import idxr.*;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p>Do merge sort for long lists and something simpler for short lists!</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class NodeTermList extends InvertedList {
//	static public boolean STORE_ENCODED  = true;
//	static public boolean LOAD_ENCODED  = true;


	public NodeTermList(int initialCapacity) {
		super(new TermCounter(), initialCapacity);
	}

	public NodeTermList() {
		this(DEFAULT_INITIAL_CAPACITY);
	}


	public Object clone() {
		return new NodeTermList(capacity());
	}


	public void mingle(NodeTermList list) {
		append(list);
		mingle();
		// All this is not the most efficient way of mingling the two
		// lists but will do for now
	}

	private void mingle() {
		int end = 0, lenAdj = 0;
		TermCounter current, next;

		// First sort the list (by term#)
		Arrays.sort(elements);
		// Then, mingle elements of the same term# adjusting the counter accord.
		current = (TermCounter) elements[0];
		for (int i = 1; i < length(); i++) {
			next = (TermCounter) elements[i];
			if (next.getTermNo() == current.getTermNo()) {
				// If same term#, just adjust counter
				current.incCount();
				lenAdj++;
			} else {
				// If different term# make next element current element
				// and add up counters for next element
				end++;
				current = (TermCounter) elements[end];
				if (end != i) {
					next.copy(current);
				}
			}
		}
		setLength(length() - lenAdj);  // Adjust length for mingled elements
	}


	/*+******************************************************************
	 * Storable implementation
	 ********************************************************************/

	 /**
	  *
	  * @param out
	  * @throws IOException
	  */
	public void store(DataOutput out) throws IOException {
		storeEncoded(out);
	}

	/**
	 *
	 * @param out
	 * @return
	 * @throws IOException
	 */
	public long storeEncoded(DataOutput out) throws IOException {
		int end = 0, lenAdj = 0;
		TermCounter current, next;

		// First sort and mingle the list
		mingle();
		// For the rest, InvertedList knows how to do the storage part
		super.store(out);
		return -1;
		// Bit length could be computed here by asking each element how many
		// bytes it hase taken, but we are too lazy right now to implement it;
		// also, it's unneccessary slow
	}


	/**
	 *
	 * @param in
	 * @throws IOException
	 */
	public void load(DataInput in) throws IOException {
		loadEncoded(in);
	}

	/**
	 *
	 * @param in
	 * @throws IOException
	 */
	public void loadEncoded(DataInput in) throws IOException {
		//Loading is easier then storing as no mingling is required
		int len = in.readInt();
		if (len > capacity()) {
			adjustCapacity(len);
		}
		for (int i = 0; i < len; i++) {
			((TermCounter) elements[i]).load(in);
		}
		setLength(len);
	}

	/**
	 *
	 * @return
	 */
	public int byteSize() {
		return -1;  // Don't know as TermCounter's are variable size
	}


}