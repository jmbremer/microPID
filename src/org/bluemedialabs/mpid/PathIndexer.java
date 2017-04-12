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
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import org.bluemedialabs.mpid.ival.*;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;



/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class PathIndexer extends Indexer {
	static private final int MAX_DEPTH = 128;

	private DataOutputSequence fileSeq;
	private BitDataOutput bitOut = new BitDataOutput();
	private LongArrayLists lists;
	private long bits;
	private boolean written = false;


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public void index(String cfgName, Configuration config)
			throws IOException {
		System.out.println("\n__________________PathIndexer___________________");
		DataFileOutputChannelSeq outSeq;
		Args args;
		PathIndexer indexer;
		String indexTemp = config.getProperty(cfgName, "IndexTemp");
		String tmpFileBaseName = indexTemp + "/" +
			config.getProperty(cfgName, "NodeDfTmpFileBaseName");
		int inMemListElems =
				config.getIntProperty(cfgName, "InMemNodeDfListElems");

		NodeDfMingler.removeOldFiles(tmpFileBaseName);
		args = createArgs(config.getProperty(cfgName, "SourceHome"));

		outSeq = new DataFileOutputChannelSeq(tmpFileBaseName, 128000);
		indexer = new PathIndexer(args.xds, args.guide, outSeq, inMemListElems);
		indexer.start();
		indexer.finalize();
		outSeq.close();
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public PathIndexer(XmlDecoderStream in, DataGuide guide,
			DataOutputSequence fileSeq, int listsElemCount) {
		super(in, guide);
		this.fileSeq = fileSeq;
		lists = new LongArrayLists(listsElemCount, guide.getNodeCount() + 1);
	}


	public void finalize() throws IOException {
		if (!written) {
			writeData(true);
			fileSeq.close();
			written = true;
		}
	}


	void writeData(boolean finalize) throws IOException {
		DataGuide guide = getDataGuide();
		int posBitLen;
		Iterator it;
		int count;
		long pos;

//		System.out.println("\nWriting data to file " + fileSeq.getSeqNumber() + "...");
		bitOut.reattach(fileSeq);
		// Actually write data
		for (int i = 1; i <= guide.getNodeCount(); i++) {
//			if (i == 74) {
//				System.out.print("!!!");
//			}
//			int c = 0;
			if (!lists.isEmpty(i)) {
				posBitLen = guide.getNode(i).getTotalPosBitLen();
				// Write node#
				bitOut.write(i, 32);
				// Write # of list elements
				count = lists.getElemCount(i);
				bitOut.write(count, 32);
//				System.out.print(i + ") [" + count + "/" + posBitLen + "]");
				// Write position bits
				it = lists.listIterator(i);
				while (it.hasNext()) {
//					c++;
//					if (c > 137) {
//						System.out.print(" ");
//					}
					pos = ((MutableLong) it.next()).getValue();
					bitOut.write(pos, posBitLen);
//					if (i == 74) {
//						System.out.print(" " + pos);
//					}
//					System.out.print(" " + pos);
				}
//				if (i == 74) {
//					System.out.print(c);
//				}
//				System.out.println();
			}
		}
		bitOut.flush();
		lists.clear();
		// Go to the next temporary file in the sequence
		if (!finalize) {
			fileSeq.next();
		}
	}



	public void onAttribute(GuideNode node, HashMap termCounts) {
		handleNode(node, true);
	}

	private void handleNode(GuideNode node, boolean attrib) {
		// Add node to node index
		long bits = node.getPosBits();

		if (lists.isFull()) {
			try {
				writeData(false);
			} catch (IOException e) {
				throw new RuntimeException("Problems adding writing temp. node "
						+ "data to disk", e);
			}
		}
		lists.add(node.getNo(), bits);
	}


	public void onElementStart(GuideNode node) {
		handleNode(node, false);
	}

	public void onElementEnd(GuideNode node, HashMap termCounts) {}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	public static void main(String[] args) throws Exception {
		StopWatch watch = new StopWatch();
		Configuration config = Configuration.load(args);
		String cfgName = args[0];

		watch.start();
		PathIndexer.index(cfgName, config);
		watch.stop();
		System.out.println("Complete elapsed time: " + watch);
	}
}