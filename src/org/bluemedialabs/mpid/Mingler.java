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
import java.nio.CharBuffer;
import java.text.ParseException;
import org.bluemedialabs.io.*;
import org.bluemedialabs.mpid.ival.IvalDfList;
import org.bluemedialabs.util.*;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Mingler extends MyPrint {
	static public final int READ_BUFFER_SIZE    = 65536;    // 64K
	static public final int WRITE_BUFFER_SIZE   = 1048576;  // 1MB
//	static public final int INDEX_BUFFER_SIZE   = 16384;
	static public final boolean USE_CHANNELS    = true;

	static public final boolean DEFAULT_BIT_LOADSTORE = true;

//	static public Terms terms = new Terms();  -- only for DEBUGGING

	private DataGuide guide;



	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

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
		DataFileFilter filter;
		File dir;
		File[] dataFiles = null;
		try {
			filter = new DataFileFilter(fullBaseName);
			dir = filter.getDirectory();
			dataFiles = dir.listFiles(filter);
			if (dataFiles != null) {
				for (int i = 0; i < dataFiles.length; i++) {
					dataFiles[i].delete();
				}
			} // otherwise, there is nothing to remove...
		} catch (Exception e) {
			System.out.println("Failed to remove files matching '"
							   + fullBaseName + "', because:");
			e.printStackTrace();
		}
		return (dataFiles != null? dataFiles.length: 0);
	}


	/**
	 * Mingles files of inverted lists to a single file of mingled inverted
	 * list.
	 *
	 * @param repDir
	 * @param baseName
	 * @throws IOException
	 */
	static public void mingle(String repDir, String baseName, InvertedList list,
							  boolean sort, int recordCount, boolean bitLoad)
			throws IOException {
		IndexSeqBitOutput out;  // The ouput destinaton
		MyFile[] files;      // Wrappers around the input files
		InvertedList mainList = list;
		MutableInteger minIndex = new MutableInteger();
		MutableInteger minRecNo = new MutableInteger();
		int recNo = 0, i;
		StopWatch watch = new StopWatch();
		PercentPrinter pprint = new PercentPrinter(recordCount);

		// (-1) Load terms...
//		terms= Terms.load(repDir + "/terms");
		baseName = repDir + "/" + baseName;
		System.out.print("Mingling '" + baseName.substring(0,
					baseName.lastIndexOf('.')) + "' data files...");

		// (0) Create destination file
		if (USE_CHANNELS) {
			// FOR CHANNELS...
			out = IndexSeqOutputChannel.create(baseName.substring(0,
					baseName.lastIndexOf('.')), 524288, 2097152, bitLoad);
		} else {
			// FOR STREAMS...
			out = IndexSeqOutputStream.create(baseName.substring(0,
					baseName.lastIndexOf('.')), bitLoad);
		}

//		baseName = "/F/tmp/termdf-i.data";

		// (I) Find and create files for the given base name
		DataFileFilter filter = new DataFileFilter(baseName);
		// First, create the actual files...
		File dir = filter.getDirectory();
		File[] dataFiles = dir.listFiles(filter);
		sortFiles(dataFiles);
		files = new MyFile[dataFiles.length];
		System.out.println("  .." + files.length + " files...");
		// ..then create our file wrappers
		for (i = 0; i < files.length; i++) {
			System.out.print("Creating file wrapper " + (i + 1) + "...");
			files[i] = new MyFile(dataFiles[i], (InvertedList) list.clone(), bitLoad);
			System.out.println(files[i] + "...");
		}

		// (II) Determine smallest index of currently smallest record #
		findMinRecord(files, minIndex, minRecNo);
		System.out.println("Mingling records...");
		pprint.notify(0);
		watch.start();
		while (minIndex.value >= 0) {
			recNo++;
			if (minRecNo.value > recNo) {
//				System.out.print("...some records without data... ");
				// At least one record number does not occur in any of the input
				// files. Thus, make sure to keep record numbers in sync:
				for (i = recNo; i < minRecNo.value; i++) {
					out.write(null);
				}
				recNo = minRecNo.value;
			}
			mainList.clear();
			mainList.setNo(recNo);
			for (i = minIndex.value; i < files.length; i++) {
				if (!files[i].isEmpty() && files[i].getRecNo() == recNo) {
//					System.out.print("..appending file " + i + " (term '"
//						+ terms.get(files[i].getRecNo()).getName() + "')) ..");
//					System.out.println("Appending list\n" + files[i].getInvList());
					mainList.append(files[i].getInvList());
					// Don't forget to fetch the next record(!):
					try {
//						System.out.print(".." + i + ".");
//						if (i == 7) {
//							System.out.print("!!!");
//						}
						files[i].next();
					} catch (OutOfMemoryError e) {
						System.out.println("Run out of memory while loading "
								+ "new data from file " + i + ", current main "
								+ "list length is " + mainList.length());
//								+ ", supposed additional list length is "
//								+ files[i].getNextLength() + " bytes");
						throw e;
					}
				}
			}
			// Sort list
			if (sort) {
				mainList.sort();
			}
//			System.out.print("list_length=" + mainList.length());
//			System.out.println("\nWriting merged list:\n" + mainList);

			// Store list in whatever fashion the list thinks is best
			// (compressed/uncompressed)
			out.write(mainList);

			pprint.notify(minRecNo.value);

//			System.out.println("  [written]");
			findMinRecord(files, minIndex, minRecNo);
		} // While
		watch.stop();
		System.out.println();
		out.close();
		System.out.println("Done mingling files (raw time = " + watch + ").");
	} // mingle()


	/**
	 * Determines the smallest index of the record with the currently smallest
	 * number. Returns both the array index and the related record number.
	 *
	 * @param files
	 * @param minIndex ..., -1 if no more records at all were available.
	 * @param minRecNo
	 */
	static private void findMinRecord(MyFile[] files, MutableInteger minIndex,
									   MutableInteger minRecNo) {
		minRecNo.setValue(Integer.MAX_VALUE);
		minIndex.setValue(-1);

		for (int i = 0; i < files.length; i++) {
			if (!files[i].isEmpty() && files[i].getRecNo() < minRecNo.getValue()) {
				minRecNo.setValue(files[i].getRecNo());
				minIndex.setValue(i);
			}
		}
	}


	/**
	 * Makes sure the files are in their order of creation, which is not
	 * the lexicographical order. Either we do this sorting or we have to
	 * do an expensive sorting of single list elements later.
	 *
	 * @param files
	 */
	static private void sortFiles(File[] files) {
		int len = baseNameLen(files[0].getName());
		String name1, name2;
		String num1, num2;
		int n1, n2;
		File tmp;

		// Good, old selection sort ;-)
		for (int i = 0; i < files.length - 1; i++) {
			for (int j = i + 1; j < files.length; j++) {
				name1 = files[i].getName();
				name2 = files[j].getName();
				num1 = name1.substring(len);
				num2 = name2.substring(len);
				n1 = Integer.parseInt(num1);
				n2 = Integer.parseInt(num2);
				if (n1 > n2) {
					// Swap files
					tmp = files[i];
					files[i] = files[j];
					files[j] = tmp;
				}
			}
		}
	}

	static private int baseNameLen(String name) {
		int len = name.length() - 1;
		while (name.charAt(len) == '0'
			   || name.charAt(len) >= '1' && name.charAt(len) <= '9') {
			len--;
		}
		return (len + 1);
	}


	/**
	 * Please, no instances of this class...
	 */
	private Mingler() {}


	/************************************************************************
	 * Stores all information to one of the files being mingled.
	 ************************************************************************/
	static private class MyFile {
		private File file;
		private DataInput in;   // BufferedInputChannel/DataInputStream
		private BitDataInput bitIn = null;
		private MutableInteger recNo = new MutableInteger();
		private InvertedList first; // current first record
		private boolean empty = false;
		private DataGuide guide;
		private boolean bitLoad;

		private MyFile(File file, InvertedList list, boolean bitLoad)
				throws IOException {
			// Create input stream from file
			this.file = file;
			this.first = list;
			this.bitLoad = bitLoad;

			if (USE_CHANNELS) {
				// FOR CHANNLES...
				in = BufferedInputChannel.create(file.getPath(), READ_BUFFER_SIZE);
			} else {
				// FOR FILES...
				in = new DataInputStream(new BufferedInputStream(
						new FileInputStream(file.getPath()), READ_BUFFER_SIZE));
			}
			if (bitLoad) {
				bitIn = new BitDataInput(in);
			}
			// Read first record
			next();
		}

		protected boolean isEmpty() {
			return empty;
		}

		protected void next() throws IOException {
			next(first);
		}

		protected void next(InvertedList first) throws IOException {
			if (!empty) {
				try {
					if (!bitLoad) {
						recNo.load(in);
						// Just in case the list needs to know its # to be loaded:
						first.setNo(recNo.getValue());
						//	 System.out.print(" ..next record: " + recNo + ".");
						//	 System.out.flush();
						//	 System.out.print(
						//	 terms.get(recNo.getValue()).getName() + "')..");
						first.load(in);
					} else {
						recNo.setValue(bitIn.read(32));
						first.setNo(recNo.getValue());
						first.load(bitIn);
					}
				} catch (EOFException e) {
					empty = true;
				} catch (IllegalArgumentException e2) {
					System.out.println("Problems reading from file " + file
									   + ":");
					e2.printStackTrace();
				}
			}
		}



		protected int getRecNo() {
			return recNo.getValue();
		}

		protected InvertedList getInvList() {
			return first;
		}

		public String toString() {
			return file.toString();
		}
	}


	/************************************************************************
	 * File filter for data files with a sequential, positive number attached
	 * to them.
	 ************************************************************************/
	static private class DataFileFilter implements FilenameFilter {
		private File dir;
		private String fileName;

		private DataFileFilter(String baseName) {
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
			System.out.println("Creating filter for files '" + fileName + "'" +
								" in directory '" + path + "'...");
		}

		private File getDirectory() {
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



	/************************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		System.out.println("\n____________________Mingler_____________________");

		// Extract and check the arguments
		CharBuffer repDirBuf = CharBuffer.allocate(256);
		CharBuffer baseFileNameBuf = CharBuffer.allocate(128);
		MutableBoolean useIval = new MutableBoolean();
		MutableBoolean term = new MutableBoolean();
		Configuration config =
				obtainArguments(args, repDirBuf, baseFileNameBuf, useIval, term);

		// Prepare the rest of the required components
		String repDir = repDirBuf.toString();
		String baseFileName = baseFileNameBuf.toString();
		DataGuide guide = DataGuide.load(config, args[0]);
		Decoder decoder;   // Need it for termCount
		int recCount;
		InvertedList li;
		StopWatch watch = new StopWatch();

		// Create inverted list according to node id type
		if (useIval.value) {
			li = new IvalDfList(1000, guide);
		} else {
			if (term.value) {
				/*
				 * Use TermDfList for encoded storage:
				 */
				li = new TermDfList(guide);
//				li = new InvertedList(new PathId());
			} else {
				li = new NodeDfList(guide);
			}
		}

		System.out.println("Starting mingling...");
		watch.start();
		if (term.value) {
			// Need term count as record count
			decoder = Decoder.load(config, args[0]);
			recCount = decoder.getTermCount();
		} else {
			if (useIval.value) {
				recCount = guide.countUniqueNames(null);
			} else {
				recCount = guide.getNodeCount();
			}
		}
		// Note: recCount is just for the mingler's PercentPrinter
		// NEED TO TAKE CARE OF SORTING/NOT SORTING !!!
		Mingler.mingle(repDir, baseFileName, li, false, recCount,
		// This is the regular setting...
					   DEFAULT_BIT_LOADSTORE);
		// ..this is for termdf-p as a temporary solution
		// (but already obsolete!)
