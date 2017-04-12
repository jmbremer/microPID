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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Iterator;
//import org.bluemedialabs.io.*;
import org.bluemedialabs.util.Configuration;
import org.bluemedialabs.util.MyMath;
import org.bluemedialabs.util.Queue;
import org.bluemedialabs.util.StopWatch;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class DepIndexer extends Indexer {
//	static private final int MAX_DEPTH = 128;
	static final DecimalFormat percent = new DecimalFormat("##.#%");

//	private String fileName;
	private int nodeCount;
	private NodeDepStats[] nodeStats;
	private int treeDepth;
	private int[][] counts;


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public void index(String cfgName, Configuration config)
			throws IOException {
		System.out.println("\n__________________DepIndexer___________________");
		DepIndexer indexer;
//		String indexHome = config.getProperty(cfgName, "IndexHome");
		String sourceHome = config.getProperty(cfgName, "SourceHome");
		String statsHome = config.getProperty(cfgName, "StatsHome");
		String depStatsFileName = config.getProperty(cfgName, "DepStatsBaseName")
							+ config.getProperty(cfgName, "StatsFileEnding");
		Args args = createArgs(sourceHome);

		indexer = new DepIndexer(args.xds, args.guide);
		indexer.start();
		indexer.finalize();
		printStats(indexer.getNodeStats(), args.guide, cfgName,
								 statsHome + "/" + cfgName + "-" + depStatsFileName);
	}


	static void printStats(NodeDepStats[] stats, DataGuide guide, String id,
						   String depStatsFileName) throws IOException {
		PrintStream ps = new PrintStream(new FileOutputStream(depStatsFileName));
		Queue q = new Queue();

		ps.println("___________________________________________");
		ps.println("| NODE DEPENDENCY STATISTICS FOR >>>" + id + "<<<");
		ps.println("|  Generated on " + new java.sql.Timestamp(new java.util.Date().getTime()));
		ps.println("|__________________________________________");
		ps.println();
		ps.println("===========XML Source Statistics===========");
		ps.println("# of DataGuide nodes..... " + guide.getNodeCount());
		ps.println("Total # of nodes......... " + guide.getTotalNodeCount());
		ps.println("Max. depth............... " + guide.getMaxDepth());
		ps.println();
		ps.println("============General Statistics=============");
		printGeneralStats(stats, guide, ps);
		ps.println();
		ps.println("============Complete Statistics============");
		for (int i = 2; i < stats.length; i++) {
			// The 2 is for leaving out the root node!
			if (stats[i] != null) {
				ps.println();
				ps.println();
				ps.println("__________ CONTEXT NODE >>>" +
						   guide.getNode(i).getName() + "<<< __________");
				ps.println(stats[i].toString(guide, i));
			} else {
				q.enqueue(guide.getNode(i).getName() + "(#" + i + ")  " );
			}
		}
		ps.println();
		ps.println("\n___Nodes without meaningful node dependencies___");
		ps.println();
		while (!q.isEmpty()) {
			ps.print(q.dequeue());
		}
		ps.println("<Root node:> " + guide.getNode(1).getName() + " (#1)");
		ps.println();
		ps.println();
		ps.println("=============Notations=====================");
		ps.println();
		printNotations(ps);
		ps.println("___________________________________________");
		ps.close();
	}


	static void printGeneralStats(NodeDepStats[] stats, DataGuide guide,
							 PrintStream ps) throws IOException {
		int nodeCount = guide.getNodeCount();
		NodeDepStats s;
		long theorMaxPairs = MyMath.fac(nodeCount - 2);
		int actualPairs = 0;
		int equivPairs = 0, weakContrPairs = 0, strongContrPairs = 0;
		int aImplB = 0, bImplA = 0;

		for (int i = 2; i < stats.length; i++) {
			if (stats[i] != null) {
				s = stats[i];
				actualPairs += s.getPairCount();
				for (int j = 0; j < s.getPairCount(); j++) {
					if (s.isEquivalent(j)) equivPairs++;
					if (s.isWeakContradict(j)) weakContrPairs++;
					if (s.isStrongContradict(j)) strongContrPairs++;
					if (s.isAimpliesB(j)) aImplB++;
					if (s.isBimpliesA(j)) bImplA++;
				}
			}
		}
		ps.println("Theoret. max. pairs..... " +
				 (theorMaxPairs > 0? String.valueOf(theorMaxPairs): "<beyond 64 bits>"));
		ps.println("Actual pairs............ " + actualPairs + "  (not including root)");
		  ps.print("  ...equivalent......... " + equivPairs + " (");
		ps.println(percent.format(equivPairs / (double) actualPairs) + ")");
		ps.print("  ...purely implied..... " + (aImplB + bImplA - 2 * equivPairs) + " (");
		ps.println(percent.format((aImplB + bImplA - 2 * equivPairs) / (double) actualPairs) + ")");
		ps.print("    ...A implies B...... " + aImplB + " (");
		ps.println(percent.format(aImplB / (double) actualPairs) + ")");
		ps.print("    ...B implies A...... " + bImplA + " (");
		ps.println(percent.format(bImplA / (double) actualPairs) + ")");
		  ps.print("  ...weak contradict.... " + weakContrPairs + " (");
		ps.println(percent.format(weakContrPairs / (double) actualPairs) + ")");
		  ps.print("  ...strong contradict.. " + strongContrPairs + " (");
		ps.println(percent.format(strongContrPairs / (double) actualPairs) + ")");
	}

	static void printNotations(PrintStream ps) throws IOException {
		ps.println("Theoret. max. pairs   (nodeCount - 2)!");
		ps.println("Actual pairs          Pairs with meaningful dependencies (no nodes on same path)");
		ps.println("Equivalent            Nodes always occur together  <->");
		ps.println("Purely implied        A -> B or B -> A but not A <-> B");
		ps.println("A/B implies B/A       A -> B / B -> A, inculding A <->");
		ps.println("Weak contradict       Nodes never occur together");
		ps.println("Strong contradict     Exactly either of the nodes is always present");
	}



	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public DepIndexer(XmlDecoderStream in, DataGuide guide) {
		super(in, guide);
//		this.fileName = fileName;
		nodeCount = guide.getNodeCount();
		nodeStats = new NodeDepStats[nodeCount + 1];
		treeDepth = guide.getMaxDepth();
		init();
		counts = new int[treeDepth + 1][nodeCount + 1];

	}

	private void init() {
		DataGuide guide = getDataGuide();
		int[] pairCount = new int[nodeCount + 1];
		int contextNode, endNo;
		GuideNode node;
		NodeDepStats stats;

		// Pass one over XDG to determine how many node pairs have to be
		// allocated for each (context) node
		for (contextNode = 1; contextNode <= nodeCount; contextNode++) {
			node = guide.getNode(contextNode);
			endNo = node.getEndNo();
			if (node.isLeaf()) {
				pairCount[contextNode] = 0;
			} else {
				for (int i = contextNode + 1; i < endNo; i++) {
					for (int j = i + 1; j <= endNo; j++) {
						if (!guide.isDescendant(i, j)) {
							// This is a valid pair of nodes
							pairCount[contextNode]++;
						}
					}
				}
				// That's all; pretty expensive, isn't it.
			}
		}
		// Pass one and a half: create node statistics objects
		nodeStats[0] = null;
		for (int i = 1; i <= nodeCount; i++) {
			if (pairCount[i] > 0) {
				nodeStats[i] = new NodeDepStats(pairCount[i]);
			} else {
				nodeStats[i] = null;
			}
		}

		// Pass two over XDG to actually create the pairs nodes
		// (Same construction as above...)
		for (contextNode = 1; contextNode <= nodeCount; contextNode++) {
//			if (contextNode == 15) {
//				System.out.print(' ');
//			}
			node = guide.getNode(contextNode);
			endNo = node.getEndNo();
			stats = nodeStats[contextNode];
			if (!node.isLeaf()) {
				for (int i = contextNode + 1; i < endNo; i++) {
					for (int j = i + 1; j <= endNo; j++) {
						if (!guide.isDescendant(i, j)) {
							// This is a valid pair of nodes
							stats.addNodePair((short) i, (short) j);
						}
					}
				}
			}
		}
	}


	public void finalize() throws IOException {
		// Write statistics to file...
	}


	public void onAttribute(GuideNode node/*, HashMap termCounts*/) {
		handleStats(node, true);
	}

	private void handleStats(GuideNode node, boolean attrib) {
		int depth = getCurrentDepth();
		int no = node.getNo();
		int endNo = node.getEndNo();

		// Now, we have seen the current node one more time
		counts[depth - 1][no]++;
		// Figure out which node pairs have been seen, but only...
		if (nodeStats[no] != null) {
			// ..if there are any pairs to keep track of
			nodeStats[no].addStats(counts[depth]);
		}
		// Accumulate counters for the next lower (higher?) depth
		for (int i = no + 1; i <= endNo; i++) {
			counts[depth - 1][i] += counts[depth][i];
			counts[depth][i] = 0;
		}

	}


	public void onElementStart(GuideNode node) {

	}

	public void onElementEnd(GuideNode node/*, HashMap termCounts*/) {
		handleStats(node, false);
	}


	public NodeDepStats[] getNodeStats() { return nodeStats; }


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	public static void main(String[] args) throws Exception {
		StopWatch watch = new StopWatch();
		Configuration config = Configuration.load(args);
		String cfgName = args[0];

		watch.start();
		DepIndexer.index(cfgName, config);
		watch.stop();
		System.out.println("Complete elapsed time: " + watch);
	}
}