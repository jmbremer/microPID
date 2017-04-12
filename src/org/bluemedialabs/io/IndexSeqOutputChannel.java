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
import org.bluemedialabs.util.MyMath;
import org.bluemedialabs.util.StopWatch;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class IndexSeqOutputChannel extends IndexSeqBitOutput {
	private IndexOutChannel indexOut;
	private WritableDataChannel dataOut;

//	private long dataPos;       // Make that a MutableLong!!
	private int recordCount = 0;
	private int headerSize;
	private long lastPosition = -1;

	private MutableLong ml = new MutableLong();


/*+**************************************************************************
 * Class functions
 ****************************************************************************/

   /**
	*
	* @param baseName
	* @param idxBufferSize
	* @param dataBufferSize
	* @return
	* @throws IOException
	*/
	static public IndexSeqOutputChannel create(String baseName, int idxBufferSize,
			int dataBufferSize, boolean bitOutput)	throws IOException {
		BufferedOutputChannel idxOut = BufferedOutputChannel.create(
				baseName +  IndexSeqFile.INDEX_FILE_ENDING, idxBufferSize);
		BufferedOutputChannel dataOut = BufferedOutputChannel.create(
				baseName +  IndexSeqFile.DATA_FILE_ENDING, dataBufferSize);
		return new IndexSeqOutputChannel(idxOut, dataOut, bitOutput);
	}

	/**
	 *
	 * @param baseName
	 * @return
	 * @throws IOException
	 */
	static public IndexSeqOutputChannel create(String baseName, boolean bitOutput)
			throws IOException {
		return create(baseName, BufferedOutputChannel.MIN_BUFFER_SIZE,
					  BufferedOutputChannel.MIN_BUFFER_SIZE, bitOutput);
	}

	/**
	 * Creates a new index sequential output channel from the given index and
	 * data channel. This function in particular allows for both channels to
	 * be easily created on different devices (disks) which might speedup
	 * writing somewhat.
	 *
	 * @param idxChannel
	 * @param dataChannel
	 * @return A newly created index sequential output channel.
	 * @throws IOException
	 */
	static public IndexSeqOutputChannel create(WritableDataChannel idxChannel,
			WritableDataChannel dataChannel, boolean bitOutput) throws IOException {
		return new IndexSeqOutputChannel(idxChannel, dataChannel, bitOutput);
	}


/*+**************************************************************************
 * Object functions
 ****************************************************************************/

	public IndexSeqOutputChannel(WritableDataChannel idx, WritableDataChannel seq,
								 boolean bitOutput)
			throws IOException {
		super(bitOutput);
		indexOut = new IndexOutChannel(idx, MINUS_ONE);
		dataOut = seq;
		writeHeader();
		lastPosition = dataOut.position();
	}

	public IndexSeqOutputChannel(WritableDataChannel idx, WritableDataChannel seq)
			throws IOException {
		this(idx, seq, DEFAULT_BIT_OUTPUT);
	}

	private void writeHeader() throws IOException {
		IndexSeqFile.Header header;

		header = new IndexSeqFile.Header(new Date());
		header.store(dataOut);
		// Here, some header information is missing, of course.
		// But, that can be fixed as soon as the data destination
		// is opened as an IndexSeqFile.
//		dataPos = header.byteSize();
//		log.log("IdxSeqOutputStream created with header " + header);
		headerSize = header.byteSize();
	}


	public float getAvgRecLen() throws IOException {
		float avg;
		if (recordCount == 0) {
			avg = 0;
		} else {
			avg = (float) (
					(double) (dataOut.position() - headerSize) / recordCount);
		}
		return avg;
	}

	public int getRecordCount() {
		return recordCount;
	}


	public int write(Storable obj) throws IOException {
		long pos;

		if (obj == null) {
			// There is no object for the current record position
			indexOut.write(MINUS_ONE);
		} else {
			lastPosition = dataOut.position();
			ml.setValue(lastPosition);
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
		}
		return ++recordCount;
	}


	public DataOutput getDataOutput() {
		return dataOut;
	}

	// This method has to be called AFTER the data record has been written!
	// Caller needs to make sure, flush() has been called before!
	public int nextRecord() throws IOException {
		long pos = dataOut.position();
		if (pos == lastPosition) {
			// Nothing has been written in between
			indexOut.write(MINUS_ONE);
		} else {
			// Something has been written thus, it is save to write the
			// actual current position
			ml.setValue(lastPosition);
			indexOut.write(ml);     // Write current start position
			// ...no object to be stored here...
			lastPosition = pos;
		}
		return ++recordCount;
	}


	public void close() throws IOException {
		indexOut.close();
		dataOut.close();
	}


	public String toString() {
		StringBuffer buf = new StringBuffer(128);

		buf.append("(idxSize=");
		buf.append(indexOut.getRecordCount() * indexOut.getRecordLen());
		buf.append(", recCount=");
		buf.append(indexOut.getRecordCount());
		buf.append(", dataPos=");
		try {
			buf.append(dataOut.position());
		} catch (IOException e) {
			buf.append("<could not be determined>");
		}
		buf.append(")");
		return buf.toString();
	}

	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
//		final int RECORD_LEN = 1024;
		final String TEXT = "This is a little test string that gets longer "
				+ "and longer and longer and even longer..................";

		IndexSeqOutputChannel isoc = IndexSeqOutputChannel.create("F:/Java/ISAM", false);
		StopWatch watch = new StopWatch();
		String str;
		CharBuffer buf;
		StorableCharBuffer sbuf;

		// Write some simple data...
		buf = CharBuffer.wrap(TEXT);
		sbuf = new StorableCharBuffer(buf);
		System.out.println("Starting to write data... (time is 0)");
		watch.start();
		for (int i = 1; i <= 1000000; i++) {
			buf.position(0);
			buf.limit(MyMath.random(1, TEXT.length() - 1));
			isoc.write(sbuf);
		}
		isoc.close();
		watch.stop();
		System.out.println("Done writing data. (time is " + watch + ")");
		System.out.println("Avg. record length is " + isoc.getAvgRecLen());
		System.out.println("Channel is " + isoc);
	}
}