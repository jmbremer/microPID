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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.bluemedialabs.io.Storable;


/**
 * <p>A counter for a term occurrence within a document fragments.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Counter implements Comparable<Counter>, Storable {
	static public final int BYTE_SIZE = PathId.BYTE_SIZE + 1;

	private PathId pid = new PathId();
	private byte count;


	public Counter(PathId pid, byte count) {
		this.pid = pid;
		this.count = count;
	}

	public Counter(PathId pid, int count) {
		this(pid, (byte) count);
	}

	public Counter() {}


	public Object clone() {
		Counter c = new Counter();
		c.setPid(new PathId(pid));
		c.setCount(count);
		// leave next alone
		return c;
	}

	public void copyInto(Counter c) {
		c.setPid(pid);
		c.setCount(count);
		// again, leave next alone
	}


	public void setPid(PathId p) { p.copy(pid); }
	public PathId getPid() { return pid; }

	public void setCount(int count) {
		if (count > Byte.MAX_VALUE) {
//				throw new IllegalArgumentException(
			System.out.println("\nNeeded to store counter "
				+ count + " in a byte");
			count = 127;
		}
		this.count = (byte) count;
	}
	public void setCount(byte count) {
		this.count = count;
	}
	public byte getCount() { return count; }


	public String toString() {
		StringBuffer buf = new StringBuffer(60);
//        buf.append("(pid=");
		buf.append(pid);
//        buf.append(", count=");
//        buf.append(count);
//		buf.append(")");
		return buf.toString();
	}


	/************************************************************************
	 * Comparable implementation
	 ************************************************************************/

	public boolean equals(Object obj) {
		Counter c = (Counter) obj;
		return (pid.equals(c.getPid()) && count == c.getCount());
	}

	public int compareTo(Counter c) {
		return (pid.compareTo(c.getPid()));
	}


	/************************************************************************
	 * Storable implementation
	 ************************************************************************/

	public void store(DataOutput out) throws IOException {
		pid.store(out);
		out.writeByte(count);
	}

	public void load(DataInput in) throws IOException {
		pid.load(in);
		count = in.readByte();
	}

	public int byteSize() {
		return BYTE_SIZE;
	}

}