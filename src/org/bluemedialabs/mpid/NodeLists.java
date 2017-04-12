package org.bluemedialabs.mpid;

import java.io.*;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.Configuration;
import org.bluemedialabs.util.MyMath;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class NodeLists {
	static private final String FILE_SUFFIX = ".tmp";
	static private final int DEFAULT_TOTAL_BUFFER_SIZE = 8388608; // 8MB

	private DataGuide guide;
	private String tmpDir;
	private int addrBitLen;  // -1 for counters instead of addresses
	private String dstFileBaseName;
	private BitDataOutput[] bdos;  // The bit output wrapper
	private BufferedOutputChannel files[];  // The underlying stream
	private int bufferSize;
	private int elemCounts[];
	private int totalCounts[];
	private int addrBitLens[] = null;
	private int listCount;

	private boolean noEquivNodes = false;



	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	// This is valid only for indexing/storing physical addresses related
	// to the A-index right now!!!
	static public NodeLists create(String cfgName, Configuration config)
			throws IOException {
		// Obtain configuration arguments
		String indexHome = config.getProperty(cfgName, "IndexHome");
		String indexTemp = config.getProperty(cfgName, "IndexTemp");
		String physAddrFileBaseName =
				config.getProperty(cfgName, "PhysAddrFileBaseName");
		DataGuide guide = DataGuide.load(config, cfgName);
		// Determine length of bit address for source
		int bitLen = PhysAddrIndexer.sourceAddrBitLen(cfgName, config);
		/*
		 * For counters, we have to extend this here...
		 */
		return new NodeLists(guide, indexTemp, bitLen, indexHome + "/"
							 + physAddrFileBaseName);
	}



	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public NodeLists(DataGuide guide, String tmpDir, int addrBitLen,
					 String dstFileBaseName) throws IOException {
		this.guide = guide;
		this.tmpDir = tmpDir;
		this.addrBitLen = addrBitLen;
		this.dstFileBaseName = dstFileBaseName;
		listCount = guide.getNodeCount();
		bufferSize = DEFAULT_TOTAL_BUFFER_SIZE / listCount;
		bdos = new BitDataOutput[listCount + 1];
		files = new BufferedOutputChannel[listCount + 1];
		elemCounts = new int[listCount + 1];
		totalCounts = new int[listCount + 1];
		if (addrBitLen <= 0) {
			addrBitLens = new int[listCount + 1];
			addrBitLens[0] = -1;
		}
		totalCounts[0] = 0;
		for (int i = 0; i <= listCount; i++) {
			bdos[i] = null;
			files[i] = null;
			if (i > 0) {
				totalCounts[i] = guide.getNode(i).getCount();
				if (addrBitLen <= 0) {
					// Variable-size elements bit lengths
					addrBitLens[i] = guide.getNode(i).getDfWordCountBitLen();
				}
			}
			elemCounts[i] = 0;
		}
		// Make sure tmp directory exists
		File file = new File(tmpDir);
		if (!file.exists()) {
			file.mkdir();
		}
	}


	public void finalize() {

	}

//	public void setNoEquivNodes(boolean a) { noEquivNodes = a; }
//	public boolean isNoEquivNodes() { return noEquivNodes; }


	protected void mingle(StorageCondition cond) throws IOException {
		IndexSeqOutputChannel ch = IndexSeqOutputChannel.create(
				dstFileBaseName, false);  // No bit output!
		BufferedInputChannel in;
		// Close all yet open files
		for (int i = 1; i <= listCount; i++) {
			if (files[i] != null) {
				bdos[i].flush();
				files[i].flush();
				files[i].close();
			}
		}
		// Now, open one file after the other and mingle them
		for (int i = 1; i <= listCount; i++) {
//			System.out.println("\nRecord " + i + "... ");
			/*
			 * ATTENTION EVERYONE! THE ATTRIBUTE CONDITION IS ONLY GOOD
			 * FOR A-INDEX!
			 */
//			if (!(noEquivNodes && guide.getNode(i).isEquivToParent()
//				  && guide.getNode(i).isAttrib())) {
				// Open tmp file

			if (cond.store(i)) {
				in = BufferedInputChannel.create(tmpDir + "/" + i + FILE_SUFFIX);
				ch.write(new RecordWriter(in));
				in.close();
			} else {
				// Just skip this record
				ch.write(null);
//				System.out.println("  Skipping node# " + i + " ("
//								   + guide.getNode(i).getName() + ")");
			}
		}
		ch.close();
	}


	public void add(int no, long value) throws IOException {
		if (files[no] == null) {
			// Time to create this file...
			files[no] = BufferedOutputChannel.create(
					tmpDir + "/" + no + FILE_SUFFIX, bufferSize);
			bdos[no] = new BitDataOutput(files[no]);
		}
		if (addrBitLen > 0) {
			bdos[no].write(value, addrBitLen);
		} else {
			bdos[no].write(value, addrBitLens[no]);
		}
		elemCounts[no]++;
		if (elemCounts[no] == totalCounts[no]) {
			// No more values to be expected
			bdos[no].flush();
			files[no].flush();
			files[no].close();
			bdos[no] = null;
			files[no] = null;
		}
	}


	/*+**********************************************************************
	 * RecordWriter
	 ************************************************************************/

	/**
	 * Writes an input record related to a single node# to the final combined
	 * (index-sequential) file.
	 *
	 * @author J. Marco Bremer
	 * @version 1.0
	 */
	static class RecordWriter implements Storable {
		private DataInput in;

		public RecordWriter(DataInput in) {
			this.in = in;
		}

		public void store(DataOutput out) throws IOException {
			byte b;
//			int i;
//			String s;

			try {
				while (true) {
					b = in.readByte();
//					i = (b & 0xFF);
//					s = Integer.toHexString(i);
//					System.out.print(" " + s);
					out.writeByte(b);
				}

			} catch (EOFException e) {
				// We are finished here.
			}
		}


		public void load(DataInput in) throws IOException {
			throw new UnsupportedOperationException();
		}


		public int byteSize() {
			return -1;
		}
	} // END: RecordWriter


	/*+**********************************************************************
	 * RecordWriter
	 ************************************************************************/

	 /**
	  * Writes an input record related to a single node# to the final combined
	  * (index-sequential) file.
	  *
	  * @author J. Marco Bremer
	  * @version 1.0
	  */
	static protected interface StorageCondition {

		public boolean store(int nodeNo);

	} // END: StorageCondition

	static public class DefaultStorageCondition implements StorageCondition {

		public boolean store(int nodeNo) { return true; }
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	public static void main(String[] args) throws Exception {
		Configuration config = Configuration.load(args);
		String cfgName = args[0];
		NodeLists lists = NodeLists.create(cfgName, config);
		for (int i = 1; i <= 22; i++) {
			lists.add(i, 1);
		}
		lists.add(1, 2);
		lists.add(2, 2);
		lists.mingle(new DefaultStorageCondition());
	}
}