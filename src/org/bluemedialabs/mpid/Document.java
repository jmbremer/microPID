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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.bluemedialabs.io.Storable;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */

public class Document implements Cloneable, Storable {
	private int no = 0;
	private String id = null;
	private int wordCount = 0;      // Length
	private int uniqueCount = 0;    // Might be used in the future (e.g., SMART)


	protected Document(int no) {
		this.no = no;
	}
	protected Document() {
		this(0);
	}

	public Object clone() {
		Document doc = new Document(no);
		clone(doc);
		return doc;
	}

	void clone(Document doc) {
		doc.id = id;
		doc.wordCount = wordCount;
		doc.uniqueCount = uniqueCount;
	}

	void setNo(int no) { this.no = no; }
	public int getNo() { return no; }

	void setId(String id) { this.id = id;	}
	public String getId() { return id; }

	void setWordCount(int wc) { wordCount = wc; }
	void incWordCount(int wc) { wordCount += wc; }
	public int getWordCount() { return wordCount; }

	void setLength(int len) { setWordCount(len); }
	public int length() { return getWordCount(); }

	void setUniqueCount(int uc) { uniqueCount = uc; }
	public int getUniqueCount() { return uniqueCount; }

	public String toString() {
		StringBuffer buf = new StringBuffer(id.length() + 60);

		buf.append("(no=");
		buf.append(no);
		buf.append(", id='");
		buf.append(id);
		buf.append("', length=");
		buf.append(wordCount);
		buf.append(", unique=");
		buf.append(uniqueCount);
		buf.append(")");
		return buf.toString();
	}


	/*+**********************************************************************
	 * Storable implementation
	 ************************************************************************/

	 public void store(DataOutput out) throws IOException {
		out.writeInt(no);
		out.writeInt(wordCount);
		out.writeInt(uniqueCount);
		out.writeUTF(id);
	}

//	public void store(IndexSeqOutputStream out) throws IOException {
//		out.write(this);
//	}

	public void load(DataInput in) throws IOException {
		no = in.readInt();
		wordCount = in.readInt();
		uniqueCount = in.readInt();
		id = in.readUTF();
	}

//	public void load(IndexSeqFile file) throws IOException {
//
//	}

	public int byteSize() {
		return -1;  // don't know
	}
}