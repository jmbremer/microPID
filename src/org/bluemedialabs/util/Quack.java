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
 * <p>A combination of Queue and Stack. Allows to stack elements but also to
 * enqueue them at the end of the current stack.</p>
 * <p>For Java 1.x compatibility reasons, none of the new Java 1.2 classes like
 * LinkedList or Stack are used for the implementation. <em>Clearly, this
 * doesn't apply anymore. So, this class should be eliminated in favor of
 * standard library classes.</em></p
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Quack extends Queue {

	 public Quack() {
		  super();
	 }

	 public void push(Object obj) {
		  if (first == null) {
				first = allocateItem(obj, null);
				last = first;
		  } else {
				first = allocateItem(obj, first);
		  }
		  qsize++;
	 }

	 public Object pop() {
		  return dequeue();
	 }

	 public Object top() {
		return first.value;
	}


	static public void main(String[] args) throws Exception {
		Quack q = new Quack();

		q.push("A"); p(q);
		q.push("B"); p(q);
		q.push("C"); p(q);
		q.pop(); p(q);
		q.push("D"); p(q);
//		System.out.println(q);
		q.pop(); p(q);
		q.pop(); p(q);
		q.push("E"); p(q);
//		System.out.println(q);
		System.out.println(q.getSize());
		for (int i = 1; i <= 100; i++) {
			if (i % 10 == 0) {
				q.pop();
			} else {
				q.push(new Integer(i));
			}
		}
		System.out.println(q.getSize());
		p(q);
	}

	static private void p(Quack q) {
		System.out.println(q);
	}
}
