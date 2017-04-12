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
import org.bluemedialabs.io.Storable;
import org.bluemedialabs.util.CloneableObject;
import org.bluemedialabs.util.MyPrint;


/**
 * <p>Just a fully-qualified path, efficiently encoded.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class PathId implements CloneableObject, Comparable, Storable {
	/**
	 * The number of bytes this PID requires if stored stand-alone in a
	 * fixed-length representation.
	 */
	static public final int BYTE_SIZE = 10;

	/**
	 * Return when compared to another PID that is a child of this PID.
	 */
	static public final int PARENT  = 1;
	/**
	 * Return when compared to another PID that is identical to this PID.
	 */
	static public final int IDENTICAL = 0;
	/**
	 * Returned when this PID is compared to another PID that is the parent of
	 * this PID.
	 */
	static public final int CHILD   = -1;
	/**
	 * Returned when a comparison to another PID results in no special
	 * relationship.
	 */
	static public final int UNRELATED = -42;

	/**
	 * Used to align the string representation of PIDs to their total position
	 * bit length.
	 */
	static private final String ZEROS = "000000000000000000000000000000000";

	private short nodeNo;   // Data guide node identifier
	private long posBits;    // Appended position bits


	/**
	 * Constructs a new PID.
	 *
	 * @param nodeNo The related data guide node number.
	 * @param posBits The position bits.
	 */
	public PathId(short nodeNo, long posBits) {
		this.nodeNo = nodeNo;
		this.posBits = posBits;
	}

	/**
	 * Constructs a new PID.
	 *
	 * @param pid The PID used to initialize the new PID.
	 */
	public PathId(PathId pid) {
		this(pid.nodeNo, pid.posBits);
	}

	/**
	 * Constructs an empty PID initializing the internal properties to
	 * guaranteed invalid values to make sure that not properly assigning
	 * the values will lead to an exception.
	 */
	public PathId() {
		this((short) -1, -1);
	}


	/**
	 * Returns a new PID identical to this PID.
	 *
	 * @return A new identical PID.
	 */
	public Object clone() {
		PathId pid = new PathId(nodeNo, posBits);
		return pid;
	}


	/**
	 * Replaced by copy().
	 *
	 * @param pid
	 * @deprecated
	 */
	public void setTo(PathId pid) {
		nodeNo = pid.nodeNo;
		posBits = pid.posBits;
	}

	/**
	 * Copies this PID's properties to the given PID's properties.
	 *
	 * @param obj The PathId to copy this PID to.
	 */
	public void copy(Object obj) {
		PathId pid = (PathId) obj;
		pid.nodeNo = nodeNo;
		pid.posBits = posBits;
	}

	/**
	 * Is this PID an ancestor of the given PID or the very same PID?
	 */
	public boolean contains(PathId pid, DataGuide guide) {
		long adjposBits;
		int posLen = getPosLen(guide);
		int pidPosLen = pid.getPosLen(guide);

		if (guide.isAncestor(nodeNo, pid.getNodeNo())) {
			adjposBits = pid.getPosBits() >>> (pidPosLen - posLen);
			if ((posBits - adjposBits) == 0) {
				// Indeed, we just found a descendant of ours
				return true;
			}
		} else if (nodeNo == pid.getNodeNo()
				&& (posBits - pid.getPosBits()) == 0) {
			// The adjposBits calculation is unnecessary here!
			// It's actually the exact same node!
			return true;
		}
		return false;
	}

	/**
	 *
	 * @param no
	 */
	public void setNodeNo(short no) { nodeNo = no; }
	/**
	 *
	 * @param no
	 */
	public void setNodeNo(int no) { setNodeNo((short) no); }
	/**
	 *
	 * @return
	 */
	public short getNodeNo() { return nodeNo; }

	/**
	 *
	 * @param bits
	 */
	public void setPosBits(long bits) { posBits = bits; }
	/**
	 *
	 * @return
	 */
	public long getPosBits() { return posBits; }

