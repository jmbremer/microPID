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
//import gui.TableRow;
import org.bluemedialabs.io.Storable;
import org.bluemedialabs.util.MutableInteger;


/**
 * <p>A token as found in the original XML input. Tokens have a 1:1 relationship
 * with atomic syntactical constructs from the input data with the exception of
 * spaces. Tokens are used for (Huffmann) encoding.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Token implements Cloneable, Codeables.Codeable, Comparable<String>,
		Serializable, Storable/*, TableRow*/ {
	static public final CountComparator COUNT_COMPARATOR =
			new CountComparator();
//	static public final NumberComparator NUMBER_COMPARATOR =
//			new NumberComparator();
	static public final LexicalComparator LEXICAL_COMPARATOR =
			new LexicalComparator();
	static public final CodeLenComparator CODE_LENGTH_COMPARATOR =
			new CodeLenComparator();

	static private MutableInteger mutInt = new MutableInteger();

//	static public final int TEXT_TYPE        = 0;
//	static public final int ELEMENT_TYPE     = 1;
//	static public final int ATTRIBUTE_TYPE   = 2;

	private String name = null;     // String representation of the token
//	private byte type = 0;
	private int count = 0;          // Total occurrence count for the token
	protected byte huffCodeLen = 0; // # of bits required to encode this Token


	/**
	 * Constructs a new token with the given string representation.
	 *
	 * @param name The Token's string representation.
	 */
	public Token(String name, int count) {
//		if (type < 0 || type > 2) {
//			throw new IllegalArgumentException
		this.name = name.intern();
		this.count = count;
	}

	/**
	 * Constructs a new empty Token. (Needed for loading a Token from a stream,
	 * for instance.)
	 */
	public Token() {}


	// Why would anyone clone a Token as it is read-only anyway!??
	public Object clone() {
		return new Token(name, count);
	}


	/**
	 * Returns the Token's name--its string representation.
	 */
	public String getName() {
		return name;
	}

//	public void setCount(int c) {
//		count = c;
//	}
	protected void incCount(int inc) {
		count += inc;
	}
	protected void incCount() {
		count++;
	}
	public int getCount() {
		return count;
	}

	public void setCodeLen(byte len) {
		huffCodeLen = len;
	}
	public byte getCodeLen() {
		return huffCodeLen;
	}

//	public int hashCode() {
//		return no;
//	}

	/**
	 * Returns a string representation of this Token object. Same as {@link
	 * #getName}.
	 *
	 * @returns The string representation of this object.
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer(60);
		buf.append("('");
		buf.append(name);
		buf.append("', count=");
		buf.append(count);
		buf.append(", hclen=");
		buf.append(huffCodeLen);
		buf.append(")");
		return buf.toString();
	}


/*+**************************************************************************
 * Comparable implementation
 ****************************************************************************/

	/**
	 * <p>Compares this and the supplied object based on their string
	 * representation. The latter one is supposed to
	 * be a Token. However, any object with a string representation will do.
	 * Thus, all object types can be supplied whether it makes sense or not.</p>
	 *
	 * @param obj The object to compare us to.
	 * @returns the String comparisons result between the two objects
	 */
	public int compareTo(String str) {
		// throws a NullPointerException (intentionally) if obj is null
		return name.compareTo(str);
	}


/*+**************************************************************************
 * Serializable & Storable implementation
 ****************************************************************************/

	public void store(DataOutput out) throws IOException {
		out.writeInt(count);
		out.writeByte(huffCodeLen);
		out.writeUTF(name);
	}

	public void load(DataInput in) throws IOException {
		count = in.readInt();
		huffCodeLen = in.readByte();
		name = in.readUTF();
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
 * TableRow implementation
 ****************************************************************************/

	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int col) {
		String str;
		switch (col) {
			case 0: str = "Name"; break;
			case 1: str = "Count"; break;
			case 2: str = "C-len"; break;
			default: str = null;
		}
		return str;
	}

	public Object getValueAt(int column) {
		Object obj;
		switch (column) {
			case 0: obj = name; break;
			case 1: mutInt.setValue(count); obj = mutInt; break;
			case 2: mutInt.setValue(huffCodeLen); obj = mutInt; break;
			default: obj = null;
		}
		return obj;
	}

	public Comparator getComparator(int column) {
		Comparator comp;
		switch (column) {
			case 0: comp = LEXICAL_COMPARATOR; break;
			case 1: comp = COUNT_COMPARATOR; break;
			case 2: comp = CODE_LENGTH_COMPARATOR; break;
			default: comp = null;
		}
		return comp;
	}


/*+**************************************************************************
 * All available comparators
 ****************************************************************************/

	/**
	 * Compares two tokens solely based on their frequence.
	 */
	static private class CountComparator implements Comparator {
		public int compare(Object obj1, Object obj2) {
			Token t1 = (Token) obj1;
			Token t2 = (Token) obj2;
			if (t1 == null || t2 == null) {
				throw new NullPointerException("Trying to compare null tokens ("
					+ "token 1 is '" + t1 + "', token 2 is '" + t2 + "'");
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
	 * A purely lexicographical Token comparator. Allows subclasses that by
	 * default use a more sophisticated comparison (e.g. {@link RankedToken}) to
	 * be sorted lexicographically. Based on Token's compareTo() method.
	 */
	static private class LexicalComparator implements Comparator<Token> {
		public int compare(Token t1, Token t2) {
			return t1.name.compareTo(t2.name);
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


/*	static public class NumberComparator implements Comparator {
		public int compare(Object obj1, Object obj2) {
			Token t1 = (Token) obj1;
			Token t2 = (Token) obj2;
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
	}*/


	/**
	 * Compares two tokens based on the following criteria (in sequence of their
	 * importance): huffman code length, occurrence frequence, lexicographical.
	 */
	static public class CodeLenComparator implements Comparator {
		public int compare(Object obj1, Object obj2) {
			Token t1 = (Token) obj1;
			Token t2 = (Token) obj2;
			byte l1 = t1.huffCodeLen;
			byte l2 = t2.huffCodeLen;
			if (l1 < l2) {
				return -1;
			} else if (l1 > l2) {
				return 1;
			} else {
				// On equal length the occurrence frequence determines
				// the order. The reasoning here is to preserve the order
				// from high-frequency to low frequence tokens as far as
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
			if (obj instanceof CodeLenComparator) {
				return true;
			} else {
				return false;
			}
		}
	}

}