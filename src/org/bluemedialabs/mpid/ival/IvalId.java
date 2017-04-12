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
package org.bluemedialabs.mpid.ival;

import java.io.*;
import org.bluemedialabs.io.Storable;
import org.bluemedialabs.util.CloneableObject;
import org.bluemedialabs.util.MyMath;
import org.bluemedialabs.util.MyPrint;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class IvalId implements Comparable, CloneableObject, Storable {
	static public final int BYTE_SIZE = 9;

	private int start;
	private int end;
	private byte level;


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public int computeNoBitLen(int totalNodeCount) {
		return (int) Math.ceil(1 + MyMath.log2(totalNodeCount));
	}

	static public int computeLevelBitLen(int maxDepth) {
		return (int) Math.ceil(1 + MyMath.log2(maxDepth));
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public IvalId(int s, int e, byte l) {
		start = s;
		end = e;
		level = l;
	}

	// Convenience constructor
	public IvalId(int s, int e, int l) {
		this(s, e, (byte) l);
	}

	public IvalId() {}


	public Object clone() {
		return new IvalId(start, end, level);
	}

	public void copy(Object obj) {
		IvalId id = (IvalId) obj;
		id.start = start;
		id.end = end;
		id.level = level;
	}

	public void setStart(int s) { start = s; }
	public int getStart() { return start; }

	public void setEnd(int e) { end = e; }
	public int getEnd() { return end; }

	public void setLevel(byte l) { level = l; }
	public byte getLevel() { return level; }

	public String toString() {
		StringBuffer buf = new StringBuffer(30);

		buf.append(start);
		buf.append(":");
		buf.append(end);
		buf.append("/");
		buf.append(level);
		return buf.toString();
	}



	/************************************************************************
	 * Comparable implementation(s)
	 ************************************************************************/

	// Used in Counter
	public int compareTo(Object obj) {
		IvalId iid = (IvalId) obj;
		return (start - iid.start);
	}

	public boolean equals(Object obj) {
		IvalId iid = (IvalId) obj;
		return (start == iid.start);
	}



	/************************************************************************
	 * Storable implementation
	 ************************************************************************/

	public void store(DataOutput out) throws IOException {
		out.writeInt(start);
		out.writeInt(end);
		out.writeByte(level);
	}

	public void load(DataInput in) throws IOException {
		start = in.readInt();
		end = in.readInt();
		level = in.readByte();
	}

	public int byteSize() {
		return BYTE_SIZE;
	}
}