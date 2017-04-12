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
 * <p>A simple wrapper around an int that--unlike Integer--allows to change the
 * underlying value. Mutable integers are particularly useful for hash tables
 * that need to include changeable ints.</p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class MutableInteger implements CloneableObject/*, Comparable !!! */, Reuseable,
		Storable {
	/**
	 * The actual underlying int value.
	 */
	public int value;

	/**
	 * Constructs a new mutable integer with the given int value.
	 */
	public MutableInteger(int value) {
		this.value = value;
	}

	public MutableInteger() {
		this(0);
	}

	public Object clone() {
		return new MutableInteger(value);
	}

	public void copy(Object obj) {
		MutableInteger m = (MutableInteger) obj;
		m.value = value;
	}

	public void reuse(int value) {
		this.value = value;
	}

	public void reset() {
		value = 0;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public void add(int i) {
		value += i;
	}

	public void inc() {
		value++;
	}

	public int hashCode() {
		return value;
	}

	public boolean equals(Object obj) {
		MutableInteger mi;
		if (obj == null || !(obj instanceof MutableInteger)) {
			return false;
		}
		mi = (MutableInteger) obj;
		return (hashCode() == mi.hashCode());
	}

	public String toString() {
		return String.valueOf(value);
	}


	/*+**********************************************************************
	 * Storable implementation
	 ************************************************************************/

	public void store(DataOutput out) throws IOException {
		out.writeInt(value);
	}

	public void load(DataInput in) throws IOException {
		value = in.readInt();
	}

	public int byteSize() {
		return 4;
	}

}