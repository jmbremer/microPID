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

import java.io.IOException;
import java.io.Reader;
import org.bluemedialabs.util.MutableString;


/**
 * <p>
 * ...The maximum length supported for any text element (tag, content text)
 * is 32K. This is the maximum length of Strings in Java (at least when written
 * to disk as there a short is used to store the length--though this is not
 * necessarily the length of the string which is expected to be shorter if some
 * non-ASCII characters are used).</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class XmlParser {
	private Reader input;
	/**
	 * If 32K is the maximum number of characters of Strings (on disk), then
	 * a 64K long buffer guarantees us that we will always find text components
	 * in the original document that start before 32K in the buffer completly
	 * in the buffer so that no splitting of text tokens returned is required...
	 */
	private char[] buf = new char[64 * 1024];
	private int start = 0;      // current start position of new text
	private int available = 0;  // number of characters available in buffer
	private Token.Type lastTokenType;

	public XmlParser(Reader input) throws IOException {
		if (input == null) {
			throw new NullPointerException("Input to be parsed may not be null");
		}
		this.input = input;
		readMore();
		lastTokenType = Token.Type.EndTag;
	}

	private void readMore() throws IOException {
		// a) copy remaining few characters to beginning of buffer (if necessary)
		if (start < available) {
			System.arraycopy(buf, start, buf, 0, available - start);
			start = 0;
		}
		// b) fill buffer up to 64k
		input.read(buf, start, 64 * 1024 - available);
	}

	public Token nextToken() {
		// HERE, JUST A LITTLE BIT IS MISSING YET ;-)
		return null;
	}


	static public class Token {
		MutableString str = new MutableString();
		Type type = null;

		public Token() {}

		public void set(Type type, char[] text, int start, int len) {
			type = type;
			str.reuse(text, start, len);
		}

		public Type getType() {
			return type;
		}

		public MutableString getText() {
			return str;
		}

		static public class Type {
			static public final Type StartTag;
			static public final Type EndTag;
			static public final Type Text;

			static private int START_TAG    = 1;
			static private int END_TAG      = 2;
			static private int TEXT         = 3;

			private int t;

			static {
				StartTag= new Type(START_TAG);
				EndTag  = new Type(END_TAG);
				Text    = new Type(TEXT);
			}

			private Type(int type) {
				t = type;
			}
		} // Type
	}

}