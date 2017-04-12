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
import org.bluemedialabs.mpid.*;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p>Do merge sort for long lists and something simpler for short lists!</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class NodeDfList extends InvertedList {
	static public boolean STORE_ENCODED  = true;
	static public boolean LOAD_ENCODED  = true;

	static public String REP_HOME = null;

	private DataGuide guide;
	private int totalPosLen = -1;
	private int stored = -1;
	private boolean completeStorage = false;


	public NodeDfList(int initialCapacity, DataGuide guide) {
		super(new MutableLong(), initialCapacity);
		if ((STORE_ENCODED || LOAD_ENCODED) && guide == null) {
			throw new NullPointerException("Need a valid document guide to "
					+ "support compressed storage but guide is null");
		}
		this.guide = guide;

	}

	public NodeDfList(DataGuide guide) {
		this(DEFAULT_INITIAL_CAPACITY, guide);
	}

	public NodeDfList() {
		this(DEFAULT_INITIAL_CAPACITY, null);
	}


	public Object clone() {
		return new NodeDfList(capacity(), guide);
	}


	public void setNo(int no) {
		super.setNo(no);
		// Predetermine node-related values
		totalPosLen = //guide.getNode(getNo()).getTotalPosBitLen();
				/*
				 * ONLY FOR LA TIMES!!!
				 */
			Math.min(guide.getNode(getNo()).getTotalPosBitLen(), 32);
//		System.out.println("..New node# " + no + " with bit length "
//							+ totalPosLen + "...");
	}

	public int getStored() { return stored; }

	public void setCompleteStorage(boolean b) {
		completeStorage = b;
	}


	/*+******************************************************************
	 * Storable implementation
	 ********************************************************************/

	/**
	 * Stores the ids using only the required number of bits for start, end,
	 * and level. The number of bits required is derived from the data guide
	 * statistics.
	 */
	public void store(BitOutput out) throws IOException {
		if (getNo() < 0) {
			// This means nobody has bothered initializing our related objects
			// (a node here) number!
			throw new IllegalStateException("No valid node# has been assigned "
					+ "to this node-df-list thus, the bit length cannot be "
					+ "determined from the data guide");
			// This check also makes sure totalPosLen is properly initialized!
		}
		if (!completeStorage) {
			storeWithGaps(out);
		} else {
			// Or, alternatively:
			storeComplete(out);
		}
	}

	private void storeComplete(BitOutput out) throws IOException {
		MutableLong bits;
		long len = 0;

		out.write(length(), 32);
		len += 32;
		// Write id elements
		for (int i = 0; i < length(); i++) {
			bits = (MutableLong) elements[i];
			out.write(bits.getValue(), totalPosLen);
		}
		len += length() * totalPosLen;
	}

	private void storeWithGaps(BitOutput out) throws IOException {
//		int stored = 0;
		MutableLong bits;
		long lastBits = -10;
		int physIdxLen;
		long len = 0;
		int length;

		// Determine # of elements to be stored
		stored = 0;
		length = length();
		for (int i = 0;  i < length; i++) {
			bits = (MutableLong) elements[i];
			if (bits.getValue() > lastBits + 1) {
				stored++;
			}
			lastBits = bits.getValue();
		}
		physIdxLen = computePhysIdxLength(length);
		// We have all the information we need, so, start writing
		out.write(length(), 32);
		out.write(stored, 32);
		len += 64;
		// Write id elements (same as above only with actually writing the data
		lastBits = -10;
		for (int i = 0; i < length(); i++) {
			bits = (MutableLong) elements[i];
			if (bits.getValue() > lastBits + 1) {
				// Need to store this one
				out.write(bits.getValue(), totalPosLen);
				out.write(i, physIdxLen);
			}
			lastBits = bits.getValue();
		}
		len += length() * (totalPosLen + physIdxLen);
		// Who is supposed to do this where? (-> We, here!)
//		bitOut.flush(); // -> not any more!?!
//		return len;

	}


	public void load(BitInput in) throws IOException {
		if (getNo() < 0) {
			// This means nobody has bothered initializing our related objects
			// (a node here) number!
			throw new IllegalStateException("No valid node# has been assigned "
					+ "to this node-df-list thus, the bit length cannot be "
					+ "determined from the data guide");
		}
		try {
			if (!completeStorage) {
				loadWithGaps(in);
			} else {
				loadComplete(in);
			}
		} catch (EOFException e) {
			if (length() != 0) {
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

	private void loadWithGaps(BitInput in) throws IOException {
		int bits, nextBits;
		int lastBits = -10;
		int physIdxLen, index;
		int nextIndex;
		int length;
		long len = 0, seen;
		int elemIdx = 0;

		length = in.read(32);
		stored = in.read(32);
		len += 64;
		physIdxLen = computePhysIdxLength(length);
		if (length > capacity()) {
			adjustCapacity(length);
		}
		setLength(length);
		// Load first element (there should always be at least on element)
		bits = in.read(totalPosLen);
		index = in.read(physIdxLen);
		((MutableLong) elements[0]).setValue(bits);
		nextIndex = length;
		// <index> isn't used as of now...
		for (int i = 1; i < stored; i++) {
			// Just load stored element
			nextBits = in.read(totalPosLen);
			nextIndex = in.read(physIdxLen);
			// Now, look whether indices indicate some missing
			// (not stored) elements
//			if (nextIndex - index > 1) {  -- unnecessary!!
				// Aha! Some bits have not been stored because they are just
				// a continuation of the <bits>-encoded position number
				for (int j = 1; j < (nextIndex - index); j++) {
					((MutableLong) elements[++elemIdx]).setValue(++bits);
				}
//			}
			((MutableLong) elements[++elemIdx]).setValue(nextBits);
			bits = nextBits;
			index = nextIndex;
		}
		// Here, we potentially still have to handle the last elementi
			for (int j = 1; j < (length - index); j++) {
				// Being here means, some more elements to be added
				((MutableLong) elements[++elemIdx]).setValue(++bits);
		}
		// That's it!?!
		assert (elemIdx == length - 1): "Have loaded (and constructed) "
				  + (elemIdx + 1) + " PID elements " + ", but the length is "
				  + "supposed to be " + length;
	}

	static private int computePhysIdxLength(int length) {
		return (length == 1? 1 :(int) Math.ceil(MyMath.log2(length)));
	}

	private void loadComplete(BitInput in) throws IOException {
		MutableLong bits;
		long len = 0;
		int length = 0;

		try {
			length = in.read(32);
			len += 32;
//			System.out.print(" ..list len. is " + mylength + "..");
			if (length > capacity()) {
				adjustCapacity(length);
			}
			setLength(length);
			for (int i = 0; i < length; i++) {
				bits = (MutableLong) elements[i];
				bits.setValue(in.read(totalPosLen));
			}

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
		if (getNo() < 0) {
			// This means nobody has bothered initializing our related objects
			// (a node here) number!
			throw new IllegalStateException("No valid node# has been assigned "
					+ "to this node-df-list thus, the bit length cannot be "
					+ "determined from the data guide");
		}
		return 4 + (int) Math.ceil((totalPosLen * length()) / 8);
		// 4 bytes for length, BS bytes per element
	}



	/************************************************************************
	 * TEST
	 ************************************************************************/

	static public void loadStoreTest(IdxrConfig config, String cfgName)
			throws IOException {
		DataGuide guide = DataGuide.load(config.getDataGuideFileName(cfgName));
		NodeDfList li = new NodeDfList(20, guide);
		BufferedOutputChannel out = BufferedOutputChannel.create(
				"/home/bremer/Java" + "/nodedflist.test");
		BitDataOutput bitOut = new BitDataOutput(out);
		MutableLong m = new MutableLong();
		int i = 0;
		for (int j = 0; j < 16; ++i) {
			m.value = j;
			m.copy(li.get(j));
//			if (j % 4 == 3) j += 2; else j++;
			j++;
		}
		li.setLength(i);
		li.setNo(4);  // The "Author" node (totalNumLen = 4)
		pl("Storing list: " + li);
		li.store(bitOut);
		bitOut.flush();
		out.close();
		NodeDfList li2 = new NodeDfList(40, guide);
		BufferedInputChannel in = BufferedInputChannel.create(
				"/home/bremer/Java" + "/nodedflist.test");
		BitDataInput bitIn = new BitDataInput(in);
		li2.setNo(4);
		li2.load(bitIn);
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
		NodeDfList li = new NodeDfList(guide);


		// ... needs update
		for (int i = 1; i <= termCount; i++) {
			ch.get(i, li);
//			term = terms.get(i);
//			pl(i + ")" + term.getName() + "\t" + term.getCount() + "\t" + li);
		}
		ch.close();
	}
*/


/*	static private void dumpListFile(int fileNo) throws IOException {
		BufferedInputChannel ch = BufferedInputChannel.create(
				Config.REP_HOME + "/nodedf-p.data" + fileNo);
		BitDataInput in = new BitDataInput(ch);
		MutableLong no = new MutableLong(-1);
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		NodeDfList li = new NodeDfList(guide);
		int liLen = -1;

		try {
			while (true) {
				no.setValue(in.read(32));
				p("\n_________" + no + "_________\t"); System.out.flush();
				li.setNo(no.getValue());
				li.load(in);
				liLen = li.length();
				pl(liLen + "\t" + li);
			}
		} catch (EOFException e) {
			pl("End of input channel reached after list# " + no);
			pl("(" + e + ")");
		}
		ch.close();
	}
*/


	static private void dumpIdxSeqFile() throws IOException {
		IndexSeqSource source =
				new IndexSeqChannel(REP_HOME + "/nodedf-p", true);
		MutableLong termNo = new MutableLong(-1);
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		NodeDfList li = new NodeDfList(guide);
		int i = -1;
//		Terms terms = Terms.load(Config.REP_HOME + "/" + TERMFILE_BASENAME);
//		Term term;

		try {
			for (i = 1; i <= source.getRecordCount(); i++) {
				// Load record-related term
//				term = terms.get(i);
				// Load actual list
				li.setNo(i);
				source.get(i, li);
				pl("\n" + i + ") " //+ (term.getCount() != li.length()? "***": "")
				   /*+ term.getName() + "\t" + term.getCount()*/ + "\t" + li);
			}
		} catch (EOFException e) {
			pl("End of input channel reached after term# " + i);
			pl("(" + e + ")");
		}
		source.close();
	}


	static private void statIdxSeqFile() throws IOException {
		int totalCount = 0;
		int storedCount = 0;
		int maxLiLen = -1;
		long totalBitLen = 0;
		long storedBitLen = 0;
		int entryBitLen;

		IndexSeqSource source =
				new IndexSeqChannel(REP_HOME + "/nodedf-p", true);
		MutableLong termNo = new MutableLong(-1);
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		NodeDfList li = new NodeDfList(guide);
		int i = -1;
//		Terms terms = Terms.load(Config.REP_HOME + "/" + TERMFILE_BASENAME);
//		Term term;

		try {
			PercentPrinter printer = new PercentPrinter(source.getRecordCount());
			for (i = 1; i <= source.getRecordCount(); i++) {
				// Load record-related term
//				term = terms.get(i);
				// Load actual list
				li.setNo(i);
				source.get(i, li);
				totalCount += li.length();
				storedCount += li.getStored();
				entryBitLen = guide.getNode(i).getTotalPosBitLen() +
						computePhysIdxLength(li.length());
				totalBitLen += li.length() * entryBitLen;
				storedBitLen += li.getStored()  * entryBitLen;
				maxLiLen = Math.max(maxLiLen, li.length());
				printer.notify(i);
			}
		} catch (EOFException e) {
			pl("End of input channel reached after term# " + i);
			pl("(" + e + ")");
		}
		source.close();
		pl("Total number of entries.................. " + totalCount);
		pl("Stored number of entries................. " + storedCount + " ("
		   + (Math.round(storedCount / (double) totalCount * 100) / (double) 100) + "%)");
		pl("Total storage space for entries [bits]... " + totalBitLen);
		pl("Actual storage space for entries [bits].. " + storedBitLen + " ("
		   + (Math.round(storedBitLen / (double) totalBitLen * 100) / (double) 100) + "%)");
	}


	public static void main(String[] args) throws Exception {
//		IndexSeqFile index = new IndexSeqFile(DATA_HOME + "/XMLstandard/termdf");
//		int termCount = index.getRecordCount();
//		NodeDfList li = new NodeDfList(null);
		StopWatch watch = new StopWatch();


		IdxrConfig config = (IdxrConfig) IdxrConfig.load(args[1]);
		REP_HOME = config.getIndexHome(args[0]);
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
//		dumpListFile(Integer.parseInt(args[0]));
		dumpIdxSeqFile();
//		statIdxSeqFile();

		watch.stop();
		System.out.println("\nTime for operation: " + watch);
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
