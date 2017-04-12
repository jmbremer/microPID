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
import org.bluemedialabs.io.Base128;
import org.bluemedialabs.io.Storable;
import org.bluemedialabs.util.CloneableObject;


/**
 * A term number and related counter, so far used for storing the number of
 * nodes of a certain node number which contain a certain term.
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TermCounter implements CloneableObject, Comparable, Storable {
	static public final int BYTE_SIZE = -1;
	static private final Base128 base128 = new Base128();

	private int termNo = 0;
	private int count  = 0;


	public TermCounter(int termNo, int count) {
		this.termNo = termNo;
		this.count = count;
	}

	public TermCounter() {}


	public void setTermNo(int no) { termNo = no; }
	public int getTermNo() { return termNo; }

	public void setCount(int c) { count = c; }
	public void incCount() { count++; }
	public int getCount() { return count; }


	/*+******************************************************************
	 * CloneableObject implementation
	 ********************************************************************/

	public Object clone() {
		return new TermCounter(termNo, count);
	}

	public void copy(Object obj) {
		TermCounter tc = (TermCounter) obj;
		tc.termNo = termNo;
		tc.count = count;
	}


	/*+******************************************************************
	 * Comparable implementation
	 ********************************************************************/

	public int compareTo(Object obj) {
		TermCounter tc = (TermCounter) obj;

		return termNo - tc.getTermNo();
	}

	public boolean equals(Object obj) {
		return (obj instanceof TermCounter
				&& termNo == ((TermCounter) obj).getTermNo());
	}


	/*+******************************************************************
	 * Storable implementation
	 ********************************************************************/

	public void store(DataOutput out) throws IOException {
		base128.writeInt(termNo, out);
		base128.writeInt(count, out);
	}

	public void load(DataInput in) throws IOException {
		termNo = base128.readInt(in);
		count = base128.readInt(in);
	}

	public int byteSize() {
		return BYTE_SIZE;
	}

}
