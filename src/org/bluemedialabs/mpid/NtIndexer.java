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
import org.bluemedialabs.mpid.*;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class NtIndexer extends Indexer {
	static public final String NODETERM_FILE_BASENAME= "nodeterm";
	static public final String NODESTATS_FILENAME    = "nodestat.data";
//	static private final int MAX_DEPTH = 128;

	static public final int IN_MEM_NODETERM_LIST_ELEMS = 4000000; // 00

	private DfLists nodeLists;
	private TermCounter nodeCount = new TermCounter();

//	private NodeCounter nodeCount = new NodeCounter();
	private String repDir;  // Needed to create stats file

//	private int position = 0;  // The current XML node/IvalId start position
//	private int[] startPosStack = new int[MAX_DEPTH];
//	private int stackTop = 0;  // Next free element on stack


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public void index(String repDir) throws IOException {
		System.out.println("\n__________________NtIndexer___________________");
		DataFileOutputChannelSeq outSeq;
		DfLists termLists, nodeLists;
		Args args;
		NtIndexer indexer;
		String baseFileName = repDir +"/" + NODETERM_FILE_BASENAME
					  + ".data";

		Mingler.removeOldFiles(baseFileName);
		args = createArgs(repDir);
		// The +1 is important as we are beginning our terms with 1
		// instead of 0 as usually
		outSeq = new DataFileOutputChannelSeq(baseFileName, 64000);
		nodeLists = new DfLists(new TermCounter(), IN_MEM_NODETERM_LIST_ELEMS,
								args.guide.getNodeCount() + 1, outSeq, args.guide,
								new NodeTermList(), false, false);
		indexer = new NtIndexer(args, nodeLists, repDir);
		indexer.start();
		indexer.finalize();
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public NtIndexer(XmlDecoderStream in, DataGuide guide, DfLists nodeLists,
					 String repDir) {
		super(in, guide);
		this.nodeLists = nodeLists;
		this.repDir = repDir;
	}

	public NtIndexer(Args args, DfLists nodeLists, String repDir) {
		this(args.xds, args.guide, nodeLists, repDir);
	}

	public void finalize() throws IOException {
		nodeLists.finalize();
		// Store node statistics
//		storeNodeStats();
	}

//	private void storeNodeStats() throws IOException {
//		BufferedOutputChannel out = BufferedOutputChannel.create(
//				repDir + "/" + NODESTATS_FILENAME);
//		// Write length = # of nodes field
//		out.writeInt(stats.length - 1);
//		// Write all the single node stats
//		for (int i = 1; i < stats.length; i++) {
//			stats[i].store(out);
//		}
//		out.flush();
//		out.close();
//	}


	public void onAttribute(GuideNode node, HashMap termCounts) {
		handleNode(node, termCounts);
	}

	private void handleNode(GuideNode node, HashMap termCounts) {
		int currTermCount = termCounts.size();

		// Add terms to term index
		if (currTermCount > 0) {
			// If there are any terms to index...
			Iterator it = termCounts.entrySet().iterator();
			Map.Entry entry;
			MutableInteger termNo;
			MutableInteger counter;

//			try {
				while (it.hasNext()) {
					entry = (Map.Entry) it.next();
					termNo = (MutableInteger) entry.getKey();
					counter = (MutableInteger) entry.getValue();
					/*
					 * Actually maintaining node-term information
					 * appears to be too expensive!??.....
					 * So, we might have to stick to the statistics only.
					 */
//					nodeCount.setTermNo(termNo.getValue());
//					nodeCount.setCount(1);
//					nodeLists.add(no, nodeCount);
				}
//			} catch (IOException e) {
//				throw new RuntimeException("Problems adding terms for node "
//						+ node + " to node-term lists", e);
//			}
		}
	}


	public void onElementStart(GuideNode node) {
		// Nothing to do here!?!
	}

	public void onElementEnd(GuideNode node, HashMap termCounts) {
		handleNode(node, termCounts);
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	public static void main(String[] args) throws Exception {
		StopWatch watch = new StopWatch();
		Configuration config = Configuration.load(args[1]);

		watch.start();
		NtIndexer.index(config.getProperty(args[0], "SourceHome"));
		watch.stop();
		System.out.println("Complete elapsed time: " + watch);
	}
}