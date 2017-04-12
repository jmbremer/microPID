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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
//import java.util.*;
import java.util.zip.GZIPInputStream;
import org.bluemedialabs.util.*;


/**
 * <p> </p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class XmlCleaner {
	private BufferedReader in;
	private Writer out;
	private char ch = 0;
	private boolean eof = false;
	private Quack tagStack = new Quack();
	private PercentPrinter pprinter;
	private long count = 0;


	public XmlCleaner(BufferedReader in, Writer out, long fileLength) {
		this.in = in;
		this.out = out;
		pprinter = new PercentPrinter(fileLength);
	}


	protected void nextChar() throws IOException {
		int c = in.read();
		if (c < 0) {
			eof = true;
			ch = 0;
		} else {
			ch = (char) c;
			count++;
		}
//		System.out.print(ch);
		if (count % 1000 == 0) {
			pprinter.notify(count);
		}
	}

	protected void writeChar() throws IOException {
		if (!eof) {
			out.write(ch);
		}
	}

	public void clean() throws IOException{
		CharBuffer buf = CharBuffer.allocate(100);
		boolean endTag;
		String tag, startTag;

		while (!eof) {
			nextChar();
			if (ch == '<') {
				writeChar();
				nextChar();
				endTag = parseTag(buf);
				buf.flip();
				if (ch != '>') {
					System.out.println("Missing '>' for tag '" + buf.toString()
									   + "' at file pos. " + count);
					out.write('>');
					writeChar();
//					printFollowing(1000);
//					System.exit(1);
				}
				if (!endTag) {
					tagStack.push(buf.toString());
				} else {
					startTag = (String) tagStack.pop();
					tag = buf.toString();
					if (tag.compareTo(startTag) != 0) {
						System.out.println("Start tag '" + startTag + "' and endTag '"
								+ tag + "' do not match");
						printFollowing(1000);
						System.exit(1);
					}
				}
			}
			writeChar();
		}

	}

	private boolean parseTag(CharBuffer buf) throws IOException {
		boolean endTag = false;
		if (ch == '/') {
			endTag = true;
			writeChar();
			nextChar();
		}
		buf.clear();
		while (isIdentifier(ch) && !eof) {
			buf.put(ch);
			writeChar();
			nextChar();
		}
		return endTag;
	}

	private boolean isIdentifier(char ch) {
		return ('a' <= ch && ch <= 'z'
			 || 'A' <= ch && ch <= 'Z'
			 || ch == '_'
			 || '0' <= ch && ch <= '9');
	}


	private void printFollowing(int n) throws IOException {
		System.out.println();
		System.out.println("The succeeding text is: ");
		while (!eof && n-- > 0) {
			System.out.print(ch);
			nextChar();
		}
	}



	static public void main(String[] args) throws Exception {
//		String fileName = "/aramis/Data/Source/tmp.xml";
//		File file = new File(fileName);
//		long size = file.length();
//		BufferedReader br = new BufferedReader(new InputStreamReader(
//				/*new GZIPInputStream(*/new FileInputStream(file)/*)*/));
//		Writer w = new BufferedWriter(new FileWriter(fileName.substring(0, fileName.length() - 4) + "2.xml"));
//		String line = br.readLine();
//
//		while (!line.startsWith("]>")) {
//			w.write(line + "\r\n");
//			line = br.readLine();
//		}
//		w.write(line + "\r\n");
//
//
//		XmlCleaner cleaner = new XmlCleaner(br, w, size);
//		cleaner.clean();
//		w.flush();
//		w.close();

//		cleanFedReg();

