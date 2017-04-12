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
package org.bluemedialabs.io;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class LineReader extends InputStream {
	public final static int BUFFER_SIZE = 128;
	private char[] buffer = new char[BUFFER_SIZE];
	private boolean empty = false;
	private InputStream in;

	public LineReader(InputStream is) {
		in = is;
	}

	/*
	public LineReader(InputStream is, String encoding)
			throws UnsupportedEncodingException {
		super(is, encoding);
	}
	*/

	/**
	 * Reads all characters from the current stream position until a
	 * '\r' + '\n' [or single '\n'] is reached.
	 */
	public String readLine() throws IOException {
		int c;
		char ch, end;
		int count;

		if (empty) {
			return null;
		}
		StringBuffer strBuf = new StringBuffer(256);
		count = 0;
		c = (char) read();
		ch = (char) c;
		while (ch != '\n' && c != -1) {
			while (c != -1 && ch != '\r' && ch != '\n') {
				if (count >= BUFFER_SIZE) {
					strBuf.append(buffer);
					count = 0;
				}
				buffer[count++] = ch;
				c = read();
				ch = (char) c;
			}
			if (ch == '\r') {
				c = read();
				ch = (char) c;
			}
		}
		if (c == -1) {
			empty = true;
		}
		// append last buffer up to cout;
		strBuf.append(buffer, 0, count);
		//System.out.println("%%% Read line '" + strBuf.toString() + "'");
		return strBuf.toString();

		/*
		if (c == -1) {
			end = 0;
		} else {
			end = '\n';
		}

			// read until end of stream


		if (count < BUFFER_SIZE - 1) {
			buffer[count++] = end;
			for (; count < BUFFER_SIZE; count++) {
				buffer[count] = 0;
			}
			strBuf.append(buffer);
		} else {
			strBuf.append(buffer);
			strBuf.append(end);
		}
		System.out.print(">" + strBuf.toString() + "<");
		return strBuf.toString();
		*/
	}

	private void fillBuffer(int pos) {
		for (; pos < BUFFER_SIZE; pos++) {
				buffer[pos] = 0;
		}
	}

	public int read() throws IOException {
		return in.read();
	}

	public static void main(String[] args) {
		LineReader lr = new LineReader(System.in);
		System.out.println("Done.");
	}
}