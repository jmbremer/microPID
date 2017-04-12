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
 * <p>Constructs a new buffered input channel wrapping the supplied channel.
 * This class is essentially a combination of a ReadableByteChannel and a
 * ByteBuffer.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class BufferedInputChannel implements ReadableDataChannel {
	static public final int MIN_BUFFER_SIZE = 4096; // 4KB
	static private Charset utf8 = Charset.forName("UTF8");
	private ReadableByteChannel channel;
	private ByteBuffer buffer;
	private int bufferSize;
	private CharsetDecoder decoder = utf8.newDecoder();
	private CharBuffer strBuffer = CharBuffer.allocate(1024);
	long pos = 0;


/*+**************************************************************************
 * Class functions
 ****************************************************************************/

	static public BufferedInputChannel create(String fileName, int bufferSize)
			throws IOException {
		FileInputStream in = new FileInputStream(fileName);
		return new BufferedInputChannel(in.getChannel(), bufferSize);
	}

	static public BufferedInputChannel create(String fileName)	throws IOException {
		return create(fileName, MIN_BUFFER_SIZE);
	}


/*+**************************************************************************
 * Object functions
 ****************************************************************************/

	/**
	 * Constructs a new buffered input channel wrapping the supplied channel.
	 * This class is esentially a merger between a WritableByteChannel and a
	 * ByteBuffer.
	 *
	 * @param channel The channel to be wrapped.
	 * @param bufferSize The size in bytes of the employed buffer.
	 */
	public BufferedInputChannel(ReadableByteChannel channel, int bufferSize) {
		this.channel = channel;
		// Create buffer
		this.bufferSize = Math.max(bufferSize, MIN_BUFFER_SIZE);
		buffer = ByteBuffer.allocateDirect(this.bufferSize);
		buffer.position(buffer.capacity());
	}

	public BufferedInputChannel(ReadableByteChannel channel) {
		this(channel, MIN_BUFFER_SIZE);
	}

	public void finalize() {
		try {
			close();
		} catch (Exception e) {
		}
	}

	public boolean isOpen() {
		return channel.isOpen();
	}

	public void close() throws IOException {
		if (channel.isOpen()) {
			channel.close();
		}
	}

	public long position() {
		return pos;
	}


	protected ByteBuffer getBuffer() {
		return buffer;
	}

	protected int readBuffer() throws IOException {
		buffer.clear();
		return channel.read(buffer);
	}


	public byte readByte() throws IOException {
		checkRead(1);
		byte b = buffer.get();
		pos++;
		return b;
	}

	public int readUnsignedByte() throws IOException {
		checkRead(1);
		byte b = buffer.get();
		pos++;
		return (((int) b) & 0xFF);
	}

	private void checkRead(int bytesNeeded) throws IOException {
		if (buffer.remaining() < bytesNeeded) {
			// Copy remaining content to the beginning of the buffer
			buffer.compact();
			channel.read(buffer);
			buffer.flip();
			if (buffer.remaining() < bytesNeeded) {
				// Uhh, bad, somebody is trying to get a too large chunk of
				// data out of our internal buffer
				if (!buffer.hasRemaining()) {
					throw new EOFException(bytesNeeded + " Bytes requested "
							+ "but no more data left");
				} else if (bytesNeeded > bufferSize) {
					throw new IllegalStateException(bytesNeeded + " Bytes requested "
							+ "as one chunk but buffer size is only " + bufferSize);
				} else {
					throw new IllegalStateException(bytesNeeded +
							" Bytes required to put object, however, the internal "
	   + "buffer seems to be of smaller size, thus object can only put itself "
	+ "by directly accessing the internal buffer");
				}
			}
		}
	}

	public int skipBytes(int skip) throws IOException {
		int r, s = skip;

		// Discard buffer contents and as much further input as requested
		r = buffer.remaining();
		while (r > 0 && s >= bufferSize) {
			s -= r;
			buffer.clear();
			r = channel.read(buffer);
		}
		if (s > 0 && r > 0) {
			r = Math.min(s, r);
			buffer.position(buffer.position() + r);
			s -= r;
		}
		return skip - s;
	}


	public void readFully(byte[] bb) throws IOException {
		/*
		if (bb.length <= bufferSize) {
			// Regular read possible
			checkRead(bb.length);
			buffer.get(bb);
		} else {
			// Need to do multiple reads
			int len = bb.length;
			int pos = 0;
			int more;
			while (pos < len) {
				checkRead(bufferSize); // fill buffer to maximum
				more = Math.min(bufferSize, len - pos);
				buffer.get(bb, pos, more);
				pos += more;
			}
		}
		*/
		readFully(bb, 0, bb.length);
	}

	public void readFully(byte[] bb, int offset, int length) throws IOException {
		if (length <= bufferSize) {
			// Regular read possible
			checkRead(bb.length);
			buffer.get(bb, offset, length);
		} else {
			// Need to do multiple reads
			int endPos = offset + length;
			int pos = offset;
			int more;
			while (pos < endPos) {
				checkRead(bufferSize); // fill buffer to maximum
				more = Math.min(bufferSize, endPos - pos);
				buffer.get(bb, pos, more);
				pos += more;
			}
		}
		// This is cheating, we should determine the actual length read..
		//...extend checkRead!!
		pos += length;
//		return length;
	}

	public int read(ByteBuffer buf) throws IOException {
		int len = buf.remaining();
		int limit, r;

		if (len <= bufferSize && buf.hasArray()) {
			// Regular access through buffer possible
			checkRead(len);
			buffer.get(buf.array(), buf.arrayOffset() + buf.position(), len);
		} else {
			// Have to access channel directly (with the unfortunate problem
			// that the next data items are likely already in the buffer :-( )
			r = Math.min(len, bufferSize);
			checkRead(r);
			limit = buffer.limit();
			buffer.limit(buffer.position() + r);
			buf.put(buffer);
			buffer.limit(limit);
			if (len > r) {
				// Need to read more but fortunately, the buffer is empty now
				// thus we can freely read from the underlying channel directly
				int read = channel.read(buf);
				r += read;
				pos += r;
				return r;
			}
		}
		pos += len;
		return len;
	}


	public boolean readBoolean() throws IOException {
		checkRead(1);
		pos++;
		return (buffer.get() != 0);
	}

	public char readChar() throws IOException {
		checkRead(2);
		pos += 2;
		return buffer.getChar();
	}

	public short readShort() throws IOException {
		checkRead(2);
		pos += 2;
		return buffer.getShort();
	}

	public int readUnsignedShort() throws IOException {
		checkRead(2);
		short s = buffer.getShort();
		pos += 2;
		return (0xFFFF & s);
	}

	public int readInt() throws IOException {
		checkRead(4);
		pos += 4;
		return buffer.getInt();
	}

	public long readLong() throws IOException {
		checkRead(8);
		pos += 8;
		return buffer.getLong();
	}

	public float readFloat() throws IOException {
		checkRead(4);
		pos += 4;
		return buffer.getFloat();
	}

	public double readDouble() throws IOException {
		checkRead(8);
		pos += 8;
		return buffer.getDouble();
	}

	public int readCharBuffer(CharBuffer cb) throws IOException {
		CoderResult cr;
		int len, limit, more;

		// First, get the short encoding the number of bytes to be read for
		// this character string
		checkRead(2);
		buffer.mark();
		len = readShort();
//		if (len > strBuffer.capacity()) {
//			throw new IllegalStateException("Asked to encode a " + len
//					+ " Bytes long string, but internal buffer has only "
//	 + strBuffer.capacity() + " Bytes");
//		}
		// Then, read len bytes and decode them
		checkRead(len);
		limit = buffer.limit();
		buffer.limit(buffer.position() + len);
		decoder.reset();
		cr = decoder.decode(buffer, cb, true);
		if (cr == CoderResult.UNDERFLOW) {
			// Everything ok and all the data has been converted
			more = 0;
			pos += 2 + len;
		} else if (cr == CoderResult.OVERFLOW) {
			// Destination buffer too small; caller needs to provide more space
			more = buffer.remaining();
			buffer.reset();
		} else {
			// Something must be wrong with the input!
			more = -1;
			buffer.reset();
		}
		// Resest buffer limit to make rest of buffer available
		buffer.limit(limit);
		return more;
	}

	public String readUTF() throws IOException {
		strBuffer.clear();
		readCharBuffer(strBuffer);
		return strBuffer.toString();
	}

	public String readLine() throws IOException {
		throw new UnsupportedOperationException();
	}


	static public void main(String[] args) throws Exception {
		FileInputStream fis = new FileInputStream("F:/Java/test.index");
		FileChannel fc = fis.getChannel();
		BufferedInputChannel bic = new BufferedInputChannel(fc, 128000);
		StopWatch watch = new StopWatch();
		long l, err = 0, m, n;
		CharBuffer buf = CharBuffer.allocate(100);
		long byteCount = 0;

		watch.start();
		for (long i = 0; i < 5000000 && err < 20; i++) {
			if ((l = bic.readLong()) != i) {
				System.out.println("Found " + l + " instead of expected " + i);
				err++;
			}
			buf.clear();
			m = bic.readCharBuffer(buf);
			if (m != 0) {
				System.out.println("m = " + m);
			}
			buf.flip();
			byteCount += buf.remaining() + 2;
//			System.out.println(buf);
//			System.out.flush();
			n = bic.readInt();
			if (n != (int) l) {
				System.out.println("Long != int " + l + "/" + n);
			}
			byteCount += 12;
		}
		bic.close();
		watch.stop();
		float kb = (float) byteCount / 1024;
		System.out.println("Reading of " + ((int) kb) + " KB took " +
						   watch + " (= " + (1000 * kb / watch.getTime())
						   + " KB/s = " + (0.9765625 * kb / watch.getTime())
						   + " MB/s)");
	}
}