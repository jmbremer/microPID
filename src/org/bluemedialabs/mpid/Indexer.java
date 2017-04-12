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
import java.util.List;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Indexer {
	static final boolean USE_CHANNELS = true;
	static private final int SOURCE_MAX_DEPTH = 60;

	private XmlDecoderStream in;
	private DataGuide guide;

	private Quack hashTables;    // Stack of hash tables for terms
	private Quack unusedTables;  // Currently unused hash tables
	private Quack mutIntPools;   // Stack of pools of mutable integers
	private Quack unusedPools;   // Currently unused pools
	private HashMap currentHash = null; //
	private	Pool currentPool = null; // Pool of mutable integers for a certain depth

	private long bitPosition;    // Current node bit position within XML source

	// Some values of statistical value...
	protected Stats stats;
	private int currentDepth = 0;  // Always the current depth in the tree!

	private int[] dfWordCount = new int[SOURCE_MAX_DEPTH];

	static protected PrintStream out = System.out;


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public void index(Configuration config, String cfgName)
			throws IOException {
		String fileName = config.getProperty(cfgName, "StatsBase") + "/" + cfgName + "-" +
					config.getProperty(cfgName, "IndexStatsBaseName") +
					config.getProperty(cfgName, "StatsFileEnding");
		PrintStream ps = new PrintStream(new FileOutputStream(fileName));
		String dgFileName = config.getProperty(cfgName, "SourceHome") + "/" +
					config.getProperty(cfgName, "DataGuideFileName");
		index(config.getProperty(cfgName, "SourceHome"), dgFileName, ps);
		System.out.print("Dumping DataGuide file...");
		DataGuide.dumpDataGuide(config, cfgName, false);
		System.out.println("done.");
	}

	static public void index(String repDir, String dgFileName, PrintStream ps)
			throws IOException {
		Args args = createArgs(repDir);
		args.guide.resetNodeCounters();
		Indexer indexer = new Indexer(args);
		indexer.out = ps;
		indexer.start();
		System.out.print("\nStoring DataGuide...");
		BufferedOutputChannel ch = BufferedOutputChannel.create(dgFileName);
		args.guide.store(ch);
		ch.flush();
		ch.close();
		System.out.println("done.");
		printStats(indexer.stats, args.guide);
	}

	static public void index(String repDir) throws IOException {
		index(repDir, "guide.tree", System.out);
	}

	static protected Args createArgs(String repDir) throws IOException {
		String SOURCE_FILE_NAME = repDir + "/xml.data";
		BitInput input;
		Tokens tokens = new Tokens();
		Token token, nextToken;
		String name, nextName;
		StringBuffer strBuf = new StringBuffer(256);
		String str;

		// Create a stream/channel for the input
		if (!USE_CHANNELS) {
			// FOR STREAMS...
			DataInputStream bis = new DataInputStream(new BufferedInputStream(
					new FileInputStream(SOURCE_FILE_NAME)));
			input = new BitDataInput(bis);
		} else {
			// FOR CHANNELS...
			BufferedInputChannel bic = new BufferedInputChannel(
					(new FileInputStream(SOURCE_FILE_NAME)).getChannel(), 245760);
			input = new BitDataInput(bic);
		}

		// Load tokens and construct huffman coder, etc.
		DataGuide guide = DataGuide.load(repDir + "/guide.tree");
		System.out.println("The maximum bit length for any PathId in this "
			+ "data guide is " + guide.computeMaxPidLength());

		// Tokens and Terms are replaced by single Decoder now!

		Decoder decoder = Decoder.load(repDir + "/decoder.data");

		System.out.print("Constructing decoder input stream...");
		XmlDecoderStream xds = new XmlDecoderStream(input, decoder);
		System.out.println("done.");

		Args args = new Args();
		args.xds = xds;
		args.guide = guide;
		args.termCount = decoder.getTermCount();
		return args;
	}

	static protected void printStats(Stats stats, DataGuide guide) {
		out.println("\n__________Indexing Statistics__________");
		out.println(stats);
		out.println();
		out.println("DataGuide and Related Node Statistics.....");
		out.println("Node#| Count| PidLen| Total [min,max] words| Total [min,max] terms| Data guide tree");
		printNodeStats(stats.nodeStats, guide);
		out.println("\n_______________________________________");
	}

	static private int maxPidLength;

	static protected void printNodeStats(NodeStats[] stats, DataGuide guide) {
		GuideNode n;
		// Determine how long node# will get in terms of characters
		int numLen = charLen(guide.getNodeCount());
		int[] fieldLens = new int[7];

		// Determine max. stat. field lengths in characters
		determineFieldLens(stats, fieldLens);
		maxPidLength = guide.computeMaxPidLength();
		// Get actual root (considered as depth 0)
		n = (GuideNode) guide.getRoot().getChildren().getFirst();
		printNodeStats(stats, n, 0, numLen, fieldLens);
	}

	static private int charLen(int no) {
		return (no == 0? 1: (int) Math.ceil(Math.log(no + 1) / Math.log(10)));
	}

	static private int maxCharLen(int a, int b) {
		return Math.max(a, charLen(b));
	}

	static private void determineFieldLens(NodeStats[] stats, int[] fl) {
		for (int i = 0; i < stats.length - 1; i++) {
			fl[0] = maxCharLen(fl[0], stats[i + 1].count);
			fl[1] = maxCharLen(fl[1], stats[i + 1].wordCount);
			// THE FOLLOWING IS A NECCESSARY CORRECTION!...
			// (..that should better have been taken care of earlier...)
			if (stats[i + 1].minWordCount > stats[i + 1].maxWordCount) {
				stats[i + 1].minWordCount = 0;
			}
			fl[2] = maxCharLen(fl[2], stats[i + 1].minWordCount);
			fl[3] = maxCharLen(fl[3], stats[i + 1].maxWordCount);
			fl[4] = maxCharLen(fl[4], stats[i + 1].termCount);
			// THE FOLLOWING IS A NECCESSARY CORRECTION!
			if (stats[i + 1].minTermCount > stats[i + 1].maxTermCount) {
				stats[i + 1].minTermCount = 0;
			}
			fl[5] = maxCharLen(fl[5], stats[i + 1].minTermCount);
			fl[6] = maxCharLen(fl[6], stats[i + 1].maxTermCount);
		}
	}

	static private void printNodeStats(NodeStats[] stats, GuideNode n,
									   int depth, int noLen, int[] fieldLens) {
		String PREFIX = "|   ";
		StringBuffer buf = new StringBuffer(100);
		List children;
		Iterator it;
		int no = n.getNo();

		if (n != null) {
			// Print node#
			for (int i = 0; i < (noLen - charLen(n.getNo())); i++) {
				System.out.print(' ');
			}
			print(n.getNo(), noLen);
			out.print("  ");

			// Print node statistics
			print(stats[no].count, fieldLens[0]);
			out.print(' ');
			print(n.getTotalPosBitLen(), charLen(maxPidLength));
			out.print(' ');
			print(stats[no].wordCount, fieldLens[1]);
			out.print(" [");
			print(stats[no].minWordCount, fieldLens[2]);
			out.print(',');
			print(stats[no].maxWordCount, fieldLens[3]);
			out.print("] ");
			out.print(' ');
			print(stats[no].termCount, fieldLens[4]);
			out.print(" [");
			print(stats[no].minTermCount, fieldLens[5]);
			out.print(',');
			print(stats[no].maxTermCount, fieldLens[6]);
			out.print("]   ");

			// Print properly formatted data guide information
			// Construct prefix
			for (int d = 0; d < depth; d++) {
				out.print(PREFIX);
			}
			// Append node name and number
			if (n.isAttrib()) {
				out.print('-');
			} else {
				out.print('+');
			}
			out.print(n.getName());
			out.println();

			// Iteratively do the same for all children
			children = n.getChildren();
			if (children != null && (it = children.iterator()).hasNext()) {
				do {
					n = (GuideNode) it.next();
					printNodeStats(stats, n, depth + 1, noLen, fieldLens);

				} while (it.hasNext());
			}
		}
	}

	static private void print(int i, int len) {
		final String EMPTY = "                    ";
		int iLen = charLen(i);

		out.print(EMPTY.substring(0, len - iLen));
		out.print(i);
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public Indexer(XmlDecoderStream in, DataGuide guide) {
		this.in = in;
		this.guide = guide;
		hashTables = new Quack();
		mutIntPools = new Quack();
		unusedTables = new Quack();
		unusedPools = new Quack();
		// Pre-initialize hash tables used for term occ. counting
		for (int i = 0; i <= guide.getMaxDepth(); i++) {
			unusedTables.push(new HashMap(200));
			unusedPools.push(new Pool(new MutableInteger(), 200));
		}
		stats = new Stats(guide);
	}

	public Indexer(Args args) {
		this(args.xds, args.guide);
	}


//	public void setPrintStream(PrintStream p) {
//		out = p;
//	}

	public DataGuide getDataGuide() { return guide; }


	public void start() throws IOException {
		int code;
		GuideNode currentNode = guide.getRoot();
		GuideNode lastNode;
		MutableInteger count, tmp;
		MutableInteger termNo = new MutableInteger();
		PercentPrinter pprint = new PercentPrinter(guide.getTotalNodeCount());
		int nodeCount = 0;
		long pos; // Current code's bit position

		pos = in.getBitPosition();
		while ((code = in.nextCode()) != Decoder.EOF) {
			// INSIGNIFICANT TOKENS (Neither index term nor structure)
			while (code == 0) {
				pos = in.getBitPosition();
				code = in.nextCode();
			}
//			System.out.print(" **" + code);

			// REGULAR TERM to be indexed
			if (code > 0) {
				termNo.setValue(code);
				// Have we seen this term before?...
				count = null;
				try {   // Just for debugging!!
					count = (MutableInteger) currentHash.get(termNo);
				} catch (NullPointerException e) {
					System.out.println("Tiny problem...there's no hash table...");
				}
				if (count == null) {
					// ..no counter for this term there as of now thus claim one
					// ..for counter and...
					count = (MutableInteger) currentPool.claim();
					count.setValue(1);
					// ..for term#
					tmp = (MutableInteger) currentPool.claim();
					tmp.setValue(code);
					currentHash.put(tmp, count);
				} else {
					count.inc();
				}
			}

			// ELLEMENT/ATTRIBUTE start
			else if (Decoder.isNode(code)) {

				nodeCount++;

				if (currentNode.isAttrib()) {

//***					System.out.print("A");

					// Seeing a new new node while in an attribute can only
					// mean the current attribute has ended, thus...
					currentDepth++;
					onAttribute(currentNode, currentHash);
					currentDepth--;
					releaseResources();
					// Return to attribute's parent *element*
					currentNode = currentNode.getParent();
				} else {

				}

				// Keep track of node bit positions
				bitPosition = pos;
//***				System.out.print("\n" + bitPosition);



				lastNode = currentNode;
				currentNode = currentNode.getChild(
						guide.getNode(- code).getName());
				if (currentNode == null) {
					throw new IllegalStateException("Expecting node " + (-code)
							+ " ('" + guide.getNode(- code).getName()
							+ "') as child of node '" + lastNode.getName() + "', but that "
							+ "node does not have a child with this name ("
							+ nodeCount + " processed so far)");
				}
				if (!currentNode.isAttrib()) {
					currentDepth++;
					onElementStart(currentNode);
				}
//				System.out.print("\n___" + currentNode.getName() + "___\t");

				// Regardless of wether we are in an element or attribute:
				// Obtain new hash table related to this element and
				// keep old one with the parent's element
				obtainResources();

				pprint.notify(nodeCount); //stats.elementCount + stats.attributeCount);
			}

			// END OF ELEMENT'S ATTRIBUTES
			else if (code == Decoder.ALL_ATTRIBUTE_END) {
				// Same notification as above
				currentDepth++;
				onAttribute(currentNode, currentHash);
				currentDepth--;
				releaseResources();
				currentNode = currentNode.getParent();
			}

			// ELEMENT END
			else if (code == Decoder.ELEMENT_END) {
//				System.out.println("\n---<" + currentNode.getName() + ">---");
				// Store term info from current hash table
				onElementEnd(currentNode, currentHash);
				currentDepth--;
				// Increment child counter NOT before generating PID!
				currentNode.incCurrentChildCount();

				// When we leave this element all its children's child
				// counts should be reset!
				// (There's no need to do the same for attributes as we
				//  know they never have children and can only occur
				//  once within an element.)
				// ..reset all children's child counters to zero!.
				currentNode.resetChildCounts();

				// Release resources and move one level up in the data guide
				releaseResources();
				currentNode = currentNode.getParent();
			}

			else {
				throw new IllegalStateException("Aehemm, something has been "
						+ "forgotten here; a state is not covered while "
						+ "indexing");
			}

			pos = in.getBitPosition();
		}
	}

	private void releaseResources() {
		unusedTables.push(currentHash);
		currentHash = (HashMap) hashTables.pop();
		unusedPools.push(currentPool);
		currentPool = (Pool) mutIntPools.pop();
	}

	private void obtainResources() {
		if (currentHash != null) { hashTables.push(currentHash); }
		currentHash = (HashMap) unusedTables.pop();
		currentHash.clear();
		// ...same with current pool of integers
		if (currentPool != null) { mutIntPools.push(currentPool); }
		currentPool = (Pool) unusedPools.pop();
		currentPool.releaseAll();
	}


	public void onAttribute(GuideNode node, HashMap termCounts) {
		// currentDepth++;  -- done in start() now!
		stats.attributeCount++;
		stats.nodesAtDepth[currentDepth]++;
		handleStats(node, termCounts, true);
		// currentDepth--;  -- s. above
	}

	protected void handleStats(GuideNode node, HashMap termCounts,
							   boolean attrib) {
		int wordCount = 0, termCount;  // For # of non-unique and unique terms
		int no = node.getNo();
		int currTermCount = termCounts.size();

		// Add first part of node statistics
		stats.nodeStats[no].count++;
		stats.nodeStats[no].termCount += currTermCount;
		currTermCount = Math.min(Short.MAX_VALUE, currTermCount);
		stats.nodeStats[no].minTermCount = (short) Math.min(currTermCount, stats.nodeStats[no].minTermCount);
		stats.nodeStats[no].maxTermCount = (short) Math.max(currTermCount, stats.nodeStats[no].maxTermCount);


//		if (currTermCount == 0) { return;  /* Skip all the rest... */ }

		Iterator it = termCounts.entrySet().iterator();
		Map.Entry entry;
		MutableInteger counter;
		termCount = currTermCount;
		while (it.hasNext()) {
			entry = (Map.Entry) it.next();
			counter = (MutableInteger) entry.getValue();
			stats.maxCounter = Math.max(counter.getValue(), stats.maxCounter);
			wordCount += counter.getValue();
			stats.counts[(counter.getValue() > Stats.MAX_COUNTER?
						  Stats.MAX_COUNTER: counter.getValue())]++;
		}
		stats.wordCount += wordCount;
		stats.wordsAtDepth[currentDepth] += wordCount;
		// Terms and counters have a one-to-one relationship!
		stats.countsPerPidNumLen[node.getPosPartCount()] += termCount;
		stats.countsPerPidBitLen[node.getTotalPosBitLen()] += termCount;
		if (attrib) {
			stats.attribWordCount += wordCount;
			stats.countsAtDepth[currentDepth - 1] += termCount;
		} else {
			stats.maxElemWordCount = Math.max(wordCount, stats.maxElemWordCount);
			stats.countsAtDepth[currentDepth] += termCount;
		}

		// Add rest of node statistics
		stats.nodeStats[no].wordCount += wordCount;
		wordCount = Math.min(Short.MAX_VALUE, wordCount);
		// Set overflow if such occurred!...
		stats.nodeStats[no].minWordCount =
				(short) Math.min(wordCount, stats.nodeStats[no].minWordCount);
		stats.nodeStats[no].maxWordCount =
				(short) Math.max(wordCount, stats.nodeStats[no].maxWordCount);

		// Word/term statistics kept in DataGuide
		// ..per node information for words
		node.incTotalWordCount(wordCount);
		node.setMinWordCount(Math.min(node.getMinWordCount(), wordCount));
		node.setMaxWordCount(Math.max(node.getMaxWordCount(), wordCount));
		// ..per node information for terms
		node.incTotalTermCount(currTermCount);
		node.setMinTermCount(Math.min(node.getMinTermCount(), currTermCount));
		node.setMaxTermCount(Math.max(node.getMaxTermCount(), currTermCount));
		// ..per DF information
		dfWordCount[currentDepth] += wordCount;
		wordCount = dfWordCount[currentDepth];
		node.incTotalDfWordCount(wordCount);
		node.setMinDfWordCount(Math.min(node.getMinDfWordCount(), wordCount));
		node.setMaxDfWordCount(Math.max(node.getMaxDfWordCount(), wordCount));
		dfWordCount[currentDepth - 1] += wordCount;
		dfWordCount[currentDepth] = 0;   // ..because its the end of the node!

		if (currentDepth == 1) {
			// We are done with this source
			computeExpectation();
		}
	}

	private void computeExpectation() {
		System.out.print("\nComputing expectations...");
		Iterator it = getDataGuide().iterator();
		while (it.hasNext()) {
			GuideNode n = (GuideNode) it.next();
			n.setExpWordCount(Math.round(n.getTotalWordCount() / (float) n.getCount()));
			n.setExpDfWordCount(Math.round(n.getTotalDfWordCount() / (float) n.getCount()));
			n.setExpTermCount(Math.round(n.getTotalTermCount() / (float) n.getCount()));
			// Some unrelated corrections
			if (n.getMinWordCount() == Integer.MAX_VALUE) {
				n.setMinWordCount(0);
			}
			if (n.getMinDfWordCount() == Integer.MAX_VALUE) {
				n.setMinDfWordCount(0);
			}
			if (n.getMinTermCount() == Integer.MAX_VALUE) {
				n.setMinTermCount(0);
			}
			if (n.getMinChildCount() == Integer.MAX_VALUE) {
				n.setMinChildCount(0);
			}
		}
		System.out.println("done.");
	}

	public void onElementStart(GuideNode node) {
		// currentDepth++;  -- done in start() now
		stats.elementCount++;
		stats.nodesAtDepth[currentDepth]++;
	}

	public void onElementEnd(GuideNode node, HashMap termCounts) {
		handleStats(node, termCounts, false);
		// currentDepth--;  -- done in start() now
	}

	public long getBitPosition() {
		return bitPosition;
	}

	public int getCurrentDepth() {
		return currentDepth;
	}


	private void printHash(HashMap map) {
		Iterator it = map.entrySet().iterator();
		Map.Entry entry;

		System.out.print("(");
		while (it.hasNext()) {
			entry = (Map.Entry) it.next();
			System.out.print(((Term) entry.getKey()).getName() + "/"
					+ ((MutableInteger) entry.getValue()).getValue() + "  ");
		}
		System.out.println();
	}



	/************************************************************************
	 * <p>A simple collection of arguments required to construct any Indexer.
	 * Used to simplify the provision of these parameters.</p>
	 *
	 * @author J. Marco Bremer
	 * @version 1.0
	 ************************************************************************/
	static public class Args {
		public XmlDecoderStream xds = null;
		public DataGuide guide = null;
		public int termCount = -1;
	} // Args



	/************************************************************************
	 * <p>A collection of standard statistics gathered during indexing.</p>
	 *
	 * @author J. Marco Bremer
	 * @version 1.0
	 ************************************************************************/
	static public class Stats {
		static public final int MAX_TREE_DEPTH = 100;
		static public final int MAX_COUNTER = 8;
		/** # of non-unique terms (words) */
		public int wordCount = 0;
		/** # of (XML) elements */
		public int elementCount = 0;
		/** Max. # of words in an element */
		public int maxElemWordCount = 0;
		/** # of (XML) attributes */
		public int attributeCount = 0;
		/** # of terms within attribute */
		public int attribWordCount = 0;
		/** The largest term counter found in the whole source */
		public int maxCounter = -1;
		/** # of counters with a certain value, all counters above MAX_COUNTER
		 are considered 100. */
		public int[] counts;
		/** # of nodes (elements and attributes) at a certain depth */
		public int[] nodesAtDepth;
		/** # of words at a certain depth */
		public int[] wordsAtDepth;
		/** # of unique terms at a certain depth (adjusted, i.e., attribute
		 terms count with their related elements) */
		public int[] countsAtDepth;
		/** # of unique terms per PID numCount */
		public int[] countsPerPidNumLen;
		/** # of unique terms per PID numCount */
		public int[] countsPerPidBitLen;

		public NodeStats[] nodeStats;


		public Stats(DataGuide guide) {
			counts = new int[MAX_COUNTER + 1];
			// The +1 is, because counters range from 1..MAX_XOUNTER
			nodesAtDepth = new int[MAX_TREE_DEPTH + 1];
			wordsAtDepth = new int[MAX_TREE_DEPTH + 1];
			countsAtDepth = new int[MAX_TREE_DEPTH + 1];
			countsPerPidNumLen = new int[MAX_TREE_DEPTH + 1];
			// The root element is considered depth 1
			countsPerPidBitLen = new int[64 + 1]; // prepared for 64 Bits
			// Init node statistics
			nodeStats = new NodeStats[guide.getNodeCount() + 1];
			for (int i = 1; i <= guide.getNodeCount(); i++) {
				nodeStats[i] = new NodeStats();
			}
		}

		private void initArray(int[] a) {
			// Are array elements initialized to zero per VM specification??
			for (int i = 0; i < a.length; i++) {
				a[i] = 0;
			}
		}

		public String toString() {
			StringBuffer buf = new StringBuffer(2048);
			int depth, maxDepth;

			buf.append(  "Total # of nodes.............. ");
			buf.append(String.valueOf(elementCount + attributeCount));
			buf.append("\n   ...# of elements........... ");
			buf.append(elementCount);
			buf.append("\n   ...# of attributes......... ");
			buf.append(attributeCount);
			buf.append("\nNodes at tree depth........... ");
			buf.append(tableToString(nodesAtDepth, 1, MAX_TREE_DEPTH));
//			depth = 0;
//			while (nodesAtDepth[++depth] > 0) {
//				buf.append("\n   ");
//				buf.append(depth);
//				buf.append("\t");
//				buf.append(nodesAtDepth[depth]);
//			}
			buf.append("\nTotal # of words................ ");
			buf.append(wordCount);
			buf.append("\nAvg (max) words per element..... ");
			buf.append(Math.round((wordCount - attribWordCount)
					   / (double) elementCount * 10000) / (double) 10000);
			buf.append(" (");
			buf.append(maxElemWordCount);
			buf.append(")");
			buf.append("\nAvg. # of words per attribute... ");
			buf.append(Math.round(attribWordCount
					   / (double) attributeCount * 10000) / (double) 10000);
			buf.append("\nWords at tree depth............. ");
			buf.append(tableToString(wordsAtDepth, 1, MAX_TREE_DEPTH));
//			maxDepth = Stats.MAX_TREE_DEPTH;
//			while (termsAtDepth[maxDepth] == 0) { maxDepth--; }
//			depth = 0;
//			while (++depth <= maxDepth) {
//				buf.append("\n   ");
//				buf.append(depth);
//				buf.append("\t");
//				buf.append(termsAtDepth[depth]);
//			}
			int totalCounters = 0;
			for (int i = 0; i < countsAtDepth.length; i++) {
				totalCounters += countsAtDepth[i];
			}
			buf.append("\nTotal # of counters............. ");
			buf.append(totalCounters);
			buf.append("\nCounters at tree depth.......... ");
			buf.append(tableToString(countsAtDepth, 1, MAX_TREE_DEPTH));
			buf.append("\nCounters per PID parts.......... ");
			buf.append(tableToString(countsPerPidNumLen, 0, MAX_TREE_DEPTH));
			buf.append("\nCounters per PID bit length..... ");
			int len = 0;
			while (countsPerPidBitLen[len] == 0) { len++; }
			buf.append(tableToString(countsPerPidBitLen, len, 64));
			buf.append("\nAvg (total) storage space f. PID positions... ");
			// Compute resulting total and average storage space:
			long sum = 0;
			for (int i = len; i <= 64; i++) {
				sum += countsPerPidBitLen[i] * i;
			}

			buf.append(Math.round(sum / (double) totalCounters * 10000)
					   / (double) 10000);
			buf.append(" Bits (");
			buf.append(Math.round(sum / (double) (8 * 1024)));
			buf.append(" KB)");
			buf.append("\nMaximum term counter............ ");
			buf.append(maxCounter);
			buf.append("\nCounters per counter size (abs./%/%words)....... ");
			buf.append(tableToString(counts, 1, MAX_COUNTER, wordCount));
//			maxDepth = Stats.MAX_COUNTER;
//			while (termsAtDepth[maxDepth] == 0) { maxDepth--; }
//			depth = 0;
//			while (++depth <= maxDepth) {
//				buf.append("\n   ");
//				buf.append(depth);
//				buf.append("\t");
//				buf.append(counts[depth]);
//			}
			return buf.toString();
		}

		static public String tableToString(int[] a, int start, int end,
				int altTotal) {
			StringBuffer buf = new StringBuffer((end - start) * 20);
			int max, total = 0;
			double x, sum = 0, altSum = 0;

			// Determine maximum index to be printed
			max = end;
			while (a[max] == 0) { max--; }
			// Determine total number of objects
			for (int i = start; i <= max; i++) {
				total += a[i];
			}
			for (int i = start; i <= max; i++) {
				buf.append("\n   ");
				buf.append(i);
				buf.append("\t");
				buf.append(a[i]);
				buf.append("\t(");
				x = Math.round(a[i] / (double) total * 1000) / (double) 10;
				sum += x;
				buf.append(x);
				buf.append("%)");
				if (altTotal > 0) {
					buf.append("\t");
					if (i < max) {
						x = Math.round(a[i] * i / (double) altTotal * 1000) / (double) 10;
					} else {
						x = 100 - altSum;
					}
					altSum += x;
					buf.append(x);
					buf.append("%");
				}
			}
			buf.append("\nSum\t" + total + "\t(" +
						(Math.round(sum * 10) / (double) 10) + "%)\t" +
						(Math.round(altSum * 10) / (double) 10));
			buf.append("\n");
			return buf.toString();
		}

		static public String tableToString(int[] a, int start, int end) {
			return tableToString(a, start, end, -1);
		}

	} // Stats


	/************************************************************************
	 * Collection of node-term statistics to go into node-term file.
	 ************************************************************************/
	static protected class NodeStats implements Storable {
		static public final int BYTE_SIZE = 24;

		public int count          = 0; // # of instances of this node
		public int wordCount      = 0; // Total number of words (non-unique terms)
		public short minWordCount = Short.MAX_VALUE; // Min. # of words in a single node
		public short maxWordCount = 0; // Max. (note that avg. can be derived)
		public int termCount      = 0; // # of unique words
		public short minTermCount = Short.MAX_VALUE; // Min.
		public short maxTermCount = 0; // Max.

		private int overflows = 0;     // # of times a short didn't suffice to
									   // store the # of nodes a term occ. in

		protected void setOverflows(int over) { overflows = over;	}
		protected int getOverflows() { return overflows; }


		public String toString() {
			StringBuffer buf = new StringBuffer(100);

			buf.append("Node Statistics");
			buf.append("...");

			return buf.toString();
		}


		/*+******************************************************************
		 * Storable implementation
		 ********************************************************************/

		public void store(DataOutput out) throws IOException {
			out.writeInt(count);
			out.writeInt(wordCount);
			out.writeShort(minWordCount);
			out.writeShort(maxWordCount);
			out.writeInt(termCount);
			out.writeShort(minTermCount);
			out.writeShort(maxTermCount);
			out.writeInt(overflows);
		}

		public void load(DataInput in) throws IOException {
			count = in.readInt();
			wordCount = in.readInt();
			minWordCount = in.readShort();
			maxWordCount = in.readShort();
			termCount = in.readInt();
			minTermCount = in.readShort();
			maxTermCount = in.readShort();
			overflows = in.readInt();
		}

		public int byteSize() {
			return BYTE_SIZE;
		}

	} // NodeStats



/*+**************************************************************************
 * TEST
 ****************************************************************************/

	static public void passTwoB(String repDir, int inMemCounts) throws IOException {
		String FILE_NAME = repDir + "/xml.data";
		Tokens tokens = new Tokens();
		Token token, nextToken;
		String name, nextName;
		StringBuffer strBuf = new StringBuffer(256);
		String str;

		// Create a stream for the input
		// FOR STREAMS...
//		BufferedInputStream bis = new BufferedInputStream(
//				new FileInputStream(FILE_NAME));
//		BitInputStream is = new BitInputStream(bis);
		// FOR CHANNELS...
		BufferedInputChannel bic = new BufferedInputChannel(
				(new FileInputStream(FILE_NAME)).getChannel(), 245760);
		BitDataInput is = new BitDataInput(bic);

		// Load tokens and construct huffman coder, etc.
		DataGuide guide = DataGuide.load(repDir + "/guide.tree");
		System.out.println("The maximum bit length for any PathId in this "
			+ "data guide is " + guide.computeMaxPidLength());

		// Tokens and Terms are replaced by single Decoder now!

//		System.out.print("Loading tokens...");
//		IndexSeqFile tokenFile = new IndexSeqFile(repDir + "/tokens");
//		tokens.load(tokenFile);
//		System.out.print(tokens.getUniqueCount() + "..");
//		System.out.println("done.");
		/*
		System.out.print("Deriving terms...");
		Terms terms = new Terms(tokens);
		System.out.println("done.");
		*/
//		Terms terms = new Terms();
//		System.out.print("Loading terms...");
//		tokenFile = new IndexSeqFile(repDir + "/terms");
//		terms.load(tokenFile);
//		System.out.print(terms.getTermCount() + "..");
//		System.out.println("done.");

		Decoder decoder = Decoder.load(repDir + "/decoder.data");

		//Print terms
//		System.out.println("Terms are...");
//		System.out.println(terms.toString() + "...");
//		for (int i = 1; i <= terms.getTermCount(); i++) {
//			System.out.println(terms.get(i).toString());
//		}
//		System.out.println();

		System.out.print("Constructing decoder input stream...");
		XmlDecoderStream xds = new XmlDecoderStream(is, decoder/*tokens*/);
		System.out.println("done.");

		System.out.print("Removing old temporary indexing files...");
		int r = DfMerger.removeOldFiles(repDir + "/termdf.data");
		System.out.println(r + "..done.");

		System.out.print("Constructing term-df counter class...");
		// FOR STREAMS...
		DataOutputSequence dos = new DataFileOutputStreamSeq(
				repDir + "/termdf.data", 245760);
		// FOR CHANNELS...
//		DataOutputSequence dos = new DataFileOutputChannelSeq(
//				repDir + "/termdf.data", 245760);
		TermDfCounters counters = new TermDfCounters(inMemCounts,
				decoder.getTermCount(), dos, guide);
		System.out.println("done.");

		TermDfIndexer indexer = new TermDfIndexer(xds, guide, /*terms,*/ counters);
		indexer.totalTokenCount = tokens.getTotalCount();
		indexer.start();
		counters.finalize();

//		for (int i = 1; i <= terms.getTermCount(); i++) {
//			int c = terms.terms[i].getCount();
//			int cc = terms.terms[i].indexCount;
//			if (c != cc) {
//				System.out.println("Term " + i + "\t" + cc + "/" + c);
//			}
//		}

		System.out.println("Storage space for index: "
			+ (counters.overallSpace + decoder.getTermCount() * 8) + " bytes");
		System.out.println("\nTotal # of counters stored: " + indexer.counterCount);
		System.out.println("Counters with value 1:      " + indexer.oneCount);
		System.out.println("Detailed statistics: ");
		int i = 140;
		while (counters.countStats[--i] == 0);
		while (i >= 0) {
			System.out.println(i + ",\t" + counters.countStats[i]);
			i--;
		}
		long space = (counters.overallSpace >>> 3) + decoder.getTermCount() * 8;
		System.out.println("Storage space for index: "
			+ counters.overallSpace + "bits + " + (decoder.getTermCount() * 8)
			+ " bytes (=" + space + " bytes =" + ((int) Math.round(space / 1024))
			+ " KB)");

		// Store some of the meta data to a file
		BufferedWriter bw = new BufferedWriter(
				new FileWriter(repDir + "/idxstats.txt"));
		bw.write("Total # of counters.......... " + indexer.counterCount);
		bw.newLine();
		bw.write("Counters per counter size.... ");
		i = 140;
		while (counters.countStats[--i] == 0);
		while (i >= 0) {
			bw.newLine();
			bw.write(i + ",\t" + counters.countStats[i]);
			i--;
		}
		bw.flush();
		bw.close();
	}


	static public void main(String[] args) throws Exception {
		if (args.length != 2) {
			printUsage();
			System.exit(1);
		}
		// Load configuration
		IdxrConfig config = (IdxrConfig) IdxrConfig.load(args[1]);
		Indexer.index(config, args[0]);
	}

	static protected void printUsage() {
		System.out.println();
		System.out.println("Expecting exactly 2 arguments for idxr.Indexer:");
		System.out.println("(1) The configuration name");
		System.out.println("(2) The configuration file name");
	}

}



 /*
  * OLD version
  *
 public void start() throws IOException {
 int state;
 String str;
 Token token;
 Term term;
 GuideNode currentNode = guide.getRoot();
 String prefix = new String();
 HashMap currentHash = null;    //
 Pool currentPool = null;       // Pool of mutable integers for a certain depth
 MutableInteger count;
 boolean inAttrib = false;
 PathId pid = new PathId();
 int depth = 0;

 while ((token = in.nextToken()) != null) {
	 if (++currentTokenCount % 1000000 == 0) {
		 System.out.print(".." + ((int) currentTokenCount / 1000000)
				 + "mio");
	 }
	 str = token.getName();
//			if (str.compareToIgnoreCase("streets") == 0) {
//				System.out.print(str + "..");
//			}
//			System.out.print(str + "..");
//			System.out.flush();
//			if (c > 302290) {
//				System.out.print("  '" + str + "' (" + currentNode.getName()
//					+ "/" + currentNode.getParent().getName() + ")");
//			}

	 // Do we have any special token here?
	 if (str.charAt(0) == '<') {
		 if (str.charAt(1) == '/') {
			 state = ELEM_END;
		 } else if (str.charAt(1) == '@') {
			 state = ALL_ATTRIB_END;
		 } else {
			 state = ELEM_START;
		 }
	 } else if (str.charAt(0) == '@' && str.length() > 1) {
		 state = ATTRIB_START;
	 } else {
		 state = REGULAR;
	 }
	 // Now, we know what state we are getting into...


	 if (state == ELEM_END || (inAttrib && state == ATTRIB_START)
			 || state == ALL_ATTRIB_END) {
		 // It's a tag's end or an attribute's end
		 depth--;
		 // Store term info from current hash table
//					System.out.print("\nWriting back hash...");
		 currentNode.getPid(pid);

		 addCounters(currentHash, pid);

		 if (state == ELEM_END) {
			 // Increment child counter not before generating PID!
			 currentNode.incCurrentChildCount();

			 // When we leave this element all its children's child
			 // counts should be reset!
			 // (There's no need to do the same for attributes as we
			 //  know the never have children an can only occur
			 //  once within an element.)
			 // ..reset all children's child counters to zero!..

			 currentNode.resetChildCounts();
		 }

//					printHash(currentHash);
		 // Release resources and move on level up in the data guide
		 unusedTables.push(currentHash);
		 currentHash = (HashMap) hashTables.pop();
		 currentPool.releaseAll();
		 unusedPools.push(currentPool);
		 currentPool = (Pool) mutIntPools.pop();
		 currentNode = currentNode.getParent();
		 inAttrib = false;
//					prefix = prefix.substring(2);
//					System.out.println(prefix + str);
	 }

	 if (state == ELEM_START || state == ATTRIB_START) {
		 // It's and element's/attribute's start
		 depth++;
		 // Keep track of whether we are in an attibute
		 // (because attrib's ends are hard to detect otherwise)
		 if (state == ATTRIB_START) {
			 inAttrib = true;
		 }


//				try {
			 currentNode = currentNode.getChild(str.substring(1));

//				} catch (NullPointerException e) {
//					System.out.println("Null pointer exception while getting "
//						+ "node " + currentNode + "'s child with name '"
//						+ str + "' (depth=" + depth + ", c=" + currentTokenCount + ")...");
//					System.out.println("Current node stack:");
//                    System.out.flush();
//				}
//                if (currentNode == null) {
//                    System.out.println("Have a null child '" + str.substring(1)
//                            + "' ('" + str + "')");
//                    printHash(currentHash);
//                }

//                    System.out.print(prefix + str);
//                    currentNode.getPid(pid);
//                    System.out.println("\t" + pid);
//					prefix += "  ";


		 // Replace current hash table
		 if (currentHash != null) hashTables.push(currentHash);
		 currentHash = (HashMap) unusedTables.pop();
		 currentHash.clear();
		 // Replace current pool
		 if (currentPool != null) mutIntPools.push(currentPool);
		 currentPool = (Pool) unusedPools.pop();
		 currentPool.releaseAll();
	 }

	 if (state == REGULAR && Terms.acceptAsTerm(str)) {
		 term = terms.get(str.toLowerCase());
//				System.out.println(prefix + term);
		 // Have we seen this term before?...
		 count = (MutableInteger) currentHash.get(term);
		 if (count == null) {
			 // ..no counter for this term there as of now
			 count = (MutableInteger) currentPool.claim();
			 count.setValue(1);
			 currentHash.put(term, count);
		 } else {
			 count.inc();
		 }
	 }

 }

 }
	*/
