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

import java.text.DecimalFormat;
import java.util.HashMap;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */

public class StemmedTerms {
	private PorterStemmer stemmer;  // the stemmer to be used; fixed for now
	private Terms terms;            // the unstemmed terms
	private HashMap stems;
	private int stemCount = 0;

	static private PorterStemmer staticStemmer = new PorterStemmer();
	static public String stem(String word) {
		staticStemmer.add(word.toCharArray(), word.length());
		staticStemmer.stem();
		return staticStemmer.toString();
	}


	public StemmedTerms(/*PorterStemmer stemmer*/ Terms terms) {
		Term term;
		String stemStr;
		Stem stem;

		stemmer = new PorterStemmer();
		this.terms = terms;
		// initialize hash table
		stems = new HashMap((int) (terms.getTermCount() / 0.75));
//		System.out.println("Determining # of stems... (time " + watch + ")");
		stemCount = 0;
		for (int i = 1; i <= terms.getTermCount(); i++) {
			term = terms.get(i);
			stemmer.add(term.getName().toCharArray(), term.getName().length());
			stemmer.stem();
			stemStr = stemmer.toString();
			// Add or update stem in hash table
			stem = (Stem) stems.get(stemStr);
			if (stem != null) {
				// have seen the stem before
				stem.addTerm(term);
				stemCount++;
			} else {
				stems.put(stemStr.intern(), new Stem(stemStr, term));
			}
		}
		System.out.println("Constructed stemmed terms; " + terms.getTermCount()
			+ " map to " + stemCount + " stems");
//		System.out.println();
	}

	/**
	 * Returns the stem object related to the unstemmed string supplied.
	 */
	public Stem get(String term) {
		String stemStr;
		// determine stem as a string
		stemmer.add(term.toCharArray(), term.length());
		stemmer.stem();
		stemStr = stemmer.toString();
		// return related stem object
		return (Stem) stems.get(stemStr);
	}

	// Not multi-threading capable!
	public Stem get(Term term) {
		return get(term.getName());
	}

	public int getStemCount() {
		return stemCount;
	}


	public String toString() {
		StringBuffer buf = new StringBuffer(60);
		DecimalFormat doubleFormat = new DecimalFormat();
		doubleFormat.setMaximumFractionDigits(2);
		doubleFormat.setGroupingSize(1000);

		buf.append("(stemCount=");
		buf.append(stemCount);
		buf.append(" (");
		buf.append(doubleFormat.format(
				(double) terms.getTermCount() / stemCount));
		buf.append(" stems/term)");
		buf.append(")");
		return buf.toString();
	}

//	static public void main(String[] args) {
//		StemmedTerms stemmedTerms1 = new StemmedTerms();
//	}
}