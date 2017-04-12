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

import java.io.DataInput;
import java.io.IOException;
//import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;


/**
 * @author J. Marco Bremer
 * @version 1.0
 */
public interface ReadableDataChannel extends ReadableByteChannel, DataInput {

	/**
	 * Inherited from Channel.
	 *
	 * @return
	 */
//	public boolean isOpen();

	/**
	 * Inherited from Channel.
	 * @throws IOException
	 */
//	public void close() throws IOException;

	/**
	 * Returns the current position, i.e., the starting point for the next
	 * read operation, within this channel. The position is relative to the
	 * position at open time and may not reflect the actual position in an
	 * implementing channel.
	 *
	 * @return The current channel position.
	 */
	public long position() throws IOException;


	/**
	 * Inherited from ReadableByteChannel.
	 *
	 * @param buf
	 * @return
	 * @throws IOException
	 */
//	public int read(ByteBuffer buf) throws IOException;

//	public int read() throws IOException;
//	public void readFully(byte[] bb) throws IOException ;
//	public void readFully(byte[] bb, int offset, int length) throws IOException;

	/**
	 * <p>Reads a the next Byte from the underlying channel and interprets
	 * everything but zero as true.</p>
	 *
	 * @see java.io.DataInput.readBoolean()
	 * @return
	 * @throws IOException
	 */
//	public boolean readBoolean() throws IOException;
//	public char readChar() throws IOException;
//	public byte readByte() throws IOException;
//	public short readShort() throws IOException;
//	public int readInt() throws IOException;
//	public long readLong() throws IOException;
//	public float readFloat() throws IOException;
//	public double readDouble() throws IOException;
//	public String readUTF() throws IOException;

	/**
	 * <p>Retrieves a string from the channel and stores it into the supplied
	 * buffer. If the buffer is too small to completely store the string, the
	 * number of additional <em>bytes</em> to fully retrieve the string is
	 * returned. The next call to this method with a sufficiently large buffer
	 * will return the complete string, not only the missung part. The caller
	 * has to make sure the given buffer is reset to hold the full string.</p>
	 * <p>The encoding of the string read is implementation specific and
	 * should be clearly specified by any implementor of this interface.
	 * Multiple, different encodings may be supported. Then, a selection should
	 * be made possible by an additional method which is not part of this
	 * interface.</p>
	 *
	 * @param cb The character buffer to retrieve the string into.
	 * @return The number of additional <em>bytes</em> required to retrieve
	 * this string.
	 * @throws IOException Whenever such occurs reading from the underlying
	 * channel.
	 */
	public int readCharBuffer(CharBuffer cb) throws IOException;

}