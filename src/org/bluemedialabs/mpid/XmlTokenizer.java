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

import org.bluemedialabs.util.FlagSet;
import org.bluemedialabs.util.MutableString;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class XmlTokenizer {

//	int[] DELIMITERS = new int{
//		0x0020,     // space (' ')
//		0x002C,     // comma (',')
//		0x002E,     // period ('.')
//		0x003B,     // semi colon (';')
//		0x003A,     // colon (':')
//		0x002D,     // hyphen ('-')
//		0x003C,     // <
//		0x003E,     // >
//		0x0022,     // "
//		0x000D,     // carriage return ('\r')
//		0x000A,     // line feed ('\n')
//		0x0021,     // !
//		0x003F,     // ?
//		0x0027,     // '
//		0x0028,     // left parenthesis ('(')
//		0x0029,     // right parenthesis (')')
//		0x002F,     // slash ('/')  ..more to come...
//		0x0009,     // horizontal tabulation ('\t')
//		0x000B,     // vertical tabulation
//		0x002A,     // *
//		0x005B,     // [
//		0x005D,     // ]
//		0x005F      // _
//};

   /**
	* The maximum number of characters any token may consist of. This is to
	* avoid generating very long tokens for strings whose delimiters are not
	* yet recognized or who are simply garbage from an interpretable text
	* point of view.
	*/
	static public final int MAX_TOKEN_LENGTH = 64;

	/**
	 * The maximum number of continuous digits any number may consits of
	 * before being broken into pieces. This is to avoid numbers of which
	 * quite a few unique ones exist swamp our term dictionary.
	 */
	static public final int MAX_NUMBER_LENGTH = 4;

	FlagSet delims = new FlagSet();
	char[] text;
	int start;  // the start position of the passage to be tokenized
	int len;    // the length of the passage
	int end;
	int pos = 0;
	MutableString token = new MutableString();


	public XmlTokenizer(char[] text, int start, int len) {
		reuse(text, start, len);
		initDelims();
	}

	public XmlTokenizer(char[] text, int start) {
		this(text, start, text.length);
	}

	public XmlTokenizer(char[] text) {
		this(text, 0, text.length);
	}


	public void reuse(char[] text, int start, int len) {
		this.text = text;
		this.start = start;
		this.len = len;
		this.end = start + len;
		pos = start;
		token.reset();
	}

	private void initDelims() {
		setDelimRange(0, 0x2F);     // NUL -- '?'
		setDelimRange(0x3A, 0x40);  // ':' -- '@'
		setDelimRange(0x5B, 0x60);  // '[' -- '''
		setDelimRange(0x7B, 0x7F);  // '{' -- DEL
		// All higher range delimiters will have to be discovered over time.
	}

	private void setDelimRange(int from, int to) {
		for (int i = from; i <= to; i++) {
			delims.set(i);
		}
	}


	public boolean nextToken(MutableString token) {
		int tokenEnd;

		if (pos >= end) {
			// no more tokens to parse
			return false;
		}
		if (isDelimiter(text[pos])) {
			// Just return the single character delimiter,...
			// ..with one exception which are entities = "&...;"
			if (text[pos] == '&') {
				// Try to find number plus ';'
				tokenEnd = pos + 1;
				while (tokenEnd < end && isDigit(text[tokenEnd])) {
					tokenEnd++;
				}
				// The next character has to be a semicolon
				if (tokenEnd < end && text[tokenEnd] == ';') {
					tokenEnd++;
					token.reuse(text, pos, tokenEnd - pos);
					pos = tokenEnd;
					return true;
				}
			}
			// It's just the delimiter...
			token.reuse(text, pos, 1);
			pos++;
		} else if (isDigit(text[pos])) {
			// Parse a non-delimiter character sequence limited to 4 digits
			// in a row
			parseNumber(token, pos);
		} else {
			// Parse a non-delimiter character sequence of at most maximum
			// length
			// HERE WE HAVE A NEW CASE TO HANDLE IDs LIKE catalog123 WHICH
			// SHOULD BEST BE PARSED INTO catalog1 AND 23
			tokenEnd = pos;
			while (++tokenEnd < end && !isDelimiter(text[tokenEnd])
				   && !isDigit(text[tokenEnd])
				   && tokenEnd  - pos <= MAX_TOKEN_LENGTH);
			if (tokenEnd < end && isDigit(text[tokenEnd])) {
				// This is what we are looking for, e.g., person0815
				tokenEnd++;
				// Take just one digit of the text-attached digits
				// and leave the rest for the next token
			} // ...otherwise, it was a delimiter, so nothing special here
			token.reuse(text, pos, tokenEnd - pos);
			pos = tokenEnd;
		}
		return true;
	}


	protected boolean isDelimiter(char ch) {
		boolean b = delims.test((int) ch);
//		System.out.println("'" + ch + "'");
//		if (b) {
//			System.out.println("is a delimiter");
//		} else {
//			System.out.println("is not a delimiter");
//		}
		return b;
	}

//	protected boolean isAlpha(char ch) {
//		return ((ch >= '\u0041'/* A */ && ch <= '\u005A'/* Z */)
//			||  (ch >= '\u0061'/* a */ && ch <= '\u007A'/* z */));
//	}

	protected boolean isDigit(char ch) {
		return (ch >= '\u0030'/* 0 */ && ch <= '\u0039'/* 9 */);
	}

	// Simple solution for now
	protected void parseNumber(MutableString token, int pos) {
		int tokenEnd = pos;
		while (++tokenEnd < end && isDigit(text[tokenEnd])
				&& tokenEnd - pos < MAX_NUMBER_LENGTH);
		token.reuse(text, pos, tokenEnd - pos);
		this.pos = tokenEnd;
	}



	static public void main(String[] args) {
		String text = "123456,444 a6\nafg7ds7;7.56748";
		String text2 = "earn " +
"more about these creatures from the past in the \"Whale's Tales\" family program " +
"offered in conjunction with the Kokoro animated dinosaur exhibit at the Natural " +
"History Museum of Los Angeles County, 1:30 p.m., Jan. 14. Call (213) 744-3335 " +
"or 744-3534. " +
"</P> " +
"<P> " +
"MARINE SCIENCE " +
"</P> " +
"<P> " +
"Bruce Robison, research biologist at the Monterey Bay Aquarium Research " +
"Institute, will discuss the current status and future prospects for deep-sea " +
"research and exploration at the Santa Barbara Museum of Natural History, 7:30 " +
"p.m., Jan. 12. Call (805) 966-7107. " +
"</P> " +
"<P> " +
"The marine food chain and marine life will be explored in a series of films at " +
"the George C. Page Museum, Los Angeles, 1:30 p.m., Jan 10. Call (213) 857-6311. " +
"</P> " +
"<P> " +
"GEOLOGY " +
"</P> " +
"<P> " +
"The California State University Desert Studies Center near Baker is sponsoring " +
"a class Jan. 6-8 on \"The Mojave Desert: The Last 60 Million Years,\" which will " +
"travel to several sites in the area to examine major geological events. The " +
"center is also offering a class on the \"Archeology of the Mojave Desert,\" Jan. " +
"20-22. Call &1111; <fish id=\"fish712\"type=\"shark\"...";

		XmlTokenizer tok = new XmlTokenizer(text2.toCharArray());
		MutableString str = new MutableString();

		while (tok.nextToken(str)) {
			System.out.println(str);
		}
	}
}