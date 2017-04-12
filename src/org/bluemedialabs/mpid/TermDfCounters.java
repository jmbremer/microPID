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
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.MyMath;
import org.bluemedialabs.util.PercentPrinter;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TermDfCounters /*implements Storable*/ {
	private DataOutputSequence out;
	private int[] termStart;    // start index of inverted list, -1 if invalid
	private int[] termEnd;      // current end of term inverted list
//	private int[] next;
	private int termCount;
	private CounterElement[] dfCounts;
	private InvertedDfList invList;
	private int usedCounts;     // number of currently used doc counters
	private DataGuide guide;
	private boolean stored = false;


	public int[] countStats = new int[256];
//	Counter dfCount = new Counter();
	Base128 b128 = new Base128();


	public TermDfCounters(int countCapacity, int termCount,
			DataOutputSequence out, DataGuide guide) {
		this.termCount = termCount;
		this.out = out;
		this.guide = guide;
		dfCounts = new CounterElement[countCapacity];
		invList = new InvertedDfList(100, guide);
		for (int i = 0; i < countCapacity; i++) {
			dfCounts[i] = new CounterElement();
		}
		termStart = new int[termCount + 1];
		termEnd = new int[termCount + 1];
//		next = new int[countCapacity];
		reset();
	}

	public void finalize() throws IOException {
//		try {  --- What was this for???
//			super.finalize();
//		} catch (Throwable e) {
//		}
		if (!stored) {
		// make sure the last counters are written
//		try {
			this.store(out);
			out.close();
//		} catch
		// don't do this:  out = out.next();
			reset();    // just in case finalize() is called twice!
			stored = true;
		}
	}

	private void reset() {
		for (int i = 0; i <= termCount; i++) {
			termStart[i] = -1;
			termEnd[i] = -1;
		}
		for (int i = 0; i < dfCounts.length; i++) {
			dfCounts[i].next = -1;
		}
		usedCounts = 0;
	}

	// tdfi is only temporary!!!
	public void addCount(int termNo, PathId pid, int count, TermDfIndexer tdfi)
			throws IOException {
		// No more counter elements left??

		countStats[count]++;

		if (usedCounts >= dfCounts.length) {
			// Doesn't work anymore??? :
//			System.out.print(".." +
//					(Math.round(1000 * (double) tdfi.currentTokenCount / tdfi.totalTokenCount) / 10)
//					+ "%...");
			System.out.println("\nTermDfCounters capacity of " + dfCounts.length
				+ " reached, storing counters to file " + out.getSeqNumber()
				+ "...");
			// capacity reached, so write everything back
			this.store(out);
			// ..and prepare next DataOuput
			out = out.next();
			reset();
			System.out.println("\nDone storing counters.");
		}
		CounterElement dfCount = dfCounts[usedCounts];

		// Store new counter information into next available counter element
		dfCount.setPid(pid);
		dfCount.setCount(count);
		dfCount.setNext(-1);
		// Check for first element
		if (termStart[termNo] == -1) {
			// first counter for this term#
			termStart[termNo] = usedCounts;
		} else {
			// Adjust related "list"
			// (Here we know that termEnd has a valid element for termNo!)
			dfCounts[termEnd[termNo]].next = usedCounts;
		}
		// In any case, the end is now here:
		termEnd[termNo] = usedCounts;
		usedCounts++;
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
//		BitDataOutput bitOut = new BitDataOutput(out);
		PercentPrinter p = new PercentPrinter(usedCounts);
		int storedCounts = 0, percent, lastPercent = -1;

		int avgLen = 0, maxLen = -1;

		for (int i = 1; i <= termCount; i++) {
			if ((percent = p.getPercent()) % 5 == 0 && percent != lastPercent) {
				lastPercent = percent;
			}
			if (termStart[i] >= 0) {
				pos = termStart[i];
				if (pos >= 0) {

					// STORAGE
					out.writeInt(i);        // write term# (= record#)

					// DEBUGGING:
					System.out.print("\n________Term# " + i + "_________");

					// Determine number of counters
					elemCount = countElements(pos);

//					if (elemCount == 32244) {
//						analyze(i);
//					}

//					b128.writeInt(elemCount, bitOut);
//					bitOut.flush();
					// Copy counters to inverted list and sort list ( :-( )
					invList.clear();
					if (elemCount > invList.capacity()) {
						invList.adjustCapacity(elemCount);
					}
					for (int j = 0; j < elemCount; j++) {
						dfCounts[pos].copyInto(invList.get(j));
						// Go to next
						pos = dfCounts[pos].getNext();
					}
					invList.setLength(elemCount);
					// SORT
					invList.sort();
					// Write inverted list

					// STORAGE
					invList.store(out);
//					try {
//						System.out.println("\n" + i + ")\t" + invList.length() + "\t" + invList);
//					} catch (Exception e) {
//						e.printStackTrace();
//						throw new RuntimeException();
//					}

					maxLen = Math.max(elemCount, maxLen);
					avgLen += elemCount;

					storedCounts += elemCount;
				}

				/*
				 * Old writing routine
				 *
				out.writeInt(countElements(pos));

				// Write first element
				dfCounts[pos].getPid().store(out);
				out.writeByte(dfCounts[pos].getCount());

				overallSpace += dfCounts[pos].getPid().getNumLen();
				overallSpace += guide.getNodeNoBits();
				overallSpace += gammaCodeLen(dfCounts[pos].getCount());
				pos = dfCounts[pos].getNext();

				// Write other elements
				while (pos > 0) {
					dfCounts[pos].getPid().store(out);
					out.writeByte(dfCounts[pos].getCount());

					overallSpace += dfCounts[pos].getPid().getNumLen();
					overallSpace += guide.getNodeNoBits();
					overallSpace += gammaCodeLen(dfCounts[pos].getCount());
					pos = dfCounts[pos].getNext();
				}
				 *
				 */
			} // if

			// PRINT PROGRESS
			// (enable for large data)
//			p.notify(storedCounts);


		} // for

//		System.out.println();
//		System.out.println("Maximum # of inv. list elements..." + maxLen);
//		System.out.println("Avg. # of inv. list elements......" + (avgLen / termCount));
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
		start = dfCounts[start].next;
		while (start > 0) {
			count++;
			start = dfCounts[start].next;
		}
		return count;
	}

	// FOR DEBUGING ONLY
	private void analyze(int termNo) {
		int count = 0;
		int pos = termStart[termNo];
		CounterElement c = dfCounts[pos];

		System.out.println("\nAnalyzing data for term " + termNo + "...");
		while (pos > 0) {
			count++;
			c = dfCounts[pos];
			pos = c.getNext();
		}
	}

	public void load(DataInput in) throws IOException {
		throw new UnsupportedOperationException(
				"TermDfCounters are not supposed to be loaded");
	}

	public int byteSize() {
		return -1;  // don't know
	}


/*+**************************************************************************
 * CounterElement class
 ****************************************************************************/

	/**
	 * Counter class extended by a next pointer that allows counters to be
	 * used as list elements.
	 */
	static protected class CounterElement extends Counter {
		private int next = 0;   // index of next counter, -1 if last element

		public CounterElement(PathId pid, byte count) {
			super(pid, count);
		}
		public CounterElement(PathId pid, int count) {
			super(pid, count);
		}
		public CounterElement() {}

		public void setNext(int n) { next = n; }
		public int getNext() { return next; }
	}

}