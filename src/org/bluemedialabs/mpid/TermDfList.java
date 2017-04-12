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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TermDfList extends InvertedList {
	static public boolean STORE_ENCODED  = true;
	static public boolean LOAD_ENCODED  = true;

	private DataGuide guide;
//	private int totalPosLen = -1;
	private int nodeNoLen;


	/*+**********************************************************************
	 * Class functions
	 ************************************************************************/

	static protected void constructGroupedFile(Configuration config, String cfgName,
			int totalCounters) throws IOException {
		String indexTemp = config.getProperty(cfgName, "IndexTemp");
		String termFileBaseName = config.getProperty(cfgName, "PidTermFileBaseName");
		String indexHome = config.getProperty(cfgName, "IndexHome");
		String dstFileName = config.getProperty(cfgName, "GroupedPidTermFileBaseName");
		String mapFileName = config.getProperty(cfgName, "TermDfMappingFileName");
		IndexSeqSource source =
				new IndexSeqChannel(indexTemp + "/" + termFileBaseName);
		int recCount = source.getRecordCount();
		IndexSeqBitOutput dest = IndexSeqOutputChannel.create(
				indexHome + "/" + dstFileName, true);
		int recNo = 0;  // Expected record# in destination
		DataGuide guide = DataGuide.load(config, cfgName);
		TermDfList li = new TermDfList(guide);
		NodeDfList outLi = new NodeDfList(guide);
		TermNodeMap map = new TermNodeMap(recCount, guide.getNodeCount());
		int termNo = -1, nodeNo = -1, prevNodeNo = -1, pos = -1;
		PercentPrinter printer = new PercentPrinter(
				(totalCounters > 0? totalCounters: recCount));
		PathId pid;
		MutableLong posBits = new MutableLong();
		int liLen, currentCounts = 0;

		outLi.setCompleteStorage(true);
		try {
			for (termNo = 1; termNo <= recCount; termNo++) {
				// Load and sort the list
//				System.out.println("\nTerm " + termNo + "...");
				source.get(termNo, li);
				liLen = li.length();
				// Unnecessary as long as we have made sure that the order
				// in which we have seen elements (PIDs) on parsing has been
				// presered (especially when merging temporary files):
				// STILL NEED TO DO THIS! WHY???
				li.sort();

				prevNodeNo = -1;
				// Extract node#-related sublists and store them as new records
				for (pos = 0; pos < li.length(); pos++) {
					pid	= (PathId) li.get(pos);
					nodeNo = pid.getNodeNo();
					if (nodeNo != prevNodeNo) {
//						MyPrint.pl("  (" + termNo + "," + nodeNo + ")");
						// Store accumulated list as a new record
						if (prevNodeNo > 0) {
							outLi.setNo(prevNodeNo);
							dest.write(outLi);
							recNo = map.addMapping(termNo, prevNodeNo);
							outLi.clear();
						}
//						MyPrint.p("  recNo=" + recNo + ",r=" + r + " ");
						prevNodeNo = nodeNo;
					}
					// Append the position bits to current output list
					posBits.setValue(pid.getPosBits());
					outLi.add(posBits);
				}
				// Write last list (unless it's empty? -> cannot be!)
				outLi.setNo(nodeNo);
				dest.write(outLi);
				recNo = map.addMapping(termNo, nodeNo);
				outLi.clear();
				// Keep the user informed about the progress
				currentCounts += liLen;
				printer.notify(
						(totalCounters > 0? currentCounts: termNo));
			}
		} catch (EOFException e) {
			pl("End of input channel reached after term# " + termNo);
			pl("(" + e + ")");
		} catch (Exception e2) {
			// Try to save whatever there is to save...
			// ..but first some information
			pl("Current term#............ " + termNo);
			pl("Max term#................ " + recCount);
			pl("Current pos.............. " + pos);
			pl("Current node#............ " + nodeNo);
			pl("Previous node#........... " + prevNodeNo);
			pl("Last map record#......... " + recNo);
			pl("Exception: " + e2);
		} finally {
			// Now that we are done creating the file, we should't forget
			// to store the mappings without which the data is worthless
			BufferedOutputChannel out = BufferedOutputChannel.create(
					indexHome + "/" + mapFileName);
			map.store(out);
			out.close();

			source.close();
			dest.close();
		}
	}


	/*+**********************************************************************
	 * Object functions
	 ************************************************************************/

	/**
	 * Same as NodeDfList but with full PathId's as elements instead of just
	 * MutableInteger's storing only the position bits.
	 *
	 * @param initialCapacity
	 * @param guide
	 */
	public TermDfList(int initialCapacity, DataGuide guide) {
		super(new PathId(), initialCapacity);
		if ((STORE_ENCODED || LOAD_ENCODED) && guide == null) {
			throw new NullPointerException("Need a valid document guide to "
					+ "support compressed storage but guide is null");
		}
		this.guide = guide;
		nodeNoLen = guide.getNodeNoBits();
	}

	public TermDfList(DataGuide guide) {
		this(DEFAULT_INITIAL_CAPACITY, guide);
	}

	public TermDfList() {
		this(DEFAULT_INITIAL_CAPACITY, null);
	}


	public Object clone() {
		return new TermDfList(capacity(), guide);
	}


//	public void setNo(int no) {
//		super.setNo(no);
//		// Predetermine node-related values
//		totalPosLen = guide.getNode(getNo()).getTotalPosBitLen();
//	}


	/*+******************************************************************
	 * Storable implementation
	 ********************************************************************/

	/**
	 * Stores the ids using only the required number of bits for start, end,
	 * and level. The number of bits required is derived from the data guide
	 * statistics.
	 */
	public void store(BitOutput out) throws IOException {
		PathId pid;
		int posLen;
		long len = 0;
		int nodeNo = -1;
		// FOR DEBUGGING:
		int maxLen = -1;

		out.write(length(), 32);
		len += 32;
		for (int i = 0; i < length(); i++) {

//			if (i >= 20020) {
//				len += (5 -5);
//			}

			pid = (PathId) elements[i];
			nodeNo = pid.getNodeNo();
			if (nodeNo > guide.getNodeCount()) {
				throw new IllegalStateException("Have a node# " + nodeNo
						+ " > nodeCount");
			}
			out.write(nodeNo, nodeNoLen);
			posLen = guide.getNode(nodeNo).getTotalPosBitLen();
			/*
			 * ONLY FOR LA TIMES!!!
			 */
			// posLen = Math.min(32, posLen);
			out.write(pid.getPosBits(), posLen);
			len += posLen;

			// DEBUG:
//			System.out.println("i=" + i + "\t node#=" + nodeNo + "\t posBits="
//							   + pid.getPosBits());

			maxLen = Math.max(posLen, maxLen);
		}
		len += length() * nodeNoLen;
//		System.out.println(nodeNo + ") Max. pos len: " + maxLen);
//		return len;
	}

	private void checkNo() {
		if (getNo() < 0) {
			// This means nobody has bothered initializing our related objects
			// (a node here) number!
			throw new IllegalStateException("No valid node# has been assigned "
					+ "to this term-df list, thus, the bit length cannot be "
					+ "determined from the data guide");
			// This check also makes sure totalPosLen is properly initialized!
		}
	}

	public void sort() {
		if (length() > 1) {
			if (length() <= 100) {
				quickSort();
			} else {
				Arrays.sort(elements, 0, length());
			}
		}
	}

	/**
	 * Not a quicksort but a quick sort for small lists.
	 */
	private void quickSort() {
		CloneableObject obj;
		for (int i = 0; i < length() - 1; i++) {
			for (int j = i + 1; j < length(); j++) {
				if (((PathId) elements[i]).compareTo(elements[j]) > 0) {
					// Swap counts
					obj = elements[i];
					elements[i] = elements[j];
					elements[j] = obj;
				}
			}
		}
	}


	/**
	 * For the final storage grouped by node# and as single records constructing
	 * a TermNodeMap on the fly.
	 *
	 * @param in
	 * @throws IOException
	 */
	public void storeSpecial(BitInput in) throws IOException {

	}


	public void load(BitInput in) throws IOException {
		PathId pid;
		int nodeNo, posLen = -1;
		long len = 0;
		int length = 0;
		long posBits;
		// FOR DEBUGGING:
		int lastNodeNo = -1;
		int diff;

		try {
			length = in.read(32);
			len += 32;
			if (length > capacity()) {
				adjustCapacity(length);
			}
			for (int i = 0; i < length; i++) {

//				if (i >= 20020) {
//					len += (5 - 5);
//				}

				pid = (PathId) elements[i];
				nodeNo = in.read(nodeNoLen);
				pid.setNodeNo(nodeNo);
				try {
					posLen = guide.getNode(nodeNo).getTotalPosBitLen();
				} catch (Exception e) {
					System.out.println("\nException.  i=" + i + ", node#="
									   + nodeNo);
					System.out.flush();
					throw new RuntimeException(e);
				}
				// FOR DEBUGGING:
//				if (nodeNo != lastNodeNo) {
//					System.out.print(" (" + nodeNo + " " + posLen + ")");
//					lastNodeNo = nodeNo;
//				}

				/*
				 * ONLY FOR LA TIMES!!!
				 * (Not necessary anymore!)
				 */
				// posLen = Math.min(32, posLen);
				if (posLen <= 31) {
					posBits = in.read(posLen);
				} else {
					// We need to do some extra work to load more than 32 bits!
					posBits = in.read(posLen - 31);
					posBits <<= 31;
					posBits |= in.read(31);
				}
				pid.setPosBits(posBits);
				// DEBUG:
//				System.out.println("i=" + i + "\t node#=" + nodeNo + "\t posBits="
//								   + pid.getPosBits() + " (" + posLen + ")");
			}
			setLength(length);

		} catch (EOFException e) {
			if (length != 0) {
				// Seems there was no data left but no inconsistencies
				throw e;
//				throw new IOException("Loading of partial inverted list; "
//					+ "length is " + mylength + ", but "
//					+ "only " + (i - 1) + " counters available (" + e + ")");
			}
			// else: everything is fine
		} catch (IOException e) {
			// Something wrong with the data!
			throw new IOException("IO exception while trying to load inverted "
				+ "term-df list. ... (" + e + ")");
		}
	}

	public int byteSize() {
		return -1;
	}



	/************************************************************************
	 * TEST
	 ************************************************************************/

/*
	static public void loadStoreTest() throws IOException {
		MyMath.setSeed(13);
		DataGuide guide = DataGuide.load("/home/bremer/Data/XMLsmall/guide.tree");
		TermDfList li = createRandom(100, 23, guide);
		BufferedOutputChannel out = BufferedOutputChannel.create(
				Config.JAVA_HOME + "/invlist.test");
		pl("Storing list: " + li);
		li.store(out);
		out.close();
		TermDfList li2 = new TermDfList(1000, guide);
		BufferedInputChannel in = BufferedInputChannel.create(
				Config.JAVA_HOME + "/invlist.test");
		li2.load(in);
		pl("Loaded list:  " + li2);
		in.close();
	}
*/

/*
 * Add later...
 *
	static private void dumpIndex() throws IOException {
//		final String REP_DIR = Config.REP_HOME;
		IndexSeqChannel ch = new IndexSeqChannel(Config.REP_HOME + "/termdf");
		int termCount = ch.getRecordCount();
		DataGuide guide = DataGuide.load(Config.REP_HOME + "/guide.tree");
		TermDfList li = new TermDfList(guide);


		// ... needs update
		for (int i = 1; i <= termCount; i++) {
			ch.get(i, li);
//			term = terms.get(i);
//			pl(i + ")" + term.getName() + "\t" + term.getCount() + "\t" + li);
		}
		ch.close();
	}
*/

	static private void dumpListFile(IdxrConfig config, String cfgName, int no)
			throws IOException {
		BufferedInputChannel ch = BufferedInputChannel.create(
				config.getIndexHome(cfgName) + "/"
				+ config.getPidTermFileBaseName(cfgName) + ".data" + no);
		MutableInteger termNo = new MutableInteger(-1);
		DataGuide guide = DataGuide.load(config, cfgName);
		TermDfList li = new TermDfList(guide);

		try {
			while (true) {
				termNo.load(ch);
				p("\n_________" + termNo + "_________\t"); System.out.flush();
				li.load(ch);
//				if (termNo.getValue() >= 142) {
//					pl("There.");
//				}
				pl(li.length() + "\t" + li);
			}
		} catch (EOFException e) {
			pl("End of input channel reached after term# " + termNo);
			pl("(" + e + ")");
		}
		ch.close();
	}


	static private void dumpIdxSeqFile(IdxrConfig config, String cfgName)
			throws IOException {
		String indexTemp = config.getProperty(cfgName, "IndexTemp");
		String fileName = config.getProperty(cfgName, "PidTermFileBaseName");
		IndexSeqSource source =
				new IndexSeqChannel(indexTemp + "/" + fileName);
		MutableInteger termNo = new MutableInteger(-1);
		DataGuide guide = DataGuide.load(config, cfgName);
		TermDfList li = new TermDfList(guide);
		int i = -1;
		Terms terms = Terms.load(config, cfgName);
		Term term;

		try {
			for (i = 1; i <= source.getRecordCount(); i++) {
				// Load record-related term
				term = terms.get(i);
				// Load actual list
				source.get(i, li);
				pl("\n" + i + ") " //+ (term.getCount() != li.length()? "***": "")
				   + term.getName() + "\t" + term.getCount() + "\t" + li);
			}
		} catch (EOFException e) {
			pl("End of input channel reached after term# " + i);
			pl("(" + e + ")");
		}
		source.close();
	}

/*
	static private void statIdxSeqFile() throws IOException {
		IndexSeqSource source = new IndexSeqChannel(REP_HOME + "/termdf_i");
		MutableInteger termNo = new MutableInteger(-1);
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		TermDfList li = new TermDfList(guide);
		int i = -1;

		int[] countCount = new int[500];
		int totalCount = 0;
		int maxLiLen = -1;

		try {

			for (i = 1; i <= source.getRecordCount(); i++) {
				//				termNo.load(ch);
				source.get(i, li);

				// Extract statistics
				for (int j = 0; j < li.length(); j++) {
					countCount[li.get(j).getCount()]++;
				}
				maxLiLen = Math.max(maxLiLen, li.length());
				totalCount += li.length();
			}

			pl("Total # of IvalIds...... " + totalCount);
			pl("IvalIds per size........ ");
			int k = 500;
			while (countCount[--k] == 0);
			while (k > 0) {
				System.out.println("\t.." + k + "\t" + countCount[k]);
				k--;
			}
			pl("Maximum list length...... " + maxLiLen);
			pl("Avg. list length......... " + (totalCount / source.getRecordCount()));


		} catch (EOFException e) {
			pl("End of input channel reached after term# " + i);
			pl("(" + e + ")");
		}
		source.close();
	}
*/


	public static void main(String[] args) throws Exception {
//		IndexSeqFile index = new IndexSeqFile(DATA_HOME + "/XMLstandard/termdf");
//		int termCount = index.getRecordCount();
//		TermDfList li = new TermDfList(null);
		StopWatch watch = new StopWatch();
		int totalCounters = -1;


		// Check validity of input
		if (args.length != 2) {
			printUsage();
		}
		// Load configuration
		IdxrConfig config = (IdxrConfig) IdxrConfig.load(args[1]);

		watch.start();

//		pl("In-memory test: ");
//		inMemTest(new IvalId());
//		pl("\nLoad-store test:");
//		loadStoreTest();


//		for (int i = 1000; i > 990; i--) {
//			index.get(i, li);
//			out.println(li + "\n\n");
//		}

//		dumpIndex();
//		dumpListFile(config, args[0], 1);
//		dumpIdxSeqFile(config, args[0]);
//		statIdxSeqFile();

		if (args.length > 2) {
			totalCounters = Integer.parseInt(args[2]);
		}
		constructGroupedFile(config, args[0], totalCounters);

		watch.stop();
		System.out.println("\nTime for operation: " + watch);
	}

	static private void printUsage() {
		System.out.println();
		System.out.println("Expecting exactly 2 arguments for idxr.TermDfList:");
		System.out.println("(1) The configuration name");
		System.out.println("(2) The configuration file name");
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
}