//					   false);
		watch.stop();
		System.out.println("Done mingling.");
		System.out.println("Time taken:  " + watch);
	}


	static private Configuration obtainArguments(String[] args,
			CharBuffer repDir, CharBuffer baseFileName, MutableBoolean useIval,
			MutableBoolean term) throws IOException, ParseException {
		repDir.clear();
		baseFileName.clear();

		if (args.length != 4) {
			printUsage(args, "wrong # of arguments");
			return null;
		} else {
			// args[1] is supposed to be a configuration file name, thus...
			Configuration config = Configuration.load(args);
			String cfgName = args[0];
			repDir.put(config.getProperty(cfgName, "IndexTemp"));
			String type = args[2].toLowerCase();
			if (!(type.length() == 1
				  && (type.compareTo("t") == 0 || type.compareTo("n") == 0)
				  || type.compareTo("term") == 0 || type.compareTo("node") == 0)) {
				printUsage(args, "invalid file type");
			} else {
				if (type.charAt(0) == 't') {
					term.setValue(true);
				} else {
					term.setValue(false);
				}
			}
			String id = args[3].toLowerCase();
			if (id.charAt(0) == 'p') {
				// Files are PID-based
				useIval.setValue(false);
				if (term.value) {
					baseFileName.put(config.getProperty(
							cfgName, "PidTermFileBaseName") + ".data");
				} else {
					baseFileName.put(config.getProperty(
							cfgName, "PidNodeFileBaseName") + ".data");
				}
			} else {
				// Files are IID-based
				useIval.setValue(true);
				if (term.value) {
					baseFileName.put(config.getProperty(
							cfgName, "IidTermFileBaseName") + ".data");
				} else {
					baseFileName.put(config.getProperty(
							cfgName, "IidNodeFileBaseName") + ".data");
				}
			}
			repDir.flip();
			baseFileName.flip();
			return config;
		}
	}


	static public void printUsage(String[] args, String cause) {
		pl("Mingler requires exactly 3 arguments:");
		pl("(1) Configuration id");
		pl("(2) Configuration file name");
		pl("(3) File type ('[t]erm'/'[n]ode')");
		pl("(4) Node id type ('[p]id'/'[i]id')");
		pl();
		p("Supplied " + args.length + " arguments are:");
		for (int i = 0; i < args.length; i++) {
			p("  " + args[i]);
		}
		pl("   (" + cause + ")");
		System.exit(1);
	}
}