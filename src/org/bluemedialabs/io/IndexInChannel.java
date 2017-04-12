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
import org.bluemedialabs.util.MutableInteger;


/**
 *
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class IndexInChannel {
	private ReadableDataChannel in;
	private IndexFile.Header header;
	private int recordLen;
	private int recordCount = 0;


	public IndexInChannel(ReadableDataChannel in, Storable obj)
			throws IOException {
		this.in = in;
		readHeader(obj);
	}

	private void readHeader(Storable obj)
			throws IllegalArgumentException, IOException {
		header = new IndexFile.Header(-1, null);
		header.load(in);
//		log.log("IndexInputStream created with header " + header);
		recordLen = header.getRecordLen();
		if (obj.byteSize() != recordLen) {
//			log.log("Invalid record length in IndexInputStream");
			throw new IllegalArgumentException("Index file contains objects "
				+ "of (fixed) length " + recordLen + ", but given sample "
				+ "object has length " + obj.byteSize());
		}
	}


	/**
	 * Returns the size of each object (=record) within this index strore.
	 */
	public int getRecordLen() {
		return recordLen;
	}

	/**
	 * Returns the number of objects (=records) currently read from this stream.
	 */
	public int getRecordCount() {
		return recordCount++;
	}


	public void read(Storable obj) throws IOException {
		// The following check could be eliminated in the final version:
		if (obj.byteSize() != recordLen) {
			throw new IllegalArgumentException("Something is wrong with the "
				+ "object to be read. Object has byte size " + obj.byteSize()
				+ "but fixed index file record length is " + recordLen);
		}
		recordCount++;
		// Let the object write itself to us (or rather our super class)
		obj.load(in);
	}

	public void close() throws IOException {
		in.close();
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		BufferedInputChannel bic = BufferedInputChannel.create("F:/Java/fos.test");
		MutableInteger mutInt = new MutableInteger(1);
		IndexInChannel iic = new IndexInChannel(bic, mutInt);
		long sum = 0;

		// Write some simple data...
		for (int i = 1; i <= 1000000; i++) {
			iic.read(mutInt);
			if (i <= 100) {
				System.out.println(mutInt);
				sum += mutInt.getValue();
			}
		}
		iic.close();
		System.out.println("Sum of first 100 numbers found in index file is "
				+ sum);
	}

}