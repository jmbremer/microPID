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


/**
 * <p>A simple wrapper around a double that--unlike Double--allows to change the
 * underlying value. Mutable floats are particularly useful for hash tables
 * that need to include changeable ints.</p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class MutableDouble implements Cloneable, Reuseable {
	/**
	 * The actual underlying int value.
	 */
	public double value;

	/**
	 * Constructs a new mutable integer with the given int value.
	 */
	public MutableDouble(double value) {
		this.value = value;
	}

	public MutableDouble() {
		this(0.0);
	}

	public Object clone() {
		return new MutableDouble(value);
	}

	public void reuse(double value) {
		this.value = value;
	}

	public void reset() {
		value = 0;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public double getValue() {
		return value;
	}

	public void add(double f) {
		value += f;
	}

	public int hashCode() {
		return (int) value; //:-(
	}

	public boolean equals(Object obj) {
		MutableDouble mi;
		if (obj == null || !(obj instanceof MutableDouble)) {
			return false;
		}
		mi = (MutableDouble) obj;
		return (value == mi.value);
	}
}