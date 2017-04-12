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

import java.io.*;
import java.nio.CharBuffer;
import java.util.Comparator;
import org.bluemedialabs.io.ReadableDataChannel;
import org.bluemedialabs.io.Storable;
import org.bluemedialabs.io.WritableDataChannel;


/**
 * <p>...</p>
 * <p><em>The prior implementation of Comparable was more elegant in that the
 * comparison based on the Object type allowed to compare MutableStrings to both
 * other MutableStrings and regular String objects. Now, implicit comparison is
 * only amongst MutableString objects. Hope that doesn't break anything.</em>
 * </p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class MutableString implements Cloneable, Comparable<MutableString>, 
    /*Comparable<String>,*/ Comparator<MutableString>, Serializable, Storable {
	private char[] text;
	private int offset;
	private int len;    // actual length in wrapper and non-wrapper mode
	private boolean wrapper;
//	private int size = -1;   // size in bytes determ. during last store operation
//	private CharBuffer loadBuffer = CharBuffer.allocate(256); // for load!

	// Not needed anymore/yet.
	static public int calcUtfSize(String str) {
		int size = 2;   // Length filed
		char c;

		for (int i = 0; i < str.length(); i++) {
			c = str.charAt(i);
			if (c <= '\u007F') {
				 if (c == '\u0000') {
					 size += 2;
				 } else {
					 size++;
				 }
			} else if (c <= '\u07FF') {
				size += 2;
			} else {
				size += 3;
			}
		}
		return size;
	}

	// Not needed anymore/yet.
	static public int calcUtfSize(MutableString str) {
		int size = 2;   // Length filed
		char c;

		for (int i = 0; i < str.length(); i++) {
			c = str.charAt(i);
			if (c <= '\u007F') {
				 if (c == '\u0000') {
					 size += 2;
				 } else {
					 size++;
				 }
			} else if (c <= '\u07FF') {
				size += 2;
			} else {
				size += 3;
			}
		}
		return size;
	}


	public MutableString(char[] text, int offset, int count) {
		if (text == null && (offset != 0 || count != 0)) {
			throw new IllegalArgumentException("Invalid indexes (offset="
				+ offset + ", len=" + count + ") for null text");
		} else if (text != null && text.length < offset + count) {
			throw new IllegalArgumentException("Cannot create mutable string "
				+ "in wrapper mode as supplied text array positions are "
				+ "invalid: text length is " + text.length + ", but start "
				+ "position plus length is " + (offset + count) + " (offset="
				+ offset + ", len=" + count + ")");
		}
		this.text = text;
		this.offset = offset;
		len = count;
		wrapper = true;
	}

	// Pretty inefficient...
	public MutableString(String str) {
		this(str.toCharArray(), 0, str.length());
		wrapper = false;
	}

	public MutableString(int size) {
		this(new char[size], 0, size);
		wrapper = false;
	}

	public MutableString() {
		this(null, 0, 0);
	}


	public Object clone() {
		return new MutableString(text, offset, len);
	}

	public void reuse(char[] text, int offset, int count) {
		if (text == null && (offset != 0 || count != 0)) {
			throw new IllegalArgumentException("Invalid indexes (offset="
				+ offset + ", len=" + count + ") for null text");
		} else if (offset < 0 || count < 0 || text.length < offset + count) {
			throw new IllegalArgumentException("Cannot create mutable string "
				+ "in wrapper mode as supplied text array positions are "
				+ "invalid: text length is " + text.length + ", but start "
				+ "position plus length is " + (offset + count) + " (offset="
				+ offset + ", len=" + count + ")");
		}
		this.text = text;
		this.offset = offset;
		this.len = count;
		wrapper = true;
	}

	public void reuse(String str) {
		if (isWrapper() || text.length < str.length()) {
			reuse(str.toCharArray(), 0, str.length());
			wrapper = false;
		} else {
			str.getChars(0, str.length(), text, 0);
		}
	}

	public void reset() {
		if (isWrapper()) {
			text = null;
		}
		offset = 0;
		len = 0;
	}


	public boolean isWrapper() {
		return wrapper;
	}

	public char charAt(int index) {
		if (index < 0 || index >= len) {
			throw new ArrayIndexOutOfBoundsException("Valid indices are 0.."
				+ (len - 1) + ", but supplied index is " + index);
		}
		return text[offset + index];
	}

	public void decLength(int newLen) {
		if (newLen > len || newLen < 0) {
			throw new IllegalArgumentException("Decrease length method may not "
				+ "be used to increas length, but current length is " + len
				+ " and requested length is " + newLen);
		}
		len = newLen;
	}

	public int length() {
		return len;
	}

	public void toLowerCase() {
		if (isWrapper()) {
			text = toCharArray();
			offset = 0;
			wrapper = false;
		}
		for (int i = offset; i < offset + len; i++) {
			text[i] = Character.toLowerCase(text[i]);
		}
	}


	public void append(MutableString str) {
		prepareAppend(str.length());
		// Just append string to existing array
		System.arraycopy(str.text, 0, text, len, str.length());
		len += str.length();
	}

	private void prepareAppend(int strlen) {
		char[] txt;
		int size =  len + strlen;
		if (wrapper || text.length < size) {
			// :-(  Need to create a completely new array
			txt = new char[size];
			System.arraycopy(text, offset, txt, 0, len);
			text = txt;
			offset = 0;
			wrapper = false;
		}
	}

	public void append(String str) {
		prepareAppend(str.length());
		// copy (append) supplied string (into now guaranteed long enough array)
		str.getChars(0, str.length(), text, len);
		len += str.length();
	}

	// wrapper mode by default!!!
	public MutableString substring(int beginIndex, int endIndex) {
		// check indices!!!
		return new MutableString(text, offset + beginIndex,
				endIndex - beginIndex);
	}

	public MutableString substring(int beginIndex) {
		return substring(beginIndex, offset + len);
	}

	// wrapper always, too
	public void trim() {

		// ...
	}


	/**
	 * TODO: 
	 * @see String.hashCode()
	 */
	public int hashCode() {
		if (len == 0) {
			return 0;
		}
		// Compute the polynomial in the most efficient way:
		// (What's this solution called again?)
		int sum = text[offset];
		int factor = 31;
		for (int i = offset + 1; i < offset + len; i++) {
			sum *= factor;
			sum += text[i];
		}
		return sum;
	}


	public char[] toCharArray() {
		char[] txt = new char[len];
		System.arraycopy(text, offset, txt, 0, len);
		return txt;
	}

	public String toString() {
		String str;
		if (text == null) {
			str = "".intern();
		} else {
			str = new String(text, offset, len);
		}
		return str;
	}


	/*+**********************************************************************
	 * Comparable and Comparator implementation
	 ************************************************************************/

	public int compareTo(MutableString str) {
		int r = offset;
		int s = str.offset;
		char[] text2 = str.text;
		int strlen  = str.length();
		while (r < offset + len && s < str.offset + str.len) {
			if (text[r] < text2[s]) {
				return -1;
			} else if (text[r] > text2[s]) {
				return 1;
			}
			r++; s++;
		}
		// Hmm, maybe the two objects are identical!?
		if (len < strlen) {
			return -1;
		} else if (len > strlen) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public int compareTo(String str) {
	    // Same as above
        int r = offset;
        int s = 0;
        int strlen = str.length();
        while (r < offset + len && s < strlen) {
            if (text[r] < str.charAt(s)) {
                return -1;
            } else if (text[r] > str.charAt(s)) {
                return 1;
            }
            r++;
            s++;
        }
        if (len < strlen) {
            return -1;
        } else if (len > strlen) {
            return 1;
        } else {
            return 0;
        }
	}
	
	public int compare(MutableString str1, MutableString str2) {
	    return (str1.compareTo(str2));
	}

	/**
	 * Checks for equality based on the string representation of the given
	 * object.
	 */
	public boolean equals(Object obj) {
		int r, s;
		if (obj instanceof MutableString) {
			MutableString str = (MutableString) obj;
			r = offset;
			s = str.offset;
			while (r < offset + len && s < str.offset + str.len) {
				if (text[r++] != str.text[s++]) {
					return false;
				}
			}
			return (len == str.len);
		} else {
			// Same thing, just with String's charAt()
			String str = obj.toString();
			r = offset;
			s = 0;
			while (r < offset + len && s < str.length()) {
				if (text[r++] != str.charAt(s++)) {
					return false;
				}
			}
			return (len == str.length());
		}
	}


	/*+*********************************************************************
	 * Serializable implementation
	 ***********************************************************************/

	 protected void writeObject(ObjectOutputStream out) throws IOException{
		// rather expensive for a write operation
		String str = new String(text, offset, len);
		out.writeUTF(str);
		// No need to write offset and wrapper...
	 }

	 protected void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		String str = in.readUTF();
		// Expensive and somewhat awkward! It seems better to just read
		// back character sequences as strings!
		text = str.toCharArray();
		len = str.length();
		offset = 0;
		wrapper = false;
	 }

	/*+*********************************************************************
	 * Storable implementation
	 ***********************************************************************/

	 /**
	  * Writes this mutable string to the supplied data output source. 
	  */
	public void store(DataOutput out) throws IOException {
		// EXPENSIVE!!!
		if (out instanceof WritableDataChannel) {
			CharBuffer buf = CharBuffer.wrap(text, offset, len);
			((WritableDataChannel) out).writeCharBuffer(buf);
		} else {
			out.writeUTF(new String(text, offset, len));
		}
	}

	/**
	 * Reads a string from the supplied data input source into this mutable
	 * string.
	 */
	public void load(DataInput in) throws IOException {
		// ALSO EXPENSIVE
		if (in instanceof ReadableDataChannel) {
			CharBuffer buf = CharBuffer.wrap(text, offset, len);
			((ReadableDataChannel) in).readCharBuffer(buf);
			buf.flip();
			len = buf.remaining();
		} else {
			String str = in.readUTF();
			len = str.length();
			if (wrapper || len > text.length) {
				// Bad luck, need to allocate new memory
				text = str.toCharArray();
				offset = 0;
			} else {
				// Can reuse our memory
				str.getChars(0, len, text, 0);
			}
		}
	}

	/**
	 * Returns the number of bytes the implementing object's state is encoded
	 * into when storing the object, or -1 if the object is of variable size.
	 */
	public int byteSize() {
		return -1;
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		String str = "This is a little test.";
		char[] chrs = str.toCharArray();
		MutableString mStr = new MutableString(chrs, 0, chrs.length);
		int i;

		System.out.println("String s is        '" + str + "'");
		System.out.println("Char array c is    '" + chrs.toString() + "'");
		System.out.println("MutableString m is '" + mStr + "'");
		i = mStr.compareTo(str);
		System.out.println("M compared to s is: " + i);
		// TODO: Check whether the following line 
		//    (a) worked in earlier Java versions,
		//    (b) was used anywhere in production code,
		//    (c) is useful.
		// i = str.compareTo(mStr);
		System.out.println("S compared to m is: " + i);
	}
}