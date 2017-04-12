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
 * <p>A queue of longs that uses an internal, dynamically resized array and
 * avoids the overhead of storing the longs as objects,
 * e.g., Long or MutableLong.</p>
 * <p>RIGHT NOW THE QUEUE IS ONLY USEFUL FOR ENQUEUING, ....</P>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class LongQueue implements IQueue {
	static public final int DEFAULT_INITIAL_CAPACITY  = 100;
	static public final float DEFAULT_INCREASE_FACTOR = (float) 1.1;

	private long[] queue;
	private int qsize = 0;      // # of actually stored elements
	private int qhead = 0;
	private float increaseFactor = DEFAULT_INCREASE_FACTOR;
	private MutableLong ml = new MutableLong();


	public LongQueue(int capacity) {
		queue = new long[capacity];
	}

	public LongQueue() {
		this(DEFAULT_INITIAL_CAPACITY);
	}


	public void enqueue(Object obj) {
		assert (obj instanceof MutableLong);
		if (qsize >= queue.length) {
			// Need to increase capacity
			increaseCapacity();
		}
		queue[qsize++] = ((MutableLong) obj).getValue();
	}

	private void increaseCapacity() {
		int newCapacity = Math.max(queue.length + 1,
								   (int) (queue.length * increaseFactor));
		long[] q = new long[newCapacity];
		System.arraycopy(queue, 0, q, 0, queue.length);
		queue = q;
	}

	public void enqueueLong(long l) {
		if (qsize >= queue.length) {
			// Need to increase capacity
			increaseCapacity();
		}
		queue[qsize++] = l;
	}

	/**
	 * Removes and returns the first element in the queue.
	 *
	 * @return
	 */
	public Object dequeue() {
		assert (qsize > qhead);
		ml.setValue(queue[qhead++]);
		return ml;
	}

	public long dequeueLong() {
		assert (qsize > qhead);
		return queue[qhead++];
	}


	/**
	 * Removes all elements from the queue.
	 */
	public void clear() {
		qsize = 0;
		qhead = 0;
	}

	/**
	 * Checks whether there are any elements enqueued right now.
	 * @return
	 */
	public boolean isEmpty() {
		return (qsize == qhead);
	}

	/**
	 * Returns the total number of currently enqueued elements.
	 *
	 * @return
	 */
	public int size() {
		return (qsize - qhead);
	}


	public void setCapacity(int newCapacity) {
		assert (newCapacity >= qsize);
		if (newCapacity != queue.length) {
			long[] q = new long[newCapacity];
			System.arraycopy(queue, 0, q, 0, qsize);
			// Also move elements to the very beginning of the array...

			queue = q;
		}
	}

	public int capacity() {
		return queue.length;
	}


	public void setIncreaseFactor(float f) {
		assert (f > 1);
		increaseFactor = f;
	}

	public float getIncreaseFactor() {
		return increaseFactor;
	}
}