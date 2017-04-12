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
package org.bluemedialabs.mpid.ival;

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
public class IvalIndexer extends Indexer {
	static private final int MAX_DEPTH = 128;
	private DfLists termLists;
	private DfLists nodeLists;
	private IvalId iid = new IvalId();

	private int position = 0;  // The current XML node/IvalId start position
	private int[] startPosStack = new int[MAX_DEPTH];
	private int stackTop = 0;  // Next free element on stack


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public void index(String cfgName, Configuration config)
			throws IOException {
		System.out.println("\n__________________IvalIndexer___________________");
		DataFileOutputChannelSeq outSeq;
		DfLists termLists, nodeLists;
		Args args;
		IvalIndexer indexer;
		String indexTemp = config.getProperty(cfgName, "IndexTemp");
		String termFileBaseName =
				indexTemp + "/" + config.getProperty(cfgName, "IidTermFileBaseName")
				+ ".data";
		String nodeFileBaseName =
				indexTemp + "/" + config.getProperty(cfgName, "IidNodeFileBaseName")
				+ ".data";
		int inMemTermDfListElems =
				config.getIntProperty(cfgName, "IidInMemTermDfListElems");
		int inMemNodeDfListElems =
				config.getIntProperty(cfgName, "IidInMemNodeDfListElems");


		Mingler.removeOldFiles(termFileBaseName);
		Mingler.removeOldFiles(nodeFileBaseName);
		args = createArgs(config.getProperty(cfgName, "SourceHome"));
		outSeq = new DataFileOutputChannelSeq(termFileBaseName, 64000);
		termLists = new DfLists(new IvalId(), inMemTermDfListElems,
								args.termCount, outSeq, args.guide,
								new IvalDfList(5000, args.guide), false, true);
		// The above +1 is important as we are beginning our terms with 1
		// instead of 0 as usually
		outSeq = new DataFileOutputChannelSeq(nodeFileBaseName, 64000);
		nodeLists = new DfLists(new IvalId(), inMemNodeDfListElems,
								args.guide.countUniqueNames(null), outSeq, args.guide,
								new IvalDfList(2000, args.guide), false, true);
		indexer = new IvalIndexer(args, termLists, nodeLists);
		indexer.start();
		indexer.finalize();
		System.out.println("\nNODE START (1) AND END (" + indexer.getPosition()
						   + ") POSITIONS USED FOR INDEXING (Please, write this down!)");
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public IvalIndexer(XmlDecoderStream in, DataGuide guide,
			DfLists tdfLists, DfLists nodeDfLists) {
		super(in, guide);
		termLists = tdfLists;
		nodeLists = nodeDfLists;
	}

	public IvalIndexer(Args args, DfLists tdfLists, DfLists nodeDfLists) {
		this(args.xds, args.guide, tdfLists, nodeDfLists);
	}

	public void finalize() throws IOException {
		termLists.finalize();
		nodeLists.finalize();
	}

	public int getPosition() {
		return position;
	}


	public void onAttribute(GuideNode node, HashMap termCounts) {
		handleNodeAndTerms(node, termCounts, ++position, position);
	}

	private void handleNodeAndTerms(GuideNode node, HashMap termCounts,
									int startPos, int endPos) {
		// Add node to node index
		iid.setStart(startPos);
		iid.setEnd(endPos);
		iid.setLevel((byte) getCurrentDepth());
		try {
			nodeLists.add(getDataGuide().getNodeLabelNo(node.getName()), iid);
		} catch (IOException e) {
			throw new RuntimeException("Problems adding ival node " + iid
										+ " to nodeLists", e);
		} catch (ArrayIndexOutOfBoundsException e2) {
			System.out.println("Adding node " + node + " with label# "
							   + getDataGuide().getNodeLabelNo(node.getName()));
			throw e2;
		}
		// Add terms to term index
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
					//				counter = (MutableInteger) entry.getValue();
					termLists.add(termNo.getValue(), iid);
				}
			} catch (IOException e) {
				throw new RuntimeException("Problems adding terms for ival node "
						+ iid + " to termLists", e);
			}
		}
	}


	public void onElementStart(GuideNode node) {
		// Remember position of start tag
		startPosStack[stackTop++] = ++position;
	}

	public void onElementEnd(GuideNode node, HashMap termCounts) {
		handleNodeAndTerms(node, termCounts, startPosStack[--stackTop],
						   ++position);
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	public static void main(String[] args) throws Exception {
		StopWatch watch = new StopWatch();
		Configuration config = Configuration.load(args);
		String cfgName = args[0];

		try {
			watch.start();
			IvalIndexer.index(cfgName, config);
			watch.stop();
			System.out.println("Complete elapsed time: " + watch);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}