//		cleanDblp();

		cleanNASA();
	}


	/*+**********************************************************************
	 * SOME SPECIFIC CLEANING FUNCTIONS
	 ************************************************************************/

	static public void cleanFedReg() throws IOException {
		String fileName = "/aramis/Data/Source/CongressRec.xml";
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		HtmlEntities entities = new HtmlEntities("./entities.txt");
		String line;
		// Temporary:
//		HashMap entities = new HashMap();
//		MutableInteger m;
//		HashMap.Entry entry;

		line = br.readLine();
		System.out.println(line);
		System.out.println();
		System.out.println(entities.toXml("Congress_Record"));
//		System.out.println("<!DOCTYPE Federal_Register [");
//System.out.println("  <!ENTITY reg \"(TM)\">");
//System.out.println("  <!ENTITY cir \"o\">");
//System.out.println("  <!ENTITY rsquo \"'\">");
//System.out.println("  <!ENTITY ncirc \"n\">");
//System.out.println("  <!ENTITY ge \"&gt;=\">");
//System.out.println("  <!ENTITY uuml \"ue\">");
//System.out.println("  <!ENTITY para \"\n\n\">");
//System.out.println("  <!ENTITY cacute \"c\">");
//System.out.println("  <!ENTITY ccedil \"c\">");
//System.out.println("  <!ENTITY utilde \"u\">");
//System.out.println("  <!ENTITY sect \"Sect. \">");
//System.out.println("  <!ENTITY ouml \"oe\">");
//System.out.println("  <!ENTITY cent \"Cent\">");
//System.out.println("  <!ENTITY mu \"micro-\">");
//System.out.println("  <!ENTITY hyph \"-\">");
//System.out.println("  <!ENTITY ntilde \"n\">");
//System.out.println("  <!ENTITY acirc \"o\">");
//System.out.println("  <!ENTITY euml \"ue\">");
//System.out.println("  <!ENTITY ugrave \"u\">");
//System.out.println("  <!ENTITY auml \"ae\">");
//System.out.println("  <!ENTITY bull \"o \">");
//System.out.println("  <!ENTITY atilde \"a\">");
//System.out.println("  <!ENTITY amp \"\">");
//System.out.println("  <!ENTITY ocirc \"o\">");
//System.out.println("  <!ENTITY times \"*\">");
//System.out.println("  <!ENTITY Ccedil \"C\">");
//
//System.out.println("  <!ENTITY Ouml \"Oe\">");
//System.out.println("  <!ENTITY lt \"\">");
//System.out.println("  <!ENTITY blank \" \">");
//System.out.println("  <!ENTITY Kuml \"K\">");
//System.out.println("  <!ENTITY Iuml \"I\">");
//System.out.println("  <!ENTITY gt \"\">");
//System.out.println("  <!ENTITY Euml \"Eu\">");
//
//System.out.println("  <!ENTITY eacute \"e\">");
//System.out.println("  <!ENTITY nacute \"n\">");
//System.out.println("  <!ENTITY aacute \"a\">");
//System.out.println("  <!ENTITY uacute \"u\">");
//System.out.println("  <!ENTITY sacute \"s\">");
//System.out.println("  <!ENTITY racute \"r\">");
//System.out.println("  <!ENTITY pacute \"p\">");
//System.out.println("  <!ENTITY oacute \"o\">");
//System.out.println("  <!ENTITY iacute \"i\">");
//System.out.println("  <!ENTITY lacute \"l\">");
//
//System.out.println("  <!ENTITY Gacute \"G\">");
//System.out.println("  <!ENTITY Eacute \"E\">");
//
//
//System.out.println("  <!ENTITY agrave \"a\">");
//System.out.println("  <!ENTITY egrave \"e\">");
//System.out.println("  <!ENTITY ograve \"o\">");
//System.out.println("  <!ENTITY Agrave \"A\">");
//System.out.println("  <!ENTITY Egrave \"E\">");
//		System.out.println("]>");
		System.out.println();
		while ((line = br.readLine()) != null) {
			if (notEmpty(line)) {
				System.out.println(line);
//				int i = line.indexOf('&');
//				int j = line.indexOf(';', i);
//				if (i > 0 && j > 0) {
//					line = line.substring(i, j + 1);
//					m = (MutableInteger) entities.get(line);
//					if (m != null) {
//						m.inc();
//					} else {
//						entities.put(line, new MutableInteger(1));
//					}
//
//				}
			}
		}
//		Iterator it = entities.entrySet().iterator();
//		while (it.hasNext()) {
//			entry = (HashMap.Entry) it.next();
//			System.out.println(entry.getKey() + "\t- " + entry.getValue());
//		}
	}

	static private boolean notEmpty(String line) {
		if (line.length() == 0) return false;
		int pos = 0;
		char ch;

		do {
			ch = line.charAt(pos++);
		} while (pos < line.length()
				 && ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r');
		return (ch != ' ' && ch != '\t' || ch != '\n' || ch != '\r');
	}


	static public void cleanDblp() throws IOException {
		String fileName = "/aramis/Data/Source/ProtSeqDB.xml";
		BufferedReader br = new BufferedReader(new FileReader(fileName));
//		HtmlEntities entities = new HtmlEntities("./entities.txt");
		String line;

		line = br.readLine();
		System.out.println(line);
		line = br.readLine();
		System.out.println("<!-- " + line + " -->");
//		System.out.println(entities.toXml("dblp"));
		System.out.println();
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}
		br.close();
	}


	static public void cleanNASA() throws IOException {
		String fileName = "/aramis/Data/Source/NASA.xml";
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		BufferedWriter bw = new BufferedWriter(new FileWriter(
				"/aramis/Data/Source/NASA2.xml"));
		String line;
		int c;
		char ch;

		while ((c = br.read()) >= 0) {
			ch = (char) c;
			if (ch > 127) {
				System.out.println(c + "\t- '" + ch + "'");
				switch (c) {
					case 0x9C:
						bw.write((int) ' '); // Ctrl !?
						break;
					case 0xA0:
						bw.write((int) ' ');
						break;
					case 0xA1:
						bw.write((int) '!');
						break;
					case 0xB4:
						bw.write((int) '\'');
						break;
					case 0xB9:
						bw.write((int) '1');
						break;
					case 0xC2:
						bw.write((int) 'A');
						break;
					case 0xC6:
						bw.write((int) 'A');
						bw.write((int) 'E');
						break;
					case 0xF2:
						bw.write((int) 'o');
						break;
				}
			} else {
				bw.write(c);
			}
		}
		bw.flush();
		bw.close();
	}


	static public void cleanProtSeqDB() throws IOException {
		String fileName = "/aramis/Data/Source/ProtSeqDB.xml";
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		BufferedWriter bw = new BufferedWriter(new FileWriter(
				"/aramis/Data/Source/NASA2.xml"));
		String line;
		int c;
		char ch;

	}

}