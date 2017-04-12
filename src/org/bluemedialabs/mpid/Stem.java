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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * <p>Maintains the mapping between a term stem and all of its related terms.
 * </p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Stem {
	String name;
	List terms;

	public Stem(String name, Term term) {
		this.name = name;
		terms = new LinkedList();
		terms.add(term);
	}

	public String getName() {
		return name;
	}

	public void addTerm(Term term) {
		terms.add(term);
	}

	public List getTerms() {
		return terms;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer(name.length() + terms.size() * 20);
		Iterator it = terms.iterator();

//		buf.append("(");
		buf.append(name);
		buf.append(" (");
		buf.append(it.next());  // we know that there is at least one term!
		while (it.hasNext()) {
			buf.append(", ");
			buf.append(it.next());
		}
		buf.append(")");
		return buf.toString();
	}
}