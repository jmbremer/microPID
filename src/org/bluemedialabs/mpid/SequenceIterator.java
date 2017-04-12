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

import java.io.IOException;
import org.bluemedialabs.io.DbIterator;
import org.bluemedialabs.util.Queue;


/**
 * <p>NOT FINISHED YET!!!</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class SequenceIterator implements DbIterator {
	private Queue q =  new Queue();
	private DbIterator currentIt = null;


	public SequenceIterator() {}


	public void finalize() {
		try {
			closeAll();
		} catch (IOException e) {
			// Nothing to be done about it...
			System.out.println("Unexplainable problems closing sequence "
							   + "iterators");
		}
	}

	public void closeAll() throws IOException {
		if (currentIt != null) {
			currentIt.close();
			currentIt = null;
		}
		while (!q.isEmpty()) {
			currentIt = (DbIterator) q.dequeue();
			currentIt.close();
		}
	}


	public void addIterator(DbIterator it) {
		assert (it != null);
		if (currentIt == null) {
			// This is the first iterator that is added
			currentIt = it;
		} else {
			q.enqueue(it);
		}
	}

	private void checkState() {
		if (currentIt == null) {
			throw new IllegalStateException("Cannot execute any operation on "
					+ "a sequence iterator before the first iterator has been "
					+ "added");
		}
	}

	public boolean hasNext() {
		/**@todo Implement this java.util.Iterator method*/
		throw new java.lang.UnsupportedOperationException("Method hasNext() not yet implemented.");
	}
	public Object next() {
		/**@todo Implement this java.util.Iterator method*/
		throw new java.lang.UnsupportedOperationException("Method next() not yet implemented.");
	}

	public void remove() {
		checkState();
		currentIt.remove();
	}


	public int skip(int n) throws IOException {
		checkState();

		return -42;
	}

	public void close() throws IOException {
		closeAll();
	}

}