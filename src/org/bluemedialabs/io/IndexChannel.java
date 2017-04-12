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
import java.nio.channels.*;
import java.util.Date;
import org.bluemedialabs.util.MutableInteger;


/**
 * A simpler version of an Index Sequential Access File ({@link IsaFile}) for
 * records of fixed length. Fixed-length records do not require maintaining
 * separate data and index files. The records  can rather be stored directly
 * in the index file.
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class IndexChannel implements Index {
	protected RandomAccessFile file;
	protected RandomAccessChannel channel;
	protected Header header = new Header();
	private String fileName;
	private int currentRecNo = 1;
	private long fileOffset;

	// The following is to handle read-only file systems that cause
	// problems with writing the header.
	// Future solution: Eliminate the stupid header alltogether!
	private boolean dontWriteHeader = false;


	/**
	 * <p>Constructs a new indexed file where records are of the type of the
	 * supplied example object. (The object is used to determine the byte size
	 * of the records/objects to be stored and also to make sure only
	 * fixed-length objects are supplied. The example object is only required,
	 * if the file is opened in write and truncation mode. Otherwise, the
	 * record length is automatically determined from the file.</p>
	 * <p>Record numbers start with <em>one</em> (1)!</p>
	 */
	public IndexChannel(String name, Storable sample)
			throws FileNotFoundException, IOException {
		// Create underlying file
		try {
			file = new RandomAccessFile(name, "rw");
		} catch (FileNotFoundException e) {
			file = new RandomAccessFile(name, "r");
			dontWriteHeader = true;
		}
		fileName = name;
		channel = new RandomAccessChannel(file.getChannel());
		// Read (and possibly update) file header
		readHeader(sample.byteSize());
		fileOffset = header.byteSize() - header.getRecordLen();
		// THE OFFSET IS SET SO THAT THE FILE POSITION CALCULATION
		// FOR SEEKING IN SET AND GET ARE AS EFFICIENT AS POSSIBLE.
	}

	/**
	 * Reads header and updates record count value which might not have been
	 * known so far.
	 */
	private void readHeader(int sampleLen) throws IOException {
		long len = channel.size();

		channel.position(0);
		header.load(channel);
		// Some checks,...first, the object and file record lengths...
		if (sampleLen != header.getRecordLen()) {
			String errorMsg = "Index file seems to contain different objects "
				+ "than expected as the sample object size is " + sampleLen
				+ ", but the index file header reports a record length of "
				+ header.getRecordLen();
//			log.log(errorMsg);
			throw new IllegalStateException(errorMsg);
		}
		// ...then, the total length of the index file
		len -= header.byteSize();
		if (len % header.getRecordLen() != 0) {
			String errorMsg = "The index file '" + fileName
				+ "' appears to be corrupted. File length must be a multiple "
				+ "of record length, but record length is "
				+ header.getRecordLen() + ", file length (not including header) "
				+ " is " + len;
//			log.log("ERROR: " + errorMsg);
			throw new IllegalStateException(errorMsg);
		}
		header.setRecordCount((int) (len / header.getRecordLen()));
	}

	public void writeHeader() throws IOException {
		// All header information has been updated already here, except...
		header.setLastModified(System.currentTimeMillis());
		channel.position(0);
		header.store(channel);
	}


	public int getRecordCount() {
		return header.getRecordCount();
	}

	public int getCurrentRecNo() {
		return currentRecNo;
	}

	public String getFileName() {
		return fileName;
	}


	public void set(int recNo, Storable obj) throws IOException {
		checkIndex("setting", recNo);
		seekRecord(recNo);
		obj.store(channel);
	}

	private void checkIndex(String method, int recNo)
			throws IndexOutOfBoundsException {
		if (recNo < 1 || recNo > header.getRecordCount()) {
			String errorMsg = "Index " + recNo + " out of bounds while "
				+ method + " record, valid "
				+ "indices i are 1 <= i <= " + header.getRecordCount();
//			log.log(errorMsg);
			throw new IndexOutOfBoundsException(errorMsg);
		}
	}

	private void seekRecord(int recNo) throws IOException {
		long recPos;
		if (currentRecNo != recNo) {
			recPos = header.getRecordLen() * recNo + fileOffset;
			channel.position(recPos);
		}
		currentRecNo = recNo + 1; // ..because now, we are right after recNo
	}

	public void get(int recNo, Storable obj) throws IOException {
		checkIndex("getting", recNo);
		seekRecord(recNo);
		obj.load(channel);
	}

	public void close() throws IOException {
		// Make sure the header contains all the newest information
		if (!dontWriteHeader) {
			writeHeader();
		}
		file.close();
	}

	// ??? IsaFile only an interface implemented
	//  a) by a FixedRecFile
	//  b) by a FixedRecFile + a VarRecFile
	// (are these files subclasses of RecordFile??? -- I think rather not!)
	// In addition to IsaFile define buffered IsaFileStream and
	// IsaInputStream (also for both fixed and varrec files???)
	// maybe only buffered data(in/out)put streams required???
	// (-> can use data output/input stream !!!)


