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

import java.io.IOException;
import org.bluemedialabs.io.BitDataChannel;
//import org.bluemedialabs.io.DbIterator;
import org.bluemedialabs.util.MutableLong;


/**
 * <p>A position number-specific DB iterator.</p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public abstract class JoinNnoDbIterator extends NnoDbIterator {
	// Base properties
//	private int posNo;
	private BitDataChannelPool pool;
	private BitDataChannel in;
	private int entryCount;
//	private DataGuide guide;
	// Join-related properties
	private NnoDbIterator[] joinIt = null;  // = null means, nothing to join
	private int[] selfShift = null;
	private int[] otherShift = null;
	private MutableLong nextPosNo = null;
	private MutableLong tmpPosNo = new MutableLong();


	public JoinNnoDbIterator(int nno, BitDataChannelPool pool, long bitPos,
						  int entryCount, DataGuide guide) throws IOException {
		super(nno, guide);
		this.pool = pool;
		this.entryCount = entryCount;

		if (pool != null) {
			// ...and there are cases in which the pool may legally be null
			// (s. counter iterator over a constant counter)
			in = pool.claim();
			in.bitPosition(bitPos);
		}
	}

	public void finalize() {
		if (pool != null) {
			// Nobody has bothered closing this iterator yet!
			// (Note that it should be reasonable to assume that (in != null) )
			pool.release(in);
			pool = null;
		}
	}


	// This method should only be called if really necessary as, for instance,
	// in the TIndex iterators.
	void setEntryCount(int count) { entryCount = count; }
	void decEntryCount() { entryCount--; }
	void decEntryCount(int dec) { entryCount -= dec; }
	int getEntryCount() { return entryCount; }

	BitDataChannel getDataChannel() { return in; }


	public void close() throws IOException {
		if (pool != null) {
//			throw new IllegalStateException("This iterator apparently has "
//					+ "already been closed");
//		} else {
			pool.release(in);
			// The following makes sure the resource will never be
			// released twice:
			pool = null;
			in = null;
		}
	}

	abstract boolean hasNextImpl();

	abstract Object nextImpl();

	public boolean hasNext() {
//		MutableLong next;

		if (joinIt == null) {
			return hasNextImpl();
		} else {
//			if (nextPosNo != null) {
//				return true;
//			} else if (!hasNext{
//				// Fetch next element
//				next =
//
//			}
			return false;
		}
	}
	public Object next() {
		if (joinIt == null) {
			return nextImpl();
		} else {
			// ...
			return null;
		}
	}


	/**
	 * ...note that, if you join an iterator while this iterator is already
	 * in use (next() has already been called) there might be inconsistencies
	 * in the sequence in which elements are returned.
	 *
	 * @param it
	 */
	public void join(NnoDbIterator it) {
		int count = checkAndAdd(it);
		// Make sure to fetch the very next element (if there is any)

	}

	/**
	 *
	 * @param it
	 * @return The current number of iterators for joining.
	 * @throws IllegalArgumentException
	 */
	private int checkAndAdd(NnoDbIterator it) throws IllegalArgumentException {
		NnoDbIterator[] tmpIt;
		int[] tmpShift1, tmpShift2;
		int len;
		int nno = it.getNodeNo();
		DataGuide guide = getDataGuide();
		GuideNode node = guide.getNode(nno);
		GuideNode thisNode = guide.getNode(getNodeNo());
		GuideNode branchPoint = guide.getNode(getNodeNo());
		int shift;
		boolean first;

		if (joinIt == null) {
			// This is the first element to be joined
			joinIt = new JoinNnoDbIterator[1];
			selfShift = new int[1];
			otherShift = new int[1];
			len = 1;
//			joinIt[0] = it.getPosNo(); -- what's this for?
			nextPosNo = new MutableLong();
			first = true;
		} else {
			// Increase array length and copy elements
			len = joinIt.length + 1;
			tmpIt = new JoinNnoDbIterator[len];
			tmpShift1 = new int[len];
			tmpShift2 = new int[len];
			for (int i = 0; i < joinIt.length; i++) {
				tmpIt[i] = joinIt[i];
				tmpShift1[i] = selfShift[i];
				tmpShift2[i] = otherShift[i];
			}
			joinIt = tmpIt;
			selfShift = tmpShift1;
			otherShift = tmpShift2;
			first = false;
		}
		len = joinIt.length;
		joinIt[len - 1] = it;
		// Determine branch point
		while (! (node.equals(branchPoint) || node.isDescOf(branchPoint)
				  || branchPoint.isDescOf(node))) {
			branchPoint = branchPoint.getParent();
		}
		selfShift[len - 1] = thisNode.getTotalPosBitLen()
					 - branchPoint.getTotalPosBitLen();
		otherShift[len - 1] = node.getTotalPosBitLen()
					  - node.getTotalPosBitLen();
		return len;
	}
}