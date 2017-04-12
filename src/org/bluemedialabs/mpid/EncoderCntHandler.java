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

import java.io.OutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.bluemedialabs.io.BitOutput;
//import io.LogStream;
import org.bluemedialabs.util.*;


/**
 * <p>SPACE HANDLING BETWEEN ATTRIBUTES ETC.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class EncoderCntHandler extends ContntHandler {
	static public final String HEADER =
		"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n\n";
	static private final int MAX_ELEMENT_LENGTH = 256;  // ???

	private int docNo = 0;
	private int depth = 0;  // current depth in XML tree (root = 1, doc = 2)
	private XmlTokenizer tokenizer;
	private HuffmanCoder coder;
	private BitOutput xmlOut;
	// Some frequently used variables (no synchronization issues!)
	private StringBuffer content = null;
	private Token token;
	private MutableInteger tokenNo = new MutableInteger();
	int code;
	private StringBuffer strBuf = new StringBuffer(MAX_ELEMENT_LENGTH);
	private PercentPrinter percentPrinter;
	private long nodeCount = 0;

	// Prequery some frequently used tokens
	private int spaceCode, spaceCodeLen;
	private int elemEndCode, elemEndCodeLen;
	private int attribEndCode, attribEndCodeLen;


	public EncoderCntHandler(XmlTokenizer tokenizer, Tokens tokens,
			HuffmanCoder coder, BitOutput xmlOut, long totalNodeCount) {
		super(tokens);
		this.tokenizer = tokenizer;
		this.coder = coder;
		this.xmlOut = xmlOut;
//		if (xmlOut != null) {
//			content = new StringBuffer(32768);
//		}
		prefetchTokenCodes();
		percentPrinter = new PercentPrinter(totalNodeCount);
	}

	private void prefetchTokenCodes() {
		// Space
		System.out.println("Prefetching some token codes...");
		token = tokens.get(" ", tokenNo);
		spaceCode = coder.encode(token, tokenNo.getValue());
		spaceCodeLen = token.huffCodeLen;
		// Element end
		token = tokens.get("</>", tokenNo);
		elemEndCode = coder.encode(token, tokenNo.getValue());
		elemEndCodeLen = token.huffCodeLen;
		// Attribute end
		token = tokens.get("<@>", tokenNo);
		if (token != null) {
			attribEndCode = coder.encode(token, tokenNo.getValue());
			attribEndCodeLen = token.huffCodeLen;
		} else {
			System.out.println("Attribute end token not found, assuming no "
				+ "attributes present...");
			attribEndCode = -1;
			attribEndCodeLen = 128; //!!!
		}
		System.out.println("Done prefetching codes.");
		System.out.flush();
	}

	private void encode(String str) throws IOException {
		token = tokens.get(str, tokenNo);
		code = coder.encode(token, tokenNo.getValue());
		xmlOut.write(code, token.huffCodeLen);
//		System.out.println(token + ", code=" + Integer.toBinaryString(code));
	}

	private void encode(MutableString str) throws IOException {
		token = tokens.get(str, tokenNo);
		code = coder.encode(token, tokenNo.getValue());
		xmlOut.write(code, token.huffCodeLen);
//		System.out.println(token + ", code=" + Integer.toBinaryString(code));
	}

	private void encode(int code, int len) throws IOException {
		xmlOut.write(code, len);
	}


	// Parser calls this for each element in a document
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes attribs) throws SAXException {
		GuideNode node;
		char[] text;
		StringBuffer buf;
		String str;

		try {
			// we are one level deeper down the XML tree now
			depth++;
			// find this element as a token
//			strBuf.delete(0, strBuf.length() + 1);
			// ...replaced by the following on 2003-feb-21
			strBuf.setLength(0);
			strBuf.append('<');

			// NOW CASE-SENSITIVE ON DEMAND...
			if (XmlEncoder.CASE_SENSITIVE) {
				strBuf.append(qName);
			} else {
				strBuf.append(qName.toUpperCase());
			}



			// ..and encode the element
//			System.out.println("Writing code for element '" + strBuf.toString()
//					+ "'...");
			encode(strBuf.toString());

			// ...
			// add all of the attributes
			for (int i = 0; i < attribs.getLength(); i++) {
//  			System.out.println("Adding attribute '" + atts.getQName(i) + "'...");
//				strBuf.delete(0, strBuf.length() + 1);
				strBuf.setLength(0);
				strBuf.append('@');

				// NOW CASE-SENSITIVE ON DEMAND...
				if (XmlEncoder.CASE_SENSITIVE) {
					strBuf.append(attribs.getQName(i));
				} else {
					strBuf.append(attribs.getQName(i).toLowerCase());
				}


				encode(strBuf.toString());

				text = attribs.getValue(i).toCharArray();   // expensive!
				characters(text, 0, text.length);
			}
			// Write attribute list end pseudo tag...
			if (attribs.getLength() > 0) {
				// ..but only if there where any attributes
				encode(attribEndCode, attribEndCodeLen);
				nodeCount += attribs.getLength();
			}
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {

		try {
			encode(elemEndCode, elemEndCodeLen);
		} catch (IOException e) {
			throw new SAXException(e);
		}

		depth--;
		if (depth == 1) {
			// just ended a document (supposedly)
			docNo++;
			/*
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
			*/
		}
		nodeCount++;
		if (nodeCount % 100 == 0) {
			percentPrinter.notify(nodeCount);
		}
	}

	public void characters(char[] text, int offset, int len)
			throws SAXException {
//		log.log("Found " + len + " more characters:");
//		System.out.println(chars);
		try {
			MutableString str = new MutableString(null, 0, 0);
			tokenizer.reuse(text, offset, len);
			while (tokenizer.nextToken(str)) {
//			    log.log("Next token is '" + str + "'.");
				encode(str);
//			    System.out.println("Added term '" + str + "'.");
			}
		} catch (IOException e) {
//			e.printStackTrace();
			throw new SAXException(e);
		}
	}



}