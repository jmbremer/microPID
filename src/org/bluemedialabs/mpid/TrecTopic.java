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
import org.bluemedialabs.io.Storable;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TrecTopic implements Storable {
	private int number = 0;
	private String title = null;
	private String description = null;
	private String narrative = null;


	static public Collection loadTopics(String fileName)
			throws IOException, FileNotFoundException {
		FileInputStream fis = new FileInputStream(fileName);
		DataInputStream dis = new DataInputStream(fis);
		TrecTopic topic;
		LinkedList li = new LinkedList();
		boolean eof = false;

		while (!eof) {
			try {
				topic = new TrecTopic();
				topic.load(dis);
//				System.out.println(topic);
				li.add(topic);
			} catch (EOFException e) {
				eof = true;
			}
		}
		return li;
	}


	public TrecTopic() {}

	public int getNumber() { return number; }
	public String getTitle() { return title; }
	public String getDescription() { return description; }
	public String getNarrative() { return narrative; }

	public String toString() {
		StringBuffer buf = new StringBuffer(2048);

		buf.append("<top>\n");
		buf.append("  <num> ");
		buf.append(number);
		buf.append(" </num>\n");
		buf.append("  <title> ");
		buf.append(title);
		buf.append(" </title>\n");
		buf.append("  <desc> ");
		buf.append(description);
		buf.append(" </desc>\n");
		buf.append("  <narr> ");
		buf.append(narrative);
		buf.append(" </narr>\n");
		buf.append("</top>\n");
		return buf.toString();
	}


	/*+**********************************************************************
	 * Storable implementation
	 ************************************************************************/

	public void store(DataOutput out) throws IOException {
		throw new UnsupportedOperationException("Storing of TrecTopic(s) not "
			+ "supported yet");
	}

	public void load(DataInput in) throws IOException {
//		BufferedReader br = null;
		String line;
		int pos, num;

//		if (in instanceof InputStream) {
//			br = new BufferedReader(new InputStreamReader((InputStream) in));
//		} else {
//			throw new UnsupportedOperationException("Loading of TREC topic is "
//				+ "only supported via an input stream, sorry");
//		}

		line = in.readLine();
		if (line == null) {
			throw new EOFException("Cannot load topic as there is no data left "
				+ "to be read");
		}
		while (line != null) {
			line.trim();
			if (line.length() == 0) {
				// ignore empty lines
				line = in.readLine();
			} else if (line.startsWith("</top>")) {
				// finished after topic end tag
				line = null;
			} else {
				if (line.startsWith("<num>")) {
					// extract number
					pos = line.lastIndexOf(":");
					try {
						number = Integer.parseInt(line.substring(pos + 2).trim());
					} catch (NumberFormatException e) {
//						e.printStackTrace();
						System.out.println("Current line is '" + line + "'");
						System.out.println("Number substring is '"
							+ line.substring(pos + 2) + "'");
						throw e;
					}
				} else if (line.startsWith("<title>")) {
					title = line.substring(7).trim();
				} else if (line.startsWith("<desc>")) {
					description = readLines(in);
				} else if (line.startsWith("<narr>")) {
					narrative = readLines(in);
				}
				line = in.readLine();
			}
		}
	}

	private String readLines(DataInput in) throws IOException {
		StringBuffer buf = new StringBuffer(1000);
		String line;
		while ((line = in.readLine()).length() > 0) {
			buf.append(line);
			buf.append(" ");
		}
		buf.setLength(buf.length() - 1);
		return buf.toString();
	}

	public int byteSize() {
		return -1;  // don't know
	}



	static public void main(String[] args) throws Exception {
		Collection c = TrecTopic.loadTopics("F:/Data/topics.401-450.txt");
		Iterator it = c.iterator();

		while (it.hasNext()) {
			System.out.println(it.next());
		}
	}
}