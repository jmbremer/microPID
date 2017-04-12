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
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.util.*;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.Configuration;
import org.bluemedialabs.util.MutableInteger;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class IrStatsIndexer extends Indexer {
	static private final int SOURCE_MAX_DEPTH = 60;

	private Terms terms;
	private String termFileName;
	private String irTermFileName;
	private MutableInteger termNo;
	private Term term;
	private boolean passOne = true;
	private int[] dfWordCount = new int[SOURCE_MAX_DEPTH];


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public void index(Configuration config, String cfgName)
				throws IOException {
		String fileName = config.getProperty(cfgName, "SourceHome") + "/"
				  +config.getProperty(cfgName, "TermFileBaseName");
		String comprSrcFileName = config.getProperty(cfgName, "SourceHome") + "/"
				  +config.getProperty(cfgName, "ComprSourceFileName");
		String irFileName = config.getProperty(cfgName, "SourceHome") + "/"
				  +config.getProperty(cfgName, "IrTermFileBaseName");

		String dgFileName =  config.getProperty(cfgName, "SourceHome") + "/"
		   +config.getProperty(cfgName, "DataGuideFileName");
//		Terms terms = Terms.load(config, cfgName);  -- DISABLED
		Terms terms = null;
		index(config.getProperty(cfgName, "SourceHome"), terms, irFileName,
			  dgFileName, comprSrcFileName, null);
//		Terms.dumpTermFile(config, cfgName, true);  -- Why does this not work???

	}

	static public void index(String repDir, Terms terms,
			String irTermFileName, String dgFileName, String comprSrcFileName,
			PrintStream ps) throws IOException {
		boolean passOne = true;
		Args args = createArgs(repDir);
		IrStatsIndexer indexer = new IrStatsIndexer(args, terms, passOne);
		indexer.out = null;  // ...= null to make sure stats are not overwritten
		System.out.println("First pass over source (basic counters)...");
		indexer.start();
		try {
			indexer.finalize();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		// Store DataGuide to preserve information already collected
		System.out.print("\nStoring DataGuide...");
		BufferedOutputChannel ch = BufferedOutputChannel.create(dgFileName);
		args.guide.store(ch);
		ch.flush();
		ch.close();
		System.out.println("done.");
		// Store IR terms, if so desired
		// IR TERMS ARE DISABLED, BECAUSE TERMS SHOULD ALWAYS BE IR-STYLE,
		// I.E., NON-TERMS SHOULD BE MARKED AS SPECIAL TERMS WHEN INDEXING
		// THE VERY FIRST TIME!
//		System.out.print("Storing IR Terms...");
//		IndexSeqOutputChannel out = IndexSeqOutputChannel.create(
//				irTermFileName, false);
//		terms.store(out);
//		out.close();
		// Now it's time for the second pass
		passOne = false;
		// Reopen input stream
		BufferedInputChannel bic = BufferedInputChannel.create(
					comprSrcFileName);
		BitDataInput input = new BitDataInput(bic);
		args.xds.reuse(input);
		indexer = new IrStatsIndexer(args, terms, passOne);
		indexer.out = null;
		System.out.println("Second pass over source (derived statistics)...");
		indexer.start();
		try {
			indexer.finalize();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		// Store DataGuide again for the complete set of statistics added
		System.out.print("\nStoring DataGuide again...");
		ch = BufferedOutputChannel.create(dgFileName);
		args.guide.store(ch);
		ch.flush();
		ch.close();
		System.out.println("done.");
	}

	static public void index(String repDir) throws IOException {
		throw new IllegalStateException("This function may not be used here");
//		index(repDir, System.out);
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public IrStatsIndexer(XmlDecoderStream in, DataGuide guide,
						  Terms terms, boolean passOne) throws IOException {
		super(in, guide);
		this.terms = terms;
		this.passOne = passOne;
		for (int i = 0; i < SOURCE_MAX_DEPTH; i++) {
			dfWordCount[i] = 0;
		}
	}

	public IrStatsIndexer(Args args, Terms terms, boolean passOne)
			throws IOException {
		this(args.xds, args.guide, terms, passOne);
	}


	/**
	 * This method may potentially be very harmfull!!!
	 * IMMEDIATE ATTENTION IS NECESSARY, IF THIS CODE SHOULD
	 * EVER BE USED AGAIN!!!
	 *
	 * @throws Throwable
	 */
	public void finalize() throws Throwable {
		super.finalize();
		if (passOne) {
			// Compute exectations
			Iterator it = getDataGuide().iterator();
			while (it.hasNext()) {
				GuideNode n = (GuideNode) it.next();
				n.setExpWordCount(n.getTotalWordCount() / n.getCount());
				n.setExpDfWordCount(n.getTotalDfWordCount() / n.getCount());
				n.setExpTermCount(n.getTotalTermCount() / n.getCount());
				// Some unrelated corrections
				if (n.getMinWordCount() == Integer.MAX_VALUE) {
					n.setMinWordCount(0);
				}
				if (n.getMinDfWordCount() == Integer.MAX_VALUE) {
					n.setMinDfWordCount(0);
				}
				if (n.getMinTermCount() == Integer.MAX_VALUE) {
					n.setMinTermCount(0);
				}
				if (n.getMinChildCount() == Integer.MAX_VALUE) {
					n.setMinChildCount(0);
				}
			}
			// Accumulate total DF counters!??...

		}
	}


	public void onElementStart(GuideNode node) {
	}

	public void onElementEnd(GuideNode node, HashMap termCounts) {
		handleStats(node, termCounts, false);
	}

	public void onAttribute(GuideNode node, HashMap termCounts) {
		handleStats(node, termCounts, true);
	}


	protected void handleStats(GuideNode node, HashMap termCounts,
							   boolean attrib) {
		int wordCount = 0;  // For # of non-unique and unique terms
		int currTermCount = termCounts.size();
		int depth = node.getDepth();
		double x, exp;

		// Consider terms only if they do not occur in an ID or IDREF
		// attribute node:
		if (/*currTermCount > 0
				&&*/ !(node.isAttrib()
				&&   (node.getName().compareToIgnoreCase("id") == 0
				|| node.getName().compareToIgnoreCase("idref") == 0))) {
			/*
			 * The if ABOVE tries to avoid indexing primary key (XML id) values,
			 * but the current solution is incomplete. We would have to look
			 * at the DTD, if present, to determine which attributes are of
			 * type ID or IDREF. These attributes should not be indexed!
			 */
			// Determine word counter
			if (currTermCount > 0) {
				Iterator it = termCounts.entrySet().iterator();
				Map.Entry entry;
				MutableInteger counter;
				MutableInteger termNo;
				while (it.hasNext()) {
					entry = (Map.Entry) it.next();
					counter = (MutableInteger) entry.getValue();
					wordCount += counter.getValue();
					// TERM HANDLING IS DISABLED, S. ABOVE FOR MORE
//					if (passOne) {
//						termNo = (MutableInteger) entry.getKey();
//						// Term handling:
//						term = terms.get(termNo.getValue());
//						term.incCount(counter.getValue());
//					}
				}
			} else {
				wordCount = 0;
			}
			// THE FOLLOWING PART IS NEW!...
			if (passOne) {
				// DISABLED:
//				terms.incWordCount(wordCount); // (..except this which is old)
				// ..per node information
				node.incTotalWordCount(wordCount);
				node.setMinWordCount(Math.min(node.getMinWordCount(), wordCount));
				node.setMaxWordCount(Math.max(node.getMaxWordCount(), wordCount));
				// ..per DF information
				dfWordCount[depth] += wordCount;
				wordCount = dfWordCount[depth];
				node.incTotalDfWordCount(wordCount);
				node.setMinDfWordCount(Math.min(node.getMinDfWordCount(), wordCount));
				node.setMaxDfWordCount(Math.max(node.getMaxDfWordCount(), wordCount));
				dfWordCount[depth - 1] += wordCount;
				dfWordCount[depth] = 0;   // ..because its the end of the node!
				// ..per node information for terms
				node.incTotalTermCount(currTermCount);
				node.setMinTermCount(Math.min(node.getMinTermCount(), currTermCount));
				node.setMaxTermCount(Math.max(node.getMaxTermCount(), currTermCount));

//				GuideNode ancestor = node.getParent();
//				while (ancestor.getDepth() > 0) {
//					// This can be done afterwards!
//					ancestor.incTotalDfWordCount(wordCount);
//					ancestor = ancestor.getParent();
//				}
			} else {
				// There is less to do for the second pass as only the variances
				// need to be determined
				// ..again, for the node information...
//				x = node.getExpWordCount() - wordCount;
//				node.addVarWordSum(x * x);
				exp = node.getExpWordCount();
				node.addVarWordSum(Math.abs(exp - wordCount) / exp);
				// ..for DFs...
				dfWordCount[depth] += wordCount;
				wordCount = dfWordCount[depth];
//				x = node.getExpDfWordCount() - wordCount;
//				node.addVarDfWordSum(x * x);
				exp = node.getExpDfWordCount();
				node.addVarDfWordSum(Math.abs(exp - wordCount) / exp);
				dfWordCount[depth - 1] += wordCount;
				dfWordCount[depth] = 0;
				// ..for terms...
//				x = node.getExpTermCount() - currTermCount;
//				node.addVarTermSum(x * x);
				exp = node.getExpTermCount();
				node.addVarTermSum(Math.abs(exp - wordCount) / exp);
			}
		}
	}


//	public void setPassOne(boolean b) throws IOException {
//		if (!passOne && b) {
//			terms = Terms.load(termFileName);
//			terms.clearCounts();
//		}
//		passOne = b;
//	}
	public boolean isPassOne() { return passOne; }


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		if (args.length != 2 && args.length != 3) {
			printUsage();
			System.exit(1);
		}
		// Load configuration
		IdxrConfig config = (IdxrConfig) IdxrConfig.load(args[1]);
		IrStatsIndexer.index(config, args[0]);
	}

}