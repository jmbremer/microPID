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
import java.util.*;
import org.bluemedialabs.util.StopWatch;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Trec {

	public Trec() {
	}


	static void p(String msg) {
		System.out.print(msg);
	}

	static void pl(String msg) {
		System.out.println(msg);
	}


	static public void extractFtQrels() throws IOException {
		final String IN_FILE = "F:/Data/qrels.trec8.adhoc.parts1-5.txt";
		final String OUT_FILE = "F:/Data/qrels.trec8.adhoc.ft.txt";
		BufferedReader in = new BufferedReader(new FileReader(IN_FILE));
		BufferedWriter out = new BufferedWriter(new FileWriter(OUT_FILE));
		String line;

		while ((line = in.readLine()) != null) {
			if (line.substring(6).startsWith("FT")) {
				out.write(line);
				out.newLine();
			}
		}
		out.close();
		in.close();
	}


	static public void main(String[] args) throws Exception {
		StopWatch watch = new StopWatch();
		watch.start();

		try {
//		extractFtQrels();
//		System.exit(0);

		// Prepare query engine...
		p("Loading stop words...");
		StopWords sw = new StopWords("F:/Data/English stopwords.txt");
		pl("time " + watch);
		p("Initializing InQuery engine...");
		InQueryEngine qe = new InQueryEngine("F:/Data/tmp", sw);
		pl("time " + watch);

		// Load topics...
		p("Loading topics...");
		Collection c = TrecTopic.loadTopics("F:/Data/topics.401-450.txt");
		pl("time " + watch);

		// Walk through all topics...
		p("Querying for topics...");
		Iterator it = c.iterator();
		TrecTopic topic;
		LinkedList li = new LinkedList();
		PrintStream ps = new PrintStream(new FileOutputStream(
				"F:/Data/TRECresults401-450.txt"));
		int i = 0;
		while (it.hasNext()) {
			topic = (TrecTopic) it.next();
			p(topic.getNumber() + "..");
			qe.rankDocuments(topic.getTitle()/*getDescription()*/, li);
			// Write results to file
			int rank = 0;
			RankedDoc doc;
			Iterator docIt = li.iterator();
			while (docIt.hasNext()) {
				doc = (RankedDoc) docIt.next();
				ps.println(doc.toTrecString(topic.getNumber(), ++rank,
						"InQuery-2002-3-5"));
			}
			ps.println("\n");
			if (++i % 10 == 0) {
				pl("");
				pl("    Time " + watch);
			}
		}

		// Print final time
		watch.stop();
		pl("\nTOTAL TIME  " + watch);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}