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

import java.lang.reflect.Array;


/**
 * <p>Creates a resource pool of objects of the initially supplied type. The
 * resource object must be Cloneable in order for the pool to create more
 * resources if demand requires.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Pool /*extends MyPrint*/ {
	static public final int DEFAULT_INITIAL_CAPACITY = 10;
	static public final float INCREASE_FACTOR = 2;  // Don't set too small!

	private Object[] objects;
	private Class objClass; // The class object to obtain more objects from
	private int size;       // # of currently pre-allocated objects
	private int used;       // # of currently claimed objects


	/**
	 * Constructs a new object pool of the given object type. Note that the
	 * supplied object is only used for creating the intial object. This object
	 * is in the following used to clone more objects on demand.
	 */
	public Pool(Object object, int initialCapacity) {
		objClass = object.getClass();
		objects = new Object[initialCapacity];
		try {
			for (int i = 0; i < initialCapacity; i++) {
				objects[i] = objClass.newInstance();
			}
		} catch (IllegalAccessException e) {
			throw new NullPointerException("Cannot allocate new pool of type "
				+ objClass.toString() + " (" + e + ")");
		} catch (InstantiationException e2) {
			throw new NullPointerException("Cannot allocate new pool of type "
				+ objClass.toString() + " (" + e2 + ")");
		}
		size = initialCapacity;
		used = 0;
	}

	public Pool(Object object) {
		this(object, DEFAULT_INITIAL_CAPACITY);
	}

	public void finalize() {
		for (int i = 0; i < size; i++) {
			objects[i] = null;
		}
	}

	public Object claim() {
		Object[] newObjects;
//		Object obj;

		if (used >= size) {
			// Need to allocate more objects
			if (size >= objects.length) {
				// Need to increase array capacity first
				newObjects = new Object[(int) (objects.length * INCREASE_FACTOR)];
//						(Object[]) Array.newInstance(objects[0].getClass(),
//						(int) (objects.length * INCREASE_FACTOR));
				for (int i = 0; i < objects.length; i++) {
					newObjects[i] = objects[i];
				}
				// But: don't preallocate objects here!
				objects = newObjects;
			}
			try {
					objects[size++] = objClass.newInstance();
			} catch (IllegalAccessException e) {
					throw new NullPointerException("Cannot allocate new pool "
							+ "object of type "
							+ objClass.toString() + " (" + e + ")");
			} catch (InstantiationException e2) {
					throw new NullPointerException("Cannot allocate new pool "
							+ "object of type "
							+ objClass.toString() + " (" + e2 + ")");
			}
		}

//		try {
//			objects[used] = queen.newInstance();
//		} catch (IllegalAccessException e) {
//			throw new NullPointerException("Cannot allocate new pool object of type "
//				+ queen.toString() + " (" + e + ")");
//		} catch (InstantiationException e2) {
//			throw new NullPointerException("Cannot allocate new pool object of type "
//				+ queen.toString() + " (" + e2 + ")");
//		}

		return objects[used++];
	}

	public void releaseAll() {
		used = 0;
	}

	public int getSize() {
		return size;
	}

	public int getUsed() {
		return used;
	}

	public int getCapacity() {
		return objects.length;
	}

	public String toString() {
		StringBuffer buf =
			new StringBuffer(objClass.toString().length() * used + 100);

		buf.append("(used=");
		buf.append(used);
		buf.append(", size=");
		buf.append(size);
		buf.append(", capacity=");
		buf.append(objects.length);
//		if (used > 0) {
//			buf.append(", usedObjs=(");
//			buf.append(objects[0].toString());
//			for (int i = 1; i < used; i++) {
//				buf.append(", ");
//				buf.append(objects[i].toString());
//			}
//			buf.append(")");
//		}
		buf.append(")");
		return buf.toString();
	}


/*
	static public void main(String[] args) throws Exception {
		StopWatch watch = new StopWatch();
		MutableInteger m = new MutableInteger(), n;
		Pool p = new Pool(m, 1000);

		printMemInfo();
		watch.start();
		for (int i = 0; i < 1000000; i++) {
			n = (MutableInteger) p.claim();
			n.setValue(i);
		}
		watch.stop();
		pl("Pool after claiming 1,000,000 objects: " + p);
		printMemInfo();
		pl("Time: " + watch);
		pressKey();
		watch.reset();
		watch.start();
		for (int j = 0; j < 100; j++) {
			p.releaseAll();
			int v = MyMath.random(1000000, 2000000);
			for (int i = 0; i < v; i++) {
				n = (MutableInteger) p.claim();
				n.setValue(i);
			}
		}
		watch.stop();
		pl("Pool after doing this several times: " + p);
		printMemInfo();
		pl("Time: " + watch);
		pressKey();
	}

	static void printMemInfo() {
		Runtime r = Runtime.getRuntime();

		pl();
		pl("Total memory...." + r.totalMemory());
		pl("Max memory......" + r.maxMemory());
		pl("Free memory....." + r.freeMemory());
	}

	static void pressKey() throws Exception {
		pl();
		p("Please press a key to continue...");
		System.in.read();
		pl();
	}
*/
}