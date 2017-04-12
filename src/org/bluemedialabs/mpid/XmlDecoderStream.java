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

import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import org.bluemedialabs.io.BitInput;
import org.bluemedialabs.util.Quack;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class XmlDecoderStream {
	static private final int INITIAL_STRBUF_LENGTH = 8096;
	private BitInput is = null;
	private HuffmanCoder hc = null;
	private Decoder decoder = null;
	private boolean pureDecode;
	private Quack elemStack = null;
	private Token token = null;
	private String name;        // Variable for token name
	private StringBuffer strBuf = new StringBuffer(INITIAL_STRBUF_LENGTH);

	private int depth = 0;


	public XmlDecoderStream(BitInput is, Tokens tokens) {
		this.is = is;
		hc = new HuffmanCoder(tokens);
//		System.out.println("Huffman coder is:\n" + hc);
		elemStack = new Quack();
		pureDecode = false;
	}

	public XmlDecoderStream(BitInput is, Decoder d) {
		this.is = is;
		decoder = d;
//		System.out.println("Decoder is:\n");
		pureDecode = true;
	}


	public void reuse(BitInput is) {
		this.is = is;
		if (elemStack != null) {
			elemStack.clear();
		}
	}


	/**
	 * Reads the next token(s) as a string from the underlying stream. Usually,
	 * only a single token will be returend. But, in some cases a sequence of
	 * tokens needs to be read in order to re-construct the original data. In
	 * that case, as many tokens as necessary are combined to one string and
	 * returned.
	 *
	 */
	public String read() throws IOException {
		String str;
		if (token == null) {
			token = nextToken();
			if (token == null) {
				// Check wether there are still elements on the stack
				// JUST AN UNNECCESSARY HACK NOW:
//                if (!elemStack.isEmpty()) {
//                    name = (String) elemStack.pop();
//				    strBuf.setLength(0);
//				    strBuf.append("</");
//				    strBuf.append(name.substring(1));
//				    strBuf.append('>');
//                    return strBuf.toString();
//                }
				return null;
			}
		}
		// else: the last read look ahead already produced the next token
		name = token.getName();
//		System.out.print("..next token is '" + name + "'..");
//		System.out.flush();
		if (name.charAt(0) == '<' && name.length() > 1) {
			// Element name or other special token...
			// ..have to do some look-ahead
			if (name.charAt(1) == '/') {
				// It's the element end tag
				name = (String) elemStack.pop();
//				strBuf.delete(0, strBuf.length());
				strBuf.setLength(0);
				strBuf.append("</");
//				if (name.compareTo("<SUBJECT") == 0) {
//					System.out.print(" *just before subject* ");
//				}
				strBuf.append(name.substring(1));
//				else System.out.println("\n" + elemStack + "\n");
				strBuf.append('>');
				token = null;

				depth--;
			} else {
				depth++;

//				System.out.println("\n### Pushing element " + name);
				elemStack.push(name);
//				strBuf.delete(0, strBuf.length());
				strBuf.setLength(0);
				strBuf.append(name);
				token = nextToken();
				if (token == null) {
					// Have a starting element here but nothing else!?
					throw new IllegalStateException("Just read token '"
						+ token.getName() + " from the underlying bit input stream, "
						+ "but now the stream appears to be empty");
				}
				name = token.getName();
				if (name.charAt(0) == '@' && name.length() > 1) {
					// Have some attributes
					parseAttributes(strBuf);
				} else {
					// No attributes in sight
					strBuf.append('>');
				}
			}
			str = strBuf.toString();

		} else {
			// Nothing special, just print this token as it is
			str = name;
			token = null;
		}
		return str;
	}


	/**
	 *
	 * @param attribName
	 * @param strBuf
	 * @throws IOException
	 */
	public void readFragment(String attribName, StringBuffer strBuf)
			throws IOException {
		int depth = 0;
		String str;
		String name;

		token = nextToken();
		if (token == null) {
			throw new IllegalStateException("Expecting DF start but next token "
					+ "is null");
		}

		strBuf.setLength(0);

		if (attribName != null) {
			// Skip over parent element's structure and any attributes with
			// other name
			name = "@" + attribName;
			while (token != null
				   && token.getName().compareToIgnoreCase(name) != 0
				   && token.getName().compareTo("</>") != 0) {
				token = nextToken();
			}
			if (token.getName().compareTo("</>") == 0) {
				strBuf.append("<Error: attribute '" + name + "' not present>");
				return;
			}
		} else {
			// For a regular element, procede as usual
			name = token.getName();
		}

		if (name.charAt(0) == '@' && name.length() > 1) {
			// DF consists of only a single attribute, thus...
			// ..just extract and return this attribute
			strBuf.append(name);
			strBuf.append("=\"");
			// Read attribute value
			attribName = name;
			token = nextToken();
			if (token == null) {
				// Have a starting element here but nothing else!?
				throw new IllegalStateException("Just read attribute '"
					+ attribName + " from the underlying bit input stream, "
					+ "but now the stream appears to be empty");
			}
			name = token.getName();
			while (!(name.charAt(0) == '@' && name.length() > 1
					|| name.compareTo("<@>") == 0)) {
				// Piece together attribute value
				strBuf.append(name);
				token = nextToken();
				if (token == null) {
					// Have a starting element here but nothing else!?
					throw new IllegalStateException("Input stream exhausted "
						+ "while reading value for attribute '"
						+ attribName + "'");
				}
				name = token.getName();
			}
			strBuf.append("\"");
//			return strBuf.toString();
		}

		else if (name.charAt(0) == '<' && name.length() > 1
				 && name.charAt(1) != '/') {
			// Regular DF consisting of an XML element

			// Make sure the element stack doesn't accumulate unused items
			if (!elemStack.isEmpty()) {
				elemStack.clear();
			}
			elemStack.push(name);
			strBuf.append(name);
			// Fetch next token
			token = nextToken();
			name = token.getName();
			// Handle attributes, if there are any
			if (name.charAt(0) == '@' && name.length() > 1) {
				// An element with attributes
				parseAttributes(strBuf);
				// Fetch next token
				token = nextToken();
				name = token.getName();
			} else {
				strBuf.append('>');
			}
			depth = 1;

			while (depth > 0) {
				if (token == null) {
					// Token stream ends before coming to the DF end
					throw new IllegalStateException("No more tokens although "
							+ "DF end has not been reached yet; DF text so far "
							+ "is:\n" + strBuf.toString());
				}

				if (name.compareTo("</>") == 0) {
					// It's an element end tag
					if (elemStack.isEmpty()) {
						throw new IllegalStateException("Displaced fragment; "
								+ "closing tag without opening tag");
					}
					name = (String) elemStack.pop();
					strBuf.append("</");
					strBuf.append(name.substring(1));
					strBuf.append('>');
					// Fetch next token
					token = nextToken();
					// This should be checked in the other cases, too.
					// (We know that token == null should really only happen
					//  at the very end of a source, though.)
					if (token != null) name = token.getName();
					depth--;
				} else if (name.charAt(0) == '<' && name.length() > 1) {
					// An element start tag
					depth++;
					elemStack.push(name);
					strBuf.append(name);
					// Fetch next token
					token = nextToken();
					name = token.getName();
					if (name.charAt(0) == '@' && name.length() > 1) {
						// An element with attributes
						parseAttributes(strBuf);
						// Fetch next token
						token = nextToken();
						name = token.getName();
					} else {
						strBuf.append('>');
					}

				} else {
					// Any other token
					strBuf.append(name);
					// Fetch next token
					token = nextToken();
					name = token.getName();
				}
			} // END: while
		}

		else {
			String next = new String();
			for (int i = 0; i < 15; i++) {
				next += nextToken().getName() + "', '";
			}
			throw new IllegalStateException("The first token found while trying "
					+ "to read a DF ("
					+ (attribName == null? "not an attribute":
						   ("attribute " + attribName))
					+  ") is '" + name + "', which is not the start "
					+ "of an XML element or attribute (This only means that the "
					+ "current start position was probably out of alignment; "
					+ "the next tokens are: '" + next +"...', ...)");
		} // END: else
//		return strBuf.toString();  -- now using supplied string buffer
	}


	public void printFragment(String attribName, PrintWriter out)
			throws IOException {
		int depth = 0;
		String str;
		String name;

		token = nextToken();
		if (token == null) {
			throw new IllegalStateException("Expecting DF start but next token "
					+ "is null");
		}
		if (attribName != null) {
			// Skip over parent element's structure and any attributes with
			// other name
			name = "@" + attribName;
			while (token != null
				   && token.getName().compareToIgnoreCase(name) != 0
				   && token.getName().compareTo("</>") != 0) {
				token = nextToken();
			}
			if (token.getName().compareTo("</>") == 0) {
				out.print("<attribute '" + name + "' not present>");
			}
		} else {
			// For a regular element, procede as usual
			name = token.getName();
		}

		if (name.charAt(0) == '@' && name.length() > 1) {
			// DF consists of only a single attribute, thus...
			// ..just extract and return this attribute
			out.print(name);
			out.print("=\"");
			// Read attribute value
			attribName = name;
			token = nextToken();
			if (token == null) {
				// Have a starting element here but nothing else!?
				throw new IllegalStateException("Just read attribute '"
						+ attribName + " from the underlying bit input stream, "
						+ "but now the stream appears to be empty");
			}
			name = token.getName();
			while (!(name.charAt(0) == '@' && name.length() > 1
					 || name.compareTo("<@>") == 0)) {
				// Piece together attribute value
				out.print(name);
				token = nextToken();
				if (token == null) {
					// Have a starting element here but nothing else!?
					throw new IllegalStateException("Input stream exhausted "
							+ "while reading value for attribute '"
							+ attribName + "'");
				}
				name = token.getName();
			}
			out.print("\"");
//			return strBuf.toString();
		}

		else if (name.charAt(0) == '<' && name.length() > 1
				 && name.charAt(1) != '/') {
			// Regular DF consisting of an XML element

			// Make sure the element stack doesn't accumulate unused items
			if (!elemStack.isEmpty()) {
				elemStack.clear();
			}
			elemStack.push(name);
			out.print(name);
			// Fetch next token
			token = nextToken();
			name = token.getName();
			// Handle attributes, if there are any
			if (name.charAt(0) == '@' && name.length() > 1) {
				// An element with attributes
				printAttributes(out);
				// Fetch next token
				token = nextToken();
				name = token.getName();
			} else {
				out.print('>');
			}
			depth = 1;

			while (depth > 0) {
				if (token == null) {
					// Token stream ends before coming to the DF end
					throw new IllegalStateException("No more tokens although "
							+ "DF end has not been reached yet; DF text so far "
							+ "is:\n" + strBuf.toString());
				}

				if (name.compareTo("</>") == 0) {
					// It's an element end tag
					if (elemStack.isEmpty()) {
						throw new IllegalStateException("Displaced fragment; "
								+ "closing tag without opening tag");
					}
					name = (String) elemStack.pop();
					out.print("</");
					out.print(name.substring(1));
					out.print('>');
					// Fetch next token
					token = nextToken();
					// This should be checked in the other cases, too.
					// (We know that token == null should really only happen
					//  at the very end of a source, though.)
					if (token != null) name = token.getName();
					depth--;
				} else if (name.charAt(0) == '<' && name.length() > 1) {
					// An element start tag
					depth++;
					elemStack.push(name);
					out.print(name);
					// Fetch next token
					token = nextToken();
					name = token.getName();
					if (name.charAt(0) == '@' && name.length() > 1) {
						// An element with attributes
						printAttributes(out);
						// Fetch next token
						token = nextToken();
						name = token.getName();
					} else {
						out.print('>');
					}

				} else {
					// Any other token
					out.print(name);
					// Fetch next token
					token = nextToken();
					name = token.getName();
				}
			} // END: while
		}

		else {
			String next = new String();
			for (int i = 0; i < 15; i++) {
				next += nextToken().getName() + "', '";
			}
			throw new IllegalStateException("The first token found while trying "
					+ "to read a DF is '" + name + "', which is not the start "
					+ "of an XML element or attribute (This only means that the "
					+ "current start position was probably out of alignment; "
					+ "the next tokens are: '" + next +"...', ...)");
		} // END: else
//		return strBuf.toString();
	}


	public Token nextToken() throws IOException {
		Token token = null;
		try {
			token = (Token) hc.decode(is);
		} catch (IOException e) {
			if (e instanceof EOFException) {
				// That's ok and shows us that we have read everything there
				// is to read
			} else {
				// That's something we cannot handle
				throw e;
			}
		}
		return token;
	}

	public int nextCode() throws IOException {
//		System.out.print(" " + getBitPosition() + "->");
		return decoder.decode(is);
		// Exceptions to be caught outside for speed!?!
	}

	private void parseAttributes(StringBuffer buf) throws IOException {
		String attribName;
		name = token.getName();
		while (name.charAt(0) == '@' && name.length() > 1) {
			// Another attribute
			buf.append(' ');
			buf.append(name.substring(1));
			buf.append("=\"");
			// Read attribute value
			attribName = name;
			token = nextToken();
			if (token == null) {
				// Have a starting element here but nothing else!?
				throw new IllegalStateException("Just read attribute '"
					+ attribName + " from the underlying bit input stream, "
					+ "but now the stream appears to be empty");
			}
			name = token.getName();
			while (!(name.charAt(0) == '@' && name.length() > 1
					|| name.compareTo("<@>") == 0)) {
				// Piece together attribute value
				buf.append(name);
				token = nextToken();
				if (token == null) {
					// Have a starting element here but nothing else!?
					throw new IllegalStateException("Input stream exhausted "
						+ "while reading value for attribute '"
						+ attribName + "'");
				}
				name = token.getName();
			}
			buf.append("\"");
		}
		token = null;   // ..to make sure the <@> is not interpreted again
		buf.append('>');
	}


	private void printAttributes(PrintWriter out) throws IOException {
		String attribName;
		name = token.getName();
		while (name.charAt(0) == '@' && name.length() > 1) {
			// Another attribute
			out.print(' ');
			out.print(name.substring(1));
			out.print("=\"");
			// Read attribute value
			attribName = name;
			token = nextToken();
			if (token == null) {
				// Have a starting element here but nothing else!?
				throw new IllegalStateException("Just read attribute '"
					+ attribName + " from the underlying bit input stream, "
					+ "but now the stream appears to be empty");
			}
			name = token.getName();
			while (!(name.charAt(0) == '@' && name.length() > 1
					|| name.compareTo("<@>") == 0)) {
				// Piece together attribute value
				out.print(name);
				token = nextToken();
				if (token == null) {
					// Have a starting element here but nothing else!?
					throw new IllegalStateException("Input stream exhausted "
						+ "while reading value for attribute '"
						+ attribName + "'");
				}
				name = token.getName();
			}
			out.print("\"");
		}
		token = null;   // ..to make sure the <@> is not interpreted again
		out.print('>');
	}


	public long getBitPosition() {
		return is.bitPosition();
	}


	/**
	 * Expects the underlying bit input stream to be positioned right at the
	 * start of an XML element. The element and all enclosing elements with
	 * their content will be returned then.
	 */
//	public void readFragment(StringBuffer strBuf) {

//	}


	/**
	 * Expects the underlying bit input stream to be positioned right at the
	 * start of an XML leaf node's value. Thus, all tokens until the end of
	 * the current element will be read and returned.
	 */
	public void readValue(StringBuffer strBuf) {

	}



	public static void main(String[] args) {
//		XmlDecoderStream xmlDecoderStream1 = new XmlDecoderStream();
	}

}