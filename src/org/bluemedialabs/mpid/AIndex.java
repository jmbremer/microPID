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
public class AIndex {
	// Address count is not part of the record anymore!
	static private final int ADDR_COUNT_BIT_LEN = 0;

	private DataGuide guide;
	private long[] fileRecOffset;
	private BitDataChannelPool dataPool;
	private int addrBitLen;


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public AIndex create(Configuration config, String cfgName,
								DataGuide guide) throws IOException {
		// Determine all required configuration strings
//		String sourceHome = config.getProperty(cfgName, "SourceHome");
		String indexHome = config.getProperty(cfgName, "IndexHome");
		String physAddrFileBaseName =
				config.getProperty(cfgName, "PhysAddrFileBaseName");
		// Construct basic objects from files
		Index index = new IndexChannel(indexHome + "/" + physAddrFileBaseName
				+ IndexSeqFile.INDEX_FILE_ENDING, new MutableLong());
		BitDataChannelPool pool = new BitDataChannelPool(indexHome + "/"
				+ physAddrFileBaseName + IndexSeqFile.DATA_FILE_ENDING);
		int bitLen = PhysAddrIndexer.sourceAddrBitLen(cfgName, config);

		return new AIndex(guide, index, pool, bitLen);
	}

	static public AIndex create(Configuration config, String cfgName)
			throws IOException {
		DataGuide guide = DataGuide.load(config, cfgName);
		return create(config, cfgName, guide);
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public AIndex(DataGuide guide, Index index, BitDataChannelPool dataPool,
				   int addrBitLen) throws IOException {
		this.guide = guide;
		this.dataPool = dataPool;
		this.addrBitLen = addrBitLen;
		loadIndex(index);
	}

	private void loadIndex(Index index) throws IOException {
		MutableLong l = new MutableLong();
		int count = guide.getNodeCount();

		fileRecOffset = new long[count + 1];
		fileRecOffset[0] = -1;
//		addrCount = new int[count + 1];
//		addrCount[0] = -1;
		for (int i = 1; i <= count; i++) {
			// Get record offset
			index.get(i, l);
			// Don't forget to make a *bit* offset out of the byte offset:
			fileRecOffset[i] = l.getValue() * 8;
			// Get address counter
//			data.bitPosition(fileRecOffset[i]);
//			addrCount[i] = data.read(ADDR_COUNT_BIT_LEN);
		}
		// This is not the best location to do the following, but it hopefully
		// won't hurt either.
		index.close();
	}


	public DataGuide getDataGuide() { return guide; }


	// Prepared for addresses longer than 31 bits!
	public long getAddress(int nodeNo, int pos) throws IOException {
		GuideNode node = guide.getNode(nodeNo);
		long addr;

		if (node.isAttrib() && node.isEquivToParent()) {
			node = node.getParent();
			nodeNo = node.getNo();
			System.out.println("\nAttribute (" + nodeNo + ") with equivalent "
							   + "parent (" + node.getNo() + "), taking "
							   + "parents addresses...");
		}
		BitDataChannel data = dataPool.claim();
		try {
			data.bitPosition(fileRecOffset[nodeNo] + ADDR_COUNT_BIT_LEN
							 + (pos * addrBitLen));
			addr = data.readLong(addrBitLen);
			return addr;
//			if (addrBitLen > 31) {
//				long addr, l;
//				int diff = addrBitLen - 31;
//				addr = data.read(31);
//				l = data.read(diff);
//				addr <<= diff;
//				addr |= l;
//				return addr;
//			} else {
//				return data.read(addrBitLen);
//			}
		} catch (IOException e) {
			System.out.println("\nnode#=" + nodeNo + ", pos=" + pos);
			throw e;
		} finally {
			dataPool.release(data);
		}
	}


	public AddrIterator addrIterator(int nodeNo, int startPos, int maxCount)
			throws IOException {
		GuideNode node = guide.getNode(nodeNo);
		long bitPos;
		int entryCount;

		if (node.isAttrib() && node.isEquivToParent()) {
			node = node.getParent();
			nodeNo = node.getNo();
			System.out.println("\nAttribute (" + nodeNo + ") with equivalent "
							   + "parent (" + node.getNo() + "), taking "
							   + "parents addresses...");
		}
		bitPos = fileRecOffset[nodeNo]	+ ADDR_COUNT_BIT_LEN
			  + (startPos * addrBitLen);
		entryCount = Math.min(node.getCount() - startPos,
							  (maxCount >= 0? maxCount: Integer.MAX_VALUE));
		return new AddrIterator(nodeNo, dataPool, bitPos, entryCount, guide,
								addrBitLen);
	}

	public AddrIterator addrIterator(int nodeNo, int startPos) throws IOException {
		return addrIterator(nodeNo, startPos, -1);
	}

	public AddrIterator addrIterator(int nodeNo) throws IOException {
		return addrIterator(nodeNo, 0, -1);
	}


	public int getAddrBitLen() { return addrBitLen; }

	public int getAddrCount(int nodeNo) { return guide.getNodeCount(nodeNo); }

	public int getNodeCount() { return guide.getNodeCount(); }


	public String toString() {

		return null;
	}


	/*+**********************************************************************
	 * AddrIterator
	 ************************************************************************/

	static class AddrIterator extends JoinNnoDbIterator {
		private BitDataChannel in;
		private int addrBitLen;
		private int bitDiff = 0;
		private int currentPos = 0;
		private MutableLong l = new MutableLong();


		AddrIterator(int pno, BitDataChannelPool pool, long bitPos,
					 int entryCount, DataGuide guide, int bitLen)
				throws IOException {
			super(pno, pool, bitPos, entryCount, guide);
			in = getDataChannel();
			addrBitLen = bitLen;
			if (addrBitLen > 31) {
				bitDiff = addrBitLen - 31;
			}
		}


		boolean hasNextImpl() {
			return (currentPos < getEntryCount());
		}


		MutableLong nextImpl() {
			if (!hasNext()) {
				throw new NoSuchElementException("There are no more "
						+ "addresses left");
			}
			try {
				if (addrBitLen > 31) {
					// Read in two steps
					long ll = in.read(31);
					ll <<= bitDiff;
					ll |= in.read(bitDiff);
					l.setValue(ll);
				} else {
					l.setValue(in.read(addrBitLen));
				}
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
			currentPos++;
			return l;
		}

	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		Configuration cfg = Configuration.load(args[1]);
		AIndex index = AIndex.create(cfg, args[0]);
		DcIndex index2 = DcIndex.create(cfg, args[0]);
		XmlSource source = XmlSource.create(cfg, args[0]);
		
		int nodeNo = Integer.parseInt(args[2]);
		int startPos = 0;
		int maxCount = Integer.MAX_VALUE;
		long addr;
		long count;

		if (args.length > 3) {
			startPos = Integer.parseInt(args[3]);
		}
		if (args.length > 4) {
			maxCount = Integer.parseInt(args[4]);
		}

		// NOTE: USING ONLY THE A-INDEX TO PRINT ALL DOC. FRAGMENTS DOES
		// NOT WORK FOR ATTRIBUTES, IF THEIR RELATED LISTS ARE ONLY STORED
		// IMPLICITLY THROUGH THEIR PARENTS
		// (This is, because attributes may not occur in every parent.)
		// => NOT TRUE ANY MORE, HOPEFULLY. FIXED THIS BUG.

		/*
		System.out.println("________DFs for Node# " + args[2] + "_______");
		Iterator it = index.addrIterator(nodeNo, startPos, maxCount);
		Iterator it2 = index2.countIterator(nodeNo, startPos, maxCount);
		while (it.hasNext()) {
			addr = ((MutableLong) it.next()).getValue();
			count = ((MutableLong) it2.next()).getValue();
			System.out.println("__" + (startPos++) + "__ (count=" + count + ")");
			System.out.println(source.getDocFragment(addr));
			System.out.println();
			System.out.println("  " + addr);
		}
		*/
		printAll(index, index2, source);

	}

	static private void printAll(AIndex aIndex, DcIndex dcIndex,
								  XmlSource source)	throws IOException {
		long addr, count;
		int i;
		DataGuide guide = aIndex.getDataGuide();
		int nodeCount = guide.getNodeCount();
		StringBuffer buf = new StringBuffer(4096);

		for (int no = 1; no <= nodeCount; no++) {
			System.out.println("________DFs for Node# " + no + " ("
				+ guide.getNodeCount(no) + ")_______");
			Iterator it = aIndex.addrIterator(no, 0, 10);
			Iterator it2 = dcIndex.countIterator(no, 0, 10);
			i = 1;
			while (it.hasNext() & i <= 3) {
				addr = ((MutableLong) it.next()).getValue();
				count = ((MutableLong) it2.next()).getValue();
				System.out.println("  " + addr);

				System.out.println("__" + (i++) + "__ (count=" + count + ")");
				if (guide.getNode(no).getDepth() > 2) {
					try {
						buf.setLength(0);
						source.getDocFragment(addr, buf);
						System.out.println(buf);
						System.out.println();
					} catch (Exception e) {
						System.out.println("Cannot print DF because:");
						e.printStackTrace();
						System.out.flush();
					}
				}
			}
		}
	}

}
