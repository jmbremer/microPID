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

import org.bluemedialabs.util.MutableString;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public abstract class Stemmer {

	/**
	 * Returns the length of the stemmed string without modifying the supplied
	 * characters in any way.
	 */
	public abstract int stem(char[] str, int offset, int len);

	public int stem(char[] str, int len) {
		return stem(str, 0, len);
	}

	public int stem(char[] str) {
		return stem(str, 0, str.length);
	}

	/**
	 * Same as stem() based on a character array. The string is not modified.
	 */
	public abstract int stem(MutableString str);

	public abstract int stem(String str);
}