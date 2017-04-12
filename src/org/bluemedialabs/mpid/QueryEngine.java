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
import java.util.NoSuchElementException;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.*;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class QueryEngine {
	private PIndex pIndex;
	private AIndex aIndex;
	private DcIndex dcIndex;
	private TIndex tIndex;
	private TermNodeMap map;

	private XmlSource source;

	private DataGuide guide;
	private Terms terms;
	private Tokens tokens;
	private int termCount;
	private int nodeCount;

	private int maxPrint = -1;



	/*+**********************************************************************
	 * Class functions
	 ************************************************************************/

	static public QueryEngine create(Configuration config, String cfgName)
			throws IOException {
		final boolean HASH_TOKENS = false;
		DataGuide guide = DataGuide.load(config, cfgName);
		AIndex aIndex = AIndex.create(config, cfgName, guide);
		DcIndex dcIndex = DcIndex.create(config, cfgName, guide);
		PIndex pIndex = PIndex.create(config, cfgName, guide);
		TIndex tIndex = TIndex.create(config, cfgName, guide);
		Tokens tokens = Tokens.load(config, cfgName, HASH_TOKENS);
		Terms terms = Terms.load(config, cfgName);
		XmlSource source = XmlSource.create(config, cfgName, tokens);

		return new QueryEngine(guide, pIndex, aIndex, dcIndex, tIndex,
								tokens, terms, source);
	}



	/*+**********************************************************************
	 * Object functions
	 ************************************************************************/

	public QueryEngine(DataGuide guide, PIndex pIndex, AIndex aIndex,
					   DcIndex dcIndex, TIndex tIndex, Tokens tokens,
					   Terms terms, XmlSource source) throws IOException {
		// Directly provided parameters
		this.guide = guide;
		this.pIndex = pIndex;
		this.aIndex = aIndex;
		this.dcIndex = dcIndex;
		this.tIndex = tIndex;
		this.tokens = tokens;
		this.terms = terms;
		this.source = source;
		// Derived parameters
		termCount = terms.getTermCount();
		nodeCount = guide.getNodeCount();
	}

	protected PIndex getPIndex() { return pIndex; }
	protected AIndex getAIndex() {return aIndex; }
	protected DcIndex getDcIndex() {return dcIndex; }
	protected TIndex getTIndex() {return tIndex; }
	protected XmlSource getSource() {return source; }
	protected Tokens getTokens() { return tokens; }
	protected Terms getTerms() { return terms; }


//	public NnoIterator


	public void contains(int termNo) throws IOException {


	}

	public NnoDbIterator contains(int termNo, int nodeNo) throws IOException {
		return tIndex.posIterator(termNo, nodeNo);
	}


	public DfNnoDbIterator content(NnoDbIterator posIt, String[] terms)
			throws IOException {
		return new DfNnoDbIterator(posIt, getSource(), getPIndex(), terms);
	}

	public DfNnoDbIterator content(NnoDbIterator posIt) throws IOException {
		return new DfNnoDbIterator(posIt, getSource(), getPIndex(), null);
	}



	public DfNnoDbIterator execute(ParseTree pt) throws IOException {
		if (!pt.isPathPattern()) {
			throw new IllegalArgumentException("Sorry, right now, only path "
					+ "pattern queries are supported, but this is a tree "
					+ "pattern query");
		}

		return null;
	}


	protected void printDf(StringBuffer buf) {


	}


	public void setMaxPrint(int print) { maxPrint = print; }
	public int getMaxPrint() { return maxPrint; }


	public void query() throws IOException {

	}



	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		if (args.length < 2) {
			printUsage();
			System.exit(1);
		}
		Configuration config = (IdxrConfig) IdxrConfig.load(args[1]);
		QueryEngine qe = QueryEngine.create(config, args[0]);
		// ...do something...
	}

	static private void printUsage() {
		System.out.println();
		System.out.println("Expecting exactly 2 arguments for idxr.QueryEngine:");
		System.out.println("(1) The configuration name");
		System.out.println("(2) The configuration file name");
	}

}