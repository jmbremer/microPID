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
import org.bluemedialabs.mpid.*;
import org.bluemedialabs.mpid.ival.*;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;



/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class PhysAddrIndexer extends Indexer {
	static private final int MAX_DEPTH = 128;

//	private ArrayLists addrLists;
	private NodeLists addrLists;
	private MutableInteger addr = new MutableInteger();
	private String fileName;
//	private int addrBitLen;
	private boolean written = false;


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public void index(String cfgName, Configuration config)
			throws IOException {
		System.out.println("\n__________________PhysAddrIndexer___________________");
		Args args = createArgs(config.getProperty(cfgName, "SourceHome"));
		PhysAddrIndexer indexer;
		// Obtain configuration arguments
		String indexHome = config.getProperty(cfgName, "IndexHome");
		String indexTemp = config.getProperty(cfgName, "IndexTemp");
		String physAddrFileBaseName =
				config.getProperty(cfgName, "PhysAddrFileBaseName");
		// Determine length of bit address for source
		int bitLen = sourceAddrBitLen(cfgName, config);
		NodeLists nl = new NodeLists(args.guide, indexTemp, bitLen,
									 indexHome + "/" + physAddrFileBaseName);
		// Get the indexer going
		indexer = new PhysAddrIndexer(args, nl);
		indexer.start();
		indexer.finalize();
	}

	static public int sourceAddrBitLen(String cfgName, Configuration config)
			throws IOException {
		String srcFileName = config.getProperty(cfgName, "SourceHome") + "/"
					   + config.getProperty(cfgName, "ComprSourceFileName");
		File file = new File(srcFileName);
		long len = file.length() * 8; // File size in bits
		return ((int) Math.ceil(MyMath.log2(len)));
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public PhysAddrIndexer(XmlDecoderStream in, DataGuide guide,
						   NodeLists nodeLists) {
		super(in, guide);
		addrLists = nodeLists;
//		new ArrayLists(addr, guide.getTotalNodeCount(),
//								   guide.getNodeCount() + 1);
//		this.addrBitLen = addrBitLen;
//		this.fileName = fileName;
	}

	public PhysAddrIndexer(Args args, NodeLists nodeLists) {
		this(args.xds, args.guide, nodeLists);
	}

	public void finalize() throws IOException {
		/*
		IndexSeqOutputChannel ch;
		ListWriter writer;
		int bitSize = getAddrBitLen();

		// Store address lists...
		System.out.println();
		System.out.println("Storing physical address index...");
		System.out.println("Total # of elements.............. " +
							addrLists.getUsedElems());
		System.out.println("Length of addresses [bits]....... " + getAddrBitLen());
		ch = IndexSeqOutputChannel.create(fileName, 524288, 2097152, true);
		for (int i = 1; i < addrLists.getListCount(); i++) {
			writer = new ListWriter(addrLists, i, bitSize);
			ch.write(writer);
		}
		ch.close();
		*/
		// LEAVE THIS OFF AS IT SEEMS TO BE HARMFULL SOMEHOW
//		addrLists.setNoAttribs(false); -- now in index()
		// Ensure that data is written only once!!!
		if (!written) {
			addrLists.mingle(new PaStorageCondition(getDataGuide()));
			written = true;
		}
	}


//	public int getAddrBitLen() { return addrBitLen; }


	public void onAttribute(GuideNode node, HashMap termCounts) {
		handlePhysAddr(node, true);
	}

	private void handlePhysAddr(GuideNode node, boolean attrib) {
		/*
//		System.out.println(node.getName() + "\t- " + this.getBitPosition());
		addr.setValue((int) getBitPosition());
		try {
			addrLists.add(node.getNo(), addr);
		} catch (IOException e) {
			assert true: "Addr. list should be large enough to never cause " +
					"an overflow resulting in an IOException";
		}
		 */
		long pos = getBitPosition();
//***		System.out.print("=" + pos + node.getName() + " ");
		try {
			addrLists.add(node.getNo(), pos);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public void onElementStart(GuideNode node) {
		handlePhysAddr(node, false);
	}

	public void onElementEnd(GuideNode node, HashMap termCounts) {
	}



	/*+**********************************************************************
	 * List Storage class
	 ************************************************************************/

	 /**
	  * Writers ...
	  *
	  * @author J. Marco Bremer
	  * @version 1.0
	  */
	static class ListWriter implements BitStorable {
		private ArrayLists lists;
		private int listNo;
		private int bitSize;

		ListWriter(ArrayLists lists, int listNo, int bitSize) {
			this.lists = lists;
			this.listNo = listNo;
			this.bitSize = bitSize;
		}


		/*+******************************************************************
		 * Storable implementation
		 ********************************************************************/

		public void store(DataOutput out) throws IOException {
			throw new UnsupportedOperationException();
		}


		public void load(DataInput in) throws IOException {
			throw new UnsupportedOperationException();
		}


		public int byteSize() {
			return 0;
		}


		/*+******************************************************************
		 * BitStorable implementation
		 ********************************************************************/

		public void store(BitOutput out) throws IOException {
			Iterator it;
			int count = 0;
			MutableInteger m;

			// Pass 1: count elements
			it = lists.listIterator(listNo);
			while (it.hasNext()) {
				count++;
				it.next();
			}
			// Pass 2: Write counter and elements
			out.write(count, 32);
			it = lists.listIterator(listNo);
			while (it.hasNext()) {
				m = (MutableInteger) it.next();
				out.write(m.getValue(), bitSize);
			}
		}


		public void load(BitInput in) throws IOException {
			throw new UnsupportedOperationException();
		}


		public long bitSize() {
			return 0;
		}

	}


	/**
	 * <p>Condition under which node lists of physical addresses should be
	 * stored.</p>
	 *
	 * @author J. Marco Bremer
	 * @version 1.0
	 */
	static class PaStorageCondition implements NodeLists.StorageCondition {
		private DataGuide guide;

		public PaStorageCondition(DataGuide guide) {
			this.guide = guide;
		}

		public boolean store(int nodeNo) {
			return !(guide.getNode(nodeNo).isEquivToParent()
					 && guide.getNode(nodeNo).isAttrib());
		}
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	public static void main(String[] args) throws Exception {
		StopWatch watch = new StopWatch();
		Configuration config = Configuration.load(args);
		String cfgName = args[0];

		watch.start();
		PhysAddrIndexer.index(cfgName, config);
		watch.stop();
		System.out.println("Complete elapsed time: " + watch);
	}
}