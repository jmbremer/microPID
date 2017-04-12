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
import org.bluemedialabs.util.*;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class PIndex {
	private DataGuide guide;
	private long[] fileRecOffset;
	private BitDataChannelPool dataPool;
	private int[] posBitLen;
	private int[] idxBitLen;
	private int[] storedCount;
	private Entry entry = new Entry();
	private AIndex aIndex = null;


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public PIndex create(Configuration config, String cfgName,
								DataGuide guide) throws IOException {
		// Determine all required configuration strings
		String indexHome = config.getProperty(cfgName, "IndexHome");
		String fileBaseName =
				config.getProperty(cfgName, "NodeDfFileBaseName");
		// Construct basic objects from files
		Index index = new IndexChannel(indexHome + "/" + fileBaseName
				+ IndexSeqFile.INDEX_FILE_ENDING, new MutableLong());
		String fileName = indexHome + "/" + fileBaseName
				  + IndexSeqFile.DATA_FILE_ENDING;
		BitDataChannelPool pool = new BitDataChannelPool(
				indexHome + "/" + fileBaseName + IndexSeqFile.DATA_FILE_ENDING);
		// Determine index file size
		File dataFile = new File(indexHome + "/" + fileBaseName
				+ IndexSeqFile.DATA_FILE_ENDING);
		long size = dataFile.length();

		return new PIndex(guide, index, pool, size,
						   AIndex.create(config, cfgName));
	}

	static public PIndex create(Configuration config, String cfgName)
			throws IOException {
		DataGuide guide = DataGuide.load(config, cfgName);
		return create(config, cfgName, guide);
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public PIndex(DataGuide guide, Index index, BitDataChannelPool dataPool,
				  long indexFileSize) throws IOException {
		this.guide = guide;
		this.dataPool = dataPool;
		BitDataChannel ch = dataPool.claim();
		loadIndexEtc(index, ch, indexFileSize);
		dataPool.release(ch);
	}

	public PIndex(DataGuide guide, Index index, BitDataChannelPool channelPool,
				  long indexFileSize, AIndex aIndex) throws IOException {
		this(guide, index, channelPool, indexFileSize);
		this.aIndex = aIndex;
	}


	private void loadIndexEtc(Index index, BitDataChannel data,
							  long dataFileSize) throws IOException {
		MutableLong l = new MutableLong();
		MutableLong next = new MutableLong();
		int nodeCount = guide.getNodeCount();
		long recBitLen;
		int elemBitLen;

		fileRecOffset = new long[nodeCount + 1];
		fileRecOffset[0] = -1;
		posBitLen = new int[nodeCount + 1];
		posBitLen[0] = -1;
		idxBitLen = new int[nodeCount + 1];
		idxBitLen[0] = -1;
		storedCount = new int[nodeCount + 1];
		storedCount[0] = -1;
		for (int i = 1; i <= nodeCount; i++) {
			// Get record offset
			index.get(i, l);
			// Read storedCount from data file
			data.bitPosition(l.getValue() * 8);
			storedCount[i] = data.read(32);
			// Don't forget to make a *bit* offset out of the byte offset:
			fileRecOffset[i] = l.getValue() * 8 + 32;
			// Determine counter bit lengths
			posBitLen[i] = guide.getNode(i).getTotalPosBitLen();
			// Same for phys. addr. index entry
			idxBitLen[i] = guide.getNode(i).getIndexBitLen();
			// Finally, obtain the # of actually stored elements
//			if (i < nodeCount) {
//				index.get(i + 1, next);
//				recBitLen = (next.getValue() - l.getValue()) * 8;
//			} else {
//				recBitLen = (dataFileSize - l.getValue()) * 8;
//			}
//			elemBitLen = posBitLen[i] + idxBitLen[i];
//			storedCount[i] = /*Math.min(*/(int) (recBitLen / elemBitLen); //, guide.getNodeCount(i));
//			if (elemBitLen <= 7) {
//				// Bad luck, here bad things can happen, because it is unclear
//				// how much of the very last byte is actually utilized.
//				// (At bit length seven and larger we know that in the at most
//				//  7 unused bits, no further element could have fit!)
//				int pos = storedCount[i] * elemBitLen;
//
//			}
		}
		// This is not the best location to do the following, but it hopefully
		// won't hurt either.
		index.close();
	}

	public DataGuide getDataGuide() { return guide; }

	public int getStoredCount(int nodeNo) {	return storedCount[nodeNo];	}

	public int getPosBitLen(int nodeNo) { return posBitLen[nodeNo]; }

	public int getIdxBitLen(int nodeNo) { return idxBitLen[nodeNo]; }


	public Entry getEntry(int nodeNo, int pos) throws IOException {
		assert (pos >= 0 && pos < storedCount[nodeNo]);
		int bitLen = posBitLen[nodeNo] + idxBitLen[nodeNo];
		BitDataChannel data = dataPool.claim();
		data.bitPosition(fileRecOffset[nodeNo] + (pos * bitLen));
		entry.posNo = data.readLong(posBitLen[nodeNo]);
		entry.firstIdx = data.read(idxBitLen[nodeNo]);
		if (pos == storedCount[nodeNo] - 1) {
			// The requested entry was the last in the record
			entry.lastIdx = guide.getNodeCount(nodeNo) - 1;
		} else {
			// Skip over posBits of next entry
			data.readLong(posBitLen[nodeNo]);
			entry.lastIdx = data.read(idxBitLen[nodeNo]) - 1;
		}
		dataPool.release(data);
		return entry;
	}

	public long getAddress(int nodeNo, long posNo) throws IOException {
		int pLen = posBitLen[nodeNo];
		int iLen = idxBitLen[nodeNo];
		int stored = storedCount[nodeNo];
		long currPosNo, nextPosNo = -1;
		int currIdx, nextIdx = -1;
		int idx;
		BitDataChannel data = dataPool.claim();

		data.bitPosition(fileRecOffset[nodeNo]);
		nextPosNo = data.readLong(pLen);
		nextIdx = data.read(iLen);
//		stored--;
		do {
			currPosNo = nextPosNo;
			currIdx = nextIdx;
			stored--;
			if (stored > 0) {
				nextPosNo = data.readLong(pLen);
				nextIdx = data.read(iLen);
			} else {
				nextPosNo = -1;
			}
		} while (nextPosNo >= 0 && nextPosNo <= posNo);
		dataPool.release(data);
		idx = currIdx + (int) (posNo - currPosNo);
		// Now, actually fetch the address
		return aIndex.getAddress(nodeNo, idx);

	}


	// startPos here is the a position among the actually stored elements!?!
	public PosIterator posIterator(int nodeNo, long startPos, int maxCount)
			throws IOException {
//		assert (startPos < storedCount[nodeNo]): "Start position too big, remember that "
//				+ "positions here are actually stored entries which may be much "
//				+ "less than the total number of related nodes";
		int nodeCount = guide.getNodeCount(nodeNo);
		int bitLen = posBitLen[nodeNo] + idxBitLen[nodeNo];
		long bitPos = fileRecOffset[nodeNo];

		// The following is unneccessary in case there is nothing to retrieve
		return new PosIterator(nodeNo, dataPool, bitPos, storedCount[nodeNo],
							   guide, posBitLen[nodeNo], idxBitLen[nodeNo],
							   startPos, -1, maxCount, nodeCount - 1);
	}

	public PosIterator posIterator(int nodeNo, long startPos, long endPos)
			throws IOException {
		int nodeCount = guide.getNodeCount(nodeNo);
		long bitPos = fileRecOffset[nodeNo];

		return new PosIterator(nodeNo, dataPool, bitPos, storedCount[nodeNo],
							   guide, posBitLen[nodeNo], idxBitLen[nodeNo],
							   startPos, endPos, -1, nodeCount - 1);
	}

	public PosIterator posIterator(int nodeNo, long startPos) throws IOException {
		return posIterator(nodeNo, startPos, (int) -1);
	}

	public PosIterator posIterator(int nodeNo) throws IOException {
		return posIterator(nodeNo, (long) 0, (int) -1);
	}


	public Iterator entryIterator(int nodeNo, int startPos)
			throws IOException {
		assert (startPos < storedCount[nodeNo]): "Start position too big, remember that "
				+ "positions here are actually stored entries which may be much "
				+ "less than the total number of related nodes";
		int nodeCount = guide.getNodeCount(nodeNo);
		int bitLen = posBitLen[nodeNo] + idxBitLen[nodeNo];
		long bitPos = fileRecOffset[nodeNo] + (startPos * bitLen);

		// The following is unneccessary in case there is nothing to retrieve

		return new EntryIterator(nodeNo, dataPool, bitPos,
								 storedCount[nodeNo] - startPos, guide,
								 posBitLen[nodeNo], idxBitLen[nodeNo],
								 nodeCount - 1);
	}

	public Iterator entryInterator(int nodeNo) throws IOException {
		return entryIterator(nodeNo, 0);
	}


	/**
	 * Returns physical addresses for the position #'s in the given range.
	 * The given start and end positions may not exist. An end position of
	 * -1 returns all addresses starting from the start position.
	 *
	 * @param nodeNo
	 * @param startPos
	 * @param endPos
	 * @return
	 */
	public AddrIterator addrIterator(int nodeNo, long startPosNo, long endPosNo)
			throws IOException {
		assert (aIndex != null): "Please supply an AIndex on construction " +
					"of this PIndex to obtain direct address iterators here";
		PosIterator posIt = posIterator(nodeNo, startPosNo, endPosNo);
		// THIS IS NECESSARY IF ATTRIBUTE LISTS ARE NOT STORED:
//		GuideNode node = guide.getNode(nodeNo);
//		if (node.isAttrib()) {
//			nodeNo = node.getParent().getNo();
//		}
		return new AddrIterator(posIt, nodeNo, posIt.getStartIdx(), aIndex);
	}

	public AddrIterator addrIterator(int nodeNo, long startPosNo, int maxCount)
			throws IOException {
		assert (aIndex != null): "Please supply an AIndex on construction " +
					"of this PIndex to obtain direct address iterators here";
		PosIterator posIt = posIterator(nodeNo, startPosNo, maxCount);
//		GuideNode node = guide.getNode(nodeNo);
//		if (node.isAttrib()) {
//			nodeNo = node.getParent().getNo();
//		}
		return new AddrIterator(posIt, nodeNo, posIt.getStartIdx(), aIndex);

	}


	public int getCountBitLen(int nodeNo) { return posBitLen[nodeNo]; }

	public int getElemCount(int nodeNo) { return guide.getNodeCount(nodeNo); }

	public int getNodeCount() { return guide.getNodeCount(); }


	public String toString() {

		return null;
	}


	/*+**********************************************************************
	 * PosIterator
	 ************************************************************************/

	static class PosIterator extends JoinNnoDbIterator {
		private BitDataChannel input;
//		private int storedCount;
		private int posBitLen;
		private int idxBitLen;

		private long startPos;
		private long endPos;
		private int maxCount;
		private int maxIdx;

		private long currPosNo;
		private int currIdx;
		private long nextPosNo;
		private int nextIdx;
		private int startIdx;

		private MutableLong l = new MutableLong();


		PosIterator(int pno, BitDataChannelPool pool, long bitPos,
					int storedCount, DataGuide guide, int posBitLen,
					int idxBitLen, long startPos, long endPos, int maxCount,
					int maxIdx) throws IOException {
			super(pno, pool, bitPos, storedCount, guide);
			if (storedCount == 0) {
				throw new IllegalStateException("Must create a PosIterator "
						+ "on at least one stored element");
			}
			input = getDataChannel();

			this.posBitLen = posBitLen;
			this.idxBitLen = idxBitLen;
			if (maxCount < 0) {
				maxCount = Integer.MAX_VALUE;
			}
			if (endPos < 0) {
				endPos = Long.MAX_VALUE;
			}
			this.maxCount = maxCount;
			this.maxIdx = maxIdx;
			this.endPos = endPos;
			currPosNo = input.readLong(posBitLen);
			currIdx = input.read(idxBitLen);
			// The following is important to adjust for supplied start positions
			// that lie before the first position number entry!
			this.startPos = Math.max(startPos, currPosNo);
//			firstIdx = currIdx;
			decEntryCount();
//			this.storedCount--;
			if (getEntryCount() > 0) {
				nextPosNo = input.readLong(posBitLen);
				nextIdx = input.read(idxBitLen);
			} else {
				// There was only one elements, thus no next element
				nextPosNo = -1;
				nextIdx = -1;
			}
			advanceToStartPos();
		}

		public void advanceToStartPos() throws IOException {
			while (nextPosNo >= 0 && startPos >= nextPosNo) {
				currPosNo = nextPosNo;
				currIdx = nextIdx;
				decEntryCount();
//				storedCount--;
				if (getEntryCount() > 0) {
					nextPosNo = input.readLong(posBitLen);
					nextIdx = input.read(idxBitLen);
				} else {
					nextPosNo = -1;
					nextIdx = -1;
				}
			}
			// The current element must include the start position
			// we are seeking
			startIdx = currIdx + (int) (startPos - currPosNo);
			if (startIdx < 0) {
				throw new IllegalStateException("Impossible start index "
						+ startIdx + " for position iterator (startPos="
						+ startPos + ", endPos=" + endPos + ", currPosNo="
						+ currPosNo + ", currIdx=" + currIdx + ")");
			}
			currIdx = startIdx;
			currPosNo = startPos;
		}


		boolean hasNextImpl() {
			return (maxCount > 0 && currIdx <= maxIdx && currPosNo <= endPos);
		}


		MutableLong nextImpl() {
			if (!hasNext()) {
				throw new NoSuchElementException("There are no more "
						+ "position numbers left");
			}
			if (nextPosNo < 0 || currIdx < nextIdx) {
				l.setValue(currPosNo);
				currPosNo++;
			} else {
				// Next element is valid and already read from disk
				l.setValue(nextPosNo);
				currPosNo = nextPosNo + 1;
				decEntryCount();
//				storedCount--;
				if (getEntryCount() > 0) {
					// There are more entries stored
					try {
						nextPosNo = input.readLong(posBitLen);
						nextIdx = input.read(idxBitLen);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else {
					// Sorry, no more data to read
					nextPosNo = -1;
					nextIdx = -1;
				}
//				} else if (currIdx != maxIdx) {
//					throw new IllegalStateException("No more elements stored "
//							+ "but maximum index not reached");
//				}
			}
			currIdx++;
			maxCount--;
			return l;
		}

		public int getStartIdx() { return startIdx; }
		public int getCurrentIdx() { return currIdx - 1; }

		public int getCurrAddrIdx() { return getCurrentIdx(); }

		// We should provide a more efficient skip() exploiting the sparse
		// storage and the fact that the next element is already known...
	}



	/*+**********************************************************************
	 * EntryIterator
	 ************************************************************************/

	static class EntryIterator extends JoinNnoDbIterator {
		private BitDataChannel in;
//		private int storedCount;

		private int posBitLen;
		private int idxBitLen;
		private int maxIdx;

		private long currPosNo;
		private int currFirstIdx;

		private Entry entry = new Entry();


		EntryIterator(int pno, BitDataChannelPool pool, long bitPos,
					  int storedCount, DataGuide guide, int posBitLen,
					  int idxBitLen, int maxIdx) throws IOException {
			super(pno, pool, bitPos, storedCount, guide);
			if (storedCount <= 0) {
				throw new IllegalStateException("Must create a EntryIterator "
						+ "on at least one stored element");
			}
			this.in = getDataChannel();
			this.posBitLen = posBitLen;
			this.idxBitLen = idxBitLen;
			this.maxIdx = maxIdx;

			currPosNo = in.readLong(posBitLen);
			currFirstIdx = in.read(idxBitLen);
		}


		boolean hasNextImpl() {
			return (getEntryCount() > 0);
		}


		Object nextImpl() {
			long nextPosNo;
			int nextFirstIdx;

			if (!hasNext()) {
				throw new NoSuchElementException("There are no more "
						+ "index entries left");
			}
			entry.posNo = currPosNo;
			entry.firstIdx = currFirstIdx;
			if (getEntryCount() == 1) {
				// Only one element left
				entry.lastIdx = maxIdx;
			} else {
				// There must be at least one element to read left
				try {
					currPosNo = in.readLong(posBitLen);
					currFirstIdx = in.read(idxBitLen);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				entry.lastIdx = currFirstIdx - 1;
			}
			decEntryCount();
//			storedCount--;
			return entry;
		}

	}



	/*+**********************************************************************
	 * AddrIterator
	 ************************************************************************/

	static protected class AddrIterator implements DbIterator {
		private PosIterator posIt;
		private DbIterator aIt;
		private Address addr = new Address();


	 AddrIterator(PosIterator posIt, int nodeNo, int startPos, AIndex aIndex)
			 throws IOException {
		 this.posIt = posIt;
		 aIt = aIndex.addrIterator(nodeNo, startPos);
	 }


	 public boolean hasNext() {
		 return posIt.hasNext();
	 }


	 public Object next() {
		 if (!hasNext()) {
			 throw new NoSuchElementException("There are no more "
					 + "index entries left");
		 }
		 addr.posNo = ((MutableLong) posIt.next()).getValue();
		 addr.addr = ((MutableLong) aIt.next()).getValue();
		 return addr;
	 }

	 public int skip(int n) {
		 int skipped = 0;
		 while (hasNext() && skipped < n) {
			 next();
			 skipped++;
		 }
		 return skipped;
	 }


	 public void remove() {
		 throw new UnsupportedOperationException("Removing of elements from "
			 + "array lists is not supported at the current time");
	 }

	 public void close() throws IOException {
		 aIt.close();
		 posIt.close();
	 }
	}




	/*+**********************************************************************
	 * Entry
	 ************************************************************************/

	static public class Entry {
		long posNo;    // Pos#
		int firstIdx;  // Index in phys. addr. index first pos# is mapped to
		int lastIdx;   // Number of consequetive pos#'s which are mapped
					   // right to the next indexes
	}

	static public class Address {
		long posNo;
		long addr;
	}



	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		final boolean NUMBERS_ONLY = false;
		Configuration cfg = Configuration.load(args[1]);
		int nodeNo;
		int startPos = 0;
		int maxCount = Integer.MAX_VALUE;
		long posNo;
		int elemCount = 0;
		Address addr;

		if (args.length > 3) {
			startPos = Integer.parseInt(args[3]);
		}
		if (args.length > 4) {
			maxCount = Integer.parseInt(args[4]);
		}
		if (args.length > 2) {
			PIndex index = PIndex.create(cfg, args[0]);
			nodeNo = Integer.parseInt(args[2]);
			if (NUMBERS_ONLY) {
				System.out.println("________Pos#'s for Node# " + args[2] + "_______");
				System.out.println("  (Star pos. " + startPos + ", max. elements "
					+ maxCount + ", stored=" + index.getStoredCount(nodeNo)
					+ ", totalNodes=" + index.getDataGuide().getNodeCount(nodeNo)
					+  ")");
//				PosIterator it = index.posIterator(nodeNo, startPos, maxCount);
				AddrIterator it = index.addrIterator(nodeNo, startPos, maxCount);
				while (it.hasNext()) {
//					posNo = ((MutableLong) it.next()).getValue();
//					System.out.print("  " + posNo + "/" + it.getCurrentIdx());
					addr = (Address) it.next();
					System.out.print("  " + addr.posNo + "/" + addr.addr);
					elemCount++;
				}
				System.out.println("\n\n" + elemCount + " positions");
				System.out.println();
			} else {
//				printFragments(index, nodeNo, startPos, maxCount, args[0], cfg);

				printAll(index, maxCount, args[0], cfg);
			}
		} else {
			//			printStats(args[0], cfg);
			printStoredStats(args[0], cfg);
		}
	}


	static public void printFragments(PIndex index, int nodeNo, int startPos,
			int maxCount, String cfgName, Configuration config) throws IOException {
		DataGuide guide = index.getDataGuide();
//		AIndex aIndex = AIndex.create(config, cfgName);
		XmlSource source = XmlSource.create(config, cfgName);

		printSingle(nodeNo, index, guide, startPos, maxCount, source);
	}


	static private PrintWriter out = new PrintWriter(System.out);
	static private StringBuffer strBuf = new StringBuffer(100000);


	static private void printSingle(int nodeNo, PIndex index, DataGuide guide,
			int startPos, int maxCount, XmlSource source) throws IOException {
		Address addr;
		boolean attrib = guide.getNode(nodeNo).isAttrib();
		String name = guide.getNode(nodeNo).getName();
		Iterator it;
		int count = 10;

		try {
			out.println();
			out.println("________(" + nodeNo + ") " + name + "________");
			out.println("  (Star pos. " + startPos + ", max. elements "
						+ maxCount + ", stored=" + index.getStoredCount(nodeNo)
					+ ", totalNodes=" + index.getDataGuide().getNodeCount(nodeNo)
					+  ")");
			out.flush();
			it = index.addrIterator(nodeNo, startPos, maxCount);
			if (!attrib) {
				name = null;
			}
			while (it.hasNext() && count > 0) {
				count--;
				addr = (PIndex.Address) it.next();
				strBuf.setLength(0);
				source.getDocFragment(addr.addr, name, strBuf);
				out.println(strBuf.substring(0,
						Math.min(78, strBuf.length())));
				out.println();
			}
		} catch (Exception e) {
			System.out.println("\nException in node (" + nodeNo + ") " +
							   guide.getNode(nodeNo).getName() + ", level="
							   + (10 - count));
			e.printStackTrace(System.out);
			System.out.flush();
		}
		out.flush();
	}

	static public void printAll(PIndex index, int maxCount,
			String cfgName, Configuration config) throws IOException {
		DataGuide guide = index.getDataGuide();
		XmlSource source = XmlSource.create(config, cfgName);

		StopWatch watch = new StopWatch();
		watch.start();
		for (int no = 1; no <= guide.getNodeCount(); no++) {
			if (guide.getNode(no).getDepth() > 0) {
				printSingle(no, index, guide, 0, maxCount, source);
			}
			System.out.println();
		}
		System.out.println();
		watch.stop();
		System.out.println("\n\nTime: " + watch);
	}



	static public void printStats(String cfgName, Configuration config)
			throws IOException {
		DataGuide guide = DataGuide.load(config, cfgName);
		Iterator it = guide.iterator();
		int[] posBitLen = new int[64];
		int avgLen = 0, bitLen;
		GuideNode node;
		int end;
		int total = 0;
		int nodeCount = 0;
		int nodeCount_ = 0;

		System.out.println("Node#'s with bit len 0 but min DF word count > 1,");
		System.out.println("and such with count > 1 in [.]:");
		while (it.hasNext()) {
			node = (GuideNode) it.next();
			bitLen = node.getDfWordCountBitLen();
			avgLen += bitLen;
			posBitLen[bitLen]++;
			if (bitLen == 0) {
				nodeCount += node.getCount();
				if (node.getMinDfWordCount() > 1) {
					nodeCount_++;
					if (node.getCount() == 1) {
						System.out.print("  " + node.getNo());
					} else {
						System.out.print("  [" + node.getNo() + "]");
					}
				}
			}
		}
		System.out.println();
		System.out.println("______DF Count Index Statistics_____");
		System.out.println(" Avg. bit len.................. " +
							(avgLen / (double) guide.getNodeCount()));
		System.out.println("# of nodes with bit len 0...... " + nodeCount +
						   " (=" + (100 * nodeCount / (float) guide.getTotalNodeCount()) +
						   "%)");
		System.out.println("  ...nodes with count > 1...... " + nodeCount_);
		System.out.println("# of counters per bit len...... ");
		for (end = 63; end > 0 && posBitLen[end] == 0; end--);
		for (int i = 0; i <= end; i++) {
			System.out.println("    " + i + "\t- " + posBitLen[i]);
			total += posBitLen[i];
		}
		System.out.println("--------------------------");
		System.out.println("Total records: " + total);
	}


	static public void printStoredStats(String cfgName, Configuration config)
			throws IOException {
		PIndex index = PIndex.create(config, cfgName);
		DataGuide guide = index.getDataGuide();
		int total, stored;
		long totalSum = 0, storedSum = 0;
		long totalSize = 0, storedSize = 0, totalWithoutIdx = 0;
		long attrStoredSize = 0;
		long bitsSaved, totalBitsSaved = 0, totalBitsWasted = 0;
		int noSavingsCount = 0;
		int idxBitLen, posBitLen, elemBitLen;
		double avgPosBitLen = 0;
		int x = 0;
		PrintWriter out = new PrintWriter(new FileWriter(
				config.getProperty(cfgName, "StatsHome") + "/" + cfgName + "-"
				+ config.getProperty(cfgName, "PIndexStatsBaseName")
				+ config.getProperty(cfgName, "StatsFileEnding")));

		out.println("Stored vs. total # of elements in P-index");
		out.println();
		out.println("Node#\tTotal\t Stored");
		for (int i = 1; i < guide.getNodeCount(); i++) {
			total = guide.getNode(i).getCount();
			stored = index.getStoredCount(i);
			idxBitLen = (int) Math.ceil(MyMath.log2(total));
			posBitLen = guide.getNode(i).getTotalPosBitLen();
			elemBitLen = idxBitLen + posBitLen;
			out.println(i + "\t" + total + "\t" + stored);
			totalSum += total;
			storedSum += stored;
			totalSize += total * elemBitLen;
			storedSize += stored * elemBitLen;
			if (guide.getNode(i).isAttrib()) {
				attrStoredSize += stored * elemBitLen;
			}
			totalWithoutIdx += total * posBitLen;
			avgPosBitLen += stored * posBitLen;
			bitsSaved = total * posBitLen - stored * elemBitLen;
			totalBitsSaved += bitsSaved;
			if (bitsSaved < 0) {
				noSavingsCount++;
				totalBitsWasted += bitsSaved;
			}
			if (total > 1 && stored == 1) {
				x++;
			}
		}
		avgPosBitLen /= storedSum;
		out.println();
		out.println("sum(Total)\tsum(stored)");
		out.println(totalSum + "\t" + storedSum + " (" +
				(storedSum / (double) totalSum * 100) + "%)");
		out.println("Stored = 1, but total > 1:  " + x);
		out.println();
		out.println("Estimated size at full storage with index.... " + (totalSize / 8));
		out.println("Estimated size at full storage w/o index..... " + (totalWithoutIdx / 8));
		out.println("Estimated stored size........................ " + (storedSize / 8)
					+ " (%total w/o index = " + MyPrint.toPercent(storedSize / (double) totalWithoutIdx, 1) + ")");
		out.println("Estimated stored size without attribs........ " + ((storedSize - attrStoredSize) / 8)
					+ " (%total w/o index = " + MyPrint.toPercent((storedSize - attrStoredSize) / (double) totalWithoutIdx, 1) + ")");
		out.println("Avg. posBit length for stored elements....... " + avgPosBitLen);
		out.println("Bytes saved due to sparse storage............ " + (totalBitsSaved / 8));
		out.println("Record with worse size as for full storage... " + noSavingsCount);
		out.println("Bytes wasted due to sparse storage............ " + (- totalBitsWasted / 8));
		out.println();
		out.close();

	}

}
