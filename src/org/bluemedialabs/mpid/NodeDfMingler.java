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
import java.util.*;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class NodeDfMingler {
	static public final int INITIAL_QUEUE_SIZE = 10000;


	/*+**********************************************************************
	 * Class functions
	 ************************************************************************/

	static public void mingle(String cfgName, Configuration config)
			throws IOException {
		String indexTemp = config.getProperty(cfgName, "IndexTemp");
		String indexHome = config.getProperty(cfgName, "IndexHome");
		String dstFileName = config.getProperty(cfgName, "NodeDfFileBaseName");
		String srcFileName = config.getProperty(cfgName, "NodeDfTmpFileBaseName");
		DataGuide guide = DataGuide.load(config, cfgName);
		LinkedList li = new LinkedList();
		int fileCount = 1;
		BitDataInput in;
		BitDataInput[] ins;
		Iterator it;
		IndexSeqOutputChannel out;
//		BitDataOutput bitOut;
		NodeDfMingler mingler;
		StopWatch watch = new StopWatch();
		int nodeCount;
		int[] posBitLen;
		int[] idxBitLen;

		// Discover all input files
		try {
			while (true) {
				in = new BitDataInput(BufferedInputChannel.create(
						indexTemp + "/" + srcFileName + fileCount));
				li.add(in);
				fileCount++;
			}
		} catch (FileNotFoundException e) {
			// Nothing to do here
		}
		fileCount--;
		// Put input files into single array
		ins = new BitDataInput[fileCount];
		it = li.iterator();
		for (int i = 0; i < fileCount; i++) {
			ins[i] = (BitDataInput) it.next();
		}
		// Create output file
		out = IndexSeqOutputChannel.create(indexHome + "/" + dstFileName, true);
		// Determine pos bit lengths
		nodeCount = guide.getNodeCount();
		posBitLen = new int[nodeCount + 1];
		posBitLen[0] = -1;
		idxBitLen = new int[nodeCount + 1];
		idxBitLen[0] = -1;
		for (int i = 1; i <= nodeCount; i++) {
			posBitLen[i] = guide.getNode(i).getTotalPosBitLen();
			idxBitLen[i] = guide.getNode(i).getIndexBitLen();
		}
		// Create and start mingler
		mingler = new NodeDfMingler();
		System.out.println("Mingling " + indexTemp + "/" + srcFileName
						   + "[1.." + fileCount + "]...");
		watch.start();
		mingler.mingle(ins, out, posBitLen, idxBitLen);
		out.close();
		watch.stop();
		System.out.println("\n...done.");
		System.out.println("Time: " + watch);
	}


	/**
	 * Removes all files with the given fully-qualified base name, i.e., a
	 * file name including the full directory path only excluding a trailing
	 * sequential number.
	 *
	 * @param fullBaseName The base file name including the fully-qualified
	 *   directory path
	 * @return The number of files removed.
	 */
   static public int removeOldFiles(String fullBaseName) {
	   TmpFileFilter filter;
	   File dir;
	   File[] dataFiles = null;
	   try {
		   filter = new TmpFileFilter(fullBaseName);
		   dir = filter.getDirectory();
		   dataFiles = dir.listFiles(filter);
		   for (int i = 0; i < dataFiles.length; i++) {
			   dataFiles[i].delete();
		   }
	   } catch (Exception e) {
		   System.out.println("Failed to remove files matching '"
							  + fullBaseName + "', because:");
		   e.printStackTrace();
	   }
	   return dataFiles.length;
   }



   /*+***********************************************************************
	* Object functions
	*************************************************************************/

   public NodeDfMingler() {

   }


   public void mingle(BitDataInput[] in, IndexSeqOutputChannel out,
					  int[] posBitLen, int[] idxBitLen)
		   throws IOException {
	   int fileCount = in.length;
	   int[] headNo = new int[fileCount];
	   int currentNo;
	   RecordWriter writer = new RecordWriter();
	   LongQueue posQ = new LongQueue(INITIAL_QUEUE_SIZE);
	   IntQueue idxQ = new IntQueue(INITIAL_QUEUE_SIZE);

	   // Fetch the node#'s for the first record of each file
	   for (int file = 0; file < fileCount; file++) {
		   try {
			   headNo[file] = in[file].read(32);
		   } catch (EOFException e) {
			   // This means, there are no more records to read
			   headNo[file] = -1;
		   }
	   }
	   currentNo = min(headNo);
		// Now, start the regular mingling procedure
	   while (currentNo > 0) {
//		   System.out.print(" {" + currentNo + "}");
		   out.write(writer.reuse(in, headNo, currentNo,
								  posBitLen[currentNo],idxBitLen[currentNo],
								  posQ, idxQ));
//		   for (int file = 0; file < fileCount; file++) {
//			   // Fetch the new node#'s at the head of the input
//			   if (headNo[file] == currentNo) {
//				   try {
//					   headNo[file] = in[file].read(32);
//				   } catch (EOFException e) {
//					   // This means, there are no more records to read
//					   headNo[file] = -1;
//				   }
//			   }
//		   }
		   currentNo = min(headNo);
	   }
   }

   private int min(int[] a) {
	   int min = a[0];
	   for (int i = 1; i < a.length; i++) {
		   if (min < 0) {
			   min = a[i];
		   } else if (a[i] > 0) {
			   min = Math.min(a[i], min);
		   }
	   }
	   return min;
   }


   static class RecordWriter implements BitStorable {
	   private BitDataInput[] in;
	   private int[] headNo;
	   private int nodeNo;
	   private int posBitLen;
	   private int idxBitLen;
	   private LongQueue posQ;
	   private IntQueue idxQ;
//	   private DataGuide guide;   -- would be needed for storage optimization

//	   public RecordWriter() {}

	   public RecordWriter reuse(BitDataInput[] in, int[]  headNo, int nodeNo,
								 int posBitLen, int idxBitLen, LongQueue posQ,
								 IntQueue idxQ/*, DataGuide guide*/) {
		   this.in = in;
		   this.headNo = headNo;
		   this.nodeNo = nodeNo;
		   this.posBitLen = posBitLen;
		   this.idxBitLen = idxBitLen;
		   this.posQ = posQ;
		   this.idxQ = idxQ;
//		   this.guide = guide;
		   return this;
	   }

	   public void store(BitOutput out) throws IOException {
		   int elemCount;
		   int idx = 0;
		   long lastBits = -10, posBits;

		   posQ.clear();
		   idxQ.clear();
		   for (int file = 0; file < in.length; file++) {
			   if (headNo[file] == nodeNo) {
				   elemCount = in[file].read(32);
				   while (elemCount-- > 0) {
					   posBits = in[file].readLong(posBitLen);
					   if (posBits > lastBits + 1) {
						   posQ.enqueueLong(posBits);
						   idxQ.enqueueInt(idx);
					   }
					   lastBits = posBits;
					   idx++;
				   }
				   // Fetch new node number at the head of the file input
				   try {
					   headNo[file] = in[file].read(32);
				   } catch (EOFException e) {
					   // This means, there are no more records to read
					   headNo[file] = -1;
				   }
			   }
		   }
		   // Now, actually write the data...
		   // ..first, the number of actually stored elements...
		   int size = posQ.size();
//		   System.out.print("\n" + nodeNo + ") strd=");
		   out.write(size, 32);
//		   System.out.print(size);
//		   System.out.flush();
		   for (int i = 0; i < size; i++) {
			   posBits = posQ.dequeueLong();
			   idx = idxQ.dequeueInt();
			   out.write(posBits, posBitLen);
			   out.write(idx, idxBitLen);
//			   System.out.print("  " + posBits + "/" + idx);
		   }
//		   System.out.println();
	   }


	   public void load(BitInput in) throws IOException {
		   throw new UnsupportedOperationException();
	   }


	   public long bitSize() {
		   return -1;
	   }


	   public void store(DataOutput out) throws IOException {
		   throw new UnsupportedOperationException();
	   }


	   public void load(DataInput in) throws IOException {
		   throw new UnsupportedOperationException();
	   }

	   public int byteSize() {
		   return -1;
	   }

   }


   /************************************************************************
	* File filter for data files with a sequential, positive number attached
	* to them.
	************************************************************************/
   static class TmpFileFilter implements FilenameFilter {
	   private File dir;
	   private String fileName;

	   TmpFileFilter(String baseName) {
		   // Extract directory
		   int pos = baseName.lastIndexOf('/');
		   String path;

		   if (pos <= 0) {
			   // there seems to be no directory thus, the name not absolute
			   path = "./"; // Is that a valid path!?
		   } else {
			   path = baseName.substring(0, pos);
		   }
		   dir = new File(path);
		   fileName = baseName.substring(pos + 1, baseName.length());
	   }

	   File getDirectory() {
		   return dir;
	   }

	   public boolean accept(File dir, String name) {
		   String number;
		   int i;
		   if (dir.compareTo(this.dir) != 0 || !name.startsWith(fileName)) {
			   return false;
		   }
		   number = name.substring(fileName.length(), name.length());
		   try {
			   i = Integer.parseInt(number);
		   } catch (NumberFormatException e) {
			   // File name doesn't end with a number
			   return false;
		   }
		   if (i <= 0) {
			   return false;
		   } else {
			   return true;
		   }
	   }
   }


   /*+**********************************************************************
	* TEST
	************************************************************************/

   public static void main(String[] args) throws Exception {
	   StopWatch watch = new StopWatch();
	   Configuration cfg = Configuration.load(args);
	   String cfgName = args[0];

	   watch.start();
	   NodeDfMingler.mingle(cfgName, cfg);
	   watch.stop();
	   System.out.println("Complete elapsed time: " + watch);

	   // Some tests:
//	   DataGuide guide = DataGuide.load(cfg, cfgName);
//	   int fileCount = 1;
//	   for (int i = 1; i <= fileCount; i++) {
//		   testInputFile(cfg.getProperty(cfgName, "IndexTemp") + "/"
//			   + cfg.getProperty(cfgName, "NodeDfTmpFileBaseName") + i, guide);
//	   }
   }


	static public void testInputFile(String fileName, DataGuide guide)
			throws IOException {
		BufferedInputChannel ch = BufferedInputChannel.create(fileName);
		BitDataInput in = new BitDataInput(ch);
		int nodeNo;
		int elemCount = -42;
		int printCount;
		long posBits;
		int posBitLen;
		int idx;
		int idxBitLen;

		System.out.println("Contents of file '" + fileName + "'...");
		try {
			while (true) {
				nodeNo = in.read(32);
				elemCount = in.read(32);
				posBitLen = guide.getNode(nodeNo).getTotalPosBitLen();
				idxBitLen = guide.getNode(nodeNo).getIndexBitLen();
				System.out.print("\n" + nodeNo + ") [" + elemCount + "]");
				printCount = 100;  // Max. # of elements to be printed
				while (elemCount-- > 0) {
					posBits = in.read(posBitLen);
//					if (nodeNo == 74 && posBits == 303) {
//						System.out.println("break");
//					}
//					idx = in.read(idxBitLen);
					if (printCount-- > 0) {
						System.out.print("  " + posBits /*+ "/" + idx*/);
					}
				}
			}
		} catch (EOFException e) {
			System.out.println("\n--EOF-- (elemCount=" + elemCount + ")");
		}

	}

}