/*+**************************************************************************
 * Header
 ****************************************************************************/

	/**
	 * The index file's header information, most importantly, the size of a
	 * record and the number of records currently stored.
	 */
	static public class Header implements Storable {
		private int recordLen;
		private int recordCount = -1;   // -1 means 'unknown as of now'
		private long created;
		private long lastModified = -1;


		public Header(int recordLen, Date created) {
			if (created != null) {
				this.created = created.getTime();
			} else {
				// Assume we are not interested in this time
				this.created = 0;
			}
			this.recordLen = recordLen;
		}
		public Header(int recordLen) {
			this(recordLen, new Date());
		}

		public Header() {
			this(-1, new Date());
		}

		public int getRecordLen() {
			return recordLen;
		}
		public void incRecordCount() {
			recordCount++;
		}
		protected void setRecordCount(int count) {
			recordCount = count;
		}
		public int getRecordCount() {
			return recordCount;
		}
		public long getCreated() {
			return created;
		}
		public void setLastModified(long date) throws IllegalArgumentException {
			if (date < lastModified) {
				throw new IllegalArgumentException("New date for last write "
					+ "operation is before old date (" + date + " < "
					+ lastModified + ")");
			}
			lastModified = date;
		}
		public long getLastModified() {
			return lastModified;
		}

		public String toString() {
			StringBuffer buf = new StringBuffer(100);
			buf.append("(recordLen=");
			buf.append(recordLen);
			buf.append(", recordCount=");
			if (recordCount >= 0) {
				buf.append(recordCount);
			} else {
				buf.append("<unknown>");
			}
			buf.append(", created=");
			buf.append(new Date(created));
			buf.append(", lastModified=");
			if (lastModified >= 0) {
				buf.append(lastModified);
			} else {
				buf.append("<unknown>");
			}
			buf.append(")");
			return buf.toString();
		}


		/*+******************************************************************
		 * Storable implementation
		 ********************************************************************/

		public void store(DataOutput out) throws IOException {
			out.writeInt(recordLen);
			out.writeInt(recordCount);
			out.writeLong(created);
			out.writeLong(lastModified);
		}

		public void load(DataInput in) throws IOException {
			recordLen = in.readInt();
			recordCount = in.readInt();
			created = in.readLong();
			lastModified = in.readLong();
		}

		public int byteSize() {
			return 24; // 2 ints and 2 longs
		}
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		MutableInteger mutInt = new MutableInteger(1);
		IndexFile idx = new IndexFile("F:/Java/fos.test", mutInt);

		// Make sure there is some data...
		IndexOutChannel.main(null);
		// Change some simple data...
		idx.get(1, mutInt);
		System.out.println("Value of record 1 is " + mutInt);
		System.out.println("Changing value to 17...");
		mutInt.reuse(17);
		idx.set(1, mutInt);
		mutInt.reuse(333);
		idx.get(1, mutInt);
		System.out.println("Value of record 1 now is " + mutInt);
		mutInt.reuse(20);
		idx.set(20, mutInt);
		idx.set(77, mutInt);
		idx.close();
		// Look what we got...
		IndexInChannel.main(null);
	}

}