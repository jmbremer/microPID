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
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.Configuration;
import org.bluemedialabs.util.MutableInteger;
import org.bluemedialabs.util.MutableString;
import org.bluemedialabs.util.PercentPrinter;
// For main()
import org.bluemedialabs.util.StopWatch;
import org.bluemedialabs.util.MyPrint;
import org.bluemedialabs.util.Queue;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Terms {
	static public final int INITIAL_CAPACITY = 10000;
	private HashMap termMap = null;    // Maps term strings to MutableIntegers
	private Term[] terms;        // Contains the actual term data
//	private Term[] stems;
	private int termCount = 0;
	private int wordCount = 0;
	private boolean sorted = false;
	// Some statistics, available after computeMetadata() has been called:
	private Metadata meta = null;   // metadata not yet computed


/*+**************************************************************************
 * Class functions
 ****************************************************************************/

	static public Terms load(String baseFileName) throws IOException {
		Terms terms = new Terms();
		System.out.print("Loading terms from '" + baseFileName + "'...");
		IndexSeqFile termsFile = new IndexSeqFile(baseFileName, false);
		// The false is new and required to have terms not loaded in an
		// encoded fashion, as this is something terms do not understand..
		terms.load(termsFile);
		termsFile.close();
		System.out.print(terms.getTermCount() + "..");
		System.out.println("done.");
		return terms;
	}

	static public Terms load(Configuration config, String id)
			throws IOException {
		return load(config.getProperty(id, "SourceHome") + "/"
					+ config.getProperty(id, "TermFileBaseName"));
	}

	static public void generateFromTokens(Configuration config, String id)
			throws IOException {
		// Obtain file name from configuration
		String sourceHome = config.getProperty(id, "SourceHome");
		String termFile = sourceHome + "/"
				   + config.getProperty(id, "TermFileBaseName");

		System.out.print("\nLoading tokens...");
		Tokens tokens = Tokens.load(config, id);
		System.out.println(tokens.getUniqueCount() + "...done.");
		System.out.print("Generating terms...");
		Terms terms = new Terms(tokens);
		System.out.println(terms.getTermCount() + "...done.");
		System.out.print("Storing terms...");
		IndexSeqOutputChannel termOut =
				IndexSeqOutputChannel.create(termFile, false);
		terms.store(termOut);
		termOut.close();
		System.out.println("done.");
	}


/*+**************************************************************************
 * Object functions
 ****************************************************************************/

	public Terms() {
		terms = new Term[INITIAL_CAPACITY + 1];
		terms[0] = new Term("<dummy>", 0, -1);
		termMap = new HashMap((int) (INITIAL_CAPACITY / 0.7));    // should we do this here?
	}

	/**
	 * Constructs terms from the supplied tokens by case folding and sorting
	 * by occurrency counters.
	 */
	public Terms(Tokens tokens) {
		this();
		int tokenCount = tokens.getUniqueCount();
		int no;
		Token token;
		Term term;
		PercentPrinter pp = new PercentPrinter(tokenCount);
//		System.out.println("Constructing terms from " + tokens.getUniqueCount()
//			+ " tokens...");
		for (int i = 1; i <= tokenCount; i++) {
			token = tokens.get(i);
			// Extract all terms we are interested in (alpha numeric)
			if (acceptAsTerm(token.getName())) {
				// (inefficient but who cares here: )
				term = add(new MutableString(token.getName().toLowerCase()));
				term.incCount(token.getCount());
				pp.notify(i);
			}
		}
		pp.notify(tokenCount);
		// That should do it !?..but don't forget to:
//		System.out.println("  ..sorting terms...");
		sort();
		// Determine word count only based on the derived terms:
		wordCount = 0;
		for (int i = 1; i <= termCount; i++) {
			wordCount += terms[i].getCount();
		}
//		System.out.println("Derived " + termCount + " terms with a total "
//			+ "word count of " + wordCount + ".");
	}

	// Limited to Latin chars for now!!!
	static protected boolean acceptAsTerm(String tokenName) {
		char start = tokenName.charAt(0);
		return (EnglishTokenizer.isAlpha(start)
					|| EnglishTokenizer.isDigit(start));
	}

	/**
	 * Add and increment counter.
	 */
	public int addAndCount(MutableString name) {
		Term term = lookUpOrCreateTerm(name);
		term.incCount();
		wordCount++;
		return term.getNo();
	}

	private Term lookUpOrCreateTerm(MutableString name) {
		MutableInteger termNo = null;
		Term term = (Term) termMap.get(name);
//		try {
		if (term == null) {
//			System.out.println("New term '" + name + "'");
//			termNo = new MutableInteger(++termCount);      // !?!???
			term = new Term(name.toString().intern(), ++termCount, 0);
			termMap.put(name.toString().intern(), term);
			// check term array capacity
			if (term.getNo() >= terms.length) {
				// need to increase capacity
				increaseTermCapacity();
			}
			terms[term.getNo()] = term;
		}
//		} catch (ArrayIndexOutOfBoundsException e) {
//			System.out.println("terms.length=" + terms.length + ", termNo."
//				+ "getValue()=" + termNo.getValue());
//		}
		return term;
	}

	private void increaseTermCapacity() {
//		System.out.println();
//		System.out.println("Increasing term storage capacity from "
//			+ (terms.length - 1) + " to " + (terms.length - 1) * 2 + "...");
		Term[] t = new Term[(terms.length - 1) * 2 + 1];
		System.arraycopy(terms, 0, t, 0, terms.length);
		terms = t;
	}

	protected void clearCounts() {
		Term t;
		for (int i = 1; i <= termCount; i++) {
			t = get(i);
			t.setCount(0);
		}
		wordCount = 0;
		meta.wordCount = 0;
	}

	/**
	 * Just add.
	 */
	protected Term add(MutableString name) {
		// just make sure the term gets into our hash
		return lookUpOrCreateTerm(name);
	}

	public int getTermCount() {
		return termCount;
	}

	protected void incWordCount() {
		wordCount++;
	}
	protected void incWordCount(int inc) {
		wordCount += inc;
	}
	public int getWordCount() {
		return wordCount;
	}
	public int countWords() {
		int wc = 0;
		for (int i = 1; i <= termCount; i++) {
			wc += terms[i].getCount();
		}
		return wc;
	}

	public Term get(int no) {
		return terms[no];
	}

	public Term get(String name) {
		return (Term) termMap.get(name);
//		if (termNo == null) {
//			// don't know this term
//			return null;
//		} else {
//			return terms[((MutableInteger) termNo).getValue()];
//		}
	}

	public Term get(MutableString name) {
		return (Term) termMap.get(name);
//		Object termNo = termMap.get(name);
//		if (termNo == null) {
//			// don't know this term
//			return null;
//		} else {
//			return terms[((MutableInteger) termNo).getValue()];
//		}
	}

	public int getNo(String name) {
		Term term = get(name);
		if (term != null) {
			return term.getNo();
		} else {
			return 0;
		}
	}
	public int getNo(MutableString name) {
		Term term = get(name);
		if (term != null) {
			return term.getNo();
		} else {
			return 0;
		}
	}

	void computeMetadata() {
		long avg = 0;
		long var = 0;   // counter for variance
		long exp = 0;   // counter for expectation
		double avgCount;     // average counter over all terms
		int oneCount = 0;       // terms that occur only once
		double countExp;
		double countVar;
		double countStddev;
		int c;
		int total = 0;

		if (termCount < 1) return;  // nothing to do...
		for (int i = 1; i <= termCount; i++) {
			c = terms[i].getCount();
			total += c;
			avg += c;
			exp += c * c;
			if (c == 1) {
				oneCount++;
			}
		}
		avgCount = (double) avg / termCount;
		countExp = (double) exp / wordCount;
		for (int i = 1; i <= termCount; i++) {
			c = terms[i].getCount();
			var += (c - countExp) * (c - countExp) * c;
		}
		countVar = (double) var / wordCount;
		countStddev = Math.sqrt(countVar);
		// It remains to put the data into our metadata object
		meta = new Metadata(termCount, total, avgCount, oneCount, countExp,
				countVar, countStddev);
	}

	public Metadata getMetadata() {
		return meta;
	}


	/**
	 * ...dangerous because it changes term numbers!
	 */
	public void sort() {
		if (sorted) {
			// already done and may only be done once!
			throw new IllegalStateException("The terms may only be sorted once "
				+ "by their frequence order");
		}
		sort(Term.COUNT_COMPARATOR);
		/*
		 * Here, the numbers have changed, so we need to recalculate them
		 * in the hash table, too.
		 */
		 for (int i = 1; i <= termCount; i++) {
			Term term = terms[i];
			termMap.put(term.getName(), term);
		}
	}

	protected void sort(Comparator comparator) {
		if (termCount < 1) return;
//		System.out.println("Sorting " + termCount + " terms...");
//		System.out.print("Starting with terms: ");
//		for (int i = 1; i <= 5; i++) {
//			System.out.print(terms[i]);
//		}
//		System.out.println();
//		System.out.print("Ending with terms: ");
//		for (int i = 1; i <= 5; i++) {
//			System.out.print(terms[termCount - i + 1]);
//		}
//		System.out.println();
		// do some more sophisticated sorting sometime later...
		Arrays.sort(terms, 1, termCount + 1, comparator);
		for (int i = 1; i <= termCount; i++) {
			terms[i].setNo(i);
//			System.out.println(terms[i]);
		}
	}

	public void printAll() {
		Iterator it = termMap.values().iterator();
		Term term;
		Object obj;

		System.out.println("Term store " + this + " contains the terms:");
//		while (it.hasNext()) {
//			term = terms[((MutableInteger) it.next()).getValue()];
//			System.out.println(++i + ")  " + term);
//		}
		for (int i = 1; i <= termCount; i++) {
			System.out.println(++i + ")  " + terms[i]);
		}
	}


	public void printSortedByCount(int limit) {
		if (!sorted) {
			copySortPrint(Term.COUNT_COMPARATOR, limit);
		} else {
			for (int i = 1; i < Math.min(limit, termCount); i++) {
				System.out.println(terms[i]);
			}
		}
	}
	public void printSortedByCount() {
		printSortedByCount(termCount);
	}

	private void copySortPrint(Comparator comp, int limit) {
		Term[] t = new Term[termCount + 1];
		System.arraycopy(terms, 1, t, 1, termCount);
		Arrays.sort(t, 1, termCount, comp);
		for (int i = 1; i <= Math.min(limit, termCount); i++) {
			System.out.println(t[i]);
		}
	}

	public void printSortedByNo(int limit) {
		copySortPrint(Term.NUMBER_COMPARATOR, limit);
	}
	public void printSortedByNo() {
		copySortPrint(Term.NUMBER_COMPARATOR, termCount);
	}

	public void printLexiSorted(int limit) {
		copySortPrint(Term.LEXICAL_COMPARATOR, limit);
	}
	public void printLexiSorted() {
		copySortPrint(Term.LEXICAL_COMPARATOR, termCount);
	}


	public String toString() {
		StringBuffer buf = new StringBuffer(100);

		buf.append("(termCount=");
		buf.append(termCount);
		buf.append(", wordCount=");
		buf.append(wordCount);
		buf.append(", metadata=");
		if (meta != null) {
			buf.append(meta.toString());
		} else {
			buf.append("<not yet computed>");
		}
		buf.append(")");
		return buf.toString();
	}


	/*+**********************************************************************
	 * Pseudo Storable implementation
	 ************************************************************************/

	 public void store(IndexSeqOutput out) throws IOException {
		for (int i = 1; i <= termCount; i++) {
			out.write(terms[i]);
		}
		if (meta == null) {
			// Metadata has yet to be computed
			computeMetadata();
		}
		out.write(meta);
	}

//	public void store(IndexSeqOutputChannel out) throws IOException {
//		for (int i = 1; i <= termCount; i++) {
//			out.write(terms[i]);
//		}
//		if (meta == null) {
//			// Metadata has yet to be computed
//			computeMetadata();
//		}
//		out.write(meta);
//	}

	public void load(IndexSeqFile file) throws IOException {
		int recCount = file.getRecordCount() - 1;   // last rec = metadata!
		Term[] newTerms;

		if (terms.length != recCount) {
			newTerms = new Term[recCount + 1];  // +1 is important!
			for (int i = 1; i <= Math.min(termCount, recCount); i++) {
				// copy all already allocated terms
				newTerms[i] = terms[i];
			}
		} else {
			newTerms = terms;
		}
		if (termCount <= recCount) {
			for (int i = termCount + 1; i <= recCount; i++) {
				newTerms[i] = new Term();
			}
		} else {
			for (int i = recCount + 1; i <= termCount; i++) {
				// pseudo delete ;-)
				terms[i] = null;
			}
		}
		terms = newTerms;
		// Now we have exactly enough space to load all the terms
		for (int i = 1; i <= recCount; i++) {
			file.get(i, terms[i]);
			terms[i].setNo(i);
		}
		// Finally, load the last, the metadata record
		if (meta == null) {
			meta = new Metadata();
		}
		file.get(recCount + 1, meta);
		termCount = meta.getTermCount();
		wordCount = /*meta.getWordCount();*/ countWords();
		// Don't forget to fill up the hash map as well:
//		if (termMap.csize
		// How can we actually determine and adjust the hash map's capacity here???
		termMap.clear();
		for (int i = 1; i <= termCount; i++) {
			termMap.put(terms[i].getName(), terms[i]);
		}
	}

	public int byteSize() {
		return -1;  // don't know
	}


	/************************************************************************
	 * Encapsulates the metadata record common to all terms. The record is
	 * written as the last record into the term file/output stream.
	 ************************************************************************/
	public static class Metadata implements Storable {
		static public final int BYTE_SIZE = 44;

		int termCount;
		int wordCount;
		double avgCount;
		int oneCount;
		double countExp;
		double countVar;
		double countStddev;

		/**
		 * Constructor for writing metadata.
		 */
		Metadata(int termCount, int wordCount, double avgCount, int oneCount,
				double countExp, double countVar, double countStddev) {
			this.termCount = termCount;
			this.wordCount = wordCount;
			this.avgCount = avgCount;
			this.oneCount = oneCount;
			this.countExp = countExp;
			this.countVar = countVar;
			this.countStddev = countStddev;
		}

		/**
		 * Constructor for reading metadata.
		 */
		Metadata() {}

//		public void setMetadata(Terms terms) {
//			terms.termCount = termCount;
//			terms.wordCount = wordCount;
//			terms.avgCount = avgCount;
//			terms.oneCount = oneCount;
//			terms.countExp = countExp;
//			terms.countVar = countVar;
//			terms.countStddev = countStddev;
//		}

		public int getTermCount() { return termCount; }
		public int getWordCount() { return wordCount; }
		public double getAvgCount() { return avgCount; }
		public int getOneCount() { return oneCount; }
		public double getCountExp() { return countExp; }
		public double getCountVar() { return countVar; }
		public double getCountStddev() { return countStddev; }

		public String toString() {
			StringBuffer buf = new StringBuffer(255);
			DecimalFormat doubleFormat = new DecimalFormat();
			doubleFormat.setMaximumFractionDigits(2);
			doubleFormat.setGroupingSize(1000);

			buf.append("(avgCount=");
			buf.append(doubleFormat.format(avgCount));
			buf.append(", oneCount=");
			buf.append(oneCount);
			buf.append(" (");
			buf.append(doubleFormat.format((double) oneCount / termCount * 100));
			buf.append("%), countExp=");
			buf.append(doubleFormat.format(countExp));
			buf.append(", countVar=");
			buf.append(doubleFormat.format(countVar));
			buf.append(", countStddev=");
			buf.append(doubleFormat.format(countStddev));
			buf.append(")");
			return buf.toString();
		}


		/*+******************************************************************
		 * Storable implementation
		 ********************************************************************/

		public void store(DataOutput out) throws IOException {
			out.writeInt(termCount);
			out.writeInt(wordCount);
			out.writeDouble(avgCount);
			out.writeInt(oneCount);
			out.writeDouble(countExp);
			out.writeDouble(countVar);
			out.writeDouble(countStddev);
		}

		public void load(DataInput in) throws IOException {
			termCount = in.readInt();
			wordCount = in.readInt();
			avgCount = in.readDouble();
			oneCount = in.readInt();
			countExp = in.readDouble();
			countVar = in.readDouble();
			countStddev = in.readDouble();
		}

		public int byteSize() {
			return BYTE_SIZE;
		}
	}



	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		boolean ir = false;

		// Check validity of input
		if (args.length == 3) {
			if (args[0].compareToIgnoreCase("ir") != 0) {
				printUsage();
				System.exit(1);
			}
			ir = true;
			args[0] = args[1];
			args[1] = args[2];
		} else if (args.length != 2) {
			printUsage();
			System.exit(1);
		}

		// Load configuration
		IdxrConfig config = (IdxrConfig) IdxrConfig.load(args[1]);

//		dumpTermFile(config, args[0], ir);
		generateFromTokens(config, args[0]);
//		findInteresting(config, args[0]);

		System.exit(0);

/*
		final String REP_DIR = DATA_HOME + "/XMLmini/";
		StopWords sw = new StopWords("/F/Data/English stopwords.txt");
		Terms terms;
		Tokens tokens = new Tokens();
		int termCount, stemCount, wordCount, stopWordCount;
		Term term;
		PorterStemmer stemmer = new PorterStemmer();
		String stem;
		HashMap stems;
		MutableInteger count;
		StopWatch watch = new StopWatch();

		watch.start();
		System.out.println("Loading tokens... (time " + watch + ")");
		tokens.load(new IndexSeqFile(REP_DIR + "tokens"));
		System.out.println("..done (" + tokens.getUniqueCount() + " tokens loaded).");

		System.out.println("Deriving terms... (time " + watch + ")");
		terms = new Terms(tokens);
		termCount = terms.getTermCount();
		System.out.println("..done (" + termCount + " terms derived).");

		System.out.print("Storing terms...");
		FileOutputStream fidx = new FileOutputStream(REP_DIR + "/terms.index");
		FileOutputStream fdata = new FileOutputStream(REP_DIR + "/terms.data");
		IndexSeqOutputStream out = new IndexSeqOutputStream(fidx, fdata, false);
		terms.store(out);
		System.out.println("done.");

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
*/
	}

	static private void printUsage() {
		System.out.println();
		System.out.println("Expecting exactly 2 arguments for idxr.Terms:");
		System.out.println("[(1) \"ir\" to use the IR term file]");
		System.out.println("(2) The configuration name");
		System.out.println("(3) The configuration file name");
	}

	static public void dumpTermFile(Configuration config, String cfgName, boolean ir)
			throws IOException {
		char dirSeparat = File.separatorChar; // ...use this in the future

		// Load terms
		String sourceHome = config.getProperty(cfgName, "SourceHome");
		String sourceFileName = null;
		if (!ir) {
			sourceFileName= config.getProperty(cfgName, "TermFileBaseName");
		} else {
			sourceFileName= config.getProperty(cfgName, "IrTermFileBaseName");
		}
		Terms t = Terms.load(sourceHome + "/" + sourceFileName);

		// Prepare and write to dump file
		String dumpBase = config.getProperty(cfgName, "DumpBase");
		String fileEnding = config.getProperty(cfgName, "DumpFileEnding");
		String dumpFileName = dumpBase + "/" + cfgName;
		File file = new File(dumpFileName);
		if (!file.exists()) {
			file.mkdir();
		}
		dumpFileName += "/" + sourceFileName + fileEnding;
		PrintWriter pw = new PrintWriter(new FileWriter(dumpFileName));

		pw.println("Term file dump for '" + sourceHome + "/" + sourceFileName + "'");
		pw.println("Generated on " + new java.sql.Timestamp(new java.util.Date().getTime()));
		pw.println();
		pw.println("termCount     = " + t.getTermCount());
		pw.print("instanceCount = " + t.getWordCount());
		pw.println(" (actually counted: " + t.countWords() + ")");
		for (int i = 1; i <= t.getTermCount(); i++) {
			pw.println("" + t.get(i));
		}
		pw.flush();
		pw.close();
	}


	static void findInteresting(Configuration config, String cfgName)
			throws IOException {
		Terms terms = Terms.load(config, cfgName);
		String t;
		int len, off;
		int matchCount[] = new int[5];
		int maxMatch = 0;
		Queue q = new Queue();
		PercentPrinter p = new PercentPrinter(terms.getTermCount());

		for (int i = 1; i <= terms.getTermCount(); i++) {
			t = terms.get(i).getName();
			len = t.length();
			maxMatch = 0;
			off = t.indexOf('d');
			if (off >= 0) {
				maxMatch++;
				off = t.indexOf('d', off + 1);
				if (off >= 0) {
					maxMatch++;
					off = t.indexOf('f', off + 1);
					if (off >= 0) {
						maxMatch++;
						off = t.indexOf('r', off + 1);
						if (off >= 0) {
							maxMatch++;
							q.enqueue(t);
						}
					}
				}
			}
			matchCount[maxMatch]++;
			if (i % 100 == 0) {
				p.notify(i);
			}
		}
		p.notify(terms.getTermCount());
		System.out.println();
		System.out.println("Single match....... " + matchCount[1]);
		System.out.println("Double match....... " + matchCount[2]);
		System.out.println("Triple match....... " + matchCount[3]);
		System.out.println("Quadruple match.... " + matchCount[4]);
		System.out.println("Full matches....... ");
		while (!q.isEmpty()) {
			System.out.println("  " + q.dequeue());
		}
	}

}