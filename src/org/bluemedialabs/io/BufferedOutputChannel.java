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
import org.bluemedialabs.util.MyMath;
import org.bluemedialabs.util.StopWatch;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class BufferedOutputChannel implements WritableDataChannel {
	static public final int MIN_BUFFER_SIZE = 4096; // 4KB
	static private Charset utf8 = Charset.forName("UTF8");
	private WritableByteChannel channel;
	private ByteBuffer buffer;
	private CharsetEncoder encoder = utf8.newEncoder();;
	private ByteBuffer strBuffer = ByteBuffer.allocate(1024);
	private long pos = 0;


/*+**************************************************************************
 * Class functions
 ****************************************************************************/

	static public BufferedOutputChannel create(String fileName, int bufferSize)
			throws IOException {
		FileOutputStream in = new FileOutputStream(fileName);
		return new BufferedOutputChannel(in.getChannel(), bufferSize);
	}

	static public BufferedOutputChannel create(String fileName)
			throws IOException {
		return create(fileName, MIN_BUFFER_SIZE);
	}


/*+**************************************************************************
 * Object functions
 ****************************************************************************/

	/**
	 * Constructs a new buffered output channel wrapping the supplied channel.
	 * This class is esentially a merger between a WritableByteChannel and a
	 * ByteBuffer.
	 *
	 * @param channel The channel to be wrapped.
	 * @param bufferSize The size in bytes of the employed buffer.
	 */
	public BufferedOutputChannel(WritableByteChannel channel, int bufferSize) {
		this.channel = channel;
		// Create buffer
		bufferSize = Math.max(bufferSize, MIN_BUFFER_SIZE);
		buffer = ByteBuffer.allocateDirect(bufferSize);
	}

	public BufferedOutputChannel(WritableByteChannel channel) {
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

	public void flush() throws IOException {
		writeBuffer();
	}

	public void close() throws IOException {
		if (channel.isOpen()) {
			writeBuffer();
			channel.close();
		}
	}

	public long position() {
		return pos;
	}

	public ByteBuffer getBuffer() {
		return buffer;
	}

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
		buffer.clear();
		// Who is doing the flip() ?? -> We are now!
		return written;
	}


	public void write(int b) throws IOException {
		checkWrite(1);
		buffer.put((byte) b);
		pos++;
	}

	public void writeByte(int b) throws IOException {
		checkWrite(1);
		buffer.put((byte) b);
		pos++;
	}

	private void checkWrite(int bytesNeeded) throws IOException {
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
	}

	public void write(byte[] bb) throws IOException {
		write(bb, 0, bb.length);
	}

	public void write(byte[] bb, int offset, int length) throws IOException {
		int r, len = length, written = 0;
		while (length > 0) {
			r = Math.min(buffer.remaining(), len);
			buffer.put(bb, offset, r);
			written += writeBuffer();
			offset += r;
			len -= r;
		}
		if (written != length) {
			throw new IllegalStateException("Here is something wrong--we should "
					+ "have written " + length + " bytes but have only written "
	 + written + " bytes, what kind of cheap channel is it we are writing to?");
		}
		pos += length;
//		return length;
	}

	public int write(ByteBuffer bb) throws IOException {
		int len = bb.remaining();
		int written;

		if (len > (buffer.capacity() >>> 1)) {
			// Pretty large chunk of data thus, write it directly
			writeBuffer();
			written = channel.write(bb);
			pos += written;
			return written;
		} else {
			checkWrite(len);
			buffer.put(bb);
			pos += len;
			return len;
		}
		// WRITE SMALL DATA ITEMS INTO INTERNAL BUFFER AND TOO LARGE ONES
		// DIRECTLY TO CHANNEL (MAKING SURE TO FLUSH BUFFER FIRST TOO).
	}


	public void writeBoolean(boolean b) throws IOException {
		checkWrite(1);
		buffer.put((byte) (b? 1: 0));
		pos++;
	}

	public void writeChar(int c) throws IOException {
		checkWrite(2);
		buffer.putChar((char) c);
		pos += 2;
	}

	public void writeChars(String str) throws IOException {
		int len = str.length();
		checkWrite(2 * len);
		for (int i = 0; i < len; i++) {
			buffer.putChar(str.charAt(i));
		}
		pos += (2 * len);
	}

	public void writeBytes(String str) throws IOException {
		int len = str.length();
		checkWrite(len);
		for (int i = 0; i < len; i++) {
			buffer.put((byte) str.charAt(i));
		}
		pos += len;
	}

	public void writeByte(byte b) throws IOException  {
		checkWrite(1);
		buffer.put(b);
		pos += 1;
	}

	public void writeShort(int s) throws IOException {
		checkWrite(2);
		buffer.putShort((short) s);
		pos += 2;
	}

	public void writeInt(int i) throws IOException {
		checkWrite(4);
		buffer.putInt(i);
		pos += 4;
	}

	public void writeLong(long l) throws IOException {
		checkWrite(8);
		buffer.putLong(l);
//		buffer.clear();
//		buffer.putLong(l);
//		buffer.flip();
//		channel.write(buffer);
		pos += 8;
	}

	public void writeFloat(float f) throws IOException {
		checkWrite(4);
		buffer.putFloat(f);
		pos += 4;
	}

	public void writeDouble(double d) throws IOException {
		checkWrite(8);
		buffer.putDouble(d);
		pos += 8;
	}

	public void writeCharBuffer(CharBuffer cb) throws IOException {
		CoderResult cr;
		int limit;

		encoder.reset();
		strBuffer.clear();
		cr = encoder.encode(cb, strBuffer, true);
		if (cr == CoderResult.OVERFLOW) {
			// String buffer too small; this is an unrecoverable error so
			// far as the buffer size is fixed
			throw new IllegalStateException("Failed to encode character buffer "
					+ cb + " because internal conversion buffer is too small ( "
					+ "char buffer len=" + (cb.limit() - cb.position())
					+ "internal buffer len=" + strBuffer.capacity());
		}
		strBuffer.flip();
		if (strBuffer.limit() > Short.MAX_VALUE) {
			throw new IllegalArgumentException("Failed to encode character "
					+ "buffer because resulting byte length is out of 'short'"
					+ "range (" + strBuffer.limit() + ")");
		}
		writeShort((short) strBuffer.limit());
		limit = strBuffer.limit();
		write(strBuffer);
//		pos += limit; -- write already does this!!!
	}

	public void writeUTF(String str) throws IOException {
		writeCharBuffer(CharBuffer.wrap(str));
	}


	static public void main(String[] args) throws Exception {
		CharBuffer buf = CharBuffer.wrap("This is a little test string.", 0, 29);
		FileOutputStream fos = new FileOutputStream("F:/Java/test.index");
		FileChannel fc = fos.getChannel();
		BufferedOutputChannel boc = new BufferedOutputChannel(fc, 65);
		StopWatch watch = new StopWatch();
		long byteCount = 0;

		watch.start();
		for (long i = 0; i < 1000000; i++) {
			boc.writeLong(i);
			buf.limit(MyMath.random(1, 29));
			buf.position(0);
			boc.writeCharBuffer(buf);
			byteCount += buf.limit() + 2;
			boc.writeInt((int) i);
			byteCount += 12; // for the long and int
		}
//		boc.close();
		boc.finalize();
		watch.stop();
		float kb = (float) byteCount / 1024;
		System.out.println("Writing of " + ((int) kb) + " KB took " +
						   watch + " (= " + (1000 * kb / watch.getTime())
						   + " KB/s = " + (0.9765625 * kb / watch.getTime())
						   + " MB/s)");
	}

}