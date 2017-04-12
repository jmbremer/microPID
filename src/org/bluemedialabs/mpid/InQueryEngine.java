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
import java.util.*;
import org.bluemedialabs.util.*;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class InQueryEngine extends FlatQueryEngine {
	protected StemmedTerms stems;
	private StopWords stopWords;
	private PorterStemmer stemmer;
	private int stopWordCount;

	public InQueryEngine(String repDir, StopWords stopWords) throws IOException {
		super(repDir);
		this.stopWords = stopWords;
		stemmer = new PorterStemmer();
		System.out.println("   Creating stemmed terms...");
		stems = new StemmedTerms(terms);
		System.out.println("   ...stemmed terms are " + stems);
		System.out.println("   Counting stop words...");
		stopWordCount = countStopWords();
		System.out.println("   ..." + stopWordCount + " out of "
				+ terms.getWordCount()
				+ " words are stop words in the collection");
		System.out.println("InQuery engine created successfully.");
	}

	private int countStopWords() {
		Iterator it = stopWords.iterator();
		String word;
		Term term;
		int count = 0;

		while (it.hasNext()) {
			word = (String) it.next();
			term = terms.get(word);
			if (term != null) {
				count += term.getCount();
			}
		}
		return count;
	}


	public void rankDocuments(String query, Collection result) throws IOException {
		LinkedList li = new LinkedList();
		FlatInvertedList mainList = new FlatInvertedList(2000);
		FlatInvertedList addList = new FlatInvertedList(2000);
		FlatInvertedList.Counter counter;
		Iterator stemIt, listIt, termIt;
		Term term;
		Stem stem;
		HashMap queryTerms = new HashMap(); // maps term#'s to df
		int queryTermCount;
		HashMap docWeights = new HashMap(10000);  // maps doc#'s to weights & more
		MutableDouble docWeight = new MutableDouble();
		MutableInteger docNo = new MutableInteger();
		int totalTermWordCount;

		result.clear();
		parseQuery(query, li);
		// Now, take the stems and for each of it merge the look up the related terms
		stemIt = li.iterator();
		if (!stemIt.hasNext()) {
			// Unknown query terms, all docs are equi-relevant
			return;
		}
		while (stemIt.hasNext()) {
			totalTermWordCount = 0;
			stem = (Stem) stemIt.next();
//			System.out.println("     Handling stem " + stem + "...");
			termIt = stem.getTerms().iterator();
			// merge all lists related to the current stem
			while (termIt.hasNext()) {
				term = (Term) termIt.next();
				totalTermWordCount += term.getCount();
//				System.out.print("       ..term " + term + "...");
				if (mainList.length() == 0) {
					getInvList(term.getNo(), mainList);
				} else {
					getInvList(term.getNo(), addList);
//					System.out.print(addList.length() + "..");
					mainList.merge(addList);
				}
//				System.out.println("(" + mainList.length() + ")..");
			}
//			System.out.println("     " + mainList.length() + " related documents...");
			// now, with the single inverted list determine the actual weights
			listIt = mainList.iterator();
//			System.out.print("     Document..");

//			double alpha = Math.log(1 + totalTermWordCount)
//						/ Math.log(1 +

			while (listIt.hasNext()) {
				counter = (FlatInvertedList.Counter) listIt.next();
				int dno = counter.getNo();
				int count = counter.getValue();

//				double w = iqWeight(count, docs.get(dno).getWordCount(),
//					(terms.getWordCount() - stopWordCount) / (double) docs.getDocCount(),
//					mainList.length(), docs.getDocCount());
				double w = weight(count /* tf */, mainList.length() /* df */,
						docs.getWordCount(dno) /* doc. length */,
						docs.getDocCount() /* doc. count */,
						docs.getWordCount(dno) / (double) docs.getUniqueCount(dno)
						/* Avg. doc. tf */);


				// Create or add weight to document in hash table
				docNo.reuse(dno);
				docWeight = (MutableDouble) docWeights.get(docNo);
				if (docWeight == null) {
					docWeights.put(docNo.clone(), new MutableDouble(w));
				} else {
					docWeight.add(w);
				}
			}

		}
		// Done. For now, just print the hash map
		ArrayList rankedDocs = new ArrayList(docWeights.entrySet());
		Collections.sort(rankedDocs, new RankedDoc.MapEntryComparator());
		Iterator it = rankedDocs.iterator();
		DecimalFormat doubleFormat = new DecimalFormat();
			doubleFormat.setMaximumFractionDigits(4);
			doubleFormat.setGroupingSize(1000);
		int i = 0;
		RankedDoc doc = new RankedDoc();
		while (it.hasNext() && i++ < 1000) {
			Map.Entry entry = (Map.Entry) it.next();
			int dno = ((MutableInteger) entry.getKey()).getValue();
			double w = ((MutableDouble) entry.getValue()).value;
			docs.get(dno, doc);
			doc.setWeight((float) w);
			result.add(doc.clone());
//			System.out.println(i + ") Document " + docs.get(dno) + "\t weight=" +
//				doubleFormat.format(w));
//			System.out.println(doc.toTrecString(401, i, "TestRun-2002-3-5"));
		}

	}

	/**
	 * ... which logarithm is it actually? Base 2, natural, base 10?
	 * @param tf Term frequency
	 * @param docLen Document length in # of terms
	 * @param avgLen Average document length within collection
	 * @param df Number of documents containing given term (document frequence)
	 * @param docCount Total number of documents in collection
	 */
//	private double weight(int tf, int docLen, double avgLen, int df, int docCount) {
//		return ((tf / ((double) tf + 0.5 + 1.5 * docLen / avgLen))
//				* (Math.log(((double) docCount + 0.5) / df) / Math.log(docCount + 1)));
//	}

	static private double LOG_2 = Math.log(2);
	private double weight(int tf, int df, int docLen, int docCount,
			double avgDocTf) {
		// Query term weight
//		double q = Math.log(1 + tf) / LOG_2;
		// Document length approximation
//		double absD = Math.sqrt(0.8 * docs.getAvgUniqueTerms() + 0.2 * docUnique);
		return (Math.log(1 + tf) / Math.log(1 + avgDocTf))
				* Math.log(docCount / (double) df)
				/ docLen;
	}

	/**
	 * Parses query into word tokens eliminating stop words and converting
	 * terms into stems.
	 */
	private void parseQuery(String query, Collection words) {
		EnglishTokenizer tok = new EnglishTokenizer(query.toCharArray());
		String word;
		MutableString str = new MutableString();
		Term term;
		Stem stem;

		words.clear();
//		System.out.println("Parsing query '" + query + "' into:  ");
		while (tok.nextToken(str)) {
			word = str.toString();
			word = word.toLowerCase().trim();
			if (!stopWords.isStopWord(word)) {
				// do we know this word/term
				term = terms.get(word);
				stem = stems.get(word);
//				if (term != null) {
//					System.out.println("Known term " + term);
//				} else if (stem != null) {
//					System.out.println("Unknown word '" + word + "', but known "
//						+ " stem " + stem);
//				} else {
//					System.out.println("Unknown word '" + word + "' and stem '"
//						+ StemmedTerms.stem(word));
//				}
				// do stemming
				if (stem != null) {
					words.add(stem);
//					System.out.println("    " + stem
//						+ (word.compareTo(stem.getName()) != 0? " (STEM)  ": "  "));
				} else {
//					System.out.println("   '" + word + "' (UNKNOWN)  ");
				}
			} else {
//				System.out.println("   '" + word + "' (STOP)  ");
			}
		}
//		System.out.println();
	}


	public static void main(String[] args) throws Exception {
		StopWords sw = new StopWords("F:/Data/English stopwords.txt");
		InQueryEngine qe = new InQueryEngine("F:/Data/tmp", sw);
		FlatInvertedList li = new FlatInvertedList(1000);
		int maxTermNo = qe.getTerms().getTermCount();
		LinkedList ll;
		Iterator it;

		System.out.println("Printing a couple of inverted lists...");
		qe.getInvList(45, li);
		System.out.print(".. 45)  ");
		System.out.println(li.toString());
		qe.getInvList(112, li);
		System.out.println("..112)  " + li);
		qe.getInvList(maxTermNo, li);
		System.out.println(".." + maxTermNo + ")  " + li);

		ll = new LinkedList();
		qe.parseQuery("Does Germany export a lot of cars?", ll);
		it = ll.iterator();
		while (it.hasNext()) {
			System.out.print("   '" + ((Stem) it.next()).getName() + "'");
		}
		System.out.println();

		System.out.println("Actually ranking some documents...");
		ll.clear();
		qe.rankDocuments("Does Germany export a lot of cars?", ll);

	}

}