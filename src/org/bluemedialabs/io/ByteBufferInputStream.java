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
package org.bluemedialabs.io;

import java.io.InputStream;
import org.bluemedialabs.util.Reuseable;


/**
 * <p>Reads single bytes from an internal byte array buffer as numbers (currently
 * only integers). The buffer is not copied but referenced. No synchronization
 * is done for efficiency. In general, performance is traded for fault-savety.
 * </p>
 * 
 * @author J. Marco Bremer
 */
public class ByteBufferInputStream extends InputStream implements Reuseable {
	private byte[] buf;
	private int size;   // buffer size (to be considered)
	private int pos;    // next byte to read

	/**
	 * Constructs a new byte buffer input stream from the first <em>size</em>
	 * bytes of the given buffer.
	 */
	public ByteBufferInputStream(byte[] buffer, int size) {
		reuse(buffer, size);
	}

	public void reuse(byte[] buffer, int size) {
		this.buf = buffer;
		this.size = size;
		pos = 0;
	}

	public void reset() {
		buf = null;
		size = 0;
		pos = 0;
	}

	// implement InputStream

	/**
	 * Returns the total number of remaining bytes until this buffer becomes
	 * empty.
	 */
	public int available() {
		return (size - pos);
	}

	public void close() {
		size = 0;
		pos = 0;
		buf = null;
	}

	public void mark(int readlimit) {}

	public boolean markSupported() {
		return false;
	}

	/**
	 * Returns the next byte as an (always positive) integer. Equivalent to
	 * readInt(1).
	 */
	public int read() {
		int i;
		if (pos >= size) {
			i = -1;
		} else {
			i = buf[pos++] & 0x000000FF;
		}
		return i;
	}

	public int read(byte[] b, int off, int len) {
		int bytesLeft = size - pos;
		if (bytesLeft < len) {
			len = bytesLeft;
		}
		// now len is min(size(b), bytesLeft(buf))
		System.arraycopy(buf, pos, b, off, len);
		pos += len;
		return len;
	}

	public int read(byte[] b) {
		return read(b, 0, b.length);
	}


	public long skip(long n) {
		int newPos = pos + (int) n;
		if (newPos < size) {
			pos = newPos;
			return n;
		} else {
			// else just ignore this
			return 0;
		}
	}

	// end InputSTream implementation



	public int getSize() {
		return size;
	}


	/**
	 * <p>Returns the given number of bytes as int. The counter must not be larger
	 * than 4 or smaller than 1. If it is smaller than 4 the returned number
	 * will always be positive. Otherwise, it might be negative. The given
	 * counter is <em>not</em> checked for validity here!</p>
	 */
	public int readInt(int byteCount) {
		int i = pos + byteCount - 1;
		/*
		 * The & 0x0..0FF is necessary to avoid negative numbers as promissed
		 * in the header documentation!
		 */
		// put in most significant byte including sign
		int no = buf[i] /* & 0x000000FF */; // leave in sign !!! jmb 2001-07-22
//		if (no < 0) System.out.println("# smaller zero: " + no);
		while (--i >= pos) {
			// we expect Litte Endian (Intel) storage!!
			no = no << 8;               // make space for the next byte
			// "or" it into least sign. position
			no = no | (buf[i] & 0x000000FF);
		}
		pos += byteCount;
		return no;
	}

	public int getInt() {
		return readInt(4);
	}
}