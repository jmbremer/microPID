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
import java.util.Enumeration;
import java.util.NoSuchElementException;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public abstract class XmlSeqInputStream extends InputStream {
	static public final String HEADER =
		"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
		"\n";
	static protected final String SPACE = "\n";

	private SequenceInputStream in;


	/**
	 *
	 * @param mainDirectory
	 * @param collectionTag
	 * @param filter
	 * @param fileSequencer
	 * @throws FileNotFoundException
	 */
	protected XmlSeqInputStream(FileMatcher matcher, String collectionTag)
			throws FileNotFoundException {
//		FileMatcher matcher = new FileMatcher(mainDirectory, filter, fileSequencer);
		ByteArrayInputStream header = new ByteArrayInputStream(
			(HEADER + "<" + collectionTag + ">\n").getBytes());
		ByteArrayInputStream footer = new ByteArrayInputStream(
			("\n</" + collectionTag + ">\n").getBytes());
		in = new SequenceInputStream(header,
					  new SequenceInputStream(
						  new SequenceInputStream(
							  new InputStreamEnumeration(matcher, this)),
								  footer));
	}


	/**
	 * Generates an input stream from a source supplied by a file matcher. This
	 * method has to be implemented in order to allow for custom source handling,
	 * e.g., removing the first lines from each input file. Currently, sources
	 * may be files or input streams (s. ZipFileMatcher).
	 *
	 * @param source (a File or InputStream)
	 */
	protected abstract InputStream createInputStream(Object source)
			throws IOException;



	/*+**********************************************************************
	 * Input stream implementation
	 ************************************************************************/

	public int available() throws IOException {
		return in.available();
	}

	public void close() throws IOException {
		in.close();
	}

	public int read() throws IOException {
		return in.read();
	}

	public int read(byte[] b) throws IOException {
		return in.read(b);
	}

	public int read(byte[] b, int off, int len) throws IOException {
		return in.read(b, off, len);
	}

	public void reset() throws IOException {
		in.reset();
	}

	public long skip(long n) throws IOException {
		return in.skip(n);
	}



	/************************************************************************
	 * An input stream enumeration based on a set of XML base files.
	 ************************************************************************/
	static public class InputStreamEnumeration implements Enumeration {
		private FileMatcher matcher;
		private XmlSeqInputStream xsis;

		public InputStreamEnumeration(FileMatcher matcher, XmlSeqInputStream xsis) {
			this.matcher = matcher;
			this.xsis = xsis;
		}

		public boolean hasMoreElements() {
			return (matcher.hasMoreElements());
		}

		public Object nextElement() {
			Object source;
			InputStream is = null;

			if (!matcher.hasMoreElements()) {
				throw new NoSuchElementException("There are no more files "
						+ "left to create an input stream from");
			} else {
				try {
					source = matcher.nextElement();
					is = xsis.createInputStream(source);
				} catch (IOException e) {
//					System.out.println();
					is = null;
				}
				return is;
			}
		}

	}

}