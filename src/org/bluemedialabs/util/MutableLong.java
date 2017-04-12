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
package org.bluemedialabs.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.bluemedialabs.io.Storable;


/**
 * <p>A simple wrapper around a long that--unlike Long--allows to change the
 * underlying value. Mutable longs are particularly useful for hash tables
 * that need to include changeable longs.</p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class MutableLong implements CloneableObject, Storable {
	/**
	 * The actual underlying int value.
	 */
	public long value;

	/**
	 * Constructs a new mutable integer with the given int value.
	 */
	public MutableLong(long value) {
		this.value = value;
	}

	public MutableLong() {
		this(0);
	}

	public Object clone() {
		return new MutableLong(value);
	}

	public void copy(Object obj) {
		MutableLong m = (MutableLong) obj;
		m.value = value;
	}


	public void setValue(long value) {
		this.value = value;
	}

	public long getValue() {
		return value;
	}

	public void add(long l) {
		value += l;
	}

	public void inc() {
		value++;
	}

	public int hashCode() {
		return (int)(value ^ (value >>> 32));
	}

	public boolean equals(Object obj) {
		MutableLong ml;
		if (obj == null || !(obj instanceof MutableLong)) {
			return false;
		}
		ml = (MutableLong) obj;
		return (value == ml.getValue());
	}

	public String toString() {
		return String.valueOf(value);
	}


	/*+**********************************************************************
	 * Storable implementation
	 ************************************************************************/

	public void store(DataOutput out) throws IOException {
		out.writeLong(value);
	}

	public void load(DataInput in) throws IOException {
		value = in.readLong();
	}

	public int byteSize() {
		return 8;
	}

}