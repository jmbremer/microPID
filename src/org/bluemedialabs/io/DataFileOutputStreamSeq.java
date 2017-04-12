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

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.*;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class DataFileOutputStreamSeq implements DataOutputSequence {
	private FileOutputStream fileOut;
	private int fileCount = 0;
	private DataOutputStream out;
	private String baseFileName;
	private int bufferSize;

	public DataFileOutputStreamSeq(String baseFileName, int bufferSize)
			throws IOException {
		this.baseFileName = baseFileName;
		this.bufferSize = bufferSize;
		fileOut = new FileOutputStream(baseFileName + (++fileCount));
		out = new DataOutputStream(new BufferedOutputStream(fileOut, bufferSize));
	}

	public DataOutputSequence next() throws IOException {
		close();
		fileOut = new FileOutputStream(baseFileName + (++fileCount));
		out = new DataOutputStream(new BufferedOutputStream(fileOut, bufferSize));
		return this;
	}

	public void finalize() {
		try {
			close();
		} catch (Exception e) {
		}
	}


	public int getSeqNumber() {
		return fileCount;
	}

	public int getFileCount() {
		return fileCount;
	}

	public void close() throws IOException {
		out.flush();
		out.close();
		fileOut.close();    // have to do this???? -- don't think so!
	}

	public void write(int b) throws IOException {
		out.write(b);
	}
	public void write(byte[] b) throws IOException {
		out.write(b);
	}
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
	}
	public void writeBoolean(boolean b) throws IOException {
		out.writeBoolean(b);
	}
	public void writeByte(int b) throws IOException {
		out.writeByte(b);
	}
	public void writeShort(int s) throws IOException {
		out.writeShort(s);
	}
	public void writeChar(int c) throws IOException {
		out.writeChar(c);
	}
	public void writeInt(int i) throws IOException {
		out.writeInt(i);
	}
	public void writeLong(long l) throws IOException {
		out.writeLong(l);
	}
	public void writeFloat(float f) throws IOException {
		out.writeFloat(f);
	}
	public void writeDouble(double d) throws IOException {
		out.writeDouble(d);
	}
	public void writeBytes(String s) throws IOException {
		out.writeBytes(s);
	}
	public void writeChars(String s) throws IOException {
		out.writeChars(s);
	}
	public void writeUTF(String str) throws IOException {
		out.writeUTF(str);
	}


	public String toString() {
		StringBuffer buf = new StringBuffer(100);

		buf.append("(baseFileName='");
		buf.append(baseFileName);
		buf.append("', fileCount=");
		buf.append(fileCount);
		buf.append(")");
		return buf.toString();
	}
}