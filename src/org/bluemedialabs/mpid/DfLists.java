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
import org.bluemedialabs.mpid.DataGuide;
import org.bluemedialabs.mpid.ival.*;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p>Accumulator for lists related to a set of node numbers. In its build very
 * similar to TermDfCounters.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class DfLists extends ArrayLists implements Storable {
	/**
	 * An estimate for the maximum length of a single list as a percentage
	 * of the total element capacity of the single ArrayList-underlying array.
	 */
	static private final double ESTIMATED_MAX_LIST_LENGTH = 0.1;
	static public final boolean DEFAULT_SORTING      = false;
	static public final boolean DEFAULT_BIT_STORE    = true;

	private DataOutputSequence out;
	private BitDataOutput bitOut = null;
	private InvertedList invList;
	private boolean sort;     // Sort lists before storage?
	private boolean bitStore; // Store lists encoded?
	private DataGuide guide;
//	private Base128 b128 = new Base128();
	private boolean stored = false;


/**
 *
 * @param sample
 * @param capacity
 * @param listCount The actual number of list, not the adjusted (...)
 * @param out
 * @param guide
 * @param invList
 * @param sort
 * @param bitStore
 */
	public DfLists(CloneableObject sample, int capacity, int listCount,
				   DataOutputSequence out, DataGuide guide,
				   InvertedList invList, boolean sort, boolean bitStore) {
		super(sample, capacity, listCount + 1);
		// The "+1" is because our lists start with index 1 here!
		this.out = out;

		this.guide = guide;
		this.invList = invList;
		this.sort = sort;
		this.bitStore = bitStore;
		if (bitStore) {
			bitOut = new BitDataOutput(null);
		}
	}

	public DfLists(CloneableObject sample, int capacity, int listCount,
				   DataOutputSequence out, DataGuide guide) {
		this(sample, capacity, listCount, out, guide, new InvertedList(sample,
				(int)(capacity * ESTIMATED_MAX_LIST_LENGTH)), DEFAULT_SORTING,
				DEFAULT_BIT_STORE);
		// Instead of creating the list here once, a more runtime memory-
		// effective way would be to just create the list when storage is
		// required. However, that might be significantly slower.
		// Besides, if any inverted list other than the default list is
		// needed (s. 1st constructur) for implementing a specific storage
		// algorithm, then no runtime creation is possible anyway.
	}

	public void finalize() throws IOException {
		if (!stored) {
			this.store(out);
			out.close();
			reset();
			stored = true;
		}
	}


	protected void onFullArray() {
//		System.out.println("\nDfLists capacity of " + elements.length
//				  + " reached, storing elements to file " + out.getSeqNumber()
//				  + "...");
		// Capacity reached, so write everything back
		try {
			this.store(out);
			// ..and prepare next DataOuput
			out = out.next();
		} catch (IOException e) {
			throw new IllegalStateException("Have an IO exception during write "
					+ "back of list to '" + out + "'");
		}
		reset();
//		System.out.println("\nDone storing elements.");
	}



	/*+**********************************************************************
	 * Storable implementation
	 ************************************************************************/

	 /**
	  * Writes all term-document counters currently held in memory to the
	  * supplied output device. For each inverted list (one term and all of
	  * its related document information), first the term number, then the
	  * number of document counters and finally, the pairs of (document number,
	  * counter) are written.
	  */
	public void store(DataOutput out) throws IOException {
		int pos, elemCount;
		PercentPrinter p = new PercentPrinter(getUsedElems());
		int storedIds = 0, percent, lastPercent = -1;
		int avgLen = 0, maxLen = -1;

		if (bitStore) {
			bitOut.reattach(out);
		}
		int liCount = getListCount();
		for (int i = 1; i < liCount; i++) {
			if ((percent = p.getPercent()) % 5 == 0 && percent != lastPercent) {
				lastPercent = percent;
			}
			if (start[i] >= 0) {
				pos = start[i];
				if (pos >= 0) {
					// STORAGE
					// write list# (= record#)
					if (bitStore) {
						bitOut.write(i, 32);
					} else {
						out.write(i);
					}

					// DEBUGGING:
//					System.out.print("\n________Node# " + i + "_________");

					// Determine number of counters
					elemCount = countElements(pos);

					// Copy counters to inverted list and sort list ( :-( )
					invList.clear();
					if (elemCount > invList.capacity()) {
						invList.adjustCapacity(elemCount);
					}
					for (int j = 0; j < elemCount; j++) {
						((CloneableObject) elements[pos].getObject()).copy(invList.get(j));
						// Go to next
						pos = elements[pos].getNext();
					}
					invList.setLength(elemCount);
					invList.setNo(i);
					// SORT (but only if requested)
					if (sort) {
						invList.sort();
					}
					// Write inverted list

					// STORAGE
					if (bitStore) {
						invList.store(bitOut);
					} else {
						invList.store(out);
					}
//					try {
//						System.out.println("\n" + i + ")\t" + invList.length() + "\t" + invList);
//					} catch (Exception e) {
//						e.printStackTrace();
//						throw new RuntimeException();
//					}

					maxLen = Math.max(elemCount, maxLen);
					avgLen += elemCount;

					storedIds += elemCount;
				}
			} // if
		} // for

		if (bitStore) {
			bitOut.flush();
		}

//		System.out.println();
//		System.out.println("Maximum # of inv. list elements..." + maxLen);
//		System.out.println("Avg. # of inv. list elements......" + (avgLen / listCount));
	}

	public long overallSpace = 0;

	private int gammaCodeLen(int x) {
		if (x == 1) {
			return 1;
		} else if (x <= 3) {
			return 3;
		} else if (x <= 7) {
			return 5;
		} else {
			// Actually compute length
			return (1 + 2 * (int) Math.floor(MyMath.log2(x)));
		}
	}


	private int countElements(int start) {
		int count = 1;
		start = elements[start].getNext();
		while (start > 0) {
			count++;
			start = elements[start].getNext();
		}
		return count;
	}

	// FOR DEBUGING ONLY
	private void analyze(int termNo) {
		int count = 0;
		int pos = start[termNo];
		Element c = elements[pos];

		System.out.println("\nAnalyzing data for term " + termNo + "...");
		while (pos > 0) {
			count++;
			c = elements[pos];
			pos = c.getNext();
		}
	}

	public void load(DataInput in) throws IOException {
		throw new UnsupportedOperationException(
				"DfLists are not supposed to be loaded");
	}

	public int byteSize() {
		return -1;  // don't know
	}



	static public void main(String[] args) throws Exception {
		if (args.length != 2) {
			printUsage();
		}
		// Load configuration
		IdxrConfig config = (IdxrConfig) IdxrConfig.load(args[1]);
		String cfgName = args[0];
		DataGuide guide = DataGuide.load(config.getDataGuideFileName(cfgName));
		DataFileOutputChannelSeq outSeq =
				new DataFileOutputChannelSeq(config.getIndexHome(cfgName)
				+ "/outseq.data", 256);
		IvalDfList list = new IvalDfList(20, guide);
		DfLists lists = new DfLists(new IvalId(), 10 /* bytes buffer */,
									3 /* list#'s */, outSeq, guide,
									list, false, true);

		// Write data
		IvalId id = new IvalId();
		for (int i = 0; i < 29; i++) {
			id.setStart(i);
			id.setEnd(i + 1);
			id.setLevel((byte) ((i % 3) + 1));
			lists.add((i % 3) + 1, id);
		}
		lists.finalize();
		// Read data
		BufferedInputChannel ch = BufferedInputChannel.create(
				config.getProperty(cfgName, "TestHome") + "/outseq.data" + 1);
		BitDataInput bitIn = new BitDataInput(ch);
		int len;
		MyPrint.pl();
		try {
			while (true) {
				MyPrint.p("List# ");
				MyPrint.p(bitIn.read(32) + ": ");
				list.load(bitIn);
				MyPrint.p(list.toString());
				MyPrint.pl();
			}
		} catch (EOFException e) {
			MyPrint.pl(" -End Of File- ");
		}
	}

	static private void printUsage() {
		System.out.println();
		System.out.println("Expecting exactly 2 arguments for idxr.DfLists:");
		System.out.println("(1) The configuration name");
		System.out.println("(2) The configuration file name");
	}

}
