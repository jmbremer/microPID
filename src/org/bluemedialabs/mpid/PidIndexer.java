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
public class PidIndexer extends Indexer {
	static private final int MAX_DEPTH = 128;
	private DfLists termLists;
	private DfLists nodeLists;
	private PathId pid = new PathId();
	private MutableInteger bits = new MutableInteger();

//	private int position = 0;  // The current XML node/PathId start position
//	private int[] startPosStack = new int[MAX_DEPTH];
//	private int stackTop = 0;  // Next free element on stack


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public void index(String cfgName, Configuration config)
			throws IOException {
		System.out.println("\n__________________PidIndexer___________________");
		DataFileOutputChannelSeq outSeq;
		DfLists termLists = null, nodeLists;
		Args args;
		PidIndexer indexer;
		String indexTemp = config.getProperty(cfgName, "IndexTemp");
		String termFileBaseName =
				indexTemp + "/" + config.getProperty(cfgName, "PidTermFileBaseName")
				+ ".data";  // This should be improved...
		String nodeFileBaseName =
				indexTemp + "/" + config.getProperty(cfgName, "PidNodeFileBaseName")
				+ ".data";
		int inMemTermDfListElems =
				config.getIntProperty(cfgName, "PidInMemTermDfListElems");
		int inMemNodeDfListElems =
				config.getIntProperty(cfgName, "PidInMemNodeDfListElems");

		Mingler.removeOldFiles(termFileBaseName);
		Mingler.removeOldFiles(nodeFileBaseName);
		args = createArgs(config.getProperty(cfgName, "SourceHome"));
		outSeq = new DataFileOutputChannelSeq(termFileBaseName, 64000);
		termLists = new DfLists(new PathId(), inMemTermDfListElems,
								args.termCount + 1, outSeq, args.guide,
								new TermDfList(5000, args.guide), false, true);
//								new InvertedList(new PathId()), false, false);
		// The above +1 is important as we are beginning our terms with 1
		// instead of 0 as usually
		// DISABLED, 2003-01-23, JMB
//		outSeq = new DataFileOutputChannelSeq(nodeFileBaseName, 64000);
//		nodeLists = new DfLists(new MutableInteger(), inMemNodeDfListElems,
//								args.guide.getNodeCount() + 1, outSeq, args.guide,
//								new NodeDfList(2000, args.guide),false, true);
		nodeLists = null;
		// !!! Here we are !!!
		indexer = new PidIndexer(args, termLists, nodeLists);
		indexer.start();
		indexer.finalize();
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public PidIndexer(XmlDecoderStream in, DataGuide guide,
			DfLists tdfLists, DfLists nodeDfLists) {
		super(in, guide);
		termLists = tdfLists;
		nodeLists = nodeDfLists;
	}

	public PidIndexer(Args args, DfLists tdfLists, DfLists nodeDfLists) {
		this(args.xds, args.guide, tdfLists, nodeDfLists);
	}

	public void finalize() throws IOException {
		termLists.finalize();
//		nodeLists.finalize();
	}



	public void onAttribute(GuideNode node, HashMap termCounts) {
		handleNodeAndTerms(node, termCounts, true);
	}

	private void handleNodeAndTerms(GuideNode node, HashMap termCounts,
									boolean attrib) {
		// Add node to node index
		// DISABLED, 2003-01-23, JMB
		node.getPid(pid);
//		bits.setValue(pid.getPosBits());
//		try {
//			nodeLists.add(node.getNo(), bits);
//		} catch (IOException e) {
//			throw new RuntimeException("Problems adding pid node " + pid
//										+ " to nodeLists", e);
//		}
		// Add terms to term index
		/*
		 * DO this later!!!
		 *
		 */
		if (termCounts.size() > 0) {
			// If there are any terms to index...
			Iterator it = termCounts.entrySet().iterator();
			Map.Entry entry;
			MutableInteger termNo;
			MutableInteger counter;

			try {
				while (it.hasNext()) {
					entry = (Map.Entry) it.next();
					termNo = (MutableInteger) entry.getKey();
					// THIS IS LATER FOR IR:
//					counter = (MutableInteger) entry.getValue();
//					pid.setCount(counter.getValue());
					termLists.add(termNo.getValue(), pid);
				}
			} catch (IOException e) {
				throw new RuntimeException("Problems adding terms for PID node "
						+ pid + " to termLists", e);
			}
		}
	}


	public void onElementStart(GuideNode node) {
	}

	public void onElementEnd(GuideNode node, HashMap termCounts) {
		handleNodeAndTerms(node, termCounts, false);
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	public static void main(String[] args) throws Exception {
		StopWatch watch = new StopWatch();
		Configuration config = Configuration.load(args);
		String cfgName = args[0];

		watch.start();
		PidIndexer.index(cfgName, config);
		watch.stop();
		System.out.println("Complete elapsed time: " + watch);
	}
}