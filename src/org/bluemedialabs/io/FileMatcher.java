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
package org.bluemedialabs.io;

import java.io.*;
import java.util.*;


/**
 * <p>Takes a directory and a single file name substring and returns all
 * files in all subdirectories of the given directory that match that string.
 * </p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class FileMatcher implements Enumeration {
	static private final String SPACE = "\n\n";
	private File mainDir;
	private FilenameFilter filter;
	private Comparator comparator;  // To order file names within a directory
	protected LinkedList files;
	private boolean firstFileFetched = false;
	private File enumFile;  // used for enumeration


	/**
	 *
	 * @param dirName The fully qualified name of the root directory to be
	 *  searched.
	 * @param filePattern The simple non-regular file name substring to be
	 *  matched against the files discovered.
	 */
	public FileMatcher(String dirName, FilenameFilter filter, Comparator comp)
			throws IOException {
		mainDir = new File(dirName);
		if (!mainDir.exists()) {
			throw new FileNotFoundException("The root directory to be searched "
					+ "('" + dirName + "')" + " does not exist");
		}
		mainDir = mainDir.getAbsoluteFile();
		this.filter = filter;
		this.comparator = comp;
		files = new LinkedList();
		fileSearch(mainDir, "");
	}

//	public File getFirstFile() {
//		fileSearch(mainDir, "");
//		return getNextFile();
//	}

	protected File getNextFile() {
		String fileName;

		if (files.size() == 0) {
			return null;
		}
		// Getting here, we got at least one file
		fileName = (String) files.getFirst();
		files.removeFirst();
//		System.out.println();
//		System.err.println("Returning file '" + fileName + "' as next file "
//			+ "in sequence...");
//		System.err.flush();
		return new File(mainDir, fileName);
	}

	private void fileSearch(File file, String currentDirName) {
//			System.out.println("New directory found: '"
//						+ file.getName() + "'");
			// Have a directory, so, look for subdirectories and files
			ArrayList fileList = new ArrayList();
			File[] list = file.listFiles();
			for (int i = 0; i < list.length; i++) {
				// add regular files to a new list
				if (list[i].isFile() && filter.accept(null, list[i].getName())) {
					fileList.add(list[i]);
				}
			}
			// Sort files by name in case it is desired
			if (comparator != null) {
				Collections.sort(fileList, comparator);
			}
			// Now, add all the files to the output list
			for (int i = 0; i < fileList.size(); i++) {
				files.add(currentDirName + "/" + ((File) fileList.get(i)).getName());
			}
			// For all directories do the same recursively again
			 for (int i = 0; i < list.length; i++) {
				if (!list[i].isFile()) {
					fileSearch(list[i], currentDirName + "/"
						+ list[i].getName());
				}
			}
	}

	private void sort(File[] files) {
		File tmp;
		for (int i = 0; i < files.length - 1; i++) {
			for (int j = i + 1; j < files.length; j++) {
				if (files[i].isFile()) {
					// At least one file!
					if (files[j].isFile()
							&& comparator.compare(files[j].getName(),
								files[i].getName()) < 0) {
						// Swap files
						tmp = files[i];
						files[i] = files[j];
						files[j] = tmp;
					}
				} else if (files[j].isFile()) {
					// Only j is a file (i is a directory) => always swap
					tmp = files[i];
					files[i] = files[j];
					files[j] = tmp;
				} else {
					// Two directories => let lexicographical order decide
					if (files[i].getName().compareTo(files[j].getName()) > 0) {
						tmp = files[i];
						files[i] = files[j];
						files[j] = tmp;
					}
				}
				// Otherwise, just leave everything as it is.
			}
		}
	}


	/*+**********************************************************************
	 * Enumeration implementation
	 ************************************************************************/

	/**
	 * Starts a new enumeration of the matching files of this matcher.
	 */
//	public void initEnumeration() {
//		fileSearch(mainDir, "");
//	}

	public boolean hasMoreElements() {
		return (files.size() > 0);
	}

	public Object nextElement() {
		return getNextFile();
		/*
		InputStream is = null;
		if (hasMoreElements()) {
			try {
				is = new SequenceInputStream(
						new UncompressInputStream(
							new FileInputStream(getNextFile())),
						new ByteArrayInputStream(SPACE.getBytes()));
			} catch (FileNotFoundException e) {
				throw new IllegalStateException("File not found in FileMatcher "
					+ "enumeration, but more files should be there");
			} catch (IOException e) {
				throw new IllegalStateException("IO problem in FileMatcher "
					+ "enumeration, but more data should be there");
			}
			return is;
		} else {
			return null;
		}
		*/
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	public static void main(String[] args) throws IOException {
//		FileMatcher matcher = new FileMatcher("/cdrom/ft",
//				new TrecInputStream.FinancialTimesFilter(),
//				new TrecInputStream.FinancialTimesFilter());
//		FileMatcher matcher = new FileMatcher("/cdrom/fr94",
//				new TrecInputStream.FedRegisterFilter(),
//				new TrecInputStream.FedRegisterFilter());
//		FileMatcher matcher = new FileMatcher("/cdrom/latimes",
//				new TrecInputStream.LaTimesFilter(),
//				new TrecInputStream.LaTimesFilter());
		FileMatcher matcher = new FileMatcher("/cdrom/fbis",
				new TrecInputStream.ForeignBroadcastFilter(),
				new TrecInputStream.ForeignBroadcastFilter());

		System.out.println("Printing the list of files found...");
		File file = (File) matcher.nextElement();
		if (file == null) {
			System.out.println("No files found.");
		} else {
			while (matcher.hasMoreElements()) {
				System.out.println(file.getAbsoluteFile().getName());
				file = (File) matcher.nextElement();
			}
		}
	}

}