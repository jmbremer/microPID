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

import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.bluemedialabs.io.IndexSeqOutputStream;
import org.bluemedialabs.io.LogStream;
import org.bluemedialabs.util.MutableInteger;
import org.bluemedialabs.util.MutableString;
import org.bluemedialabs.util.Pool;
import org.bluemedialabs.util.Quack;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TermDocCntHandler extends ContntHandler {
	static public final int INITIAL_TERMDOC_HASHMAP_CAPACITY = 4000;

	private HashMap termDocCountMap =
			new HashMap(INITIAL_TERMDOC_HASHMAP_CAPACITY);
	private Pool mutIntPool = new Pool(new MutableInteger(), 4000);
	private TermDocCounters termDocCounts;
	private int docNo = 0;
	private int depth = 0;  // current depth in XML tree (root = 1, doc = 2)
	private DataGuide guide;
	private IndexSeqOutputStream docOut;
	private Document doc = new Document();
	private StringBuffer content = null;
//	private String currentTag = null;   // Points to the current tag's name
	private Quack tagNames = new Quack(); // Stack of tag names


	// Only to make the compiler happy:
	private XmlTokenizer tokenizer = null;
	private Terms terms = null;


	public TermDocCntHandler(EnglishTokenizer tokenizer, DataGuide guide,
			Terms terms, TermDocCounters termDocCounts,
			IndexSeqOutputStream docOut, LogStream log) {
		super(null/*tokenizer, terms, log*/);
		this.termDocCounts = termDocCounts;
		this.guide = guide;
		this.docOut = docOut;
		doc = new Document();
//		if (xmlOut != null) {
//			content = new StringBuffer(32768);
//		}
	}


	public void startDocument() throws SAXException {
		super.startDocument();
//		if (xmlOut != null) {
//			System.out.println("Writing XML content header...");
//			guide.getTagMap().store(xmlOut);
//		}
	}

	/**
	 * Increases the current depth within the XML tree and calls the characters
	 * method for all attributes found.
	 */
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes attribs) throws SAXException {
		char[] text;

		// we are one level deeper down the XML tree now
		depth++;
		tagNames.push(qName);
//		System.out.println(tagNames.top());
		// add all of the attributes
		for (int i = 0; i < attribs.getLength(); i++) {
//			System.out.println("Adding attribute '" + atts.getQName(i) + "'...");
			text = attribs.getValue(i).toCharArray();   // expensive!
			characters(text, 0, text.length);
		}
	}

	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		Term term;
		MutableInteger count;
		Map.Entry entry;
		int wordCount;

		depth--;
//		System.out.println("/" + tagNames.top());
		tagNames.pop();
		if (depth == 1) {
			// just ended a document (supposedly)
			docNo++;
			if (docNo % 10000 == 0) {
				System.out.println();
				System.out.println(docNo + " documents...");
				System.out.print("|");
			} else if (docNo % 1000 == 0) {
				System.out.print("|");
			} else if (docNo % 500 == 0) {
				System.out.print(";");
			} else if (docNo % 100 == 0) {
				System.out.print(".");
			}
//			System.out.println("End of document " + docNo + ", writing "
//				+ termDocCountMap.size() + " counters "
//				+ "to term-doc store...");

			// First, extract all the term data for this document...
			Iterator it = termDocCountMap.entrySet().iterator();
			wordCount = 0;
			try {
				while (it.hasNext()) {
					entry = (Map.Entry) it.next();
					term = (Term) entry.getKey();
					count = (MutableInteger) entry.getValue();
					wordCount += count.getValue();
					termDocCounts.addCount(term.getNo(), docNo,
							(short) count.getValue());
				}
			} catch (IOException e) {
				throw new SAXException("I/O exception while adding term-doc "
					+ "counter (" + e + ")");
			}

			// Then, write minimal document information
			doc.setNo(docNo);
			// The id should already have been set in characters().
			doc.setWordCount(wordCount);
			doc.setUniqueCount(termDocCountMap.size());
			try {
				docOut.write(doc);
			} catch (IOException e) {
				throw new SAXException("Failed to store basic document data ("
					+ e + ")");
			}

			// ..and finally:
			termDocCountMap.clear();
			mutIntPool.releaseAll();
		}
	}

	public void endDocument() throws SAXException {
		super.endDocument();
		try {
			termDocCounts.finalize();
		} catch (IOException e) {
			throw new SAXException("I/O exception while finalizing term-doc "
						+ "counter (" + e + ")");
		}
	}

	public void characters(char[] text, int offset, int len) {
		Term term;
		MutableInteger count;
//		log.log("Found " + len + " more characters:");
//		System.out.println(chars);
		// First, look wether any document-related meta information
		// has to be written
		if (((String) tagNames.top()).compareToIgnoreCase("docno") == 0) {
			doc.setId((new String(text, offset, len)).trim());
		}
		// Then, extract the words and add it to the inverted list structure
		MutableString str = new MutableString(null, 0, 0);
		tokenizer.reuse(text, offset, len);
		while (tokenizer.nextToken(str)) {
			term = terms.get(str);
//			log.log("Next token is term " + term);
			count = (MutableInteger) termDocCountMap.get(term);
			if (count == null) {
				count = (MutableInteger) mutIntPool.claim();
				count.setValue(1);
				termDocCountMap.put(term, count);
			} else {
				count.inc();
			}
		}
	}

}