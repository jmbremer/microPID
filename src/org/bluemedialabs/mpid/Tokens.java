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
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.table.TableModel;
import org.bluemedialabs.io.IndexSeqFile;
import org.bluemedialabs.io.IndexSeqOutputChannel;
import org.bluemedialabs.io.Storable;
import org.bluemedialabs.util.*;
// For main()
import org.bluemedialabs.io.LogStream;
import org.bluemedialabs.util.StopWatch;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Tokens implements Codeables {
	static public final int INITIAL_CAPACITY = 10000;
	private HashMap tokenMap = null;    // Maps term strings to MutableIntegers
	private Token[] tokens;        // Contains the actual term data
	private int uniqueCount = 0;
	private int totalCount = 0;
	private boolean sorted = false;

	/**
	 * Flag indicating whether hashing from token names to token numbers is
	 * required. If hashing is not required, a lot of storage space can be
	 * saved.
	 */
	private boolean hashing = false;


	/*+**********************************************************************
	 * Class functions
	 ************************************************************************/

	static public Tokens load(String baseFileName, boolean hash) throws IOException {
		System.out.print("Loading tokens from base file '" + baseFileName
				+ "'...");
		IndexSeqFile tokenFile = new IndexSeqFile(baseFileName, false);
		Tokens tokens = new Tokens();
		tokens.setHashing(hash);
		tokens.load(tokenFile);
		tokenFile.close();
		System.out.print(tokens.getUniqueCount() + "...");
		System.out.println("done.");
		return tokens;
	}

	static public Tokens load(Configuration config, String id)
			throws IOException {
		return load(config.getProperty(id, "SourceHome") + "/"
					+ config.getProperty(id, "TokenFileBaseName"), false);
	}

	static public Tokens load(Configuration config, String id, boolean hash)
			throws IOException {
		return load(config.getProperty(id, "SourceHome") + "/"
					+ config.getProperty(id, "TokenFileBaseName"), hash);
	}


	/*+**********************************************************************
	 * Object functions
	 ************************************************************************/

	public Tokens() {
		tokens = new Token[INITIAL_CAPACITY + 1];
		tokens[0] = new Token("<dummy>", -1);
		tokenMap = new HashMap((int) (INITIAL_CAPACITY / 0.7));  // should we do this here?
	}

	/**
	 * Add and increment counter.
	 */
	public int addAndCount(MutableString name) {
		MutableInteger tokenNo = (MutableInteger) tokenMap.get(name);
		int no;

		if (tokenNo == null) {
			no = lookUpOrCreateTerm(tokenNo, name.toString().intern());
		} else {
			// name is not needed, so avoid String conversion
			no = lookUpOrCreateTerm(tokenNo, null);
		}
		Token token = tokens[no];
		token.incCount();
		totalCount++;
		return no;
	}

	public int addAndCount(String name) {
		MutableInteger tokenNo = (MutableInteger) tokenMap.get(name);
		int no = lookUpOrCreateTerm(tokenNo, name);
		Token token = tokens[no];
		token.incCount();
		totalCount++;
		return no;
	}

	private int lookUpOrCreateTerm(MutableInteger tokenNo, String name) {
//		MutableInteger tokenNo = (MutableInteger) tokenMap.get(name);
		String str;
//		try {
		if (tokenNo == null) {
			// First time we see this token
//			System.out.println("New term '" + name + "'");
//			termNo = new MutableInteger(++termCount);       !?!???
			tokenNo = new MutableInteger(++uniqueCount);
//			str = name.toString().intern();
			Token token = new Token(name, 0);
			// Check term array capacity
			if (tokenNo.getValue() >= tokens.length) {
				// need to increase capacity
				increaseCapacity();
			}
			tokens[tokenNo.getValue()] = token;
			tokenMap.put(name, tokenNo);
		}
//		} catch (ArrayIndexOutOfBoundsException e) {
//			System.out.println("terms.length=" + terms.length + ", termNo."
//				+ "getValue()=" + termNo.getValue());
//		}
		return tokenNo.getValue();
	}

	private void increaseCapacity() {
//		System.out.println();
//		System.out.println("Increasing token storage capacity from "
//			+ (tokens.length - 1) + " to " + (tokens.length - 1) * 2 + "...");
		Token[] t = new Token[(tokens.length - 1) * 2 + 1];
		System.arraycopy(tokens, 0, t, 0, tokens.length);
		tokens = t;
	}

	/**
	 * Just add.
	 */
	protected void add(MutableString name) {
		MutableInteger tokenNo = (MutableInteger) tokenMap.get(name);
		// just make sure the term gets into our hash
		lookUpOrCreateTerm(tokenNo, name.toString());
	}


	public void setHashing(boolean hash) { hashing = hash; }
	public boolean isHashing() { return hashing; }


	public int getUniqueCount() {
		return uniqueCount;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public Token get(int no) {
		return tokens[no];
	}

	public Codeable getCodeable(int no) {
		return get(no);
	}

	public Token get(String name, MutableInteger no) {
		MutableInteger tokenNo = (MutableInteger) tokenMap.get(name);
		if (tokenNo == null) {
			// don't know this token
			return null;
		} else {
			no.setValue(tokenNo.getValue());
			return tokens[tokenNo.getValue()];
		}
	}

	public Token get(MutableString name, MutableInteger no) {
		MutableInteger tokenNo = (MutableInteger) tokenMap.get(name);
		if (tokenNo == null) {
			// don't know this token
			return null;
		} else {
			no.setValue(tokenNo.getValue());
			return tokens[tokenNo.getValue()];
		}
	}

	public int getNo(String name) {
		MutableInteger tokenNo = (MutableInteger) tokenMap.get(name);
		if (tokenNo == null) {
			return 0;
		} else {
			return tokenNo.getValue();
		}
	}
	public int getNo(MutableString name) {
		MutableInteger tokenNo = (MutableInteger) tokenMap.get(name);
		if (tokenNo == null) {
			return 0;
		} else {
			return tokenNo.getValue();
		}
	}


	/**
	 * ...dangerous because it changes term numbers!
	 */
	public void sort() {
		sort(Token.CODE_LENGTH_COMPARATOR);
		/*
		 * Here, the numbers have changed, so we need to recalculate them
		 * in the hash table, too.
		 */
		 for (int i = 1; i <= uniqueCount; i++) {
			Token token = tokens[i];
			MutableInteger m = (MutableInteger) tokenMap.get(token.getName());
			m.setValue(i);
		}
	}

	protected void sort(Comparator comparator) {
		if (uniqueCount < 1) return;
		// do some more sophisticated sorting sometime later...
		Arrays.sort(tokens, 1, uniqueCount + 1, comparator);
//		for (int i = 1; i <= tokenCount; i++) {
//			terms[i].setNo(i);
//			System.out.println(terms[i]);
//		}
	}

	public void printAll() {
		Iterator it = tokenMap.values().iterator();
		Token token;
		Object obj;

		System.out.println("Token store " + this + " contains the tokens:");
//		while (it.hasNext()) {
//			term = terms[((MutableInteger) it.next()).getValue()];
//			System.out.println(++i + ")  " + term);
//		}
		for (int i = 1; i <= uniqueCount; i++) {
			System.out.println(++i + ")  " + tokens[i]);
		}
	}


	public void printSortedByCount(int limit) {
		if (!sorted) {
			copySortPrint(Token.COUNT_COMPARATOR, limit);
		} else {
			for (int i = 1; i < Math.min(limit, uniqueCount); i++) {
				System.out.println(tokens[i]);
			}
		}
	}
	public void printSortedByCount() {
		printSortedByCount(uniqueCount);
	}

	private void copySortPrint(Comparator comp, int limit) {
		Token[] t = new Token[uniqueCount + 1];
		System.arraycopy(tokens, 1, t, 1, uniqueCount);
		Arrays.sort(t, 1, uniqueCount, comp);
		for (int i = 1; i <= Math.min(limit, uniqueCount); i++) {
			System.out.println(t[i]);
		}
	}

//	public void printSortedByNo(int limit) {
//		copySortPrint(Token.NUMBER_COMPARATOR, limit);
//	}
//	public void printSortedByNo() {
//		copySortPrint(Token.NUMBER_COMPARATOR, termCount);
//	}

	public void printLexiSorted(int limit) {
		copySortPrint(Token.LEXICAL_COMPARATOR, limit);
	}
	public void printLexiSorted() {
		copySortPrint(Token.LEXICAL_COMPARATOR, uniqueCount);
	}


	public String toString() {
		StringBuffer buf = new StringBuffer(100);

		buf.append("(uniqueCount=");
		buf.append(uniqueCount);
		buf.append(", totalCount=");
		buf.append(totalCount);
		buf.append(", tokens=(");
		if (uniqueCount > 0) {
			// Append the first 20 tokens
			buf.append(tokens[1]);
			int i = 1;
			while (++i < 20 && i < uniqueCount) {
				buf.append(", ");
				buf.append(tokens[i]);
			}
		}
		buf.append("))");
		return buf.toString();
	}


	/*+**********************************************************************
	 * Pseudo Storable implementation
	 ************************************************************************/

	 public void store(IndexSeqOutputChannel out) throws IOException {
		for (int i = 1; i <= uniqueCount; i++) {
//			Token t = tokens[i];
//			if (i == 35938 && tokens[i].getName().length() == 3
//				&& ((int) tokens[i].getName().charAt(1)) > 55600) {
//				tokens[i]
//				}
			out.write(tokens[i]);
//			if (i  35935 && i < 35940) {
//				System.out.println(i + ") " + tokens[i] + "  (char[0] = "
//								   + ((int) tokens[i].getName().charAt(0)));
//			}
		}
	}

	public void load(IndexSeqFile file) throws IOException {
		int recCount = file.getRecordCount();
		Token[] newTokens;

		if (tokens.length != recCount) {
			newTokens = new Token[recCount + 1];  // +1 is important!
			newTokens[0] = tokens[0];
			for (int i = 1; i <= Math.min(uniqueCount, recCount); i++) {
				// preserve space of  all already allocated terms
				newTokens[i] = tokens[i];
			}
		} else {
			newTokens = tokens;
		}
		if (uniqueCount < recCount) {
			// Need to allocate more tokens
			for (int i = uniqueCount + 1; i <= recCount; i++) {
				newTokens[i] = new Token();
			}
		} else {
			// Pseudo delete (might help a compiler) ;-)
			for (int i = recCount + 1; i <= uniqueCount; i++) {
				tokens[i] = null;
			}
		}
		tokens = newTokens;
		uniqueCount = recCount;
		totalCount = 0;
		// Now we have exactly enough space to load all the terms
		for (int i = 1; i <= uniqueCount; i++) {
			try {
				file.get(i, tokens[i]);
			} catch (UTFDataFormatException e) {
				System.out.println("Problems getting UTF name of token " + i + " ("
								   + tokens[i] + "), adjusting name...");
				String name = tokens[i].getName();
				byte codeLen = tokens[i].getCodeLen();
				StringBuffer buf;
				if (name != null) {
					buf = new StringBuffer(name.length());
					for (int j = 0; j < name.length(); j++) {
						char ch = name.charAt(j);
						if (((int) ch) > 127) {
							buf.insert(j, 'x');
						} else {
							buf.insert(j, ch);
						}
					}
					tokens[i] = new Token(buf.toString(), tokens[i].getCount());
				} else {
					tokens[i] = new Token("<UTF load error>", tokens[i].getCount());
				}
				tokens[i].setCodeLen(codeLen);
			}
			totalCount += tokens[i].getCount();
		}

		if (isHashing()) {
			// Don't forget to fill up the hash map as well:
			tokenMap = new HashMap((int) (uniqueCount / 0.7));
			// How can we actually determine and adjust the hash map's capacity
			// here to avoid having to re-create it?
			// (Furthermore, we should really reuse the mutable integers. On the
			//  other side, when are we ever going to reuse Tokens?)
			for (int i = 1; i <= uniqueCount; i++) {
				tokenMap.put(tokens[i].getName(), new MutableInteger(i));
			}
		} else {
			tokenMap = null;
		}
	}

	public int byteSize() {
		return -1;  // don't know
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		// Check validity of input
		if (args.length != 2) {
			printUsage();
			System.exit(1);
		}
		// Load configuration
		Configuration config = Configuration.load(args[1]);
//		dumpTokenFile(config, args[0]);
		dumpTokensAndTerms(config, args[0]);
		System.exit(0);

		StopWords sw = new StopWords("F:/Data/English stopwords.txt");
		Terms terms = new Terms();
		int termCount, stemCount, wordCount, stopWordCount;
		Term term;
		PorterStemmer stemmer = new PorterStemmer();
		String stem;
		HashMap stems;
		MutableInteger count;
		StopWatch watch = new StopWatch();

		watch.start();
		System.out.println("Loading terms... (time " + watch + ")");
		terms.load(new IndexSeqFile("F:/Data/tmp/terms"));
		termCount = terms.getTermCount();

		stems = new HashMap((int) (termCount / 0.7));
		System.out.println("Determining # of stems... (time " + watch + ")");
		stemCount = 0;
		for (int i = 1; i <= termCount; i++) {
			term = terms.get(i);
			stemmer.add(term.getName().toCharArray(), term.getName().length());
			stemmer.stem();
			stem = stemmer.toString();
			// Add or update stem in hash table
			count = (MutableInteger) stems.get(stem);
			if (count != null) {
				// have seen the stem before
				count.inc();
				stemCount++;
			} else {
				stems.put(stem, new MutableInteger(1));
			}
		}
		System.out.println(termCount + " terms map to " + stemCount
			+ " stems (time " + watch + ")");

		System.out.println();
	}

	static private void printUsage() {
		System.out.println();
		System.out.println("Expecting exactly 2 arguments for idxr.Tokens:");
		System.out.println("(1) The configuration name");
		System.out.println("(2) The configuration file name");
	}


	static public void dumpTokenFile(Configuration config, String cfgName)
			throws IOException {
		String sourceHome = config.getProperty(cfgName, "SourceHome");
		String fileName = config.getProperty(cfgName, "TokenFileBaseName");
		Tokens t = Tokens.load(sourceHome + "/" + fileName, false);
		String dumpBase = config.getProperty(cfgName, "DumpBase");
		String fileEnding = config.getProperty(cfgName, "DumpFileEnding");
		String dumpFileName = dumpBase + "/" + cfgName;
		File file = new File(dumpFileName);
		if (!file.exists()) {
			file.mkdir();
		}
		dumpFileName += "/" + fileName + fileEnding;
		PrintWriter pw = new PrintWriter(new FileWriter(dumpFileName));

		pw.println("Token file dump for '" + sourceHome + "/" + fileName + "'");
		pw.println("Generated on " + new java.sql.Timestamp(new java.util.Date().getTime()));
		pw.println();
		pw.println("tokenCount    = " + t.getUniqueCount());
		pw.println("instanceCount = " + t.getTotalCount());
		for (int i = 1; i <= t.getUniqueCount(); i++) {
			pw.println("" + t.get(i));
		}
		pw.flush();
		pw.close();
	}

	static public void dumpTokensAndTerms(Configuration config, String cfgName)
			throws IOException {
		String sourceHome = config.getProperty(cfgName, "SourceHome");
		String fileName = config.getProperty(cfgName, "TokenFileBaseName");
		Tokens t = Tokens.load(sourceHome + "/" + fileName, false);
		fileName = config.getProperty(cfgName, "TermFileBaseName");
		Terms tt = Terms.load(sourceHome + "/" + fileName);
		String dumpBase = config.getProperty(cfgName, "DumpBase");
		String fileEnding = config.getProperty(cfgName, "DumpFileEnding");
		String dumpFileName = dumpBase + "/" + cfgName;
		File file = new File(dumpFileName);
		if (!file.exists()) {
			file.mkdir();
		}
		dumpFileName += "/" + "tokenterm" + fileEnding;
		PrintWriter pw = new PrintWriter(new FileWriter(dumpFileName));

		pw.println("Token/term file dump for '" + sourceHome + "/" + fileName + "'");
		pw.println("Generated on " + new java.sql.Timestamp(new java.util.Date().getTime()));
		pw.println();
		pw.println("tokenCount    = " + t.getUniqueCount());
		pw.println("instanceCount = " + t.getTotalCount());
		pw.println("termCount     = " + tt.getTermCount());
		pw.println("instanceCount = " + tt.getWordCount());

		int hcLen = 0, h;
		Token token;
		Term term;
		for (int i = 1; i <= t.getUniqueCount(); i++) {
			token = t.get(i);
			h = token.getCodeLen();
			if (i <= 25 || h > hcLen) {
				hcLen = h;
				pw.print(i + " \t& '" + token.getName() + "' \t& "
						  + token.getCount() + " \t& " + token.getCodeLen());
				if (i <= tt.getTermCount()) {
					term = tt.get(i);
					pw.print(" \t& '" + term.getName() + "' \t& "
							+ term.getCount() + " \\\\");
				} else {
					pw.print(" \t&  & \\");
				}
				pw.println();
			}
		}
		pw.flush();
		pw.close();
	}

}