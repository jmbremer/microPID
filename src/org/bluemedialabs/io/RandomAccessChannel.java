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

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import org.bluemedialabs.util.StopWatch;


/**
 * <p>The write functions have to updated to fully support buffered writing
 * of whole blocks!</p>
 * <p><em>Copyright (c) 2002 by J. Marco Bremer</em></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class RandomAccessChannel implements DataChannel {
	/**
	 * <p>The minimum size of the internal buffer used to translate standard
	 * data types into bytes, and write data to the underlying channel.
	 * Must be at least larger than all the standard data types.</p>
	 */
	static public final int MIN_BUFFER_SIZE = 4096; // 4KB
	static public final int BLOCK_SIZE      = 4096;
	static private Charset utf8 = Charset.forName("UTF8");
	private FileChannel channel;
	private ByteBuffer buffer;
	private int bufferSize;
	private CharsetEncoder encoder = utf8.newEncoder();
	private CharsetDecoder decoder = utf8.newDecoder();
	/**
	 * <p>...Also notice that this size limits the size of character strings
	 * that can be written. A string has to fit into this buffer completely in
	 * order to be written to the channel.</p>
	 */
	private ByteBuffer strByteBuffer = ByteBuffer.allocate(1024);
	private CharBuffer strBuffer = CharBuffer.allocate(512);

	/**
	 * Flag that shows whether the internal channel pointer has just been
	 * repositions, or the channel has been newly opened (which is also some
	 * form of repositioning).
	 */
	private boolean repositioned = true;


	static public RandomAccessChannel create(String fileName)
			throws IOException {
		// Changed from "rw" to "r" on 2003-01-11 by jmb
		RandomAccessFile file = new RandomAccessFile(fileName, "r");
		return new RandomAccessChannel(file.getChannel());
	}


	public RandomAccessChannel(FileChannel channel, int bufferSize) {
		this.channel = channel;
		this.bufferSize = Math.max(bufferSize, MIN_BUFFER_SIZE);
		buffer = ByteBuffer.allocateDirect(bufferSize);
	}

	public RandomAccessChannel(FileChannel channel) {
		this(channel, BLOCK_SIZE);
	}


	public void finalize() {
		try {
			close();
		} catch (Exception e) {
		}
	}



	/*+**********************************************************************
	 * General routines
	 ************************************************************************/

	public boolean isOpen() {
			return channel.isOpen();
	}

	public void close() throws IOException {
		if (channel.isOpen()) {
			channel.close();
		}
	}

	public long position() throws IOException {
		return channel.position();
	}

	// Optimized for read access right now!!
	public void position(long pos) throws IOException {
		channel.position(pos);
		repositioned = true;
	}

	public void clearBuffer() {
		buffer.clear();
	}

	public long size() throws IOException {
		return channel.size();
	}

	public int skipBytes(int skip) throws IOException {
		channel.position(channel.position() + skip);
		return skip;
	}


	/*+**********************************************************************
	 * Writing routines
	 ************************************************************************/

	/**
	 * Flips (!) and write the internal buffer. This method is meant for
	 * users who directly write their content into the internal buffer. Care
	 * should be taken <em>not</em> to call flip() before calling this method!
	 *
	 * @returns The number of bytes actually written.
	 * @throws IOException
	 */
	public int writeBuffer() throws IOException {
		int written = 0;
		buffer.flip();
		written = channel.write(buffer);
//		buffer.clear();
		buffer.position(0);
		buffer.limit(bufferSize);
		return written;
	}


	public void write(int b) throws IOException {
		buffer.clear();
		buffer.put((byte) b);
		writeBuffer();
	}

