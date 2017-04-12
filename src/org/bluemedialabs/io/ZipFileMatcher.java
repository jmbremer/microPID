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
import java.util.Comparator;
import java.util.zip.*;
import org.bluemedialabs.io.ReutersInputStream.ReutersFilter;
//import org.bluemedialabs.util.MyPrint;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class ZipFileMatcher extends FileMatcher {
	private File file = null;
	private ZipFile zipFile = null;
	private ZipInputStream zipStream = null;
	private ZipEntry nextEntry = null;
	private int entryCount = 0;  // Just for informative purposes

	public ZipFileMatcher(String dirName, FilenameFilter filter,
						  Comparator comp) throws IOException {
		super(dirName, filter, comp);
		//Preopen first file
		file = getNextFile();
		zipFile = new ZipFile(file);
		zipStream = new ZipInputStream(new FileInputStream(file));
		nextEntry = zipStream.getNextEntry();
	}



  /*+**********************************************************************
   * Enumeration implementation
   ************************************************************************/

	public boolean hasMoreElements() {
		return (files.size() > 0 || nextEntry != null);
		// This is not completely sain!?
	}

	public Object nextElement() {
		ZipEntry entry;
		try {
			if (nextEntry == null) {
				// Nothing left; try to open next file, if existing, and return
				// its first entry
				System.out.print("Done with ZIP file '" + file.getName() + "'...");
				if (entryCount > 0) {
					System.out.println(entryCount + " entries...");
				}

				file = getNextFile();
				if (file == null) {
					// Nothing to do as there simply isn't any data left
					return null;
				}

				zipFile = new ZipFile(file);
				zipStream = new ZipInputStream(new FileInputStream(file));
				nextEntry = zipStream.getNextEntry();
				entryCount = 0;
				// Ready to read from another entry...
			}
			// Just go to the next entry
			entry = nextEntry;
			nextEntry = zipStream.getNextEntry();
			entryCount++;

//			System.out.println("   ..new entry '" + currentEntry.getName() + "'...");
			return zipFile.getInputStream(entry);

		} catch (FileNotFoundException e) {
			throw new IllegalStateException("File not found in ZipFileMatcher "
					+ "enumeration, but more files should be there");
		} catch (IOException e) {
			throw new IllegalStateException("IO problem in ZipFileMatcher "
					+ "enumeration, but more data should be there (" + e + ")");
		}
	}



	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws IOException {
		writeReuters();
//		readReuters();
	}


	static public void writeReuters() throws IOException {
		ReutersFilter f = new ReutersFilter();
		ZipFileMatcher m = new ZipFileMatcher("/cdrom", f, f);
		BufferedReader r = new BufferedReader(
				new InputStreamReader((InputStream) m.nextElement()));
		String line;
		int lineNum = 0;
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
				/*new GZIPOutputStream(*/new FileOutputStream(
				"/home/bremer/Data/Source/reuters.xml")/*, 1024 * 1024 * 4)*/));

		// Write header
		line = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>";
		w.write(line, 0, line.length());
		w.newLine();
		w.newLine();
		w.write("<Reuters>", 0, 9);
		w.newLine();
		// Write body
		while (r != null) {
			while ((line = r.readLine()) != null) {
				if (!line.startsWith("<?")) {
					lineNum++;
					w.write(line, 0, line.length());
					w.newLine();
				}
			}
			r = new BufferedReader(
				new InputStreamReader((InputStream) m.nextElement()));
//			if (lineNum > 1000) {
//				r = null;
//			}
		}
		// Write footer
		w.write("</Reuters>", 0, 10);
		w.newLine();
		w.flush();
		w.close();
	}


	static public void readReuters() throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(
				new GZIPInputStream(new BufferedInputStream(
				new FileInputStream("fish!!!"), 1024 * 1024))));
		String line;
		int lineNum = 0;

		while ((line = r.readLine()) != null && lineNum < 1000000) {
			lineNum++;
			System.out.println(line);
		}
	}

}