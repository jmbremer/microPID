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

import java.io.*;
import java.util.Comparator;
import org.bluemedialabs.io.Storable;


/**
 * <p>A general term identified by its string representation, here also called
 * <em>name</em>. Term objects are independent of the content they occur in.</p>
 * <p>A term can be a word as it is found in a text or a word root build by
 * stemming mulitple semantically equivalent words.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Term implements Cloneable, Comparable, Serializable,
		Storable {
	static public final CountComparator COUNT_COMPARATOR =
			new CountComparator();
	static public final NumberComparator NUMBER_COMPARATOR =
			new NumberComparator();
	static public final LexicalComparator LEXICAL_COMPARATOR =
			new LexicalComparator();
//	static public final CodeLenComparator CODE_LENGTH_COMPARATOR =
//			new CodeLenComparator();

	private String name = null; // "name" of the term (the actual term)
	private int no = -1;        // internal number
	private int count = 0;      // total number of times the term has been seen
//	protected byte huffCodeLen = 0; // # of bits required to encode this term


	/**
	 * Constructs a new term with the given name.
	 *
	 * @param name The term's string representation.
	 */
	public Term(String name, int no, int count) {
		this.name = name.intern();
		this.no = no;
		this.count = count;
	}

	/**
	 * Constructs a new empty term. (Needed for loading a term from a stream,
	 * for instance.)
	 */
	public Term() {}

	// Why would anyone clone a term as it is read-only anyway!??
	public Object clone() {
		return new Term(name, no, count);
	}

	/**
	 * Returns the term's name--its string representation.
	 */
	public String getName() {
		return name;
	}

	protected void setNo(int no) {
		this.no = no;
	}

	public int getNo() {
		return no;
	}

	protected void setCount(int c) {
		count = c;
	}
	protected void incCount(int inc) {
		count += inc;
	}
	protected void incCount() {
		count++;
	}
	public int getCount() {
		return count;
	}

	public int hashCode() {
		return no;
	}

	/**
	 * Returns a string representation of this Term object. Same as {@link
	 * #getName}.
	 *
	 * @returns The string representation of this object.
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer(60);
		buf.append("(");
		buf.append(no);
		buf.append(", ");
		buf.append(name);
		buf.append(", count=");
		buf.append(count);
		buf.append(")");
		return buf.toString();
	}


/*+**************************************************************************
 * Comparable implementation
 ****************************************************************************/

	/**
	 * <p>Compares this and the supplied object based on their string
	 * representation. The latter one is supposed to
	 * be a Term. However, any object with a string representation will do.
	 * Thus, all object types can be supplied whether it makes sense or not.</p>
	 *
	 * @param obj The object to compare us to.
	 * @returns the String comparisons result between the two objects
	 */
	public int compareTo(Object obj) {
		// throws a NullPointerException (intentionally) if obj is null
		return name.compareTo(obj.toString());
	}


/*+**************************************************************************
 * Comparator implementation
 ****************************************************************************/

	/*
	 * Comparator implementation.
	 * <p>Term implements comparator to have a way to compare derived terms like
	 * QueryTerm and RankedTerm according to their lexicographical order. That
	 * would not be posible automatically otherwise, because the overwritten
	 * compareTo would always be used for comparison.</p>
	 * <p>Note that Comparator's equals method has already been implemented
	 * above.</p>
	 */

	/**
	 * Compares the given Term objects lexicographically.
	 */
//	public int compare(Object obj1, Object obj2) {
//		Term t1 = (Term) obj1;
//		Term t2 = (Term) obj2;
//		return t1.getName().compareTo(t2.getName());
//	}

	// This is nonsense, isn't it!?? :
	/**
	 * Checks whether two <em>terms</em> are identical based on their name.
	 *
	 * @param obj The obj to compare this term to.
	 * @returns If both the given object is a term with the same name.
	 */
//	public boolean equals(Object obj) {
//		if (obj == null) {
//			return false;
//		}
//		Term term = (Term) obj;
//		// this will result in a ClassCastException if obj is not a term;
//		// that is intended
//		return (name.compareTo(term.getName()) == 0);
//	}

/*+**************************************************************************
 * Serializable & Storable implementation
 ****************************************************************************/

	public void store(DataOutput out) throws IOException {
		out.writeInt(count);
		out.writeUTF(name);
		/*
		 * Note that we do not write the term# here! We expect the number to
		 * be deducible from the position the term is stored at!
		 */
	}

	public void load(DataInput in) throws IOException {
		count = in.readInt();
//		try {
			name = in.readUTF();
//		} catch (UTFDataFormatException e) {
//			name = "<UTF load error>";
//		}
		/*
		 * The application has to set the term#!
		 */
	}

	public int byteSize() {
		return -1;
	}


	private void writeObject(ObjectOutputStream out) throws IOException {
		store(out);
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		load(in);
	}


/*+**************************************************************************
 * All available comparators
 ****************************************************************************/

	static private class CountComparator implements Comparator {

		public int compare(Object obj1, Object obj2) {
			Term t1 = (Term) obj1;
			Term t2 = (Term) obj2;
			if (t1 == null || t2 == null) {
				throw new NullPointerException("Trying to compare null terms ("
					+ "term 1 is '" + t1 + "', term 2 is '" + t2 + "'");
			}
			int c1 = t1.getCount();
			int c2 = t2.getCount();
			// Note that we want large counters to appear earlier
			if (c1 > c2) {
				return -1;
			} else if (c1 < c2) {
				return 1;
			} else {
				return 0;
			}
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj instanceof CountComparator) {
				return true;
			} else {
				return false;
			}
		}
	}


	/**
	 * A purely lexicographical term comparator. Allows subclasses that by
	 * default use a more sophisticated comparison (e.g. {@link RankedTerm}) to
	 * be sorted lexicographically.
	 */
	static private class LexicalComparator implements Comparator {

		public int compare(Object obj1, Object obj2) {
			Term t1 = (Term) obj1;
			Term t2 = (Term) obj2;
			return t1.compareTo(t2);
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj instanceof LexicalComparator) {
				return true;
			} else {
				return false;
			}
		}
	}


	static public class NumberComparator implements Comparator {
		public int compare(Object obj1, Object obj2) {
			Term t1 = (Term) obj1;
			Term t2 = (Term) obj2;
			int c1 = t1.getNo();
			int c2 = t2.getNo();
			if (c1 < c2) {
				return -1;
			} else if (c1 > c2) {
				return 1;
			} else {
				return 0;
			}
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj instanceof NumberComparator) {
				return true;
			} else {
				return false;
			}
		}
	}


/*
	static public class CodeLenComparator implements Comparator {
		public int compare(Object obj1, Object obj2) {
			Term t1 = (Term) obj1;
			Term t2 = (Term) obj2;
			byte l1 = t1.huffCodeLen;
			byte l2 = t2.huffCodeLen;
			if (l1 < l2) {
				return -1;
			} else if (l1 > l2) {
				return 1;
			} else {
				// On equal length the previously assigned number determines
				// the order. The reasoning here is to preserve the order
				// from high-frequency to low frequence terms as far as
				// possible.
				int c1 = t1.getCount();
				int c2 = t2.getCount();
				if (c1 > c2) {
					return -1;
				} else if (c1 < c2) {
					return 1;
				} else {
					// Both length and frequence are the same, thus let the
					// lexicographical order decide
					return t1.getName().compareTo(t2.getName());
				}
			}
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj instanceof NumberComparator) {
				return true;
			} else {
				return false;
			}
		}
	}
*/

}