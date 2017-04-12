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

//import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
//import org.bluemedialabs.io.LogStream;
//import org.bluemedialabs.util.MutableInteger;
//import org.bluemedialabs.util.MutableString;
//import org.bluemedialabs.util.Pool;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class ContntHandler extends DefaultHandler {
	protected Tokens tokens;


	public ContntHandler(Tokens tokens) {
		this.tokens = tokens;
	}

	/**
	 * Dummy implementation just printing a document start notification message
	 * (if not disabled).
	 */
	public void startDocument() throws SAXException {
		System.out.println("Document start...");
	}


	/**
	 * Dummy implementation just printing a document end notification message
	 * (if not disabled).
	 */
	public void endDocument() throws SAXException {
		System.out.println("Document end...");
	}


	/*+**********************************************************************
	 * ErrorHandler implementation
	 ************************************************************************/

	public void warning(SAXParseException e) throws SAXException {
		System.err.println("SAX warning: " + getExceptionMsg(e));
	}

	public void error(SAXParseException e) throws SAXException {
		String msg = "Error: " + getExceptionMsg(e);
		System.err.println(msg);
		throw new SAXException(msg);
	}

	public void fatalError(SAXParseException e) throws SAXException {
		String msg = "Fatal Error: " + getExceptionMsg(e);
		System.err.println(msg);
		throw new SAXException(msg);
	}

	/**
	 * Returns a string describing parse exception details
	 */
	private String getExceptionMsg(SAXParseException e) {
		StringBuffer buf = new StringBuffer(127);

		if (e.getSystemId() != null) {
			buf.append("URI is '");
			buf.append(e.getSystemId());
			buf.append("', ");
		}
		buf.append("line ");
		buf.append(e.getLineNumber());
		buf.append(": ");
		buf.append(e.getMessage());
		return buf.toString();
	}


	/*+**********************************************************************
	 * Main
	 ************************************************************************/

//	private static void usage() {
//		System.err.println("Usage: SAXTagCount [-v] <filename>");
//		System.err.println("       -v = validation");
//		System.exit(1);
//	}
//
//
//	static public void main(String[] args) {
//		String filename = null;
//		boolean validation = false;
//
//		args = new String[1];
//		args[0] = "/opt/jaxp/examples/samples/two_gent.xml";
//		// Parse arguments
//		for (int i = 0; i < args.length; i++) {
//			if (args[i].equals("-v")) {
//				validation = true;
//			} else {
//				filename = args[i];
//
//				// Must be last arg
//				if (i != args.length - 1) {
//					usage();
//				}
//			}
//		}
//		if (filename == null) {
//			usage();
//		}
//
//		// There are several ways to parse a document using SAX and JAXP.
//		// We show one approach here.  The first step is to bootstrap a
//		// parser.  There are two ways: one is to use only the SAX API, the
//		// other is to use the JAXP utility classes in the
//		// javax.xml.parsers package.  We use the second approach here
//		// because it allows the application to use a platform default
//		// implementation without having to specify a system property.
//		// After bootstrapping a parser/XMLReader, there are several ways
//		// to begin a parse.  In this example, we use the SAX API.
//
//		// Create a JAXP SAXParserFactory and configure it
//		SAXParserFactory spf = SAXParserFactory.newInstance();
//		spf.setValidating(validation);
//
//		XMLReader xmlReader = null;
//		try {
//			// Create a JAXP SAXParser
//			SAXParser saxParser = spf.newSAXParser();
//
//			// Get the encapsulated SAX XMLReader
//			xmlReader = saxParser.getXMLReader();
//		} catch (Exception ex) {
//			System.err.println(ex);
//			System.exit(1);
//		}
//
//		// Set the ContentHandler of the XMLReader
//		xmlReader.setContentHandler(new ContntHandler());
//
//		// Set an ErrorHandler before parsing
//		xmlReader.setErrorHandler(new MyErrorHandler(LogStream.DEFAULT_STREAM));
//
//		try {
//			// Tell the XMLReader to parse the XML document
//			xmlReader.parse(convertToFileURL(filename));
//			System.out.println("Next run...");
//			xmlReader.parse(convertToFileURL(filename));
//		} catch (SAXException se) {
//			System.err.println(se.getMessage());
//			System.exit(1);
//		} catch (IOException ioe) {
//			System.err.println(ioe);
//			System.exit(1);
//		}
//	}

}