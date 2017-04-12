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
 * <p>Wrapper class around a boolean value. This is the most recent class in
 * the set of Mutable... classes. All other related classes should be updated
 * to follow this class (in particular, they all should implement
 * CloneableObject now).</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class MutableBoolean implements CloneableObject, Reuseable, Storable {
	static public final boolean DEFAULT_VALUE = true;

	/**
	 * The actual underlying int value.
	 */
	public boolean value;

	/**
	 * Constructs a new mutable boolean with the given int value.
	 */
	public MutableBoolean(boolean value) {
		this.value = value;
	}

	/**
	 * Constructs a new mutable boolen with default value DEFAULT_VALUE.
	 */
	public MutableBoolean() {
		this(DEFAULT_VALUE);
	}

	public Object clone() {
		return new MutableBoolean(value);
	}

	public void copy(Object obj) {
		MutableBoolean mb = (MutableBoolean) obj;
		mb.setValue(value);
	}

	public void reuse(boolean value) {
		this.value = value;
	}

	public void reset() {
		value = DEFAULT_VALUE;
	}

	public void setValue(boolean value) {
		this.value = value;
	}

	public boolean getValue() {
		return value;
	}

	public void or(MutableBoolean mb) {
		value = (value || mb.getValue());
	}

	public void and(MutableBoolean mb) {
		value = (value || mb.getValue());
	}

	public void not() {
		value = ! value;
	}

	public int hashCode() {
		return (value? 1: 0);
	}

	public boolean equals(Object obj) {
		MutableBoolean mb;
		if (obj == null || !(obj instanceof MutableBoolean)) {
			return false;
		}
		mb = (MutableBoolean) obj;
		return (value == mb.getValue());
	}

	public String toString() {
		return String.valueOf(value);
	}


	/*+**********************************************************************
	 * Storable implementation
	 ************************************************************************/

	public void store(DataOutput out) throws IOException {
		out.write((byte) (value? 1: 0));
	}

	public void load(DataInput in) throws IOException {
		byte b = in.readByte();
		value = (b != 0);
	}

	public int byteSize() {
		return 1;
	}
}