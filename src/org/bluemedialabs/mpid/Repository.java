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
import java.util.Properties;
import org.bluemedialabs.io.*;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Repository {
	static public final String CONFIG_FILE_NAME = "meta.txt";
	static public final String TERMS_FILE_NAME  = "terms";
	static public final String TERMDOC_FILE_NAME= "termdoc";
	static public final String GUIDE_FILE_NAME  = "guide.tree";

	private File dir;
	private DataGuide guide = null;
	private Terms terms = null;
//	private OutputStream configOut = null;
	private IndexSeqFile termFile;
	private IndexSeqFile tdFile;

	private State state = new State();

	public Repository(Properties init, String shortName, Terms term,
			DataGuide guide) {
		// ...
	}

	/**
	 * Constructs a repository taking all configuration parameters from the
	 * supplied properties. The set of related properties has to be fully
	 * quallified with the short name given as well.
	 */
	public Repository(Properties init, String shortName) throws IOException {
		// First, determine for this repository is residing
		String repDir = init.getProperty(shortName + ".repDir");
		dir = new File(repDir);
		FileInputStream in, in2;
		Properties config = new Properties();
		FileOutputStream out;

		try {
			in = new FileInputStream(dir.toString() + "/" + CONFIG_FILE_NAME);
			config.load(in);
			in.close();
		} catch (FileNotFoundException e) {
			// If it's not there, just create it
			System.out.println("Repository " + dir + "/" + CONFIG_FILE_NAME
				+ " does not yet exist, creating a new one...");
			config.setProperty("state", State.toString(State.INITIAL));
			out = new FileOutputStream(dir + "/" + CONFIG_FILE_NAME);
			config.store(out, "-HEADER-");
			out.close();
		}
		state = new State();
		state.parseValue(config.getProperty("state"));
		if (state.getValue() != State.INITIAL) {
			switch (state.getValue()) {
				case State.ANALYZING:
					System.out.println("It seems the repository creation has "
						+ "been interrupted last time; the indexing has to be "
						+ "rerun to ensure a consistent repository");
					state.setValue(State.INITIAL);
					break;
				case State.ANALYZED:
					System.out.println("It seems pass 1 of the indexing process "
						+ "completed successfully when run last; need to do "
						+ "the actual indexing now");
					// Load all available data; that should be terms and data
					// guide
					loadGuideAndTerms();
					break;
				case State.INDEXING:
					System.out.println("It seems pass 1 of the indexing process "
						+ "completed successfully, but pass 2 was interrupted "
						+ "before completion; need to rerun "
						+ "the actual indexing");
					loadGuideAndTerms();
					state.setValue(State.ANALYZED);
					break;
				case State.INDEXED:
					System.out.println("It seems indexing was completed last "
						+ "time, but no merging has been done yet");
					// ...need to finish this...!!!

					break;
				default:
					throw new IllegalStateException("The state " + state.getValue()
						+ " is invalid; there must be something wrong in the "
						+ "code");
			}
		}
		// ...

		DataInputStream guideIn;
		dir = new File(repDir);
		// Load data guide
		guide = new DataGuide();
		guideIn = new DataInputStream(new FileInputStream(dir + "/"
			+ GUIDE_FILE_NAME));
		guide.load(guideIn);
		guideIn.close();
		// Load terms
		termFile = new IndexSeqFile(dir + TERMS_FILE_NAME);
		terms = new Terms();
		terms.load(termFile);
		termFile.close();
		//
	}

	private void loadGuideAndTerms() throws IOException {
		String guideFileName = dir + "/" + GUIDE_FILE_NAME;
		String termsFileName = dir + "/" + TERMS_FILE_NAME;
		FileInputStream in;

		try {
			in = new FileInputStream(guideFileName);
			termFile = new IndexSeqFile(termsFileName);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Repository state is '"
				+ state + "' according to the configuration file, "
				+ "but the data guide file '" + guideFileName + "' or "
				+ "the terms file with base name '" + termsFileName
				+ "' is missing");
		}
		guide = new DataGuide();
		guide.load(new DataInputStream(in));
		terms = new Terms();
		terms.load(termFile);
	}


//	public static void main(String[] args) {
//		Repository repository1 = new Repository("F:/Data/tmp");
//	}


	/************************************************************************
	 * The current state within the creation procedure of a repository.
	 ************************************************************************/
	static public class State {
		static public String[] STATE_NAMES = {
				"initial",      // Nothing done so far
				"analyzing",    // In the process of deriving term statistics
				"analyzed",     // Done deriving statistics
				"indexing",     // Indexing started but not yet completed
				"indexed",      // Done with term-dco indexing
				"merging",      // Mergind temporary term-doc files
				"final"         // Done with merging, thus completed everything
		};

		static public final int INITIAL     = 0;
		static public final int ANALYZING   = 1;
		static public final int ANALYZED    = 2;
		static public final int INDEXING    = 3;
		static public final int INDEXED     = 4;
		static public final int MERGING     = 5;
		static public final int FINAL       = 6;

		private int value = INITIAL;

		protected State() {}

		public int getValue() {
			return value;
		}

		private void next() {
			if (value == FINAL) {
				throw new IllegalStateException("Cannot go to next state as "
					+ "current state is already final");
			}
			value++;
		}

		private void setValue(int v) {
			if (v < INITIAL || v > FINAL) {
				throw new IllegalArgumentException("Cannot set state to invalid "
					+ "value " + v + ", valid states are " + INITIAL + "..."
					+ FINAL);
			} else if (v != INITIAL && v == value) {
				throw new IllegalArgumentException("Cannot make tarnsition into "
					+ "same state as current state (" + v + "), please check "
					+ "your business logic");
			}
			value = v;
		}

		private void parseValue(String v) {
			boolean valid = false;
			for (int i = INITIAL; i <= FINAL && !valid; i++) {
				if (v.compareToIgnoreCase(STATE_NAMES[i]) == 0) {
					value = i;
					valid = true;
				}
			}
			if (!valid) {
				throw new IllegalArgumentException("The supplied string '"
					+ v + "' does not represent the string representation of "
					+ "a valid state");
			}
		}

		static public String toString(int v) {
			if (v < INITIAL || v > FINAL) {
				throw new IllegalArgumentException("Cannot string representation "
					+ "of state " + v + " as that is not a invalid state "
					+ ", valid states are " + INITIAL + "..."
					+ FINAL);
			}
			return  STATE_NAMES[v];
		}

		public String toString() {
			return STATE_NAMES[value];
		}
	} // State

}