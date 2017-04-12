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
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.Configuration;
import org.bluemedialabs.util.*;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TIndex {
	static private final int ELEM_COUNT_BITLEN = 32;
	static private final int TERM_COUNT_BITLEN = 0;  // NO COUNTER YET!!!

	private DataGuide guide;
	private Index index;
	private BitDataChannelPool dataPool;
	private TermNodeMap map;
	private int[] posBitLen;
	private MutableLong addr = new MutableLong();
	private Entry entry = new Entry();



	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public TIndex create(Configuration config, String cfgName,
								DataGuide guide) throws IOException {
		// Determine all required configuration strings
		String indexHome = config.getProperty(cfgName, "IndexHome");
		String fileBaseName =
				config.getProperty(cfgName, "TermDfFileBaseName");
		String mapFileName =
				config.getProperty(cfgName, "TermDfMappingFileName");
		// Construct basic objects from files
		Index index = new IndexChannel(indexHome + "/" + fileBaseName
				+ IndexSeqFile.INDEX_FILE_ENDING, new MutableLong());
		BitDataChannelPool pool = new BitDataChannelPool(
				indexHome + "/" + fileBaseName + IndexSeqFile.DATA_FILE_ENDING);
		BufferedInputChannel bic = BufferedInputChannel.create(indexHome + "/"
				+ mapFileName);
		TermNodeMap map = new TermNodeMap();
		map.load(bic);
		bic.close();

		return new TIndex(guide, index, pool, map);
	}

	static public TIndex create(Configuration config, String cfgName)
			throws IOException {
		DataGuide guide = DataGuide.load(config, cfgName);
		return create(config, cfgName, guide);
	}



	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public TIndex(DataGuide guide, Index index, BitDataChannelPool dataPool,
				  TermNodeMap map) throws IOException {
		this.guide = guide;
		this.index = index;
		this.dataPool = dataPool;
		this.map = map;
		loadIndexEtc();
	}

	private void loadIndexEtc() {
		int nodeCount = guide.getNodeCount();

		posBitLen = new int[nodeCount + 1];
		posBitLen[0] = -1;
		for (int i = 1; i <= nodeCount; i++) {
			// Determine counter bit lengths
			posBitLen[i] = guide.getNode(i).getTotalPosBitLen();
		}
	}


	public DataGuide getDataGuide() { return guide; }

	public int getPosBitLen(int nodeNo) { return posBitLen[nodeNo]; }


	public Entry getEntry(int termNo, int nodeNo, int pos) throws IOException {
		int bitLen = posBitLen[nodeNo] + TERM_COUNT_BITLEN;
		BitDataChannel data = dataPool.claim();
		try {
			data.bitPosition(getRecAddr(termNo, nodeNo) * 8);
			int elemCount = data.read(32);
			assert (pos < elemCount);
			data.bitPosition(addr.getValue() * 8 + pos * (bitLen));
			entry.posNo = data.readLong(posBitLen[nodeNo]);
			if (TERM_COUNT_BITLEN > 0) {
				entry.count = data.read(TERM_COUNT_BITLEN);
			} else {
				entry.count = 0;
			}
		} finally {
			dataPool.release(data);
		}
		return entry;
	}

	private long getRecAddr(int termNo, int nodeNo) throws IOException {
		int recNo = map.getMapping(termNo, nodeNo);
		if (recNo < 0) {
			// Term does not occur in given node
			return -1;
		} else {
			index.get(recNo, addr);
			return addr.getValue();
		}
	}


	public GuideNode[] labelPaths(int termNo) {
		TermNodeMap.MapIterator mapIt = getMap().mapIterator(termNo);
		int size = mapIt.size();
		GuideNode[] paths = new GuideNode[size];
		MutableInteger nodeNo;

		System.out.println("\nGetting lable paths...");
		for (int i = 0; i < size; i++) {
			nodeNo = (MutableInteger) mapIt.next();
			paths[i] = guide.getNode(nodeNo.getValue());
			System.out.println("\t(" + nodeNo + ")\t " + paths[i].getLabelPath());
		}
		return paths;
	}


	public PosIterator posIterator(int termNo, int nodeNo, int maxCount)
			throws IOException {
		int recNo = map.getMapping(termNo, nodeNo);
		if (recNo < 0) {
			// Term does not occur in given node
			return null;
		} else {
			index.get(recNo, addr);
			return new PosIterator(nodeNo, dataPool, addr.getValue() * 8, guide,
									posBitLen[nodeNo], TERM_COUNT_BITLEN, maxCount);
		}
	}
	public PosIterator posIterator(int termNo, int nodeNo) throws IOException {
		return posIterator(termNo, nodeNo, -1);
	}


	public EntryIterator entryIterator(int termNo, int nodeNo, int maxCount)
			throws IOException {
		int recNo = map.getMapping(termNo, nodeNo);
		if (recNo < 0) {
			// Term does not occur in given node
			return null;
		} else {
			index.get(recNo, addr);
			return new EntryIterator(nodeNo, dataPool, addr.getValue() * 8, guide,
									  posBitLen[nodeNo], TERM_COUNT_BITLEN, maxCount);
		}
	}
	public EntryIterator entryIterator(int termNo, int nodeNo)
			throws IOException {
		return entryIterator(termNo, nodeNo, -1);
	}


	public int getCountBitLen(int termNo) { return TERM_COUNT_BITLEN; }

	public int getElemCount(int termNo, int nodeNo) throws IOException {
		int recNo = map.getMapping(termNo, nodeNo);
		int count;
		BitDataChannel data = null;

		if (recNo < 0) {
			// Term does not occur in given node
			return 0;
		} else {
			index.get(recNo, addr);
			try {
				data =  dataPool.claim();
				data.bitPosition(addr.getValue() * 8);
				count = data.read(32);
			} finally {
				if (data != null) {
					dataPool.release(data);
				}
			}
			return count;
		}
	}

	public int getNodeCount() { return guide.getNodeCount(); }

	public TermNodeMap getMap() { return map; }


	public String toString() {

		return null;
	}


	/*+**********************************************************************
	 * PosIterator
	 ************************************************************************/

	static class PosIterator extends JoinNnoDbIterator {

		private BitDataChannel in;
		private int posBitLen;
		private int countBitLen;
		private int maxCount;    // Max. # of entries to be returend

		private MutableLong posNo = new MutableLong();


		PosIterator(int pno, BitDataChannelPool pool, long bitPos,
					DataGuide guide, int posBitLen, int countBitLen,
					int maxCount) throws IOException {
			// The following "1" is a pseudo entryCount. We still have to
			// determine the real count, but for that we need the input channel.
			super(pno, pool, bitPos, 1, guide);
			assert (maxCount >= 0);
			this.in = getDataChannel();
			int entryCount =  in.read(32);
			super.setEntryCount(entryCount);
			if (maxCount > entryCount || maxCount < 0) {
				maxCount = entryCount;
			}
			this.posBitLen = posBitLen;
			this.countBitLen = countBitLen;
			this.maxCount = maxCount;
//			System.out.println("PosIterator created for " + maxCount + "elements.");
		}


		boolean hasNextImpl() {
			return (maxCount > 0);
		}


		Object nextImpl() {
			if (!hasNext()) {
				throw new NoSuchElementException("There are no more "
						+ "position numbers left");
			}
			try {
				posNo.setValue(in.readLong(posBitLen));
				// Skip term counter
				in.read(countBitLen);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			maxCount--;
			return posNo;
		}

		public void remove() {
			throw new UnsupportedOperationException("Removing of elements from "
				+ "array lists is not supported at the current time");
		}


		public int remainingElems() { return maxCount; }
	}



	/*+**********************************************************************
	 * EntryIterator
	 ************************************************************************/

	static class EntryIterator extends JoinNnoDbIterator {
		private BitDataChannel in;
		private int posBitLen;
		private int countBitLen;
		private int maxCount;    // Max. # of entries to be returend

		private Entry entry = new Entry();


		EntryIterator(int pno, BitDataChannelPool pool, long bitPos,
					  DataGuide guide, int posBitLen,
					  int countBitLen, int maxCount) throws IOException {
			// The following "1" is a pseudo entryCount. We still have to
			// determine the real count, but for that we need the input channel.
			super(pno, pool, bitPos, 1, guide);
			assert (maxCount >= 0);
			this.in = getDataChannel();
			int entryCount =  in.read(32);
			super.setEntryCount(entryCount);
			if (maxCount > entryCount || maxCount < 0) {
				maxCount = entryCount;
			}
			this.posBitLen = posBitLen;
			this.countBitLen = countBitLen;
			this.maxCount = maxCount;
		}


		boolean hasNextImpl() {
			return (maxCount > 0);
		}

		Object nextImpl() {
			if (!hasNext()) {
				throw new NoSuchElementException("There are no more "
						+ "position numbers left");
			}
			try {
				entry.posNo = in.readLong(posBitLen);
				entry.count = in.read(countBitLen);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return entry;
		}

	}



	/*+**********************************************************************
	 * Entry
	 ************************************************************************/

	static public class Entry {
		long posNo;    // Pos#
		int count;     // Term counter
	}



	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		Configuration cfg = Configuration.load(args[1]);
		int maxCount = Integer.MAX_VALUE;
		long posNo = 0;
		int elemCount = 0, falsePositive = 0;
		int termNo, nodeNo = 10;
		Terms terms = Terms.load(cfg, args[0]);
		String term;

		if (args.length < 3) {
			System.out.println("ERROR!\nExpecting at least three arguments...\n");
		}
		termNo = Integer.parseInt(args[2]);
//		termNo = 1437;
		term = terms.get(termNo).getName();
		System.out.println("Printing all occurences of term " + termNo
						   + ", '" + term + ",'...");
		if (args.length > 3) {
			nodeNo = Integer.parseInt(args[3]);
		}
		if (args.length > 4) {
			maxCount = Integer.parseInt(args[4]);
		}
		if (args.length > 0) {   // Left here for future extension...
			TIndex index = TIndex.create(cfg, args[0]);
			PIndex pIndex = PIndex.create(cfg, args[0]);
			XmlSource source = XmlSource.create(cfg, args[0]);
			DataGuide guide = pIndex.getDataGuide();
			PosIterator it;
			/*
			 * Print term/node-related list
			 */
//			System.out.println("________Pos#'s for Term# " + termNo + "/Node# "
//							   + nodeNo + "_______");
//			it = index.posIterator(termNo, nodeNo, maxCount);
//			if (it == null) {
//				System.out.println("<No entries>");
//			} else {
//				while (it.hasNext()) {
//					posNo = ((MutableLong) it.next()).getValue();
//					System.out.print("  " + posNo);
//					// addr = (Address) it.next();
//					// System.out.print("  " + addr.posNo + "/" + addr.addr);
//					elemCount++;
//				}
//				System.out.println("\n\n" + elemCount + " positions");
//			}

			Iterator mapIt = index.getMap().iterator(termNo);
			TermNodeMap map = index.getMap();
			GuideNode[] paths = index.labelPaths(termNo);
			GuideNode node;
			StringBuffer buf = new StringBuffer(4096);

			System.out.println("Term '" + term + "' occurs under the following "
							   + "label paths:");
			for (int i = 0; i < paths.length; i++) {
				System.out.println("   " + paths[i].getLabelPath());
			}
			System.out.println("[" + paths.length + " paths]");
			System.out.println();

			System.out.println("________Pos#'s for Term# " + termNo + "_______");

			elemCount = 0;
			while (mapIt.hasNext()) {

//				if (nodeNo == ((MutableInteger) mapIt.next()).getValue()) {
				nodeNo = ((MutableInteger) mapIt.next()).getValue();
				node = guide.getNode(nodeNo);

				System.out.print("\n_____Node# " + nodeNo);

				it = index.posIterator(termNo, nodeNo, maxCount);
				System.out.println(" (" + it.remainingElems() + ")  " +
								   guide.getNode(nodeNo).getLabelPath() +
								   " _____");
				if (it == null) {
					System.out.println("<No entries; there is something wrong here!>");
				} else if (node.getExpDfWordCount() > 10000) {
					System.out.println("<DFs' content not included for its huge size>");
				} else {
					while (it.hasNext()) {
						posNo = ((MutableLong) it.next()).getValue();
//						System.out.print("  " + nodeNo + "/" + posNo);
						buf.setLength(0);
						source.getDocFragment(
								pIndex.getAddress(nodeNo, posNo), buf);
						int p = indexOfIgnoreCase(buf, term);
						System.out.println();
						if (buf.length() <= 200) {
							System.out.println(buf);
						} else {
						System.out.println(buf.substring(0, 200));
						}
						if (p < 0) {
							System.out.println("***** '" + term + "' does not occur "
								+ "in this fragment!");
							falsePositive++;
						}
						elemCount++;
					}
				}

//				}

			}
			System.out.println("\n\n" + elemCount + " occurences reported,");
			System.out.println((elemCount - falsePositive) + " actual occurences.");
			System.out.println();
		}
	}

	static private int indexOfIgnoreCase(StringBuffer buf, String s) {
		String str = buf.toString().toLowerCase();
		int offset = str.indexOf(s), off;
		int len = s.length();
		int matchCount = 0;

		off = offset;
		while (off >= 0) {
			buf.insert(off + 4 * matchCount, "**");
			buf.insert(off + len + 2 + 4 * matchCount, "**");
			off = str.indexOf(s, off + 1);
			matchCount++;
		}
		return offset;
	}



	static public void printTermStats() throws IOException {
		final int INDEX_SIZE = 120554496;
		final String srcId = "SwissProt";
		String swFileName = "/home/bremer/Data/English stopwords.txt";
		String termFileName = "/aramis/Data/Source/" + srcId + "/terms";
		String termFileBaseName = "/aramis/Data/Index/" + srcId + "/termdf-pp";
		String mapFileName = "/aramis/Data/Index/" + srcId + "/termdf-pp.map";
		String guideFileName = "/aramis/Data/Source/" + srcId + "/guide.tree";
		DataGuide guide = DataGuide.load(guideFileName);
		BufferedInputChannel bic = BufferedInputChannel.create(mapFileName);
		TermNodeMap map = new TermNodeMap();
		map.load(bic);
		StopWords sw = new StopWords(swFileName);
		Terms terms = Terms.load(termFileName);
		int termCount = map.getTermCount();
		int nodeCount = guide.getNodeCount();
		IndexChannel index = new IndexChannel(termFileBaseName + ".index",
				new MutableLong());
		int recCount = index.getRecordCount();
		long indexEnd = 8 * recCount + 24;
		MutableLong addr = new MutableLong(), next = new MutableLong();
		int tno = 1;
		int recNo;
		long sum = 0;
		String term;
		int swCount = 0;

		System.out.println("# of stop words\tTerm\tRemainig size\tPercent");
		System.out.println("0\t" + INDEX_SIZE + "\t100");
		for (tno = 1; tno <= termCount; tno++) {
			term = terms.get(tno).getName();
			if (!sw.isStopWord(term)) {
				//				System.out.println(tno + ")\t " + term + "\t <not a stop word>");
			} else {
				swCount++;
				for (int dno = 1; dno <= nodeCount; dno++) {
					if ((recNo = map.getMapping(tno, dno)) > 0) {
						// Get this address...
						index.get(recNo, addr);
						// ..and the next one...
						if (recNo < recCount) {
							index.get(recNo + 1, next);
						} else {
							next.setValue(indexEnd);
						}
						sum += next.getValue() - addr.getValue() + 8;
					}
				}
//				System.out.println(tno + ")\t " + term + "\t " + memString(sum)
//									+ "  (" + swCount + " stop words so far)");
				System.out.println(swCount + "\t" + (INDEX_SIZE - sum)
					 + " (" + ((INDEX_SIZE - sum) / 1048576) + ")\t\t"
					 + (Math.round((INDEX_SIZE - sum) / (double) INDEX_SIZE * 10000)
					 / (double) 100) + "\t" + term);
			}
		}

		System.out.println("There where " + swCount + " of the "
							+ sw.getTermCount() + " known stop words found in "
							+ "this text");
		System.out.println(sum + " bytes = " + (sum / 1024) + " Kb = "
						   + (sum / 1048576) + " Mb  could be saved by "
						   + "leaving out the selected term(s)");
	}

	static private String memString(long sum) {
		return (sum + " bytes = " + (sum / 1024) + " Kb = "
				+ (sum / 1048576) + " Mb ");
	}

}
