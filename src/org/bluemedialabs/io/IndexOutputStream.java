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
import org.bluemedialabs.util.StopWatch;


/**
 * An output stream that writes objects with a fixed-length byte representation.
 * Before the first record is written a header with some meta information, e.g.,
 * the record byte size and counter, are written. However, some of the meta
 * data has to be left uninitialized as the information is available only at
 * the end of the stream writing (applies, for instance, for the record counter).
 * <p><em>Copyright (c) 2002 by J. Marco Bremer</em></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class IndexOutputStream {
	private DataOutputStream out;
	private int recordLen;
	private int recordCount = 0;
//	private LogStream log;


	public IndexOutputStream(OutputStream out, Storable obj)
			throws IOException {
		if (out instanceof DataOutputStream) {
			this.out = (DataOutputStream) out;
		} else {
			this.out = new DataOutputStream(out);
		}
		setRecordLen(obj);
	}

	private void setRecordLen(Storable obj)
			throws IllegalArgumentException, IOException {
		IndexFile.Header header;
		if (obj.byteSize() < 0) {
//			log.log("Invalid record length in IndexOutputStream");
			throw new IllegalArgumentException("Index file can only store "
				+ "fixed-size Storable(s), but given sample object has "
				+ "size " + obj.byteSize());
		}
		recordLen = obj.byteSize();
		// Create and write a new header data set
		header = new IndexFile.Header(recordLen, null);
		header.store(out);
		// Here, some header information is missing, of course.
		// But, that can be fixed as soon as the data destination
		// is opened as an IndexFile.
//		log.log("IndexOutputStream created with header " + header);
	}


	public int getRecordLen() {
		return recordLen;
	}

	public int getRecordCount() {
		return recordCount;
	}


	public void write(Storable obj) throws IOException {
		// The following check could be eliminated in the final version:
		if (obj.byteSize() != recordLen) {
			throw new IllegalArgumentException("Something is wrong with the "
				+ "object to be written. Object has byte size " + obj.byteSize()
				+ "but fixed index file record size is " + recordLen);
		}
		recordCount++;
		// Let the object write itself to us (or rather our super class)
		obj.store(out);
	}

	public void close() throws IOException {
		out.close();
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		FileOutputStream fos = new FileOutputStream("F:/Java/fos.test");
		BufferedOutputStream bos = new BufferedOutputStream(fos, 4096);
		DataOutputStream dos = new DataOutputStream(bos);
		MutableInteger mutInt = new MutableInteger(1);
		IndexOutputStream ios = new IndexOutputStream(dos, mutInt);
		StopWatch watch = new StopWatch();

		// Write some simple data...
		watch.start();
		for (int i = 1; i <= 10000000; i++) {
			mutInt.reuse(i);
			ios.write(mutInt);
		}
		ios.close();
		watch.stop();
		System.out.println("Done writing data. (time is " + watch + ")");
	}
}