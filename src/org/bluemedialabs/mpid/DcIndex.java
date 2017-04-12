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
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.Configuration;
import org.bluemedialabs.util.MutableLong;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class DcIndex {
	private DataGuide guide;
	private long[] fileRecOffset;
	private BitDataChannelPool dataPool;
	private int[] countBitLen;


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public DcIndex create(Configuration config, String cfgName,
								 DataGuide guide) throws IOException {
		// Determine all required configuration strings
//		String sourceHome = config.getProperty(cfgName, "SourceHome");
		String indexHome = config.getProperty(cfgName, "IndexHome");
		String dfCountFileBaseName =
				config.getProperty(cfgName, "DfCountFileBaseName");
		// Construct basic objects from files
		Index index = new IndexChannel(indexHome + "/" + dfCountFileBaseName
				+ IndexSeqFile.INDEX_FILE_ENDING, new MutableLong());
		BitDataChannelPool pool = new BitDataChannelPool(indexHome + "/"
				+ dfCountFileBaseName + IndexSeqFile.DATA_FILE_ENDING);

		return new DcIndex(guide, index, pool);
	}

	static public DcIndex create(Configuration config, String cfgName)
			throws IOException {
		DataGuide guide = DataGuide.load(config, cfgName);
		return create(config, cfgName, guide);
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public DcIndex(DataGuide guide, Index index, BitDataChannelPool dataPool)
			throws IOException {
		this.guide = guide;
		this.dataPool = dataPool;
		loadIndexEtc(index);
	}

	private void loadIndexEtc(Index index) throws IOException {
		MutableLong l = new MutableLong();
		int count = guide.getNodeCount();

		fileRecOffset = new long[count + 1];
		fileRecOffset[0] = -1;
		countBitLen = new int[count + 1];
		countBitLen[0] = -1;
		for (int i = 1; i <= count; i++) {
			// Get record offset
			index.get(i, l);
			// Don't forget to make a *bit* offset out of the byte offset:
			fileRecOffset[i] = l.getValue() * 8;
			// Determine counter bit lengths
			countBitLen[i] = guide.getNode(i).getDfWordCountBitLen();
		}
		// This is not the best location to do the following, but it hopefully
		// won't hurt either.
		index.close();
	}


	// Prepared for addresses longer than 31 bits!
	public long getCount(int nodeNo, int pos) throws IOException {
		int bitLen = countBitLen[nodeNo];
		BitDataChannel data = dataPool.claim();
		try {
			data.bitPosition(fileRecOffset[nodeNo] + (pos * bitLen));
			if (bitLen > 31) {
				long addr, l;
				int diff = bitLen - 31;
				addr = data.read(31);
				l = data.read(diff);
				addr <<= diff;
				addr |= l;
				return addr;
			} else {
				return data.read(bitLen);
			}
		} finally {
			dataPool.release(data);
		}
	}


	public Iterator countIterator(int nodeNo, int startPos, int maxCount)
			throws IOException {
		int constCount = -1;
		GuideNode node = guide.getNode(nodeNo);
		int bitLen = countBitLen[nodeNo];
		long bitPos = -1;
		int entryCount;

		if (bitLen == 0) {
			// All counters are in fact the same;
			// show this by setting constCount
			constCount = node.getMinDfWordCount(); // (== getMaxDfWordCount() !)
		} else {
			// The following is unneccessary in case there is nothing to retrieve
			bitPos = fileRecOffset[nodeNo] + (startPos * bitLen);
		}
		entryCount = Math.min(node.getCount() - startPos,
							  (maxCount >= 0? maxCount: Integer.MAX_VALUE));
		return new CountIterator(nodeNo, dataPool, bitPos, entryCount, guide,
				bitLen, node.getMinDfWordCount(), constCount);
	}

	public Iterator countIterator(int nodeNo, int startPos) throws IOException {
		return countIterator(nodeNo, startPos, -1);
	}

	public Iterator countIterator(int nodeNo) throws IOException {
		return countIterator(nodeNo, 0, -1);
	}


	public int getCountBitLen(int nodeNo) { return countBitLen[nodeNo]; }

	public int getElemCount(int nodeNo) { return guide.getNodeCount(nodeNo); }

	public int getNodeCount() { return guide.getNodeCount(); }


	public String toString() {

		return null;
	}


	/*+**********************************************************************
	 * CountIterator
	 ************************************************************************/

	static class CountIterator extends JoinNnoDbIterator {

		private BitDataChannel in;
//		private int elemCount;
		private int countBitLen;
		private int minValue;
		private int constCount;  // >= 0 only when all counters are the same
		private int currentPos = 0;
		private MutableLong l = new MutableLong();
		private int bitDiff = 0;


		CountIterator(int pno, BitDataChannelPool pool, long bitPos,
					  int entryCount, DataGuide guide, int bitLen,
					  int minValue, int constCount) throws IOException {
			super(pno, pool, bitPos, entryCount, guide);
			if (constCount < 0) {
				assert (pool != null);
			} else {
				l.setValue(constCount);
			}
			this.countBitLen = bitLen;
			this.minValue = minValue;
			this.constCount = constCount;
			if (countBitLen > 31) {
				bitDiff = countBitLen - 31;
			}
			in = getDataChannel();
		}


		boolean hasNextImpl() {
			return (currentPos < getEntryCount());
		}


		MutableLong nextImpl() {
			if (!hasNext()) {
				throw new NoSuchElementException("There are no more "
						+ "addresses left");
			}
			if (constCount < 0) {
				// If all counters have the same value, there is no point in
				// expensively retrieving any data from the index file (in fact,
				// we wouldn't find anything there in that case anyway, because
				// it would also be a waste to store anything).

				try {
//					if (countBitLen > 31) {
//						// Read in two steps
//						long ll = in.read(31);
//						ll <<= bitDiff;
//						ll |= in.read(bitDiff);
//						l.setValue(ll);
//					} else {
//						l.setValue(in.read(countBitLen) + minValue);
//					}
					l.setValue(in.readLong(countBitLen));
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			}
			currentPos++;
			return l;
		}

		public int skip(int n) {
//			if (constCount > 0) {
//				currentPos += n;
//			}
			int skipped = 0;
			while (hasNext() && skipped < n) {
				next();
				skipped++;
			}
			return skipped;
		}

	}


	/*+**********************************************************************
	* TEST
	************************************************************************/

	static public void main(String[] args) throws Exception {
		Configuration cfg = Configuration.load(args[1]);
		DcIndex index = DcIndex.create(cfg, args[0]);
		int nodeNo;
		int startPos = 0;
		int maxCount = Integer.MAX_VALUE;
		long count;
		int sum = 0;

		if (args.length > 3) {
			startPos = Integer.parseInt(args[3]);
		}
		if (args.length > 4) {
			maxCount = Integer.parseInt(args[4]);
		}
		if (args.length > 2) {
			nodeNo = Integer.parseInt(args[2]);
			System.out.println("________DFs for Node# " + args[2] + "_______");
			Iterator it = index.countIterator(nodeNo, startPos, maxCount);
			while (it.hasNext()) {
				count = ((MutableLong) it.next()).getValue();
				sum += count;
				System.out.println("__" + (startPos++) + "__");
				System.out.println(count);
				System.out.println();
			}
			System.out.println("Sum = " + sum);
		} else {
			printStats(args[0], cfg);
		}
	}


	static public void printStats(String cfgName, Configuration config)
			throws IOException {
		DataGuide guide = DataGuide.load(config, cfgName);
		Iterator it = guide.iterator();
		int[] countBitLen = new int[64];
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
			countBitLen[bitLen]++;
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
		for (end = 63; end > 0 && countBitLen[end] == 0; end--);
		for (int i = 0; i <= end; i++) {
			System.out.println("    " + i + "\t- " + countBitLen[i]);
			total += countBitLen[i];
		}
		System.out.println("--------------------------");
		System.out.println("Total records: " + total);


	}
}
