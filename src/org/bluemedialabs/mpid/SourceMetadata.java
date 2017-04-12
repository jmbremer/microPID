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

import java.io.*;
import org.bluemedialabs.util.Configuration;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class SourceMetadata {
	// File sizes
	private long xmlSize;
	private long compressedSize;
	private long termFileSize;
	private long tokenFileSize;
	private long pIndexSize;
	private long aIndexSize;
	private long dcIndexSize;
	private long tIndexSize;

	// Other metadata
	private int termCount;
	private int tokenCount;
	private int wordCount;
	private int nodeCount;
	private int attribCount;
	private int labelPathCount;
	private int labelCount;
	private int depth;


	static public SourceMetadata create(Configuration config, String cfgName)
			throws IOException {

		return null;
	}


	public SourceMetadata() {}

	public long getXmlSize() { return xmlSize; }
	public long getCompressedSize() { return compressedSize; }
	public long getTermFileSize() { return termFileSize; }
	public long getTokenFileSize() { return tokenFileSize; }
	public long getPIndexSize() { return pIndexSize; }
	public long getAIndexSize() { return aIndexSize; }
	public long getDcIndexSize() { return dcIndexSize; }
	public long getTIndexSize() { return tIndexSize; }

	// Other metadata
	public int getTermCount() { return termCount; }
	public int getTokenCount() { return tokenCount; }
	public int getWordCount() { return wordCount; }
	public int getNodeCount() { return nodeCount; }
	public int getAttribCount() { return attribCount; }
	public int getLabelPathCount() { return labelPathCount; }
	public int getLabelCount() { return labelCount; }
	public int getDepth() { return depth; }
}