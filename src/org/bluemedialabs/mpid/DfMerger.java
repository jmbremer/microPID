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
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.MutableInteger;
//import org.bluemedialabs.util.MutableLong;
import org.bluemedialabs.util.StopWatch;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class DfMerger {
	static public final int READ_BUFFER_SIZE    = 65536;    // 64K
	static public final int WRITE_BUFFER_SIZE   = 1048576;  // 1MB
//	static public final int INDEX_BUFFER_SIZE   = 16384;
	static public final boolean USE_CHANNELS    = true;

	static public Terms terms = new Terms();

	private DataGuide guide;


	static public int removeOldFiles(String baseName) {
		DataFileFilter filter = new DataFileFilter(baseName);
		File dir = filter.getDirectory();
		File[] dataFiles = dir.listFiles(filter);
		for (int i = 0; i < dataFiles.length; i++) {
			dataFiles[i].delete();
		}
		return dataFiles.length;
	}


	static public void merge(String repDir, String baseName)
			throws IOException {
		FileOutputStream indexOut;
		FileOutputStream dataOut;
		IndexSeqOutput out;
		MyFile[] files;
		DataGuide guide;
		InvertedDfList mainList = new InvertedDfList(2000,
				(guide = DataGuide.load(repDir + "/guide.tree")));
		int minIndex, minRecNo;
		int recNo = 0;
		int i;
		int percent = 0;
		StopWatch watch = new StopWatch();

		// (-1) Load terms...
		terms= Terms.load(repDir + "/terms");
		baseName = repDir + "/" + baseName;
		System.out.println("Merging '" + baseName.substring(0,
					baseName.lastIndexOf('.')) + "' data files...");

		// (0) Create destination file
		// FOR CHANNELS...
		if (USE_CHANNELS) {
			out = IndexSeqOutputChannel.create(baseName.substring(0,
					baseName.lastIndexOf('.')), 524288, 2097152, false);
		} else {
			// FOR STREAMS...
			out = IndexSeqOutputStream.create(baseName.substring(0,
					baseName.lastIndexOf('.')), false);
		}

		// (I) Find and create files for the given base name
		DataFileFilter filter = new DataFileFilter(baseName);
		File dir = filter.getDirectory();
		File[] dataFiles = dir.listFiles(filter);
		files = new MyFile[dataFiles.length];
		System.out.println("Merging " + files.length + " files...");
		for (i = 0; i < files.length; i++) {
			System.out.println("Loading from file " + (i + 1) + "...");
			files[i] = new MyFile(dataFiles[i], guide);
			System.out.println("   ..." + files[i]);
		}

		// (II) Determine smallest index of currently smallest record #
		minIndex = findMinRecIndex(files);
		System.out.println("Merging records...");
		watch.start();
		while (minIndex >= 0) {
			recNo++;
			minRecNo = files[minIndex].getRecNo();
			if (recNo % (Math.floor(terms.getTermCount() / 100)) == 0) {
				System.out.print(".." + (++percent) + "%..");
//			    System.out.print(recNo + " (" + minIndex + ")\t");
				if (percent % 10 == 0) {
					System.out.print(watch);
				}
			}
			if (minRecNo > recNo) {
//				System.out.print("...some records without data... ");
				// At least one record number does not occur in any of the input
				// files. Thus, make sure to keep record numbers in sync:
				for (i = recNo; i < minRecNo; i++) {
					out.write(null);
				}
				recNo = minRecNo;
			}
			mainList.clear();
			for (i = minIndex; i < files.length; i++) {
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
								+ "new data from file " + i);
						throw e;
					}
				}
			}
			// Sort list
			mainList.sort();
//			System.out.print("list_length=" + mainList.length());
//			System.out.println("\nWriting merged list:\n" + mainList);

			// FOR UNCOMPRESSED STORAGE...
			out.write(mainList);
			// FOR COMPRESSED STORAGE...
//			mainList.storeEncoded(out);

//			System.out.println("  [written]");
			minIndex = findMinRecIndex(files);
		}
		watch.stop();
		System.out.println();
		out.close();
//		indexOut.close();
//		dataOut.close();
		System.out.println("Done merging files (raw time = " + watch + ").");
	}

	/**
	 * Determines the smallest index of the record with the currently smallest
	 * number. Returns -1 if no record could be found at all.
	 */
	static private int findMinRecIndex(MyFile[] files) {
		int minRecNo = Integer.MAX_VALUE;
		int minIndex = -1;

		for (int i = 0; i < files.length; i++) {
			if (!files[i].isEmpty() && files[i].getRecNo() < minRecNo) {
				minRecNo = files[i].getRecNo();
				minIndex = i;
			}
		}
		return minIndex;
	}

	/**
	 * Merges the record at the supplied index with all records with the same
	 * number and higher index. Note that after findMinRecIndex() records
	 * with the same number can only come after this index.
	 */
	static private void merge(int index) {
		// get inv list
		// merge all later lists
		// ...!!!

	}

	private DfMerger() {}


	/************************************************************************
	 * Stores all information to one of the files being merged.
	 ************************************************************************/
	static private class MyFile {
		private File file;
		private DataInput in;   // BufferedInputChannel/DataInputStream
		private MutableInteger recNo = new MutableInteger();
		private InvertedDfList first; // current first record
		private boolean empty = false;
		private DataGuide guide;

		private MyFile(File file, DataGuide guide) throws IOException {
			// Create input stream from file
			this.file = file;
			this.guide = guide;

			first = new InvertedDfList(guide);
			if (USE_CHANNELS) {
				// FOR CHANNLES...
				in = BufferedInputChannel.create(file.getPath(), READ_BUFFER_SIZE);
			} else {
				// FOR FILES...
				in = new DataInputStream(new BufferedInputStream(
						new FileInputStream(file.getPath()), READ_BUFFER_SIZE));
			}
			// Read first record
			next();
		}

		protected boolean isEmpty() {
			return empty;
		}

		protected void next() throws IOException {
			if (!empty) {
				try {
					recNo.load(in);
//					System.out.print(" ..next record: " + recNo + ".");
//					System.out.flush();
//					System.out.print(
//						terms.get(recNo.getValue()).getName() + "')..");
					first.load(in);
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

		protected InvertedDfList getInvList() {
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
		String DATA_HOME = "somewhere";
		String REP_HOME = DATA_HOME + "/XMLsmall";
		StopWatch watch = new StopWatch();

		try {
			// The final output...
			// Merge the multiple inverted files into one
			System.out.println("Merging DF inverted files...");
			watch.start();
			DfMerger.merge(REP_HOME, "termdf.data");
			watch.stop();
			System.out.println("Done with merging.");
			System.out.println("Time for merging:  " + watch);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}