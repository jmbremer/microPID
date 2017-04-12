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
import java.util.Date;
import org.bluemedialabs.util.MutableLong;
import org.bluemedialabs.util.MutableString;


/**
 * <p> </p>
 * 
 * @author J. Marco Bremer
 */
public class IndexSeqFile extends IndexSeqBitSource {
	static public final String INDEX_FILE_ENDING  = ".index";
	static public final String DATA_FILE_ENDING   = ".data";
	static public int DEFAULT_INDEX_INPUT_BUFFER_SIZE   = 65536;    // 64 KB
	static public int DEFAULT_DATA_INPUT_BUFFER_SIZE    = 1048576;  // 1 MB
	static public int DEFAULT_INDEX_OUTPUT_BUFFER_SIZE  = 65536;    // 64 KB
	static public int DEFAULT_DATA_OUTPUT_BUFFER_SIZE   = 1048576;  // 1 MB

	protected IndexFile indexFile = null;
	protected RandomAccessFile dataFile = null;
	protected Header dataHeader = new Header();
//	protected LogStream log;
	private String baseName;
	private int currentRecNo = 1;
	private long dataFileOffset;
	private MutableLong mutLong = new MutableLong(0);

	// The following is to handle read-only file systems that cause
	// problems with writing the header.
	// Future solution: Eliminate the stupid header alltogether!
	private boolean dontWriteHeader = false;


	/**
	 * <p></p>
	 * <p>Record numbers start with <em>one</em> (1)!</p>
	 */
	public IndexSeqFile(String baseName, boolean bitInput)
			throws FileNotFoundException, IOException {
		super(bitInput);
		// Create underlying file
		indexFile = new IndexFile(baseName + INDEX_FILE_ENDING, mutLong);
		// !!! "rw" changed to "r" on 2003jan11 and then back:
		try {
			dataFile = new RandomAccessFile(baseName + DATA_FILE_ENDING, "rw");
		} catch (FileNotFoundException e) {
			// This exception is apparently thrown on a read-only file system
			dataFile = new RandomAccessFile(baseName + DATA_FILE_ENDING, "r");
			dontWriteHeader = true;
		}
		this.baseName = baseName;
		// Read (and possibly update) file header
		readDataHeader();
	}

	public IndexSeqFile(String baseName)
			throws FileNotFoundException, IOException {
		this(baseName, DEFAULT_BIT_INPUT);
	}


	/**
	 * Reads header and updates record count value which might not have been
	 * known so far.
	 */
	private void readDataHeader() throws IOException {
		dataFile.seek(0);
		dataHeader.load(dataFile);
	}

	// Needed here? no updates whatsoever done!?
	public void writeDataHeader() throws IOException {
		dataFile.seek(0);
		dataHeader.store(dataFile);
	}


	public int getRecordCount() {
		return indexFile.getRecordCount();
	}

	public int getCurrentRecNo() {
		return currentRecNo;
	}

	public String getBaseName() {
		return baseName;
	}


	public void get(int recNo, Storable obj) throws IOException {
		if (!isBitInput()) {
			seekRecord(recNo);
			obj.load(dataFile);
		} else {
			if (!( obj instanceof BitStorable)) {
				throw new IllegalStateException("Getting bit object requested "
						+ "but supplied object does not know how to "
						+ "load itself that way");
			}
			((BitStorable) obj).load(getBitInput(recNo));
		}
	}

	private void seekRecord(int recNo) throws IOException {
		// THE FOLLOWING DOES NOT WORK IF SOMEBODY DECIDES TO READ A RECORD
		// ONLY PARTIALLY!
//		if (currentRecNo != recNo) {
			indexFile.get(recNo, mutLong);   // !! Multi-threading !!
			dataFile.seek(mutLong.getValue());
//		}
		currentRecNo = recNo + 1; // ..because now, we are right after recNo
	}


	public DataInput getDataInput(int recNo) throws IOException {
		seekRecord(recNo);
		return dataFile;
	}


	public void close() throws IOException {
		// Make sure the header contains all the newest information
		if (!dontWriteHeader) {
			writeDataHeader();
		}
		indexFile.close();
		dataFile.close();
	}


/*+**************************************************************************
 * Header
 ****************************************************************************/

	/**
	 * The index file's header information, most importantly, the size of a
	 * record and the number of records currently stored.
	 */
	static public class Header implements Storable {
		private float avgRecLen = 0;
		private int recordCount = -1;   // -1 means 'unknown as of now'
		private long created;
		private long lastModified = -1;


		public Header(Date created) {
			this.created = created.getTime();
		}
		public Header() {
			this(new Date());
		}

		public float getAvgRecLen() {
			return avgRecLen;
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
			buf.append("(avgRecLen=");
			buf.append(avgRecLen);
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
			out.writeFloat(avgRecLen);
			out.writeInt(recordCount);
			out.writeLong(created);
			out.writeLong(lastModified);
		}

		public void load(DataInput in) throws IOException {
			avgRecLen = in.readFloat();
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
		IndexSeqFile file = new IndexSeqFile("F:/Java/ISAM");
		MutableString str = new MutableString();
		int recCount;
		long len = 0;

		// Make sure there is some data...
		IndexSeqOutputStream.main(null);
		recCount = file.getRecordCount();
		System.out.println("There are " + recCount + " records in the file");
		// Change some simple data...
		file.get(1, str);
		System.out.println("Value of record 1 is '" + str + "'");
		file.get(111, str);
		System.out.println("Value of record 111 is '" + str + "'");
		for (int i = 1; i <= recCount; i++) {
			file.get(i, str);
			len += str.length();
		}
		System.out.println("The total data length is " + len);
	}

}