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
package org.bluemedialabs.io;

import java.io.IOException;
import java.util.Iterator;
//import org.bluemedialabs.util.MutableLong;


/**
 * <p>...</p>
 * <p><em>Yes, we really want an Object iterator at this point as much of the
 * implementation relies on the fact that next() can return different types.
 * Like it or not.</em></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public interface DbIterator extends Iterator<Object> {

	/**
	 * Skips the next <em>n</em> elements in this iterator, if there are that
	 * many elements left to skip. Otherwise skips only the number of remaining
	 * elements.
	 *
	 * @param n
	 * @returns The number of elements skipped.
	 * @throws IOException
	 */
	public int skip(int n) throws IOException;

	/**
	 *
	 * @throws IOException
	 */
	public void close() throws IOException;

}