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
import java.util.NoSuchElementException;
import org.bluemedialabs.util.MutableInteger;
import org.bluemedialabs.util.MyMath;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TermNodeMap {
	static public final int DEFAULT_INITIAL_CAPACITY = 1000000;
	static public final double DEFAULT_PERCENT_INCREASE = 0.1;

	private int[] termStart;  // Index pos. at which mappings for term starts
	private int[] nodeNos;    // Node# to be found with term
	private int termCount;
	private int nodeCount;
	// # of elements currently used in nodeNos & recIndices. The 1 makes sure
	// we start at position 1 which relates to (file) record index 1.
	// (Record indices start with 1!)
	private int used = 1;
	private int currentTermNo = -1;


	/**
	 *
	 * @param termCount
	 * @param nodeCount
	 */
	public TermNodeMap(int termCount, int nodeCount) {
		termStart = new int[termCount + 1];
		for (int i = 0; i < termStart.length; i++) {
			termStart[i] = -1;
		}
		this.termCount = termCount;
		this.nodeCount = nodeCount;
		nodeNos = new int[DEFAULT_INITIAL_CAPACITY];
	}

	/**
	 * All arguments are supposed to be loaded and thus not supplied here.
	 */
	public TermNodeMap() {
		termCount = -1;
		nodeCount = -1;
		termStart = null;
		nodeNos = null;
		used = -1;
	}


	public int getTermCount() { return termCount; }
	public int getNodeCount() { return nodeCount; }

	/**
	 * Assume that adding of mappings happens in order, i.e., first all
	 * mappings for term 1, then for term 2, etc. are added. Furthermore,
	 * the node numbers per term are also expected to be ordered by size.
	 *
	 * @param termNo
	 * @param nodeNo
	 * @param recIndex
	 * @returns The assigned (assumed) record number for this (term#, node#).
	 */
	public int addMapping(int termNo, int nodeNo/*, int recIndex */) {
		int[] newArray;
		int len = 1;
		boolean newTerm = false;

		if (termNo > termCount) {
			throw new ArrayIndexOutOfBoundsException("Invalid term# "
					+ termNo + ", there are only " + termCount + " terms");
		} else if (nodeNo > nodeCount) {
			throw new ArrayIndexOutOfBoundsException("Invalid node# "
					+ nodeNo + ", there are only " + nodeCount + " nodes");
		}
		// Initialize current term#, if necessary
//		if (currentTermNo < 0) {
//			currentTermNo = termNo;
//		}
		// Make sure there is space to store the mapping left
		if (used >= nodeNos.length - 1) {
			increaseCapacity();
		}
		// Watch out for the start of a new term-related mapping sequence
		if (termNo > currentTermNo) {
			// This should be the start of a new sequence of term#'s related
			// to the current term#
			termStart[termNo] = used;  // Records start with 1!
			if (currentTermNo > 0) {
				newTerm = true;
			}
			currentTermNo = termNo;
		}
		// Here, we should have finished all checks and initializations,
		// so, let's get to the point...
		nodeNos[used] = nodeNo;
		return used++;
	}

	private void checkIndices(int termNo, int nodeNo)
			throws ArrayIndexOutOfBoundsException {
		if (termNo > termCount) {
			throw new ArrayIndexOutOfBoundsException("Invalid term# "
					+ termNo + ", there are only " + termCount + " terms");
		} else if (nodeNo > nodeCount) {
			throw new ArrayIndexOutOfBoundsException("Invalid node# "
					+ nodeNo + ", there are only " + nodeCount + " nodes");
		}
	}


	/**
	 * Returns the record # this (term#, node#) pair is mapped to, or -1 if
	 * the term does not occur in this node.
	 *
	 * @param termNo
	 * @param nodeNo
	 * @return
	 */
	public int getMapping(int termNo, int nodeNo) {
		int start = termStart[termNo];
		int end = (termNo < termCount? termStart[termNo + 1]: nodeNos.length);

		while (start < end && nodeNos[start] != nodeNo) start++;
		if (start == end) {
			// :-(  Nothing found
			return -1;
		} else {
			return start;
		}
	}

	public MapIterator mapIterator(int termNo) {
		int start = termStart[termNo];
		int end = (termNo < termCount? termStart[termNo + 1]: nodeNos.length);

		return new MapIterator(nodeNos, start, end);
	}

	public Iterator iterator(int termNo) {
		return mapIterator(termNo);
	}

	/**
	 * Increases the capacity by a (fixed for now) percentage.
	 */
	public void increaseCapacity() {
		// Compute the new capacity
		int c =  (int) Math.ceil(nodeNos.length * (1 + DEFAULT_PERCENT_INCREASE));
		int[] newNns = new int[c];
		System.arraycopy(nodeNos, 0, newNns, 0, nodeNos.length);
		nodeNos = newNns;
		// Hopefully the garbage collector realizes here that the old node#
		// array is not needed any more ...
	}

	/**
	 * After all (rather static) mappings have been added, a call to this
	 * function makes sure no storage space in the quite large main array
	 * is wasted.
	 */
	public void trimToSize() {
		if (used < nodeNos.length) {
			int[] newNns = new int[used];
			System.arraycopy(nodeNos, 0, newNns, 0, used);
			nodeNos = newNns;
		}
	}

	/**
	 *
	 * @return
	 */
	public int getMappingCount() { return used - 1; }


	public String toString() {
		StringBuffer buf = new StringBuffer(2000);
		int termEnd, n;

		buf.append("(termCount=");
		buf.append(termCount);
		buf.append(", nodeCount=");
		buf.append(nodeCount);
		buf.append(", mappings=(\n");
		for (int t = 1; t < termStart.length; t++) {
			buf.append("\t");
			buf.append(t);
			buf.append("\t- ");
			if (termStart[t] < 0) {
				buf.append("<no mappings!?>");
			} else {
				termEnd = (t == termCount? termStart.length: termStart[t + 1]);
				n = termStart[t];
				buf.append("(");
				buf.append(nodeNos[n]);
				buf.append(",");
				buf.append(n);
				buf.append(")");
				while (++n < termEnd) {
					buf.append(" (");
					buf.append(nodeNos[n]);
					buf.append(", ");
					buf.append(n);
					buf.append(")");
				}
			}
			buf.append("\n");
		}
		buf.append(")");
		return buf.toString();
	}


	/************************************************************************
	 * Storable implementation
	 ************************************************************************/

	public void store(DataOutput out) throws IOException {
		out.writeInt(termCount);
		out.writeInt(nodeCount);
		out.writeInt(used - 1);
		for (int i = 1; i < termStart.length; i++) {
			out.writeInt(termStart[i]);
		}
		for (int i = 1; i < used; i++) {
			out.writeInt(nodeNos[i]);
		}
	}

	public void load(DataInput in) throws IOException {
		termCount = in.readInt();
		nodeCount = in.readInt();
		used = in.readInt();
		termStart = new int[termCount + 1];
		termStart[0] = -1;
		for (int i = 1; i < termStart.length; i++) {
			termStart[i] = in.readInt();
		}
		nodeNos = new int[used + 1];
		nodeNos[0] = -1;
		for (int i = 1; i <= used; i++) {
			nodeNos[i] = in.readInt();
		}
		used++;
	}

	public int byteSize() {
		// termCount + nodeCount + mappingCount + ...
		return (4 + 4 + 4 + (4 * termCount) + (4 * used));
	}


	/************************************************************************
	 * MapIterator
	 ************************************************************************/

	static class MapIterator implements Iterator {
		private int[] nodeNos;
		private int start;
		private int end;    // Exclusive end!
		private MutableInteger nodeNo = new MutableInteger();


		MapIterator(int[] nodeNos, int start, int end) {
			this.nodeNos = nodeNos;
			this.start = start;
			this.end = end;
		}


		/**
		 * Returns the number of elments still to be expected from this
		 * iterator.
		 *
		 * @return
		 */
		public int size() {
			return (end - start);
		}


		public boolean hasNext() {
			return (start < end);
		}


		public Object next() {
			if (!hasNext()) {
				throw new NoSuchElementException("There are no more "
						+ "mappings left");
			}
			nodeNo.setValue(nodeNos[start++]);
			return nodeNo;
		}

		public void remove() {
			throw new UnsupportedOperationException("Removing of elements "
					+ "is not supported at the current time");
		}

	}



	/************************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		if (args.length != 2) {
			printUsage();
		}
		// Load configuration
		IdxrConfig config = (IdxrConfig) IdxrConfig.load(args[1]);
		TermNodeMap map = new TermNodeMap();
		DataInputStream in;

		String fileName = config.getTermDfMappingFileName(args[0]);

		System.out.println("\nTerm-node mapping info for '" + fileName + "'...");
		in = new DataInputStream(new FileInputStream(fileName));
				//"/home/bremer/Data/Reuters"*/ /*Config.REP_HOME*/ + "/" + Config.TERMDF_MAPPING_FILENAME));
		map.load(in);
		in.close();
		System.out.println("\nTotal mapping (=records in termdf-pp file)..... "
						   + map.getMappingCount());
//		System.out.println(map);
	}

	static private void printUsage() {
		System.out.println();
		System.out.println("Expecting exactly 2 arguments for idxr.TermNodeMap:");
		System.out.println("(1) The configuration name");
		System.out.println("(2) The configuration file name");
	}



	public void loadStoreTest() throws IOException {
		TermNodeMap map = new TermNodeMap(10, 20);
		DataOutputStream out;
		DataInputStream in;

		// Construct dummy map
		for (int term = 1; term <= 10; term++) {
			for (int node = MyMath.random(1, 20); node <= 20; node++) {
				map.addMapping(term, node);
			}
		}
		System.out.println("Map before storage:");
		System.out.println(map);
		map.trimToSize();
		// Prepare file
		out = new DataOutputStream(new FileOutputStream("/home/bremer/Java/termnode.map"));
		map.store(out);
		out.flush();
		out.close();
		in = new DataInputStream(new FileInputStream("/home/bremer/Java/termnode.map"));
		map.load(in);
		in.close();
		System.out.println("Map AFTER storage:");
		System.out.println(map);
	}

}