//	public void setNumLen(int len) { setNumLen((byte) len); }
//	public void setNumLen(byte len) {
//		// Do some checks...
//		if ((posBits >>> len) > 0) {
//			throw new IllegalStateException("Setting the number length to a "
//					+ "value smaller than the number bits suggest (numLen="
//					+ len + ", posBits=" + posBits +", nodeNo=" + nodeNo + ")");
//		}
//		numLen = len;
//	}
//	public byte getNumLen() { return (byte) getPosLen(guide); }

	/**
	 *
	 * @param guide
	 * @return
	 */
	public int getPosLen(DataGuide guide) {
		return guide.getNode(nodeNo).getTotalPosBitLen();
	}


	/**
	 *
	 * @param guide
	 * @return
	 */
	public String toString(DataGuide guide) {
		StringBuffer buf = new StringBuffer(50);
		String str;
		int totalNumLen;

		/*
		buf.append("(nodeNo=");
		buf.append(nodeNo);
		buf.append(", posBits=");
		str = Integer.toBinaryString(posBits);
		try {
			buf.append(ZEROS.substring(0, numLen - str.length()));
		} catch (Exception e) {
			// Ignore for now...

//			System.out.println("\nNodeNo=" + nodeNo + ", posBits=" + posBits
//					+ ", binString=" + str + ", numLen=" + numLen);
//			System.out.flush();
//			throw new IllegalStateException(e.toString());
		}
		buf.append(str);
		buf.append(", numLen=");
		buf.append(numLen);
		buf.append(")");
		*/

		buf.append(nodeNo);
		buf.append("/");
		str = Long.toBinaryString(posBits);
		// Cannot determine the position bits' length any more...
//		totalNumLen = guide.getNode(nodeNo).getTotalPosBitLen();
//		if (totalNumLen > 0) {
//			try {
//				buf.append(ZEROS.substring(0, totalNumLen - str.length()));
//			} catch (StringIndexOutOfBoundsException e) {
//				throw new RuntimeException("Index out of bounds while building "
//						+ "PID string (nodeNo=" + nodeNo + ", posBits="
//						+ posBits + ", totalNumLen=" + totalNumLen
//						+ ", numLen=" + numLen + ", numStr=" + str + ")");
//			}
//		}
//		MyPrint.pl("posBits=" + posBits + ", numLen=" + numLen
//				   + ", binStr=" + str);
		buf.append(str);
//		buf.append("/");
//		buf.append(totalNumLen);
		return buf.toString();
	}

	/**
	 *
	 * @return
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer(50);
		String str;
		/*
		buf.append("(nodeNo=");
		buf.append(nodeNo);
		buf.append(", posBits=");
		str = Integer.toBinaryString(posBits);
		try {
			buf.append(ZEROS.substring(0, numLen - str.length()));
		} catch (Exception e) {
			// Ignore for now...

//			System.out.println("\nNodeNo=" + nodeNo + ", posBits=" + posBits
//					+ ", binString=" + str + ", numLen=" + numLen);
//			System.out.flush();
//			throw new IllegalStateException(e.toString());
		}
		buf.append(str);
		buf.append(", numLen=");
		buf.append(numLen);
		buf.append(")");
		*/

		buf.append(nodeNo);
		buf.append("/");
		buf.append(Long.toBinaryString(posBits));
		// Including the numLen here does not make much sense as there are
		// totalNumLen bits (see document guide) rather than only numLen
//		buf.append("/");
//		buf.append(numLen);
		return buf.toString();
	}



	/************************************************************************
	 * Comparable implementation(s)
	 ************************************************************************/

	/**
	 * Defines an order on PIDs. The primary order criterium is the node#,
	 * the secondary criterium (for same node#'s) is the position bits.
	 *
	 * @param obj
	 * @return
	 */
	public int compareTo(Object obj) {
		PathId pid = (PathId) obj;
//		int thisBits, pidBits;

		if (nodeNo < pid.nodeNo) {
			return -1;
		} else if (nodeNo > pid.nodeNo) {
			return 1;
		} else {
			// Let the posBits speak
			// (Notice that we know here that the position length is identical!)
//			if (numLen >= pid.numLen) {
//				thisBits = posBits >>> (numLen - pid.numLen);
//				pidBits = pid.posBits;
//			} else {
//				thisBits = posBits;
//				pidBits = pid.posBits >>> (pid.numLen - numLen);
//			}
			return ((int) (posBits - pid.getPosBits()));
		}
//        throw new UnsupportedOperationException("Cannot compare pids this way!");
	}

	/**
	 * Children are smaller than we are. Thus, one is returned if we are
	 * the parent of the supplied pid. Minus one is returned if we are
	 * the child of the given pid. Zero, if we are identical. -42 is
	 */
	public int compareTo(PathId pid, DataGuide guide) {
		long adjPosBits;
		int posLen = getPosLen(guide);
		int pidPosLen = pid.getPosLen(guide);

		if (nodeNo == pid.getNodeNo() && posBits == pid.getPosBits()) {
			return IDENTICAL;
		} else if (guide.isAncestor(nodeNo, pid.getNodeNo())) {
			// pid is potential child
			adjPosBits = posBits << (pidPosLen - posLen);
			// Now both number bit strings are aligned
			if ((adjPosBits & pid.getPosBits()) == adjPosBits) {
				// Yes, its a child!
				return PARENT;   //..because we are the parent
			}
		} else if (guide.isAncestor(pid.getNodeNo(), nodeNo)) {
			// pid is potential parent
			adjPosBits = pid.getPosBits() << (posLen - pidPosLen);
			// Now again both number bit strings are aligned
			if ((posBits & adjPosBits) == adjPosBits) {
				// Yes, its a parent!
				// ....wrong!?!!!...
				return CHILD;   //..because we are the parent
			}
		}
		// No particular relationship
		return UNRELATED;
	}

	public boolean equals(Object obj) {
		PathId pid = (PathId) obj;

		return (nodeNo == pid.nodeNo && posBits == pid.posBits);
	}



	/************************************************************************
	 * Storable implementation
	 ************************************************************************/

	public void store(DataOutput out) throws IOException {
		out.writeShort(nodeNo);
		out.writeLong(posBits);
	}

	public void load(DataInput in) throws IOException {
		nodeNo = in.readShort();
		posBits = in.readLong();
	}

	public int byteSize() {
		return BYTE_SIZE;
	}

}