/*	private void checkWrite(int bytesNeeded) throws IOException {
		if (buffer.remaining() < bytesNeeded) {
			buffer.flip();
			channel.write(buffer);
			// CHECK NUMBER OF BYTES WRITTEN!?!
			buffer.clear();
			if (buffer.remaining() < bytesNeeded) {
				// Uhh, bad, somebody is trying to put a too large chunk of
				// data into our internal buffer
				throw new IllegalStateException(bytesNeeded + " Bytes required to "
						+ "write object, however, the internal buffer seems to "
	  + "be of smaller size, thus object can only write itself by directly "
   + "accessing the internal buffer");
			}
		}
	}*/

	public void write(byte[] bb) throws IOException {
		write(bb, 0, bb.length);
	}

	public void write(byte[] bb, int offset, int length) throws IOException {
		int written = 0, len = length;
		buffer.clear();
		while (len > bufferSize) {
			buffer.put(bb, offset, bufferSize);
			written += writeBuffer();
			offset += bufferSize;
			len -= bufferSize;
		}
		if ( len > 0) {
			// Some data (that will fit into buffer completly) left
			buffer.put(bb, offset, len);
			written += writeBuffer();
		}
		if (written != length) {
			throw new IllegalStateException("Here is something wrong--we should "
					+ "have written " + length + " bytes but have only written "
	 + written + " bytes, what kind of cheap channel is it we are writing to?");
		}
//		return written;
	}

	public int write(ByteBuffer bb) throws IOException {
		// Write this buffer directly rather than copying it around first
		// buffer.flip(); -- That should have been done by the caller!
		return channel.write(bb);
	}


	public void writeBoolean(boolean b) throws IOException {
		buffer.clear();
		buffer.put((byte) (b? 1: 0));
		writeBuffer();
	}

	public void writeChar(int c) throws IOException {
		buffer.clear();
		buffer.putChar((char) c);
		writeBuffer();
	}

	public void writeChars(String str) throws IOException {
		writeSingle(str, true);
	}

	public void writeBytes(String str) throws IOException {
		writeSingle(str, false);
	}

	public void writeSingle(String str, boolean chars) throws IOException {
		int len = str.length();
		int c = (chars? (buffer.capacity() >>> 1): buffer.capacity());
		int pos;

		buffer.clear();
		// Write complete chunks of c bytes
		pos = 0;
		while (len > c) {
			if (chars) {
				for (int i = pos; i < (pos + c); i++) {
					buffer.putChar(str.charAt(i));
				}
			} else {
				for (int i = pos; i < (pos + c); i++) {
					buffer.put((byte) str.charAt(i));
				}
			}
			writeBuffer();
			pos += c;
			len -= c;
		}
		// Write remaining data
		if (chars) {
			for (int i = pos; i < str.length(); i++) {
				buffer.putChar(str.charAt(i));
			}
		} else {
			for (int i = pos; i < str.length(); i++) {
				buffer.put((byte) str.charAt(i));
			}
		}
		writeBuffer();
	}

	public void writeByte(int b) throws IOException {
		buffer.clear();
		buffer.put((byte) b);
		writeBuffer();
	}

	public void writeShort(int s) throws IOException {
		buffer.clear();
		buffer.putShort((short) s);
		writeBuffer();
	}

	public void writeInt(int i) throws IOException {
		buffer.clear();
		buffer.putInt(i);
		writeBuffer();
	}

	public void writeLong(long l) throws IOException {
		buffer.clear();
		buffer.putLong(l);
		writeBuffer();
	}

	public void writeFloat(float f) throws IOException {
		buffer.clear();
		buffer.putFloat(f);
		writeBuffer();
	}

	public void writeDouble(double d) throws IOException {
		buffer.clear();
		buffer.putDouble(d);
		writeBuffer();
	}

	public void writeCharBuffer(CharBuffer cb) throws IOException {
		CoderResult cr;

		// Here, we need to copy, unfortunately
		encoder.reset();
		strByteBuffer.clear();
		cr = encoder.encode(cb, strByteBuffer, true);
		if (cr == CoderResult.OVERFLOW) {
			// String buffer too small; this is an unrecoverable error so
			// far as the buffer size is fixed and the number of bytes
			// required for encoding needs to be know BEFORE anything
			// can be written out
			throw new IllegalStateException("Failed to encode character buffer "
					+ cb + " because internal conversion buffer is too small ( "
					+ "char buffer len=" + (cb.remaining())
					+ "internal buffer len=" + strBuffer.capacity());
		}
		strByteBuffer.flip();
		if (strBuffer.limit() > Short.MAX_VALUE) {
			throw new IllegalArgumentException("Failed to encode character "
					+ "buffer because resulting byte length is out of 'short'"
					+ "range (" + strByteBuffer.limit() + ")");
		}
		writeShort((short) strByteBuffer.limit());
		write(strByteBuffer);
	}

	public void writeUTF(String str) throws IOException {
		writeCharBuffer(CharBuffer.wrap(str));
	}


	/*+**********************************************************************
	 * Reading routines
	 ************************************************************************/

	public byte read() throws IOException {
		readBuffer(1);
		return buffer.get();
	}


	/**
	 * Makes sure there are at least the requested number of bytes left. If
	 * there is not enough data left, the buffer is filled up to the
	 * <em>BLOCK_SIZE</em>.
	 *
	 * @param len
	 * @return
	 * @throws IOException
	 */
	private void readBuffer(int len) throws IOException {
		int read;
		if (repositioned) {
			// Fill the buffer starting at the new,current position
			buffer.clear();
			channel.read(buffer);
			// Make the data available for read operations
			buffer.flip();
			repositioned = false;
		} else if (len > buffer.remaining()) {
			// Preserve old data
			buffer.compact();
			// Fill the buffer up
			channel.read(buffer);
			// Make all data available
			buffer.flip();
		}
		if (buffer.remaining() < len) {
			throw new EOFException(len + " more bytes requested, but only "
								   + buffer.remaining() + " bytes left");
		}
	}

	public void readFully(byte[] bb) throws IOException {
		readFully(bb, 0, bb.length);
	}

	public void readFully(byte[] bb, int offset, int length) throws IOException {
		int srcRemain = buffer.remaining();
//		int fullLength = length;
		int pos;

		while (srcRemain < length) {
			// Put complete buffer contents into array
			buffer.get(bb, offset, srcRemain);
			length -= srcRemain;
			offset += srcRemain;
			// The following avoids requesting more bytes than necessary
			// because that might result in an EOF exception even though
			// enough data would have been available.
			readBuffer(Math.min(BLOCK_SIZE, length));
			srcRemain = buffer.remaining();
		}
		// At this point, the following obviously holds: srcRemain >= length
		// (Unfortunately, it might still be that srcRemain > length!)
		pos = buffer.limit();
		// Adjust the limit to avoid having buffer throw an overflow except.
		buffer.limit(buffer.position() + length);
		buffer.get(bb, offset, length);
		// Reset buffer limit to old position
		buffer.limit(pos);
//		return fullLength;
	}


	/**
	 * Reads as many bytes into the supplied buffer as the buffer has remaining
	 * space. If not enough data is left in the channel, an EOF exception
	 * is thrown.
	 *
	 * @param buf
	 * @return
	 * @throws IOException
	 */
	public int read(ByteBuffer buf) throws IOException {
		int srcRemain = buffer.remaining();
		int dstRemain = buf.remaining();
		int dstComplete = dstRemain;
		int pos;

		while (srcRemain < dstRemain) {
			buf.put(buffer);
			dstRemain -= srcRemain;
			// The following avoids requesting more bytes than necessary
			// because that might result in an EOF exception even though
			// enough data would have been available.
			readBuffer(Math.min(BLOCK_SIZE, dstRemain));
			srcRemain = buffer.remaining();
		}
		// At this point, the following obviously holds: srcRemain >= dstRemain
		// (Unfortunately, it might still be that srcRamin > dstRemain!)
		pos = buffer.limit();
		// Adjust the limit to avoid having buffer throw an overflow except.
		buffer.limit(buffer.position() + dstRemain);
		buf.put(buffer);
		// Reset buffer limit to old position
		buffer.limit(pos);
		return dstComplete;
	}


	public boolean readBoolean() throws IOException {
		readBuffer(1);
		return (buffer.get() != 0);
	}

	public char readChar() throws IOException {
		readBuffer(2);
		return buffer.getChar();
	}

	public byte readByte() throws IOException {
		readBuffer(1);
		return buffer.get();
	}

	public int readUnsignedByte() throws IOException {
		readBuffer(1);
		// OLD
//		byte b = buffer.get();
//		return (((int) b) & 0xFF);
		// NEW
		int b = buffer.get();
		return (b & 0xFF);
	}

	public short readShort() throws IOException {
		readBuffer(2);
		return buffer.getShort();
	}

	public int readUnsignedShort() throws IOException {
		readBuffer(2);
		short s = buffer.getShort();
		return (0xFFFF & s);
	}

	public int readInt() throws IOException {
		readBuffer(4);
		return buffer.getInt();
	}

	public long readLong() throws IOException {
		readBuffer(8);
		return buffer.getLong();
	}

	public float readFloat() throws IOException {
		readBuffer(4);
		return buffer.getFloat();
	}

	public double readDouble() throws IOException {
		readBuffer(8);
		return buffer.getDouble();
	}

	/**
	 * THIS FUNCTION HAS TO BE CHECKED FOR COMPATIBILITY WITH THE NEW, INTERNAL
	 * BUFFER MANAGEMENT!
	 *
	 * @param cb
	 * @return
	 * @throws IOException
	 */
	public int readCharBuffer(CharBuffer cb) throws IOException {
		CoderResult cr = CoderResult.UNDERFLOW;
		int len, limit, more;

		// First, get the short encoding the number of bytes to be read for
		// this character string
		len = readShort();
		if (len > strBuffer.capacity()) {
			throw new IllegalStateException("Asked to decode a " + len
				+ " Bytes long string, but internal buffer has only "
				+ strBuffer.capacity() + " bytes");
		}
		// Then, read len bytes and decode them
		decoder.reset();
		while (len > bufferSize && cr != CoderResult.OVERFLOW) {
			readBuffer(bufferSize);
			cr = decoder.decode(buffer, cb, false);
			len -= bufferSize;
		}
		if (cr == CoderResult.UNDERFLOW) {
			// Everything ok here, just need to read the rest of the data
			readBuffer(len);
			cr = decoder.decode(buffer, cb, true);
			len = 0;
			// We do not handle EOF and possibly some other things here yet!!!
		}
		if (cr == CoderResult.OVERFLOW) {
			// Destination buffer too small; caller needs to provide more space
			len = len + bufferSize;
			throw new IllegalStateException("Destination character buffer "
					+ "too small to hold data; about " + len + " more bytes "
					+ "of character data are available, please re-initiate "
					+ "read operation");
		} else if (cr != CoderResult.UNDERFLOW) {
			// Something must be wrong with the input!
			len = -1;
		}
		return len;
	}

	/**
	 * THIS FUNCTION HAS TO BE CHECKED FOR COMPATIBILITY WITH THE NEW, INTERNAL
	 * BUFFER MANAGEMENT!
	 *
	 * @return
	 * @throws IOException
	 */
	public String readUTF() throws IOException {
		/*
		strBuffer.clear();
		readCharBuffer(strBuffer);
		strBuffer.flip();
		return strBuffer.toString();
		*/

		throw new UnsupportedOperationException("readUTF() not supported until "
				+ "somebody has looked at it");
	}

	public String readLine() throws IOException {
		throw new UnsupportedOperationException();
	}


	static public void main(String[] args) throws Exception {
		RandomAccessChannel ch = RandomAccessChannel.create("F:/Java/test.index");
		CharBuffer buf = CharBuffer.wrap("This is a nice little string|.|");
		CharBuffer buf2 = CharBuffer.allocate(200);
		ch.writeLong((long) 1013);
		ch.position(10234);
		buf.flip();
		ch.writeCharBuffer(buf);
		ch.position(0);
		System.out.println("Long value at position 0 is " + ch.readLong());
		ch.readCharBuffer(buf2);
		System.out.println("Value right after that is '" + buf2 + "'");
		ch.position(10234);
		buf.clear();
		ch.readCharBuffer(buf);
		System.out.println("String at position 10234 is '" + buf + "'");
		ch.close();
	}
}