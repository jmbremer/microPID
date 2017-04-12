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
//import org.bluemedialabs.mpid.ival.*;
//import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;




/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class DfCountIndexer extends Indexer {
	static private final int MAX_DEPTH = 128;

	private NodeLists addrLists;
	private MutableInteger addr = new MutableInteger();
	private String fileName;
	private int[] dfWordCount = new int[MAX_DEPTH];
	private boolean written = false;


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public void index(String cfgName, Configuration config)
			throws IOException {
		System.out.println("\n__________________DfCountIndexer___________________");
		Args args = createArgs(config.getProperty(cfgName, "SourceHome"));
		DfCountIndexer indexer;
		// Obtain configuration arguments
		String indexHome = config.getProperty(cfgName, "IndexHome");
		String indexTemp = config.getProperty(cfgName, "IndexTemp");
		String physAddrFileBaseName =
				config.getProperty(cfgName, "DfCountFileBaseName");
		// Show that we need variable-size elements (df counters)
		int bitLen = -1;
		NodeLists nl = new NodeLists(args.guide, indexTemp, bitLen,
				indexHome + "/" + physAddrFileBaseName);
		// Get the indexer going
		indexer = new DfCountIndexer(args, nl);
		indexer.start();
		indexer.finalize();
	}



	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public DfCountIndexer(XmlDecoderStream in, DataGuide guide,
						   NodeLists nodeLists) {
		super(in, guide);
		addrLists = nodeLists;
//		new ArrayLists(addr, guide.getTotalNodeCount(),
//								   guide.getNodeCount() + 1);
//		this.addrBitLen = addrBitLen;
//		this.fileName = fileName;
	}

	public DfCountIndexer(Args args, NodeLists nodeLists) {
		this(args.xds, args.guide, nodeLists);
	}

	public void finalize() throws IOException {
		/*
		IndexSeqOutputChannel ch;
		ListWriter writer;
		int bitSize = getAddrBitLen();

		// Store address lists...
		System.out.println();
		System.out.println("Storing physical address index...");
		System.out.println("Total # of elements.............. " +
							addrLists.getUsedElems());
		System.out.println("Length of addresses [bits]....... " + getAddrBitLen());
		ch = IndexSeqOutputChannel.create(fileName, 524288, 2097152, true);
		for (int i = 1; i < addrLists.getListCount(); i++) {
			writer = new ListWriter(addrLists, i, bitSize);
			ch.write(writer);
		}
		ch.close();
		*/
		if (!written) {
			addrLists.mingle(new CountStorageCondition(getDataGuide()));
			written = true;
		}
	}


//	public int getAddrBitLen() { return addrBitLen; }


	public void onAttribute(GuideNode node, HashMap termCounts) {
		handleDfCount(node, termCounts, true);
	}

	private void handleDfCount(GuideNode node, HashMap termCounts,
							   boolean attrib) {
		int wordCount = 0;
		int termCount = termCounts.size();

		// Determine word count
		if (termCount > 0) {
			Iterator it = termCounts.entrySet().iterator();
			Map.Entry entry;
			MutableInteger counter;
			while (it.hasNext()) {
				entry = (Map.Entry) it.next();
				counter = (MutableInteger) entry.getValue();
				wordCount += counter.getValue();
			}
		} // otherwise: why bother!?

		dfWordCount[getCurrentDepth()] += wordCount;
		wordCount = dfWordCount[getCurrentDepth()];
		dfWordCount[getCurrentDepth() - 1] += wordCount;
		dfWordCount[getCurrentDepth()] = 0;
		// Add counter to counter lists
		try {
			addrLists.add(node.getNo(), wordCount - node.getMinDfWordCount());
//			if (node.getNo() == 11) {
//				System.out.println("  " + node.getNo() + "-" + wordCount + "/" + node.getMinDfWordCount());
//			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public void onElementStart(GuideNode node) {
	}

	public void onElementEnd(GuideNode node, HashMap termCounts) {
		handleDfCount(node, termCounts, false);
	}


	/**
	 * <p>Condition under which node lists of counters actually should be
	 * stored.</p>
	 *
	 * @author J. Marco Bremer
	 * @version 1.0
	 */
	static class CountStorageCondition implements NodeLists.StorageCondition {
		private DataGuide guide;

		public CountStorageCondition(DataGuide guide) {
			this.guide = guide;
		}

		public boolean store(int nodeNo) {
			return !(guide.getNode(nodeNo).getDfWordCountBitLen() == 0);
		}
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	public static void main(String[] args) throws Exception {
		StopWatch watch = new StopWatch();
		Configuration config = Configuration.load(args);
		String cfgName = args[0];

		watch.start();
		DfCountIndexer.index(cfgName, config);
		watch.stop();
		System.out.println("Complete elapsed time: " + watch);
	}



}