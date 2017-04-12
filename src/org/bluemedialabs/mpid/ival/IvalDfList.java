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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import org.bluemedialabs.mpid.*;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p>Do merge sort for long lists and something simpler for short lists!</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class IvalDfList extends InvertedList {
	static public boolean STORE_ENCODED  = true;
	static public boolean LOAD_ENCODED  = true;

	private DataGuide guide;
	private int noBitLen;
	private int levelBitLen;


	public IvalDfList(int initialCapacity, DataGuide guide) {
		super(new IvalId(0, 0, (byte) 0), initialCapacity);
		if ((STORE_ENCODED || LOAD_ENCODED) && guide == null) {
			throw new NullPointerException("Need a valid document guide to "
					+ "support compressed storage but guide is null");
		}
		this.guide = guide;
		noBitLen = IvalId.computeNoBitLen(guide.getTotalNodeCount());
		levelBitLen = IvalId.computeLevelBitLen(guide.getMaxDepth());
	}

	public IvalDfList(DataGuide guide) {
		this(DEFAULT_INITIAL_CAPACITY, guide);
	}

	public IvalDfList() {
		this(DEFAULT_INITIAL_CAPACITY, null);
	}


	public Object clone() {
		return new IvalDfList(capacity(), guide);
	}


	/*+******************************************************************
	 * BitStorable implementation
	 ********************************************************************/

	/**
	 * Stores the ids using only the required number of bits for start, end,
	 * and level. The number of bits required is derived from the data guide
	 * statistics.
	 */
	public void store(BitOutput out) throws IOException {
		IvalId id;
		long len = 0;
		int nodeNo, lastNodeNo = -1;
		int nodeNoBits = guide.getNodeNoBits() + 1;
		int numBits, totalNumLen = 0;
		int count;

		out.write(length(), 32);
		len += 32;
		// Determine # of bits required for start and end
//		pl("IvalDfList:  Total # of nodes is " + guide.getTotalNodeCount());
		noBitLen = IvalId.computeNoBitLen(guide.getTotalNodeCount());
		levelBitLen = IvalId.computeLevelBitLen(guide.getMaxDepth());
//		pl("IvalDfList:  noBitLen=" + noBitLen + "  levelBitLen="
//		   + levelBitLen);

		BitOutput bitOut = out;

		// Write id elements
		for (int i = 0; i < length(); i++) {
			id = (IvalId) elements[i];
			bitOut.write(id.getStart(), noBitLen);
			bitOut.write(id.getEnd(), noBitLen);
			bitOut.write(id.getLevel(), levelBitLen);
		}
		len += length() * (2 * noBitLen + levelBitLen);

		// Who is supposed to do this where? (-> We, here!)
//		bitOut.flush();
//		return len;
	}

	public void load(BitInput in) throws IOException {
		int i = -1;
		int length = -1;
		long len = 0;
		IvalId id;

		try {
			BitInput bitIn = in;
//			System.out.print("..loading inv. list..");
			length = in.read(32);
			len += 32;
//			System.out.print(" ..list len. is " + mylength + "..");
			if (length > capacity()) {
				adjustCapacity(length);
			}
			for (i = 0; i < length; i++) {
				id = (IvalId) elements[i];
				id.setStart(bitIn.read(noBitLen));
				id.setEnd(bitIn.read(noBitLen));
				id.setLevel((byte) bitIn.read(levelBitLen));
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
				+ "list. ... (" + e + ")");
		}
	}

	public int byteSize() {
		return 4 + IvalId.BYTE_SIZE * length();
		// 4 bytes for length, BS bytes per element
	}

	public long bitSize() {
		return -1;
	}



	/************************************************************************
	 * TEST
	 ************************************************************************/

	static protected void initList(IvalDfList li, int elemCount) {
		IvalId id = new IvalId();

		for (int i = 0; i < elemCount; i++) {
			id = (IvalId) li.elements[i];
			id.setStart(i);
			id.setEnd(Math.max(i, elemCount - i));
			id.setLevel((byte) 3);
		}
	}


	static private IvalDfList createRandom(int len, int mod,
			DataGuide guide) {
		IvalDfList li = new IvalDfList(guide);
		IvalId id = new IvalId();
		for (int i = 0; i < len; i++) {
			id.setStart(i);
			id.setEnd((len - i) % mod);
			id.setLevel((byte) (i % 8));
			li.add(id);
		}
		return li;
	}


	static public void loadStoreTest(String cfgName, Configuration config)
			throws IOException {
		MyMath.setSeed(13);
		// To be updated on next use...
		DataGuide guide = DataGuide.load("/home/bremer/Data/XMLsmall/guide.tree");
		IvalDfList li = createRandom(100, 23, guide);
		BufferedOutputChannel out = BufferedOutputChannel.create(
				config.getProperty(cfgName, "TestHome") + "/invlist.test");
		pl("Storing list: " + li);
		li.store(out);
		out.close();
		IvalDfList li2 = new IvalDfList(1000, guide);
		BufferedInputChannel in = BufferedInputChannel.create(
				config.getProperty(cfgName, "TestHome") + "/invlist.test");
		li2.load(in);
		pl("Loaded list:  " + li2);
		in.close();
	}


/*
 * Add later...
 *
	static private void dumpIndex() throws IOException {
//		final String REP_DIR = Config.REP_HOME;
		IndexSeqChannel ch = new IndexSeqChannel(Config.REP_HOME + "/termdf");
		int termCount = ch.getRecordCount();
		DataGuide guide = DataGuide.load(Config.REP_HOME + "/guide.tree");
		IvalDfList li = new IvalDfList(guide);


		// ... needs update
		for (int i = 1; i <= termCount; i++) {
			ch.get(i, li);
//			term = terms.get(i);
//			pl(i + ")" + term.getName() + "\t" + term.getCount() + "\t" + li);
		}
		ch.close();
	}
*/

/*	static private void dumpListFile(int fileNo, char type) throws IOException {
		BufferedInputChannel ch = BufferedInputChannel.create(
				Config.REP_HOME +
					   (type == 'n'? "/nodedf-i.data": "/termdf-i.data") + fileNo);
		BitDataInput in = new BitDataInput(ch);
		MutableInteger no = new MutableInteger(-1);
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		IvalDfList li = new IvalDfList(guide);
		int liLen = -1;

		try {
			while (true) {
				no.setValue(in.read(32));
				p("\n_________" + no + "_________\t"); System.out.flush();
				li.load(in);
//				if (termNo.getValue() >= 142) {
//					pl("There.");
//				}
				liLen = li.length();
				pl(liLen + "\t" + li);
			}
		} catch (EOFException e) {
			pl("End of input channel reached after list# " + no);
			pl("(" + e + ")");
		}
		ch.close();
	}


	static private void dumpIdxSeqFile(char type) throws IOException {
		IndexSeqSource source =
				new IndexSeqChannel(REP_HOME +
					 (type == 'n'? "/nodedf-i": "/termdf-i"));
		MutableInteger termNo = new MutableInteger(-1);
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		IvalDfList li = new IvalDfList(guide);
		int i = -1;
		Terms terms = null;
		Term term;

		if (type == 't') {
			terms = Terms.load(Config.REP_HOME + "/" + TERMFILE_BASENAME);
		}
		try {
			for (i = 1; i <= source.getRecordCount(); i++) {
				if (type == 't') {
					// Load record-related term
					term = terms.get(i);
					p("\n" + i + ") " //+ (term.getCount() != li.length()? "***": "")
					  + term.getName() + "\t" + term.getCount());
				} else {
					// Load record-related node label
					p("\n" + i + ") " + guide.getNodeLabel(i).getLabel());
				}
				fl();
				// Load actual list
				source.get(i, li);
				pl("\t" + li);
			}
		} catch (EOFException e) {
			pl("End of input channel reached after term#/node# " + i);
			pl("(" + e + ")");
		}
		source.close();
	}
*/

/*
	static private void statIdxSeqFile() throws IOException {
		IndexSeqSource source = new IndexSeqChannel(REP_HOME + "/termdf_i");
		MutableInteger termNo = new MutableInteger(-1);
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		IvalDfList li = new IvalDfList(guide);
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
		// Check validity of input
		if (args.length < 2) {
			printUsage();
		}
		// Load configuration
		IdxrConfig config = (IdxrConfig) IdxrConfig.load(args[1]);
		String cfgName = args[0];
//		IndexSeqFile index = new IndexSeqFile(DATA_HOME + "/XMLstandard/termdf");
//		int termCount = index.getRecordCount();
//		IvalDfList li = new IvalDfList(null);
		StopWatch watch = new StopWatch();



		watch.start();

//		pl("In-memory test: ");
//		inMemTest(new IvalId());
//		pl("\nLoad-store test:");
//		loadStoreTest(cfgName, config);


//		for (int i = 1000; i > 990; i--) {
//			index.get(i, li);
//			out.println(li + "\n\n");
//		}

//		dumpIndex();
//		dumpListFile(3); //Integer.parseInt(args[0]));
//		if (args.length > 0 && args[0].charAt(0) == 't') {
//			dumpIdxSeqFile(config, 't');
//		} else {
//			dumpIdxSeqFile('n');
//		}
//		statIdxSeqFile();

		watch.stop();
		System.out.println("\nTime for operation: " + watch);
	}


	static private void printUsage() {
		System.out.println();
		System.out.println("Expecting exactly 2 arguments for idxr.IvalDfList:");
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
	static public void fl() {
		System.out.flush();
	}
}