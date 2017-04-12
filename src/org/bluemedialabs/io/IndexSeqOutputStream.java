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
import java.nio.channels.FileChannel;
import java.util.Date;
import org.bluemedialabs.util.MutableLong;
import org.bluemedialabs.util.MutableString;
import org.bluemedialabs.util.MyMath;
import org.bluemedialabs.util.StopWatch;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class IndexSeqOutputStream extends IndexSeqBitOutput {
	private IndexOutputStream indexOut;
	private DataOutputStream dataOut;
	private FileChannel fileOut;
	private long dataPos;       // Make that a MutableLong!!
	private int recordCount = 0;
	private int headerSize;
	private long lastPosition = -1;

	private MutableLong ml = new MutableLong();
//	private LogStream log;


/*+**************************************************************************
 * Class functions
 ****************************************************************************/

	static public IndexSeqOutputStream create(String baseName, boolean bitOutput)
			throws IOException {
		OutputStream idxOut;
		FileOutputStream dataOut;

		idxOut = new FileOutputStream(baseName + IndexSeqFile.INDEX_FILE_ENDING);
	//		idxOut = new BufferedOutputStream(idxOut, DEFAULT_INDEX_OUTPUT_BUFFER_SIZE);
		idxOut = new DataOutputStream(idxOut);
		dataOut = new FileOutputStream(baseName + IndexSeqFile.DATA_FILE_ENDING);
//		dataOut = new BufferedOutputStream(dataOut, DEFAULT_DATA_OUTPUT_BUFFER_SIZE);
//		dataOut = new DataOutputStream(dataOut);
		return new IndexSeqOutputStream(idxOut, dataOut, bitOutput);
	}


/*+**************************************************************************
 * Object functions
 ****************************************************************************/

	public IndexSeqOutputStream(OutputStream idx, FileOutputStream seq, boolean bitOutput)
			throws IOException {
		super(bitOutput);
		indexOut = new IndexOutputStream(idx, MINUS_ONE);
		dataOut = new DataOutputStream(seq);
		fileOut = seq.getChannel();
		writeHeader();
		lastPosition = fileOut.position();
	}

	private void writeHeader() throws IOException {
		IndexSeqFile.Header header;

		header = new IndexSeqFile.Header(new Date());
		header.store(dataOut);
		// Here, some header information is missing, of course.
		// But, that can be fixed as soon as the data destination
		// is opened as an IndexSeqFile.
		dataPos = header.byteSize();
//		log.log("IdxSeqOutputStream created with header " + header);
		headerSize = header.byteSize();
	}


	public float getAvgRecLen() {
		float avg;
		if (recordCount == 0) {
			avg = 0;
		} else {
			avg = (float) ((double) (dataPos - headerSize) / recordCount);
		}
		return avg;
	}

	public int getRecordCount() {
		return recordCount;
	}


	public int write(Storable obj) throws IOException {
//		int pos;

		if (obj == null) {
			// There is no object for the current record position
			indexOut.write(MINUS_ONE);
		} else {
			/*
			 * I am changing the below now...
			 * instead of relying on the size provided by the stream (which
			 * is limited to 2GB) the right size has to be determined by the
			 * storable now after each store operation. jmb 2002-06-xx
			 */
			lastPosition = fileOut.position();
			ml.setValue(lastPosition);
			// THE SIZE DOES ONLY WORK FOR FILES UP TO 2GB!!!?!
//			pos = fileOut.position();
			indexOut.write(ml);     // Write current start position
			if (isBitOutput()) {
				if (!( obj instanceof BitStorable)) {
					throw new IllegalStateException("Bit storage requested "
							+ "but supplied object does not know how to "
							+ "store itself that way");
				}
				((BitStorable) obj).store(getBitOutput(dataOut));
				flushBitOutput();
			} else {
				obj.store(dataOut);
			}
			// Adjust file position using the size determined by the storable
			// during the storing.
			dataPos = fileOut.position();
			// CAREFUL! THE BYTE SIZE OF THE OBJECT MIGHT NOT BE KNOWN UNTIL NOW
			// AS DEFINED IN THE STORABLE CLASS! REALLY??
		}
		return ++recordCount;
	}

	public DataOutput getDataOutput() {
		return dataOut;
	}


	// Caller needs to make sure, flush() has been called before!
	public int nextRecord() throws IOException {
		long pos = fileOut.position();
		if (pos == lastPosition) {
			// Nothing has been written in between
			indexOut.write(MINUS_ONE);
		} else {
			// Something has been written thus, it is save to write the
			// actual current position
			ml.setValue(lastPosition);
			indexOut.write(ml);     // Write current start position
			lastPosition = pos;
			// ...no object to be stored here...
		}
		return ++recordCount;
	}


	public void close() throws IOException {
		indexOut.close();
		dataOut.close();
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		final int RECORD_LEN = 1024;
		IndexSeqOutputStream isos = IndexSeqOutputStream.create("F:/Java/ISAM", false);
		StopWatch watch = new StopWatch();
		MutableString str;

		// Write some simple data...
		str = new MutableString("This is a little test string that gets longer "
				+ "and longer and longer and even longer..................");
//		System.out.println("str has length " + str.length() + ", appending "
//						   + (RECORD_LEN - 2 - str.length()) + " more chars...");
//		for (int i = 0; i < (RECORD_LEN - 2 - str.length()); i++) {
//			System.out.print('.');
//			str = str.concat("A");
//		}
//		System.out.println("Created record of length " + (str.length() + 2)
//						   + " Bytes.");
		System.out.println("Starting to write data... (time is 0)");
		watch.start();
		for (int i = 1; i <= 1000000; i++) {
			isos.write(str.substring(0, MyMath.random(1, str.length() - 1)));
//			isos.write(new MutableString("aaa"));
		}
		isos.close();
		watch.stop();
		System.out.println("Done writing data. (time is " + watch + ")");
		System.out.println("Avg. record length is " + isos.getAvgRecLen());
	}
}