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
import java.util.Arrays;
import java.util.Iterator;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class QueryTest {
	static public final String DATA_HOME = "/home/bremer/Data";
	static public final String GUIDE_FILE = DATA_HOME + "/XMLstandard/guide.tree";
	static public final String INDEX_FILE = DATA_HOME + "/XMLstandard/termdf";

	static private DataGuide guide = null;
	static private IndexSeqSource index = null;
	static private int termCount;
	static private StopWatch watch = new StopWatch();
	static private int onePercent;


	public static void main(String[] args) throws IOException {
		// First, always load the data guide...
		guide = DataGuide.load(GUIDE_FILE);
		// Next, always open inverted index file
		// STREAMS-BASED
//		index = new IndexSeqFile(INDEX_FILE);
		// CHANNEL-BASED
		index = new IndexSeqChannel(INDEX_FILE);
		termCount = index.getRecordCount();
		onePercent = termCount / 100;
		/*
		 * Now, we are ready to do something useful...
		 */
//		obtainInvListStats();

//		DfSequence dfs = createRandomDfs(10);
//		pl(dfs.toString());

		// Make tests deterministic
//		MyMath.setSeed(117);

//		testVarInvLiSize();

		testVarDfsSize();    // THE MAJOR TEST!
	}


	static public void testVarInvLiSize() throws IOException {
		InvertedDfList li = new InvertedDfList(guide);
		int percent = 0;
		DfSequence dfs = createRandomDfs(1000);
		long time;
		long totalTime = 0;

		ple("\n");
		for (int i = 1; i <= 200; i++) {
			if (i % onePercent == 0) {
				percent++;
				pe(".." + percent + "%");
			}
			index.get(i, li);
			watch.reset();
			watch.start();
			dfs.matchInvList(li);
			watch.stop();
			time = watch.getTime();
			totalTime += time;

			pl(li.length() + "\t" + time + "\t" + ((double) time / li.length()));
		}

	}


	static public void testVarDfsSize() throws IOException {
		InvertedDfList li = new InvertedDfList(guide);
		int percent = 0;
		DfSequence dfs = null, dfs2 = null;
		long time, time2;
		long totalTime = 0, totalTime2 = 0;
		int dfsLen;
		int positiveMatches = 0, positiveMatches2 = 0;

		int LIST_NUMBER = 1;
		int MULTI_RUN = 1;  // 2
		int MULTI_RUN_IMPROVED = 1;  // 10

		int identicalElems = 0;

		ple("\n");
		pl("DFS Length\tTotal Time (avg. of " + MULTI_RUN + ")\tTotal time (improved, avg. of "
			+ MULTI_RUN_IMPROVED + ")   #" + LIST_NUMBER);
		dfsLen = 1;
		// For every major size...
		while (dfsLen <= /*16*//*4096*/16384) {
			// ..multiple times..
			index.get(LIST_NUMBER, li);   // ..same list
			// Rest of the runs (only the second part)
			for (int j = 0; j < MULTI_RUN_IMPROVED; j++) {
				dfs2 = createRandomDfs(dfsLen);
				dfs2.sort();
//				pl(dfs2.toString());
				identicalElems = analyzeDfs(dfs2);

				// Then, do the same with the improved method
				watch.reset();
				watch.start();
				dfs2.matchInvList2(li);
				watch.stop();
				time2 = watch.getTime();
				totalTime2 += time2;
				if (j < MULTI_RUN) {
					// First, use default method
					dfs = (DfSequence) dfs2.clone();
					watch.reset();
					watch.start();
					dfs.matchInvList(li);
					watch.stop();
					time = watch.getTime();
					totalTime += time;
					positiveMatches = dfs.positiveMatches;
					positiveMatches2 = dfs2.positiveMatches;
					if (positiveMatches != positiveMatches2) {
						pl();
						pl("..!!!.." + positiveMatches + "/" + positiveMatches2 + "..");
					}
				}
			}
			pl(dfsLen + "\t" + (totalTime / MULTI_RUN)
			   + "\t" + (totalTime2 / MULTI_RUN_IMPROVED)
			   + "\t identical=" + identicalElems);
			dfsLen *= 2;
		}
		pl();
		pl("Total matchings executed......" + dfs.totalMatchedCounters);
		pl("Positive matches.............." + positiveMatches);
		pl("Total matchings executed (2).." + dfs2.totalMatchedCounters);
		pl("Positive matches (2).........." + positiveMatches2);
	}


	static public void testVarDfsSize2() throws IOException {
		final int[] listsOfInterest = {/*5133, 5203, 5294, 5315, */5406/*, 5462*/};

		InvertedDfList li = new InvertedDfList(guide);
		int percent = 0;
		DfSequence dfs = null, dfs2 = null;
		long time;
		long totalTime = 0;
		int dfsLen;

		ple("\n");
		pl("DFS Length\tTotal Time 1st 100");
		dfsLen = 100; //1;
		while (dfsLen <= 100) {
			dfs = createRandomDfs(dfsLen);
			dfs.sort();
			dfs2 = (DfSequence) dfs.clone();
			// Print DFSs
			pl("____DFS_______");
			pl("" + dfs);
			pl("____DFS_(2)___");
			pl("" + dfs2);
			pl();
			for (int i = 0; i < listsOfInterest.length; i++) {
//				if (i % onePercent == 0) {
//					percent++;
//					pe(".." + percent + "%");
//				}
//				p(".." + i + "..");
				index.get(listsOfInterest[i], li);
//				pl("" + dfs+ "\n\n");
				watch.reset();
				watch.start();
				pl("____List " + listsOfInterest[i] + "_______");
				pl("" + li);
				pl();
				dfs.matchInvList(li);
				dfs2.matchInvList2(li);
//				pl();
				watch.stop();
				time = watch.getTime();
				totalTime += time;
//				pl(li.length() + "\t" + time + "\t" + ((double) time / li.length()));
			}
			pl(dfsLen +"\t" + totalTime);
			dfsLen *= 2;
		}
		pl();
		pl("Total matchings executed......" + dfs.totalMatchedCounters);
		pl("Positive matches.............." + dfs.positiveMatches);
		pl("Total matchings executed (2).." + dfs2.totalMatchedCounters);
		pl("Positive matches (2).........." + dfs2.positiveMatches);
	}


	static public void obtainInvListStats() throws IOException {
		InvertedDfList li = new InvertedDfList(guide);
		long totalElemCount = 0;
		int oneCount = 0, maxElemCount = 0;
//		Base128Integer count = new Base128Integer();
		MutableInteger count = new MutableInteger();
		int[] elemCount = new int[termCount + 1];
		Iterator it;
		PathId pid, nextPid;
		int nodeNoBits = guide.getNodeNoBits();
		long totalPidBits = 0, totalPidBitsX = 0, totalPidBitsXX = 0;
		int onePercent = termCount / 100, percent = 0;
		int cc = 0;
		PercentPrinter pp;

		// Open channel for faster access...
		BufferedInputChannel ch = BufferedInputChannel.create(
				DATA_HOME + "/XMLstandard/termdf.data", 262144);
		IndexSeqChannel.Header header = new IndexSeqChannel.Header();
		// ..making sure the header is ignored.
		header.load(ch);

//		pl("\n");
		watch.reset();
		watch.start();
		// First, collect some basic statistics regarding list lengths
		for (int i = 1; i <= termCount; i++) {
			try {
				index.get(i, count);
//				pl("" + count);
			} catch (Exception e) {
				pl("Exception while fetching inverted list " + i);
				e.printStackTrace();
			}
			elemCount[i] = count.getValue();
		}
		// Sort counters by size...
		Arrays.sort(elemCount);
		// Obtain statistics...
		for (int i = 1; i <= termCount; i++) {
			totalElemCount += elemCount[i];
			if (elemCount[i] == 1) {
				oneCount++;
			}
			if (elemCount[i] > maxElemCount) {
				maxElemCount = elemCount[i];
			}
		}
		pl("Number of lists = terms..........." + termCount);
		pl("Lists with only one counter......." + oneCount);
		pl("Maximum number of list elements..." + maxElemCount);
		pl("Avg. number of list elements......" + (totalElemCount / termCount));
		pl("Median number of list elements...."
			+ elemCount[(int) Math.floor((double) termCount / 2)]);
		pl("Total number of counters.........." + totalElemCount);
		pl("Time to load basic data..........." + watch);
//		if (true) return;

		pl();
		pp = new PercentPrinter(totalElemCount);
		watch.reset();
		watch.start();
		// Then, obtain all counters from file and do the rest...
		for (int i = 1; i <= termCount; i++) {
			li.load(ch);
			it = li.iterator();
			pid = null;
			totalPidBits += (nodeNoBits * li.length());
			cc += li.length();
			pid = ((Counter) it.next()).getPid();
			totalPidBitsX += nodeNoBits + 1;
			totalPidBitsXX += nodeNoBits + 1;
			// (There should always be at least one PID!)
			while (it.hasNext()) {
				nextPid = ((Counter) it.next()).getPid();
				if (nextPid.getNodeNo() == pid.getNodeNo()) {
					// Need just one bit for encoding!
					totalPidBitsX++;
				} else {
					// Need a full node number
					totalPidBitsX += nodeNoBits + 1;
				}
				totalPidBitsXX += GammaCoder.codeLength(
						nextPid.getNodeNo() - pid.getNodeNo()) + 1;
				pid = nextPid;
			}
			pp.notify(cc);
		}
		watch.stop();
		pl();
		pl("Node# bits (improved 1/2)........."
			+ totalPidBits + " (" + totalPidBitsX + "/" + totalPidBitsXX + ")");
		pl("Time to determine statistics......" + watch);
	}


	static public DfSequence createRandomDfs(int length) throws IOException {
		DfSequence dfs = new DfSequence(guide);
		DocFragment df;
		PathId pid = new PathId();

//		ple();
//		pe("Creating DFS of length " + length + "...");
		for (int i = 0; i< length; i++) {
			guide.getRandomNode().getRandomPid(pid);
			df = new DocFragment((PathId) pid.clone());
			dfs.add(df);
		}
//		ple("done.");
		return dfs;
	}


	static public int analyzeDfs(DfSequence dfs) {
		DocFragment df, df2;
		int eqCount = 0;

		df = dfs.get(0);
		for (int i = 1; i < dfs.length(); i++) {
			df2 = dfs.get(i);
			if (df2.equals(df)) eqCount++;
			df = df2;
		}
		return eqCount;
	}



	static public void p(String s) {
		System.out.print(s);
	}
	static public void pl(String s) {
		System.out.println(s);
	}
	static public void pl() {
		System.out.println();
	}

	static public void pe(String s) {
		System.err.print(s);
	}

	static public void ple(String s) {
		System.err.println(s);
	}
	static public void ple() {
		System.err.println();
	}
}