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

//import java.nio.CharBuffer;
//import java.util.Iterator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
//import org.bluemedialabs.io.LogStream;
//import org.bluemedialabs.util.MutableInteger;
import org.bluemedialabs.util.MutableString;
//import org.bluemedialabs.util.MyPrint;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class StatsCntHandler extends ContntHandler {
	static private final int MAX_ELEMENT_LENGTH = 256;  // ???
	protected XmlTokenizer tokenizer;
	private GuideNode guideNode;
	private int depth = 0;  // current depth in XML tree (root = 1, doc = 2)
	private int docNo = 0;  // just for printing
	private StringBuffer strBuf = new StringBuffer(MAX_ELEMENT_LENGTH);

	// For debugging:
	private int totalNodeCount = 0;


	public StatsCntHandler(XmlTokenizer tokenizer, Tokens tokens,
			DataGuide guide) {
		super(tokens);
		this.tokenizer = tokenizer;
		this.guideNode = guide.getRoot();
	}

	// Parser calls this for each element in a document
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes attribs) throws SAXException {
		GuideNode node;
		char[] text;
		StringBuffer buf;
		String str;

		totalNodeCount++;

		// we are one level deeper down the XML tree now
		depth++;
		// add this element

		// TO MAKE ELEMENT NAMES PROPERLY CASE SENSITIVE, DELETE (REPLACE)
		// THE FOLLOWING LINE...
		// ..WHICH IS NOW UNNECESSARY AS IT CAN BE SWITCHED ON DEMAND:
		if (!XmlEncoder.CASE_SENSITIVE) {
			str = qName.toUpperCase();
		} else {
			str= qName;
		}


//		if (str.compareTo("EM") == 0) {
//			MyPrint.pl("*** EM ***");
//		}
		guideNode = guideNode.getOrCreateChild(str);
		guideNode.notifyNewChild();

//		MyPrint.pl();
//		for (int i = 0; i < depth; i++) {
//			MyPrint.p("\t");
//		}
//		MyPrint.p(str);

		// add this element as a token
//		strBuf.delete(0, strBuf.length() + 1);
		strBuf.setLength(0);
		strBuf.append('<');
		strBuf.append(str);
//		strBuf.append('>');
		tokens.addAndCount(strBuf.toString());

		totalNodeCount += attribs.getLength();

		// add all of the attributes
		for (int i = 0; i < attribs.getLength(); i++) {
//			System.out.println("Adding attribute '" + atts.getQName(i) + "'...");

			// TO MAKE ELEMENT NAMES PROPERLY CASE SENSITIVE, DELETE (REPLACE)
			// THE FOLLOWING LINE...
			// ..WHICH IS NOW UNNECESSARY AS IT CAN BE SWITCHED ON DEMAND:
			if (!XmlEncoder.CASE_SENSITIVE) {
				str = attribs.getQName(i).toLowerCase();
			} else {
				str = attribs.getQName(i);
			}

//			strBuf.delete(0, strBuf.length() + 1);
			strBuf.setLength(0);
			strBuf.append('@');
			strBuf.append(str);
			tokens.addAndCount(strBuf.toString());

//			MyPrint.p("  ");
//			MyPrint.p(strBuf.toString());

			guideNode = guideNode.getOrCreateChild(str);
			guideNode.notifyNewChild();
			guideNode.setAttrib(true);
			text = attribs.getValue(i).toCharArray();   // expensive!
			characters(text, 0, text.length);
			guideNode = guideNode.returnToParent();  // change on 2003-3-4
		}
		if (attribs.getLength() > 0) {
			tokens.addAndCount("<@>");    // Attribute list end delimiter!
		}
	}

	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
//		Iterator it;
//		GuideNode child;

		tokens.addAndCount("</>");
		// Notify all parents children that there will be no more children
		// of this parent
		guideNode.notifyParentEnd();
		guideNode = guideNode.returnToParent();  // Change on 2003-3-4

		depth--;
		if (depth == 1) {
			// just ended a document (supposedly)
			docNo++;
			if (docNo % 100000 == 0) {
				System.out.print("\n" + docNo + ": ");
				System.out.print("|");
			} else if (docNo % 10000 == 0) {
				System.out.print((docNo % 100000) / 1000);
				System.out.print("|000");
			} else if (docNo % 5000 == 0) {
				System.out.print(";");
			} else if (docNo % 1000 == 0) {
				System.out.print(".");
			}
		}
	}

	public void characters(char[] text, int offset, int len) {
		int tokenCount = 0;
//		log.log("Found " + len + " more characters:");
//		System.out.println(chars);
		try {
			MutableString str = new MutableString(null, 0, 0);
			tokenizer.reuse(text, offset, len);
			while (tokenizer.nextToken(str)) {
			//			log.log("Next token is '" + str + "'.");
				tokens.addAndCount(str);
				tokenCount++;
			//			System.out.println("Added term '" + str + "'.");
			}
			guideNode.incTokenCount(tokenCount);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getTotalNodeCount() { return totalNodeCount; }

}