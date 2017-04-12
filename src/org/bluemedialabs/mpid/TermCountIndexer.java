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
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import org.bluemedialabs.mpid.ival.*;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;



/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TermCountIndexer extends Indexer {
	static private final int MAX_DEPTH = 128;

	private HashMap stopWords;
	private int totalCounts = 0;
	private int oneCounts = 0;
	private int nCounts = 0;
	private int swCounts = 0;
	private long bitSize = 0;
	private long swBitSize = 0;


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public void index(String cfgName, Configuration config)
			throws IOException {
		System.out.println("\n__________________TermCountIndexer___________________");
		Args args;
		TermCountIndexer indexer;
		String indexTemp = config.getProperty(cfgName, "IndexTemp");
		String termFileBaseName =
				indexTemp + "/" + config.getProperty(cfgName, "PidTermFileBaseName")
				+ ".data";  // This should be improved...
		int inMemTermDfListElems =
				config.getIntProperty(cfgName, "PidInMemTermDfListElems");
		Terms terms = Terms.load(config, cfgName);
		StopWords sw = new StopWords(config.getProperty(cfgName, "StopWordFileName"));
		HashMap stopWords = new HashMap(200);
		Iterator it = sw.iterator();
		int termNo;
		while (it.hasNext()) {
			termNo = terms.getNo((String) it.next());
			stopWords.put(new MutableInteger(termNo), null);
		}
		terms = null;

		args = createArgs(config.getProperty(cfgName, "SourceHome"));
//		outSeq = new DataFileOutputChannelSeq(termFileBaseName, 64000);
//		termLists = new DfLists(new PathId(), inMemTermDfListElems,
//								args.termCount + 1, outSeq, args.guide,
//								new TermDfList(5000, args.guide), false, true);

		indexer = new TermCountIndexer(args.xds, args.guide, stopWords);
		indexer.start();
//		indexer.finalize();
		System.out.println(indexer.toString());
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public TermCountIndexer(XmlDecoderStream in, DataGuide guide,
							HashMap sw) {
		super(in, guide);
		stopWords = sw;
	}


	public void onAttribute(GuideNode node, HashMap termCounts, StopWords sw) {
		handleTermCounts(node, termCounts, true);
	}

	private void handleTermCounts(GuideNode node, HashMap termCounts,
									boolean attrib) {
		if (termCounts.size() > 0) {
			// If there are any terms to index...
			Iterator it = termCounts.entrySet().iterator();
			Map.Entry entry;
			MutableInteger termNo;
			MutableInteger counter;
			int size;

			while (it.hasNext()) {
				entry = (Map.Entry) it.next();
				termNo = (MutableInteger) entry.getKey();
				counter = (MutableInteger) entry.getValue();
				totalCounts++;
				if (counter.getValue() > 1) {
					nCounts++;
					size = node.getTotalPosBitLen();
					size += 8;
					if (stopWords.containsKey(termNo)) {
						swCounts++;
						swBitSize += size;
					}
					bitSize += size;
				} else {
//					oneCounts++;
				}
			}
		}
	}


	public void onElementStart(GuideNode node) {
	}

	public void onElementEnd(GuideNode node, HashMap termCounts) {
		handleTermCounts(node, termCounts, false);
	}


	public int getTotalCounts() { return totalCounts; }
	public int getOneCounts() { return oneCounts; }
	public int getNCounts() { return nCounts; }
	public int getSwCounts() { return swCounts; }


	public String toString() {
		int byteSize = (int) (bitSize / 8);
		int byteSize2 = (int) ((bitSize - swBitSize) / 8);

		return "\ntotalCounts=" + totalCounts +
				"\noneCounts=" + oneCounts +
				"\nnCounts=" + nCounts +
				"\nswCounts=" + swCounts +
				"\nCounters that need to get stored: " + (nCounts - swCounts) +
				"\nTC-index size: " + (byteSize / 1024) + " Kb = " +
					(byteSize / 1048576) + " Mb" +
				"\nTC-index size (- s.w.'s): " + (byteSize2 / 1024) + " Kb = " +
					(byteSize2 / 1048576) + " Mb"
			   ;
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	public static void main(String[] args) throws Exception {
		StopWatch watch = new StopWatch();
		Configuration config = Configuration.load(args);
		String cfgName = args[0];

		watch.start();
		TermCountIndexer.index(cfgName, config);
		watch.stop();
		System.out.println("Complete elapsed time: " + watch);
	}
}