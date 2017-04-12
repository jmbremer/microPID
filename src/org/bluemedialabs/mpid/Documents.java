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
package org.bluemedialabs.mpid;

import java.io.IOException;
import org.bluemedialabs.io.IndexSeqFile;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Documents {
	private IndexSeqFile file;
	private int docCount;

	// This might be only temporary:
	private short[] wordCounts;
	private short[] uniqueCounts;


	public Documents(IndexSeqFile file) throws IOException {
		if (file == null) {
			throw new NullPointerException("Supplied document database file "
				+ "is null but required");
		}
		docCount = file.getRecordCount();
		this.file = file;
		initCounts();
	}

	private void initCounts() throws IOException {
		Document doc = new Document();
		wordCounts = new short[docCount + 1];
		uniqueCounts = new short[docCount + 1];
		int wc, uc;

		for (int i = 1; i <= docCount; i++) {
			file.get(i, doc);
			wc = doc.getWordCount();
			uc = doc.getUniqueCount();
			if (wc > Short.MAX_VALUE || uc > Short.MAX_VALUE) {
				throw new IllegalStateException("Maximum short number is "
					+ Short.MAX_VALUE + " but wordCount (" + wc + ") or "
					+ "uniqueCount (" + uc + ") for document " + doc
					+ " is greater than that");
			}
			wordCounts[i] = (short) wc;
			uniqueCounts[i] = (short) uc;
		}
	}

	public Document get(int no, Document doc) throws IOException {
		// Look into cache...and if the document with the given number is not
		// in there yet...
		file.get(no, doc);
		return doc;
	}

	public Document get(int no) throws IOException {
		return get(no, new Document());
	}

	public int getDocCount() {
		return docCount;
	}


	public short getWordCount(int docNo) {
		return wordCounts[docNo];
	}

	public short getUniqueCount(int docNo) {
		return uniqueCounts[docNo];
	}


	public static void main(String[] args) throws Exception {
		Documents documents1 = new Documents(null);
	}
}