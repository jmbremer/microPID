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
package org.bluemedialabs.util;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;


/**
 * <p>Prints a comma-separated list of strings using tabs. The list is enclosed
 * by '(' and ')'. Used for the standard debug output format produced by many
 * (old) application classes.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class ListFormat {

	static public void prettyPrint(String str) {
		prettyPrint(str, new PrintWriter(new OutputStreamWriter(System.out)));
	}

	static public void prettyPrint(String str, PrintWriter out) {
	    int INC = 2;        
	    String EMPTY = new String("                            ");
	    // EMPTY poses a limit on the nesting depth! Going over that limit
	    // will result in a StringIndexOutOfBoundsException!
	    int length = str.length();
	    int currInc = 0;
	    char ch;
        for (int i = 0; i < length; i++) {
            ch = str.charAt(i);
            if (ch == '(') {
                if (currInc > EMPTY.length()) {
                    // Just double the size of EMPTY!
                    EMPTY += EMPTY;
                }
                out.print("\n" + EMPTY.substring(0, currInc));
                out.print(ch);
                currInc += INC;
            } else if (ch == ')') {
                currInc -= INC;
                out.print(ch);
            } else if (ch == ',' /*&& str.charAt(i-1) == ']'*/) {
                out.print(ch + "\n" + EMPTY.substring(0, currInc - 2));
            } else {
                out.print(ch);
            }
        }
        out.print("\n");
	}

	private ListFormat() {}

}