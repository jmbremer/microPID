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
import org.bluemedialabs.io.LogStream;
import org.bluemedialabs.util.MutableInteger;
import org.bluemedialabs.util.MutableLong;

/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class FlatMerger {
	static public final int READ_BUFFER_SIZE    = 16384;    // 16K
	static public final int WRITE_BUFFER_SIZE   = 131072;   // 128K
	static public final int INDEX_BUFFER_SZIE   = 16384;


	static public void merge(String baseName)
			throws IOException {
		FileOutputStream indexOut;
		FileOutputStream dataOut;
		IndexSeqOutput out;
		MyFile[] files;
		FlatInvertedList mainList = new FlatInvertedList(4000);
		int minIndex, minRecNo;
		int recNo = 0;
		int i;

		System.out.println("Merging '" + baseName.substring(0,
					baseName.lastIndexOf('.')) + "' data files...");
		// (0) Create destination files (index + data)
//		dataOut = new FileOutputStream(baseName);
//		indexOut = new FileOutputStream(baseName.substring(
//					baseName.lastIndexOf('.')) + ".index");
//		out = new IndexSeqOutputStream(indexOut, dataOut,	LogStream.DEFAULT_STREAM);
		out = IndexSeqOutputStream.create(baseName.substring(0,
					baseName.lastIndexOf('.')), false);
		// (I) Find and create files for the given base name
		DataFileFilter filter = new DataFileFilter(baseName);
		File dir = filter.getDirectory();
		File[] dataFiles = dir.listFiles(filter);
		files = new MyFile[dataFiles.length];
		System.out.println("Merging files...");
		for (i = 0; i < files.length; i++) {
			files[i] = new MyFile(dataFiles[i]);
			System.out.println("   ..." + files[i]);
		}
		// (II) Determine smallest index of currently smallest record #
		minIndex = findMinRecIndex(files);
//		System.out.println("Merging records...");
		while (minIndex >= 0) {
			recNo++;
			minRecNo = files[minIndex].getRecNo();
//			System.out.print(recNo + " (" + minIndex + ")\t");
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
					mainList.append(files[i].getInvList());
					// Don't forget to fetch the next record(!):
					files[i].next();
				}
			}
//			System.out.print("list_length=" + mainList.length());
			out.write(mainList);
//			System.out.println("  [written]");
			minIndex = findMinRecIndex(files);
		}
//		System.out.println();
		out.close();
//		indexOut.close();
//		dataOut.close();
		System.out.println("Done merging files.");
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

	private FlatMerger() {}


	/**
	 * Stores all information to one of the files being merged.
	 */
	static private class MyFile {
		private File file;
		private DataInputStream in;
		private MutableInteger recNo = new MutableInteger();
		private FlatInvertedList first = new FlatInvertedList(); // current first record
		private boolean empty = false;

		private MyFile(File file) throws IOException {
			// Create input stream from file
			this.file = file;
			in = new DataInputStream(new BufferedInputStream(
				new FileInputStream(file), FlatMerger.READ_BUFFER_SIZE));
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
					first.load(in);
				} catch (EOFException e) {
					empty = true;
				}
			}
		}

		protected int getRecNo() {
			return recNo.getValue();
		}

		protected FlatInvertedList getInvList() {
			return first;
		}

		public String toString() {
			return file.toString();
		}
	}


	/**
	 * File filter for data files with a sequential, positive number attached
	 * to them.
	 */
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

}