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
 * A simple queue. <em>Class to be eliminated in favor of standard library
 * classes at some point.</em>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Queue implements IQueue {
	 protected Item first;
	 protected Item last;
	 protected Item firstFree = null;
	 protected int qsize = 0;

	 class Item {
		  protected Object value;
		  protected Item next;

		  public Item(Object value, Item next) {
				this.value = value;
				this.next = next;
		  }
	 }

	 public Queue() {
		  super();
	 }

	 public void enqueue(Object item) {
		  Item tmp;
		  if (last == null) {
				last = allocateItem(item, null);
				first = last;
		  } else {
				tmp = allocateItem(item, null);
				last.next = tmp;
				last = tmp;
		  }
		  qsize++;
	 }

	 protected Item allocateItem(Object value, Item next) {
		Item tmp;
		if (firstFree == null) {
			// No unused items available so far
			return new Item(value, next);
		} else {
			tmp = firstFree;
			firstFree = firstFree.next;
			tmp.value = value;
			tmp.next = next;
			return tmp;
		}
	}

	public Object dequeue() {
		Object obj = null;
		Item tmp;
		if (first != null) {
			obj = first.value;
			if (last == first) {
				last = null;
			}
			tmp = first;
			first = first.next;
			qsize--;
			// Add unused item to free list
			deallocateItem(tmp);
		  }
		  return obj;
	 }

	 protected void deallocateItem(Item item) {
		item.next = firstFree;
		firstFree = item;
	}

	 public void clear() {
		Item tmp;
		while (first != null) {
			tmp = first;
			first = first.next;
			deallocateItem(tmp);
		}
		qsize = 0;
	}


	 public boolean isEmpty() {
		  return (first == null);
	 }

	 public int getSize() {
		// only temporarily: count actual qsize
//		int qsize = 0;
//		Item t = first;
//		while (t != null && qsize <= 20) {
//			qsize++;
//			t = t.next;
//		}
		return qsize;
	}

	public int size() {
		return qsize;
	}

	public String toString() {
		Item item = first;
		StringBuffer buf = new StringBuffer(256);

		  buf.append("(");
		  if (item != null) {
			buf.append(item.value);
				item = item.next;
				while (item != null) {
					 buf.append(", ");
					 buf.append(item.value);
					 item = item.next;
				}
		  }
		  buf.append(", size=");
		  buf.append(getSize());
		  buf.append(", freeList=(");
		  item = firstFree;
		if (item != null) {
			buf.append(item.value);
			item = item.next;
			while (item != null) {
				buf.append(", ");
				buf.append(item.value);
				item = item.next;
			}
		}
		  buf.append("))");
		  return buf.toString();
	 }


	static public void main(String[] args) throws Exception {
		Queue q = new Queue();

		q.enqueue("A");
		q.enqueue("B");
		q.enqueue("C");
		q.dequeue();
		q.enqueue("D");
		System.out.println(q);
		q.dequeue();
		q.dequeue();
		q.enqueue("E");
		System.out.println(q);
	}
}