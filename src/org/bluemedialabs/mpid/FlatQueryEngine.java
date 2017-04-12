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
import org.bluemedialabs.io.IndexSeqFile;
import org.bluemedialabs.io.LogStream;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class FlatQueryEngine {
	static public final String CONFIG_FILE_NAME = "meta.txt";
	static public final String TERMS_FILE_NAME  = "terms";
	static public final String DOCS_FILE_NAME  = "docs";
	static public final String TERMDOC_FILE_NAME= "termdoc";
	static public final String GUIDE_FILE_NAME  = "guide.tree";

	protected DataGuide guide = null;
	protected Terms terms = null;
	protected Documents docs = null;

	private String repDir;
//	private OutputStream configOut = null;
	private IndexSeqFile termFile;
	private IndexSeqFile tdFile;


	public FlatQueryEngine(String repDir) throws IOException {
		DataInputStream in;
		IndexSeqFile docFile;

		System.out.println("Starting query engine for repository '" + repDir
			+ "'...");
		this.repDir = repDir;
		// Load data guide and terms
		DataInputStream guideIn;
		// (1) Data guide
		System.out.println("  Creating and loading data guide...");
		guide = new DataGuide();
		in = new DataInputStream(new FileInputStream(repDir + "/"
				+ GUIDE_FILE_NAME));
		guide.load(in);
		in.close();
		System.out.println("  ..data guide is " + guide);
		// (2) Terms
		System.out.println("  Creating and loading terms...");
		termFile = new IndexSeqFile(repDir + "/" + TERMS_FILE_NAME);
		terms = new Terms();
		terms.load(termFile);
		System.out.println("  ..terms are " + terms);
//		termFile.close();   -- leave open for future processing!?
		// (3) Documents
		System.out.println("  Preparing document store...");
		docFile = new IndexSeqFile(repDir + "/" + DOCS_FILE_NAME);
		docs = new Documents(docFile);
		// Open term-document inverted file
		System.out.println("  Opening inverted file for term-doc...");
		tdFile = new IndexSeqFile(repDir + "/" + TERMDOC_FILE_NAME);
		System.out.println("Query engine created successfully.");
	}

	public void finalize() {
		try {
			termFile.close();
			tdFile.close();
		} catch (IOException e) {
			System.out.println("Finalizing query engine for repository '"
				+ repDir + "' failed");
		}
	}

	public Terms getTerms() {
		return terms;
	}

	public DataGuide getDataGuide() {
		return guide;
	}

	public void getInvList(int no, FlatInvertedList list) throws IOException {
		tdFile.get(no, list);
	}


	public static void main(String[] args) throws Exception {
		FlatQueryEngine qe = new FlatQueryEngine("F:/Data/tmp");
		FlatInvertedList li = new FlatInvertedList(1000);
		int maxTermNo = qe.getTerms().getTermCount();

		System.out.println("Printing a couple of inverted lists...");
		qe.getInvList(45, li);
		System.out.print(".. 45)  ");
		System.out.println(li.toString());
		qe.getInvList(112, li);
		System.out.println("..112)  " + li);
		qe.getInvList(maxTermNo, li);
		System.out.println(".." + maxTermNo + ")  " + li);
	}
}