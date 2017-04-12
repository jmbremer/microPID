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
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Test extends MyPrint {
	static public final String DATA_HOME = "/aramis/Data";
	static public final String REP_HOME = "/F/Data/Source/XMark1Gb";


	static public void main(String[] args) throws Exception {
//		uncompressAndListFiles();
//		uncompressHuffFile();

		pureDecode2();
//		pureFastDecode();
//		decodeWithPositions();

//        testBitInpStream();
//		useDataGuide();
//		test64BitArithmetic();
//        concatShakespeare();
//        printStats("XMLmedium");
//		testNewIO();
//		testHashCodes();

//		testLargeFiles();

//		extractStopWords();

//		printTermStats();

//		testSystemProperties();
	}

	static public void p(String s) {
		System.out.print(s);
	}
	static public void pl(String s) {
		System.out.println(s);
	}
	static public void pl() {
		System.out.println();
	}

	static public void test64BitArithmetic() {
		long x,y;

		x = (long) Integer.MAX_VALUE;
		System.out.println("x = " + x);
		x = x << 1;
		System.out.println("x = " + x);
		x = x << 1;
		System.out.println("x = " + x);
		x = Long.MAX_VALUE >>> 1;
		System.out.println("x = " + x + " (" + Long.MAX_VALUE + " = " + (x * 2)
			+ ")");
	}

	static public void compareStringAndmutableString() throws Exception {
		MutableString m = new MutableString("Test2");
		String s = "Test2";

		System.out.println(m.compareTo(s));
		System.out.println(m.compareTo("Test2"));
		System.out.println("M hashCode = " + m.hashCode() + ",  s hashCode = "
			+ s.hashCode());

		MutableInteger[] ints = new MutableInteger[20];
		MutableInteger mi = new MutableInteger(13);
		for (int i = 0; i < ints.length; i++) {
			System.out.println(ints[i]);
//			ints[i] = mi.clone();   doesn't work like that...
		}
		for (int i = 0; i < ints.length; i++) {
			System.out.println(ints[i]);
		}
	}

	static public final String HEADER =
		"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
		"\n" +
		"<FinancialTimes>\n";

	static public final String FOOTER =
		"</FinancialTimes>\n";

	static public void uncompressAndListFiles() throws Exception {
		/*
		FileInputStream fis = new FileInputStream(
				"F:/Data/Financial_Times/FT911/FT911_1.Z");
		UncompressInputStream uis = new UncompressInputStream(fis);
		BufferedReader reader = new BufferedReader(new InputStreamReader(uis));
		String line;
		int lineNo = 0;

		// Print some input lines
		while ((line = reader.readLine()) != null && ++lineNo <= 100) {
			System.out.println(line);
	}
		reader.close();
		// Search for all file in a certain directory
		System.out.println("All directories in directory 'D/FT' that match "
			+ "'FT9*'...");
		File ftDir = new File("D:/FT");
		FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if (name.regionMatches(true, 0, "FT9", 0, 3)) {
//							&& name.charAt(name.length() - 2) == '.'
//							&& name.charAt(name.length() - 1) == 'Z') {
						return true;
					} else {
						return false;
					}
				}
			};
		String[] fileNames = ftDir.list(filter);
		for (int i = 0; i < fileNames.length; i++) {
			System.out.println(fileNames[i]);
		}
		System.out.println("All files in 'D:/FT'...");
		*/
		FileMatcher matcher =
			new FileMatcher("D:/FT", new TrecInputStream.FinancialTimesFilter(),
					new TrecInputStream.FinancialTimesFilter());
		ByteArrayInputStream header = new ByteArrayInputStream(HEADER.getBytes());
		ByteArrayInputStream footer = new ByteArrayInputStream(FOOTER.getBytes());
		SequenceInputStream body = new SequenceInputStream(matcher);
		SequenceInputStream headIs = new SequenceInputStream(header, body);
		SequenceInputStream complete = new SequenceInputStream(headIs, footer);

		InputStream in = TrecInputStream.create("D:/FT", "FinancialTimes",
				TrecInputStream.LA_TIMES);
//		InputStream is2 = new FileInputStream("F:/Temp/FT922_11.xml");
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		int lineCount = 0;
		while ((line = reader.readLine()) != null /*&& (lineCount < 1281450)*/) {
			++lineCount;
			if (lineCount % 1000000 == 0) {
				System.err.println("Line " + lineCount + "...");
			}
			System.out.println(line);
//			if (Math.abs(lineCount - 1281349) < 10) {
//				System.err.println(line);
//			}
		}

//		TrecInputStream inn = new TrecInputStream("D:/FT", "FinancialTimes",
//				new TrecInputStream.FinancialTimesFilter());

//		File file = matcher.getFirstFile();
//		while (file != null) {
//			System.out.println(file);
//			file = matcher.getNextFile();
//		}
	}


	static public void uncompressHuffFile() throws IOException {
		String REP_DIR = "F:/Data/LA Times";
		String FILE_NAME = "F:/Data/LA Times/xml.data";
		Tokens tokens = new Tokens();
		Token token;

		// Create a stream for the input
		DataInputStream bis = new DataInputStream(new BufferedInputStream(
				new FileInputStream(FILE_NAME)));
		BitDataInput is = new BitDataInput(bis);
		// Load tokens and construct huffman coder
		System.out.print("Loading tokens...");
		IndexSeqFile tokenFile = new IndexSeqFile(REP_DIR + "/tokens");
		tokens.load(tokenFile);
		System.out.println("done.");
		HuffmanCoder hc = new HuffmanCoder(tokens);

		System.out.println("Getting some tokens' code...");
		printCode(tokens, hc, "<LA_TIMES");
		printCode(tokens, hc, "<DOC");
		printCode(tokens, hc, "<DOCNO");
		printCode(tokens, hc, "la010189");
		printCode(tokens, hc, "-");
		printCode(tokens, hc, "0001");
		printCode(tokens, hc, "<DOCID");
//		System.exit(0);


		// Fetch decode data...
		for (int i = 0; i < 20; i++) {
//			if (i % 32 == 0) System.out.println();
//			else if (i % 8 == 0) System.out.print(' ');
//			System.out.print(Integer.toBinaryString(is.read()));
			token = (Token) hc.decode(is);
//
			System.out.print("   '" + token.getName() + "'");
		}
	}

	static private void printCode(Tokens tokens, HuffmanCoder hc, String name) {
		// Look up token for given token name
		MutableInteger no = new MutableInteger();
		Token t = tokens.get(name, no);
		System.out.print("Token for '" + name + "' is ");
		if (t != null) {
			int code = hc.encode(t, no.getValue());
			System.out.println(t + " [number " + no + "],   code is 0x"
					+ Integer.toHexString(code) + "="
					+ Integer.toBinaryString(code));
			System.out.println("\t" + hc.getStartCode(t.huffCodeLen));

		} else {
			System.out.println("unknown");
		}
	}

	static private void pureDecode() throws IOException {
//		String REP_DIR = "/home/bremer/Data/XMLmini";
		String FILE_NAME = REP_HOME + "/xml.data";
		Tokens tokens = new Tokens();
		Token token = null, nextToken;
		String name, nextName;
		StringBuffer strBuf = new StringBuffer(256);
		String str;

		// Create a stream for the input
		DataInputStream bis = new DataInputStream(new BufferedInputStream(
				new FileInputStream(FILE_NAME)));
		BitDataInput is = new BitDataInput(bis);
		// Load tokens and construct huffman coder
//		System.out.print("Loading tokens...");
		IndexSeqFile tokenFile = new IndexSeqFile(REP_HOME + "/tokens", false);
		tokens.load(tokenFile);
//		System.out.println("done.");
//		System.out.print("Generating terms...");
//		IndexSeqFile termFile = new IndexSeqFile(REP_DIR + "/terms",
//				LogStream.DEFAULT_STREAM);
//		Terms terms = new Terms(tokens);
//		System.out.println("done.");

		/*
		HuffmanCoder hc = new HuffmanCoder(tokens);
		MutableInteger mi = new MutableInteger();
		tokens.get("<HAPPINESS", mi);
		int i = mi.getValue();
		System.out.println("Token " + i + ":\t " + tokens.get(i) + ",  "
				+ "code = " + hc.encode(tokens.get(i), i));
		tokens.get("4", mi);
		i = mi.getValue();
		System.out.println("Token " + i + ":\t " + tokens.get(mi.getValue()) + ",  "
				+ "code = " + hc.encode(tokens.get(i), i));
		for (i = 1; i < 100; i++) {
			System.out.println("Token " + i + ":\t " + tokens.get(i) + ",  "
				+ "code = " + hc.encode(tokens.get(i), i));
		}
		for (i = 1; i < 100; i++) {
			System.out.println("Term " + i + ":\t '" + terms.get(i) + "'");
		}
		*/


		XmlDecoderStream xds = new XmlDecoderStream(is, tokens);
		int count = 0;
//		while (/*(token = xds.nextToken()) != null &&*/ count++ < 200) {
//			token = xds.nextToken();
//			System.out.println(count + ")\t'" + token + "'");
//		}
		while ((str = xds.read()) != null /*&& count++ < 200*/) {
			System.out.print(str);
//			System.out.flush();
		}

	}


	static private void pureDecode2() throws IOException {
		String FILE_NAME = REP_HOME + "/xml.data";
		Tokens tokens = Tokens.load(REP_HOME + "/tokens", false);
		Token token = null, nextToken;
		String str;
		long pos;

		// Create a stream for the input
//		DataInputStream bis = new DataInputStream(new BufferedInputStream(
//				new FileInputStream(FILE_NAME)));
//		BitDataInput is = new BitDataInput(bis);
		BitDataChannel ch = new BitDataChannel(RandomAccessChannel.create(
				REP_HOME + "/xml.data"));

		XmlDecoderStream xds = new XmlDecoderStream(ch, tokens);
		int count = 0;
//		while (/*(token = xds.nextToken()) != null &&*/ count++ < 200) {
//			token = xds.nextToken();
//			System.out.println(count + ")\t'" + token + "'");
//		}
		pos = ch.bitPosition();
		while ((str = xds.read()) != null /*&& count++ < 200*/) {
			System.out.print(str);
//			if (str.charAt(0) == '<' && str.length() > 1
//				&& str.charAt(1) != '/') {
//				System.out.print("[" + pos + "]");
//			}
//			pos = ch.bitPosition();
		}
	}


	static private void pureFastDecode() throws IOException {
		BitDataInput is = new BitDataInput(new DataInputStream(
				new BufferedInputStream(
				new FileInputStream(REP_HOME + "/xml.data"))));
		Decoder decoder = Decoder.load(REP_HOME + "/decoder.data");
		XmlDecoderStream xds = new XmlDecoderStream(is, decoder);
		Terms terms = Terms.load(REP_HOME + "/terms");
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		GuideNode node;
		int code;

		while ((code = xds.nextCode()) != Decoder.EOF) {
//			/*
			if (Decoder.isNode(code)) {
				node = guide.getNode(- code);
				if (!node.isAttrib()) {
					System.out.print("\n<" + node.getName() + ">");
				} else {
					System.out.print(" @" + node.getName());
				}
			} else if (code > 0) {
				System.out.print(" " + terms.get(code).getName());
			} else if (code == Decoder.ELEMENT_END) {
				System.out.print("</>");
			}
//			*/
//			System.out.print(code + "  ");
		}
	}


	static private void decodeWithPositions() throws IOException {
		System.out.println();
		System.out.println();
		BitDataChannel ch = new BitDataChannel(RandomAccessChannel.create(
				REP_HOME + "/xml.data"));
		Decoder decoder = Decoder.load(REP_HOME + "/decoder.data");
		XmlDecoderStream xds = new XmlDecoderStream(ch, decoder);
		Terms terms = Terms.load(REP_HOME + "/terms");
		Tokens tokens = Tokens.load(REP_HOME + "/tokens", false);
		XmlDecoderStream xds2 = new XmlDecoderStream(ch, tokens);
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");
		StringBuffer buf = new StringBuffer(4096);
		GuideNode node;
		int code;
		long pos = ch.bitPosition();

//		while ((code = xds.nextCode()) != Decoder.EOF) {
//			if (Decoder.isNode(code)) {
//				node = guide.getNode(- code);
//				if (!node.isAttrib()) {
//					System.out.print("\n<" + node.getName() + "(" + pos + ")>");
//				} else {
//					System.out.print(" @" + node.getName() + "(" + pos + ")");
//				}
//			} else if (code > 0) {
//				System.out.print(" " + terms.get(code).getName());
//			} else if (code == Decoder.ELEMENT_END) {
//				System.out.print("</>");
//			}
//			pos = ch.position();
//		}

//		System.out.println("\n______________________________________________");
//		System.out.println("Jumping to the middle of the XML source...");
//		pos = 2672;
//		ch.position(pos);
//		while ((code = xds.nextCode()) != Decoder.EOF) {
//			if (Decoder.isNode(code)) {
//				node = guide.getNode(- code);
//				if (!node.isAttrib()) {
//					System.out.print("\n<" + node.getName() + "(" + pos + ")>");
//				} else {
//					System.out.print(" @" + node.getName() + "(" + pos + ")");
//				}
//			} else if (code > 0) {
//				System.out.print(" " + terms.get(code).getName());
//			} else if (code == Decoder.ELEMENT_END) {
//				System.out.print("</>");
//			}
//			pos = ch.position();
//		}


		System.out.println("\n______________________________________________");
		pos = 0;
		System.out.println("The fragment starting at position " + pos + " is...\n");
		ch.bitPosition(pos);
		xds2.readFragment(null, buf);
		System.out.println(buf);

		System.out.println("\n______________________________________________");
		pos = 2972;
		System.out.println("The fragment starting at position " + pos + " is...\n");
		ch.bitPosition(pos);
		buf.setLength(0);
		xds2.readFragment(null, buf);
		System.out.println(buf);

	}


	static public void testBitInpStream() throws IOException {
		String REP_DIR = "/F/Data/XMLmini";
		String FILE_NAME = REP_DIR + "/xml.data";
		DataInputStream bis = new DataInputStream(new BufferedInputStream(
				new FileInputStream(FILE_NAME)));
		BitDataInput is = new BitDataInput(bis);
		int bitCount = 0;
		try {
			while (true) {
				int bit = is.read();
				bitCount++;
				if (bitCount % 8 == 0) {
					System.out.print(" ");
				}
				System.out.print(bit);
			}
		} catch (EOFException e) {
			System.out.print("Bit stream seems to be empty.");
		}
	}


	static public void useDataGuide() throws Exception {
//		final String REP_DIR= DATA_HOME + "/XMLmini";
		DataGuide guide = DataGuide.load(REP_HOME + "/guide.tree");

		System.out.println("Data guide is:\n");
		ListFormat.prettyPrint(guide.toString());
		System.out.println("\nAncestor relationships:");
		int nodeCount = guide.countNodes();
		int oldX = -1;
		for (int x = 0; x <= nodeCount; x++) {
			for (int y = 0; y <= nodeCount; y++) {
				if (guide.isAncestor(x, y)) {
					if (oldX < x) {
						oldX = x;
						System.out.print("\n" + guide.getNode(x).getName() + ":  ");
					}
					System.out.print("(" + x + "," + y + "); ");
				}
			}
		}
	}


	static public void concatShakespeare() throws IOException {
		final String REP_DIR = "/home/bremer/Data/Shakespeare";
		File dir = new File(REP_DIR);
		File[] files = dir.listFiles();
		BufferedReader bis;
		PrintWriter pw = new PrintWriter(new BufferedWriter(
				new FileWriter(REP_DIR + "/../shake.xml")));
		String line;

		pw.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
				"\n" +
				"<SHAKESPEARE>\n");
		for (int i = 0; i < files.length; i++) {
			bis = new BufferedReader(new FileReader(files[i]));
			line = bis.readLine();
			line = bis.readLine();
			line = bis.readLine();
			while((line = bis.readLine()) != null) {
				pw.write(line);
				pw.println();
			}
		}
		pw.write("</SHAKESPEARE>");
		pw.close();
	}


	static public void printStats(String repName) throws IOException {
		if (!(repName.charAt(0) == '/')) {
			repName = "/" + repName;
		}
		String repDir = DATA_HOME + repName;

		// Token statistics
		Tokens tokens = new Tokens();
		System.out.print("Loading tokens...");
		IndexSeqFile tokenFile = new IndexSeqFile(repDir + "/tokens");
		tokens.load(tokenFile);
		int uniqueTokens = tokens.getUniqueCount();
		int totalTokens = tokens.getTotalCount();
		System.out.println("done.");

		// Term statistics
		Terms terms = new Terms();
		System.out.print("Loading terms...");
		tokenFile = new IndexSeqFile(repDir + "/terms");
		terms.load(tokenFile);
		int uniqueTerms = terms.getTermCount();
		int totalTerms = terms.getWordCount();
		System.out.println("done.");

		// Guide statistics
		DataGuide guide = DataGuide.load(repDir + "/guide.tree");

		// Print results
		pl();
		pl("Unique tokens......" + uniqueTokens);
		pl("Total tokens......." + totalTokens);
		pl("Unique terms......." + uniqueTerms);
		pl("Total words........" + totalTerms);
		pl("Max. pid length...." + guide.computeMaxPidLength());
		pl("Max. depth........." + guide.getMaxDepth());
		pl("Node# bits........." + guide.getNodeNoBits());
		pl("# of nodes........." + guide.countNodes());
		pl("Node names........." + guide.countUniqueNames(null));
	}


	static public void testNewIO() {
		ByteBuffer bbuf = ByteBuffer.allocate(50);
		CharBuffer cbuf = bbuf.asCharBuffer();

		bbuf.put((byte) 0);
		bbuf.put((byte) 65);
		bbuf.put((byte) 0);
		bbuf.put((byte) 66);
		bbuf.put((byte) 0);
		bbuf.put((byte) 67);
		bbuf.put((byte) 0);
		bbuf.put((byte) 68);
		bbuf.put((byte) 0);
		bbuf.put((byte) 69);
		pl(cbuf.toString());
		bbuf.put(3, (byte) 77);
		pl(cbuf.toString());

		pl("----------------------");
//		cbuf = CharBuffer.allocate(50);
		cbuf.position(6);
		cbuf.put("|Another one|");
		cbuf.flip();
		pl(cbuf.toString());

		Charset utf8 = Charset.forName("UTF8");
		CharsetEncoder encoder = utf8.newEncoder();
		CharsetDecoder decoder = utf8.newDecoder();
		ByteBuffer bbuf2 = ByteBuffer.allocate(10);
		CoderResult cr;

		do {
			cr = encoder.encode(cbuf, bbuf2, true);
			pl("String encoded into " + bbuf2.position() + " bytes (coder result="
			   + cr + ")");
			bbuf2.clear();
		} while (cr == cr.OVERFLOW);
	}


	static public void testHashCodes() {
		String TEXT = "A little test String";
		MutableString mstr = new MutableString(TEXT);
		CharBuffer cbuf = CharBuffer.allocate(40);

		cbuf.put(TEXT);
		pl("___HASH CODES___");
		pl("TEXT hash......... " + TEXT.hashCode());
		pl("Mutable string.... " + mstr.hashCode());
		pl("Char buffer....... " + cbuf.hashCode());
		pl("------------------------------------------");

		byte b = (byte) 255;
		int i = (b & 0xFF);
		pl("b = " + b);
		pl("i = " + i);
	}


	static public void testLargeFiles() throws Exception {
		BufferedOutputChannel ch =
				BufferedOutputChannel.create("/tmp/large.file", 2097152); // 2 MB
		MutableLong l = new MutableLong(42);
		long i = -1;
		PercentPrinter p = new PercentPrinter(536870912);

		try {
			for (i = 0; i < 536870912; i++) {
				l.store(ch);
				p.notify(i);
			}
			// Now we should have written exactly 4GB
			// Let's try some more...
			l.store(ch);
			l.store(ch);
			l.store(ch);
			l.store(ch);
			ch.flush();
			ch.close();
		} catch (Exception e) {
			ple("Problems writing more data at about " + ((i * 8) >>> 20 )
				 + " MB");
			e.printStackTrace();
		}

	}


	static public void extractStopWords() throws IOException {
		String inFileName = "/home/bremer/Data/Big10-stopwords.txt";
		String outFileName = "/home/bremer/Data/Big10-sw.txt";
		BufferedReader in = new BufferedReader(new FileReader(inFileName));
		BufferedWriter out = new BufferedWriter(new FileWriter(outFileName));
		String line;
		int pos;

		while ((line = in.readLine()) != null) {
			pos = line.lastIndexOf('\t');
			System.out.println(line.substring(pos + 1));
		}
	}



	static public void printTermStats() throws IOException {
		final int INDEX_SIZE = 120554496;
		final String srcId = "SwissProt";
		String swFileName = "/home/bremer/Data/English stopwords.txt";
		String termFileName = "/aramis/Data/Source/" + srcId + "/terms";
		String termFileBaseName = "/aramis/Data/Index/" + srcId + "/termdf-pp";
		String mapFileName = "/aramis/Data/Index/" + srcId + "/termdf-pp.map";
		String guideFileName = "/aramis/Data/Source/" + srcId + "/guide.tree";
		DataGuide guide = DataGuide.load(guideFileName);
		BufferedInputChannel bic = BufferedInputChannel.create(mapFileName);
		TermNodeMap map = new TermNodeMap();
		map.load(bic);
		StopWords sw = new StopWords(swFileName);
		Terms terms = Terms.load(termFileName);
		int termCount = map.getTermCount();
		int nodeCount = guide.getNodeCount();
		IndexChannel index = new IndexChannel(termFileBaseName + ".index",
				new MutableLong());
		int recCount = index.getRecordCount();
		long indexEnd = 8 * recCount + 24;
		MutableLong addr = new MutableLong(), next = new MutableLong();
		int tno = 1;
		int recNo;
		long sum = 0;
		String term;
		int swCount = 0;

		System.out.println("# of stop words\tTerm\tRemainig size\tPercent");
		System.out.println("0\t" + INDEX_SIZE + "\t100");
		for (tno = 1; tno <= termCount; tno++) {
			term = terms.get(tno).getName();
			if (!sw.isStopWord(term)) {
//				System.out.println(tno + ")\t " + term + "\t <not a stop word>");
			} else {
				swCount++;
				for (int dno = 1; dno <= nodeCount; dno++) {
					if ((recNo = map.getMapping(tno, dno)) > 0) {
						// Get this address...
						index.get(recNo, addr);
						// ..and the next one...
						if (recNo < recCount) {
							index.get(recNo + 1, next);
						} else {
							next.setValue(indexEnd);
						}
						sum += next.getValue() - addr.getValue() + 8;
					}
				}
//				System.out.println(tno + ")\t " + term + "\t " + memString(sum)
//									+ "  (" + swCount + " stop words so far)");
				System.out.println(swCount + "\t" + (INDEX_SIZE - sum)
								   + " (" + ((INDEX_SIZE - sum) / 1048576) + ")\t\t"
					+ (Math.round((INDEX_SIZE - sum) / (double) INDEX_SIZE * 10000)
									/ (double) 100) + "\t" + term);
			}
		}

		System.out.println("There where " + swCount + " of the "
							+ sw.getTermCount() + " known stop words found in "
							+ "this text");
		System.out.println(sum + " bytes = " + (sum / 1024) + " Kb = "
						   + (sum / 1048576) + " Mb  could be saved by "
						   + "leaving out the selected term(s)");
	}

	static private String memString(long sum) {
		return (sum + " bytes = " + (sum / 1024) + " Kb = "
						   + (sum / 1048576) + " Mb ");
	}



	static void testSystemProperties() {
		Properties p = System.getProperties();
		Enumeration enume = p.propertyNames();

		while (enume.hasMoreElements()) {
			System.out.println(enume.nextElement());
		}
		System.out.println(p.getProperty("PATH"));
	}
}