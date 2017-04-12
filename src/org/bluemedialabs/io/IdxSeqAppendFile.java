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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.bluemedialabs.util.MutableLong;


/**
 * <p>An index sequential file with initially empty data records to which
 * data can gradually be appended. The index file, however, has to exist in
 * advance. This means, the size of the data file and each of its records is
 * fixed.</p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class IdxSeqAppendFile extends IndexSeqFile {
	private int[] used;

	/**
	 * Given a prebuilt index file and the length of the last record, this
	 * function creates the related data file with the right size.
	 */
//	static public void prepareDataFile(String baseFileName, int recLen)
//			throws FileNotFoundException, IOException {
//		IndexFile indexFile = new IndexFile(
//				baseFileName + IndexSeqFile.INDEX_FILE_ENDING,
//				new MutableLong());
//		int recCount = indexFile.getRecordCount();
//		MutableLong lastRecPos = new MutableLong();
//		indexFile.get(recCount, lastRecPos);
//		long len = lastRecPos.getValue() + recLen;
//		RandomAccessFile file = new RandomAccessFile(
//				baseFileName + IndexSeqFile.DATA_FILE_ENDING, "rw");
//		file.setLength(len);
//	}
//
//
	public IdxSeqAppendFile(String baseName)
			throws FileNotFoundException, IOException {
		super(baseName);
	}
//		MutableLong posOfLastRec = new MutableLong();
//		indexChannel.get(indexChannel.getRecordCount(), posOfLastRec);
//		if (dataChannel.length() <= posOfLastRec.getValue()) {
//			// That looks like the data file has not been created properly yet!
//			throw new IllegalStateException("According to the index file, the "
//				+ "position of the last record is " + posOfLastRec
//				+ ". However, the data file is only " + dataChannel.size()
//				+ " bytes long currently. Please make sure to properly create "
//				+ "a right sized (empty) data file BEFORE constructing "
//				+ "the index sequential file '" + baseName + "'");
//		}
//		used = new int[indexChannel.getRecordCount() + 1];
//		for (int i = 0; i < used.length; i++) {
//			used[i] = 0;
//		}
//	}
//
//	public void append(int recNo, Storable obj) throws IOException {
//		MutableLong a = new MutableLong();
//		MutableLong b = new MutableLong();
//		int maxRecLen;
//
//		indexChannel.get(recNo, a);
//		// Determine maximum record length...
//		if (recNo < indexFile.getRecordCount()) {
//			// Check size (which
//			indexFile.get(recNo + 1, b);
//		} else {
//			b.setValue(dataFile.length());
//		}
//		maxRecLen = (int) (b.getValue() - a.getValue());
//		// ..and check whether there is enough space left for the given object
//		if (used[recNo] + obj.byteSize() > maxRecLen) {
//			throw new IllegalArgumentException("Cannot append object of length "
//				+ obj.byteSize() + " to record " + recNo + ". The maximum "
//				+ "length allowed is " + maxRecLen + ", but the currently used "
//				+ "(" + used[recNo] + ") permits only "
//				+ (maxRecLen - used[recNo]) + " more data bytes");
//		}
//		dataFile.seek(a.getValue() + used[recNo]);
//		obj.store(dataFile);
//		// That's it already!
//	}
}