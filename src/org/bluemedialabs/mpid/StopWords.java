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

import java.io.*;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedList;
import org.bluemedialabs.util.MutableInteger;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class StopWords {
	private String fileName;
	private int termCount = 0;
	private HashMap words = null;

	public StopWords(String fileName) throws FileNotFoundException, IOException {
		this.fileName = fileName;
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		loadWords(br);
		br.close();
	}

	private void loadWords(BufferedReader br) throws IOException {
		String line;
		int lineCount = 0;
		LinkedList list = new LinkedList();
		Iterator it;

		line = br.readLine();
		while (line != null) {
			lineCount++;
			line = line.trim().intern();
			list.add(line);
			line = br.readLine();
		}
		words = new HashMap((int) (lineCount / 0.6));
		termCount = lineCount;
		it = list.iterator();
		while (it.hasNext()) {
			words.put(((String) it.next()).toLowerCase(), null);
			// The mutable integer can be used for counting anything of
			// interest in the context of stop words at some point in time
			// and until then can serve as indicator.
		}
	}

	public boolean isStopWord(String word) {
		return words.containsKey(word);
	}

	public int getTermCount() {
		return termCount;
	}

	public Iterator iterator() {
		return words.keySet().iterator();
	}
}