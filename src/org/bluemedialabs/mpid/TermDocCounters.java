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
import org.bluemedialabs.io.DataOutputSequence;
import org.bluemedialabs.io.Storable;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TermDocCounters /*implements Storable*/ {
	private DataOutputSequence out;
	private int[] termStart;    // start index of inverted list, -1 if invalid
	private int[] termEnd;      // current end of term inverted list
	private int termCount;
	private DocCount[] docCounts;
	private int usedCounts;     // number of currently used doc counters
	private boolean stored = false;


	public TermDocCounters(int countCapacity, int termCount,
			DataOutputSequence out) {
		this.termCount = termCount;
		this.out = out;
		docCounts = new DocCount[countCapacity];
		for (int i = 0; i < countCapacity; i++) {
			docCounts[i] = new DocCount();
		}
		termStart = new int[termCount + 1];
		termEnd = new int[termCount + 1];
		reset();
	}

	public void finalize() throws IOException {
		try {
			super.finalize();
		} catch (Throwable e) {
		}
		// make sure the last counters are written
//		try {
		if (!stored) {
			this.store(out);

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
//		for (int i = 0; i < docCounts.length; i++) {
//			docCounts[i].next = -1;
//		}
		usedCounts = 0;
	}

	public void addCount(int termNo, int docNo, short count) throws IOException {
		// No more counter elements left??
		if (usedCounts >= docCounts.length) {
			System.out.println("TermDocCounters capacity of " + docCounts.length
				+ " reached, storing counters to file " + out.getSeqNumber()
				+ "...");
			// capacity reached, so write everything back
			this.store(out);
			// ..and prepare next DataOuput
			out = out.next();
			reset();
		}
		DocCount docCount = docCounts[usedCounts];

		// Store new counter information into next available counter element
		docCount.docNo = docNo;
		docCount.count = count;
		docCount.next = -1;
		// Check for first element
		if (termStart[termNo] == -1) {
			// first counter for this term#
			termStart[termNo] = usedCounts;
		} else {
			// Adjust related "list"
			// (Here we know that termEnd has a valid element for termNo!)
			docCounts[termEnd[termNo]].next = usedCounts;
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
		int pos;
//		System.out.println("Storing );
		/*
		 * Old, direct storage...
		for (int i = 1; i <= termCount; i++) {
			if (termStart[i] >= 0) {
				pos = termStart[i];
				out.writeInt(i);        // write term# (= record #)
				out.writeInt(countElements(pos));
				// write elements
				out.writeInt(docCounts[pos].docNo);
				out.writeShort(docCounts[pos].count);
				pos = docCounts[pos].next;
				while (pos > 0) {
					out.writeInt(docCounts[pos].docNo);
					out.writeShort(docCounts[pos].count);
					pos = docCounts[pos].next;
				}
			}
		}
		 */
		// (1) Copy data into inverted list
	}

	private int countElements(int start) {
		int count = 1;
		start = docCounts[start].next;
		while (start > 0) {
			count++;
			start = docCounts[start].next;
		}
		return count;
	}

	public void load(DataInput in) throws IOException {
		throw new UnsupportedOperationException(
				"TermDocCounters are not supposed to be loaded");
	}

	public int byteSize() {
		return -1;  // don't know
	}


	static protected class DocCount {
		int docNo;
		short count;
		int next;       // index of next counter, -1 if last element
	}


}