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
 * <p>General interface for queues. Shouldn't take a too high IQ to understand
 * ;-)</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public interface IQueue {

	/**
	 * Enqueues an element at the end of the queue.
	 *
	 * @param obj
	 */
	public void enqueue(Object obj);

	/**
	 * Removes and returns the first element in the queue.
	 *
	 * @return
	 */
	public Object dequeue();

	/**
	 * Removes all elements from the queue.
	 */
	public void clear();

	/**
	 * Checks whether there are any elements enqueued right now.
	 * @return
	 */
	public boolean isEmpty();

	/**
	 * Returns the total number of currently enqueued elements.
	 *
	 * @return
	 */
	public int size();
}