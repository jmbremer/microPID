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

import org.bluemedialabs.util.MutableString;


/**
 *
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class EnglishTokenizer {
	static private final boolean lowerCase = true;
	char[] text;
	int start;  // the start position of the passage to be tokenized
	int len;    // the length of the passage
	int end;
	int pos = 0;
	MutableString token = new MutableString();


	public EnglishTokenizer(char[] text, int start, int len) {
		reuse(text, start, len);
	}

	public EnglishTokenizer(char[] text, int start) {
		this(text, start, text.length);
	}

	public EnglishTokenizer(char[] text) {
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


	public boolean nextToken(MutableString token) {
		int tokenEnd;

		// remove any delimiters from the current position
		while (pos < end && !isAlpha(text[pos]) && !isDigit(text[pos])) {
			pos++;
		}
		if (pos >= end) {
			// no more tokens to parse
			return false;
		} else {
			// some tokens left
			if (isAlpha(text[pos])) {
				// look for end of alpha characters
				tokenEnd = pos;
				while (++tokenEnd < end && isAlpha(text[tokenEnd]));
				token.reuse(text, pos, tokenEnd - pos);
				if (lowerCase) {
					token.toLowerCase();
				}
//				System.out.print("Alpha token from " + pos + " to " + tokenEnd
//					+ ": ");
//				for (int i = pos; i < tokenEnd; i++) {
//					System.out.print(text[i]);
//				}
//				System.out.println();
			} else if (isDigit(text[pos])) {
				while (pos < end && text[pos] == '\u0030'/* 0 */) pos++;
				tokenEnd = pos;
				while (++tokenEnd < end && isDigit(text[tokenEnd]));
//				token.reuse(text, pos, tokenEnd - pos);
					token.reuse("<number>".intern());
			} else {
				throw new IllegalStateException("Something is wrong with the "
					+ "tokenization...");
			}
			pos = tokenEnd;
			return true;
		}
	}


	static protected boolean isAlpha(char ch) {
		return ((ch >= '\u0041'/* A */ && ch <= '\u005A'/* Z */)
			||  (ch >= '\u0061'/* a */ && ch <= '\u007A'/* z */));
	}

	static protected boolean isDigit(char ch) {
		return (ch >= '\u0030'/* 0 */ && ch <= '\u0039'/* 9 */);
	}


/*
 * For Java, the following should be accomplished using the java.text package.
 * (Same with dates!)
 *
void EnglishTokenizer::parseNumber(int& pos, UnicodeString& token) {
	static char num[30];    // :-( but should be ok for now
	int tokenEnd = pos + 1;
	int digitCount = 1;
	while (tokenEnd < length && isDigit(text[tokenEnd])) {
		tokenEnd++;
		digitCount++;
	}
	// Now, check whether there is a non-digit character next
	// (presumably ',', '.', or ' ') followed by further digits.
	// If so, we assume that the number goes on after the non-digit
	// character.
	// (2001-12-03
	//  removed the digitCount++ because is seems to make more sense to
	//  just count everything before a possible decimal point; however,
	//  parsing the number to the end still seems to make sense)
	while (tokenEnd < length - 1 && isDigit(text[tokenEnd + 1])) {
		tokenEnd = tokenEnd + 2;
//		digitCount++;
		while (tokenEnd < length && isDigit(text[tokenEnd])) {
			tokenEnd++;
//			digitCount++;
		}
	}
	// build our special number class representation from the number of
	// digits counted and adjust position for the next token
	sprintf(num, NUMBER_CLASS_FORMAT, digitCount);
	token.setTo(UNICODE_STRING(num, strlen(num)));
	pos = tokenEnd;
}
 */


/*+**************************************************************************
 * Token (for future use)
 ****************************************************************************/

	static public class Token {
		static public final int TEXT      = 1;
		static public final int NUMBER    = 2;
		static public final int INTEGER   = 9;
		MutableString text; // string representation if of no other type
		double number;      // number representation if number
		int integer;        // ..special number
		int type;           // which one is it?

		public Token() {}

		public void setText(MutableString text) {
			this.text = text;
			type = TEXT;
		}

		public void setNumber(double number) {
			this.number = number;
			type = NUMBER;
		}

		public void setInteger(int number) {
			this.integer = integer;
			type = INTEGER;
		}

		public String toString() {
			String str;
			// make sure all numbers use the same format
			// when represented as string
			switch (type) {
				case TEXT:	    str = text.toString(); break;
				case NUMBER:    str = String.valueOf(number); break;
				case INTEGER:   str = String.valueOf(integer); break;
				default:    throw new IllegalStateException("Unsupported token "
					+ "type!?...");
			}
			return str;
		}
	}

}