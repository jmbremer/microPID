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
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class FlagMatrix {
	private FlagSet flags;
	private int len;

	/**
	 *
	 * @param size The matrix has square size (len x len)
	 */
	public FlagMatrix(int len) {
		this.len = len;
		flags = new FlagSet(len * len);
	}


	/**
	 * Sets the flag at the given position.
	 */
	public void set(int x, int y) {
		flags.set(getFlagSetPos(x, y));
	}

	private int getFlagSetPos(int x, int y) {
		return (len * y + x);
	}


	/**
	 * Clears the flag at the given position.
	 */
	public void clear(int x, int y) {
		flags.clear(getFlagSetPos(x, y));
	}

	/**
	 * Clears all flags.
	 */
	public void clearAll() {
		flags.clearAll();
	}

	/**
	 * Checks whether the flag at the given position is set.
	 *
	 * @param pos
	 */
	public boolean test(int x, int y) {
		return flags.test(getFlagSetPos(x, y));
	}


	protected void printFlag(int x, int y) {
		System.out.println("Flag (" + x + ", " + y + ") is "
			+ (test(x, y)? "set": "not set"));
	}



	static public void main(String[] args) {
	}
}