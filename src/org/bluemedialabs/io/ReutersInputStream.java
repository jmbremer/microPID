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
import java.util.zip.ZipInputStream;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class ReutersInputStream extends XmlSeqInputStream {
	static public final String HEADER =
		"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
		"\n";
	private FileMatcher matcher;
	private SequenceInputStream in;


	/**
	 *
	 */
	static public InputStream create(String directory, String tag)
			throws IOException {
		Object filterComp = new ReutersFilter();
		return new ReutersInputStream(directory, tag,
					 (FilenameFilter) filterComp, (Comparator) filterComp);
	}


	protected ReutersInputStream(FileMatcher matcher, String collectionTag)
			throws FileNotFoundException {
		super(matcher, collectionTag);
	}

	/**
	 *
	 */
	protected ReutersInputStream(String mainDirectory, String collectionTag,
			FilenameFilter filter, Comparator fileSequencer)
			throws IOException {
		super(new ZipFileMatcher(mainDirectory, filter, fileSequencer), collectionTag);
	}


	protected InputStream createInputStream(Object source) throws IOException {
		InputStream is = null;
		BufferedReader br;
		InputStreamReader r;
		int ch;
		String line;

//		try {
			is = (InputStream) source;

			// Remove first line from each file...
			while ((char) (ch = is.read()) != '>');// System.err.print("" + (char) ch);
//			r = new InputStreamReader(is);
//			while ((ch = r.read()) != '>') System.err.print("" + (char) ch);
//			line = br.readLine();
//			while ((line = br.readLine()).starts != '>') util.MyPrint.p("" + (char) ch);


			// Here, we should be at the end of the first line...
			// ..the rest can remain to be interpreted by the XML processor.
			is = new SequenceInputStream(is,
					new ByteArrayInputStream(SPACE.getBytes()));
//		} catch (FileNotFoundException e) {
//			throw new IllegalStateException("File not found in FileMatcher "
//					+ "enumeration, but more files should be there");
//		} catch (IOException e) {
//			throw new IllegalStateException("IO problem in FileMatcher "
//					+ "enumeration, but more data should be there");
//		}
		return is;
	}



	/************************************************************************
	 * Reuters Filter
	 ************************************************************************/
	static public class ReutersFilter implements FilenameFilter, Comparator {
		public boolean accept(File dir, String name) {
			if (name.regionMatches(true, 0, "199", 0, 3)
					&& name.endsWith(".zip")) {
				return true;
			} else {
				return false;
			}
		}

		public int compare(Object obj1, Object obj2) {
			return obj1.toString().compareTo(obj2.toString());
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj instanceof ReutersFilter) {
				return true;
			} else {
				return false;
			}
		}
	}



	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {

//		ZipInputStream zis = new ZipInputStream(new FileInputStream("/cdrom/19960922.zip"));
//		BufferedReader r = new BufferedReader(new InputStreamReader(zis));
//		System.out.println(r.readLine());
//		System.exit(0);


		InputStream reuters = ReutersInputStream.create(
				"/cdrom", "Reuters");
		BufferedReader br = new BufferedReader(new InputStreamReader(reuters));
		String line;

		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}
	}

}