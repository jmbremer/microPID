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
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.bluemedialabs.io.DataFileOutputStreamSeq;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.StopWatch;


/**
 *
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class XmlEncoder implements Runnable {
	static public final boolean CASE_SENSITIVE = false;

	static private final boolean DEFAULT_VALIDATION = false;
	static private SAXParserFactory parserFactory;
	static private int nextIndexerId;

	private int id;
	private SAXParser saxParser = null;
	private XMLReader xmlReader = null;
	// Define Tokenizer interface for the following later:
//	private EnglishTokenizer tokenizer = new EnglishTokenizer(null, 0, 0);
	private InputSource inputSource = null;
	private IndexerState state;
	private LogStream log;

	static {
		// create the single factory to obtain parsers from
		// for all indexers running
		parserFactory = SAXParserFactory.newInstance();
		parserFactory.setValidating(DEFAULT_VALIDATION);
		nextIndexerId = 1;
	}

	/**
	 * Returns the total number of indexers created throughout this program
	 * run. The returned number does not have to be the number of indexer still
	 * existing.
	 *
	 * @returns The total number of indexer objects created.
	 */
	static int getIndexerCount() {
		return nextIndexerId - 1;
	}


	static void runIndexer(InputSource parse1Source, InputSource parse2Source,
			String name) {



	}


	/**
	 * Constructs a new indexer.
	 */
	public XmlEncoder(ContntHandler handler, String contentType, LogStream log)
			throws IllegalStateException {
		if (log == null) {
			throw new NullPointerException("The log object may not be null");
		}
		this.log = log;
		// prepare XML parser (XML)
		try {
			// create parser object
			saxParser = parserFactory.newSAXParser();
			// get a reader for doing the actual parsing
			xmlReader = saxParser.getXMLReader();
			xmlReader.setContentHandler(handler);
			xmlReader.setErrorHandler(new ParseErrorHandler(this, log));
		} catch (ParserConfigurationException e) {
			log.log("XmlEncoder construction exception (parser configuration): "
				+ e);
			throw new IllegalStateException(e.toString());
		} catch (SAXException e2) {
			log.log("XmlEncoder construction exception (SAX parser): " + e2);
			throw new IllegalStateException(e2.toString());
		}
		id = nextIndexerId++;
		state = new IndexerState();
		log.log("XmlEncoder " + id + " created successfully");
	}


	public int getId() {
		return id;
	}

	public IndexerState getState() {
		return state;
	}

	public void setInputSource(InputSource source) {
		if (state.isRunning()) {
			throw new IllegalStateException("The input source may not be set "
				+ "while the XmlEncoder is running");
		}
		inputSource = source;
		if (source == null) {
			state.setWaitingForInput();
		} else {
			state.setNotYetStarted();
		}
	}

	public void setContentHandler(ContentHandler handler) {
		xmlReader.setContentHandler(handler);
		xmlReader.setErrorHandler((ErrorHandler) handler);
	}


	public void run() {
		try {
			if (!state.isNotYetStarted()) {
				log.log("XmlEncoder " + id + " asked to run even though in state '"
					+ state + "'");
				throw new IllegalStateException("XmlEncoder is not in the right "
					+ "state to run (state=" + state + ")");
			}
			state.setRunning();
			// Here now, we actually start what we are born for ;-)
			xmlReader.parse(inputSource);

			// Done with parsing the current input source
			inputSource = null;
			state.setWaitingForInput();
		} catch (RuntimeException e) {
			System.out.println("Runtime exception while running XmlEncoder "
				+ id + ":");
			e.printStackTrace();
//			throw e;
		} catch (Exception e2) {
			System.out.println("Exception while running XmlEncoder "
				+ id + ":");
			e2.printStackTrace();
			state.setException(e2);
		}
	}


	public String toString() {
		StringBuffer buf = new StringBuffer(256);

		buf.append("(id=");
		buf.append(id);
		buf.append(", state=");
		buf.append(state);
		buf.append(", input=");
		buf.append(inputSource);
		buf.append(")");
		return buf.toString();
	}


/*+**************************************************************************
 * ParseErrorHandler
 ****************************************************************************/

	/**
	 * XmlEncoder SAX parsing error handler.
	 */
	static private class ParseErrorHandler implements ErrorHandler {
		private XmlEncoder indexer;
		private LogStream log;

		ParseErrorHandler(XmlEncoder indexer, LogStream log) {
			this.indexer = indexer;
			this.log = log;
		}

		public void warning(SAXParseException e) throws SAXException {
			log.log("SAX warning in XmlEncoder " + indexer.getId() + ": "
					+ getExceptionMsg(e));
		}

		public void error(SAXParseException e) throws SAXException {
			String msg = "SAX error in XmlEncoder " + indexer.getId() + ": "
					+ getExceptionMsg(e);
			log.log(msg);
			throw new SAXException(msg);
		}

		public void fatalError(SAXParseException e) throws SAXException {
			String msg = "SAX fatal error in XmlEncoder " + indexer.getId() + ": "
					+ getExceptionMsg(e);
			log.log(msg);
			throw new SAXException(msg);
		}

		/**
		 * Returns a string describing parse exception details
		 */
		private String getExceptionMsg(SAXParseException e) {
			StringBuffer buf = new StringBuffer(127);

			if (e.getSystemId() != null) {
				buf.append("URI is '");
				buf.append(e.getSystemId());
				buf.append("', ");
			}
			buf.append("line ");
			buf.append(e.getLineNumber());
			buf.append(": ");
			buf.append(e.getMessage());
			return buf.toString();
		}
	}


/*+**************************************************************************
 * IndexerState
 ****************************************************************************/

	static public class IndexerState {
		static private final int WAITING_FOR_INPUT  = 1;
		static private final int NOT_YET_STARTED    = 2;
		static private final int RUNNING            = 3;
		static private final int EXCEPTION          = 4;

		private int state;
		private Exception ex = null;

		public IndexerState() {
			state = WAITING_FOR_INPUT;
		}

		private void setWaitingForInput() {
			ex = null;
			state = WAITING_FOR_INPUT;
		}
		public boolean isWaitingForInput() {
			return (state == WAITING_FOR_INPUT);
		}

		private void setNotYetStarted() {
			ex = null;
			state = NOT_YET_STARTED;
		}
		public boolean isNotYetStarted() {
			return (state == NOT_YET_STARTED);
		}

		private void setRunning() {
			ex = null;
			state = RUNNING;
		}
		public boolean isRunning() {
			return (state == RUNNING);
		}

		private void setException(Exception e) {
			state = EXCEPTION;
			this.ex = e;
		}
		public boolean hadException() {
			return (ex != null);
		}
		public Exception getException() {
			return ex;
		}

		public String toString() {
			String str;

			switch (state) {
				case RUNNING:           str = "running"; break;
				case NOT_YET_STARTED:   str = "not-yet-started"; break;
				case WAITING_FOR_INPUT: str = "waiting-for-input"; break;
				case EXCEPTION:         str = "exception"; break;
				default: str = "illegal"; break;
			}
			return str;
		}
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	 /**
	  *
	  * @param source
	  * @param repDir
	  * @param tokens
	  * @return The total number of nodes counted, or -1 if any excpetion
	  *   occured during this first encoding step.
	  * @throws IOException
	  */
	static public long passOne(InputSource source, String repDir,
			Tokens tokens) throws IOException {
		DataGuide guide = new DataGuide();
//		Tokens tokens = new Tokens();
		XmlTokenizer tokenizer = new XmlTokenizer(null, 0, 0);
		StatsCntHandler handler = new StatsCntHandler(tokenizer, tokens, guide);
		XmlEncoder indexer = new XmlEncoder(handler, "Pass 1 XmlEncoder",
				LogStream.DEFAULT_STREAM);
		XmlEncoder.IndexerState state;

		StopWatch watch = new StopWatch();
		indexer.setInputSource(source);
		System.out.println("Starting pass I...");
		watch.start();
		indexer.run();
		System.out.println("XmlEncoder is done with pass I.");
		System.out.println("Time taken for pass I:  " + watch);
		state = indexer.getState();
		System.out.println("XmlEncoder state is " + state);
		if (state.hadException()) {
			System.out.println("There was an exception...");
			state.getException().printStackTrace();
			return -1;
		} else {
			indexer = null;
			handler = null;
			tokenizer = null;
			System.gc();
			System.out.println("Storing data guide...\n");
			guide.assignNumbers();
			BufferedOutputChannel guideOut = BufferedOutputChannel.create(
					repDir + "/guide.tree");
			guide.store(guideOut);
			guideOut.close();
			System.out.println("Storing unprocessed tokens...\n");
			IndexSeqOutputChannel tokenOut = IndexSeqOutputChannel.create(
					repDir + "/tokens", false);
			tokens.store(tokenOut);
			tokenOut.close();
			System.out.println("Sorting and again storing tokens...");
			// The following will take care of creating a code and sorting the
			// tokens accordingly:
			HuffmanCoder coder = new HuffmanCoder(tokens);
			tokenOut = IndexSeqOutputChannel.create(repDir + "/tokens", false);
			tokens.store(tokenOut);
			tokenOut.close();
			System.out.print("Generating terms...");
			Terms terms = new Terms(tokens);
			System.out.println("storing terms...");
			tokenOut = IndexSeqOutputChannel.create(repDir + "/terms", false);
			terms.store(tokenOut);
			tokenOut.close();
			// NEW: Generate and store decoder
			System.out.println("Generating and storing decoder...\n");
			Decoder decoder = new Decoder();
			decoder.init(tokens, terms, guide, coder.getStartCodes());
			guideOut = BufferedOutputChannel.create(repDir + "/decoder.data");
			decoder.store(guideOut);
			guideOut.close();

			watch.stop();

			System.out.println("========== Token and Structure Statistics ==========");
//			terms.printSortedByNo(4);
			System.out.println();
//			terms.printSortedByCount(4);
//			System.out.println();
//			terms.printLexiSorted(4);
//			System.out.println();

//			System.out.println(guide);
			System.out.println("...left out for efficiency..");

//			System.out.println();
//			System.out.println(guide.getTagMap());
			System.out.println("================== END STATISTICS =================");
			System.out.println("Complete time elapsed:  " + watch);
			return guide.getTotalNodeCount();
		}
	}


	static public boolean passTwoAStream(InputSource source, String repDir,
			Tokens tokens, long totalNodeCount) throws IOException {
		XmlTokenizer tokenizer = new XmlTokenizer(null, 0, 0);
		if (tokens == null) {
			tokens = Tokens.load(repDir + "/tokens", true);
		}
		HuffmanCoder coder = new HuffmanCoder(tokens);
		DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(
				repDir + "/xml.data")));
		BitDataOutput xmlOut = new BitDataOutput(dos);
		EncoderCntHandler handler = new EncoderCntHandler(tokenizer, tokens,
				coder, (BitOutput) xmlOut, totalNodeCount);
		XmlEncoder indexer = new XmlEncoder(handler, "Pass 2 XmlEncoder",
				LogStream.DEFAULT_STREAM);
		XmlEncoder.IndexerState state;

		StopWatch watch = new StopWatch();
		indexer.setInputSource(source);
		System.out.println("Starting pass II...");
		watch.start();
		indexer.run();
		System.out.println("XmlEncoder is done with pass II.");
		System.out.println("Time taken for pass II:  " + watch);
		state = indexer.getState();
		System.out.println("XmlEncoder state is " + state);
		if (state.hadException()) {
			System.out.println("There was an exception...");
			state.getException().printStackTrace();
			return false;
		} else {
			xmlOut.flush();
			dos.flush();
			dos.close();
			watch.stop();
			System.out.println("Complete time elapsed:  " + watch);
			return true;
		}

	}

	static public boolean passTwoAChannel(InputSource source, String repDir,
			Tokens tokens, long totalNodeCount) throws IOException {
		XmlTokenizer tokenizer = new XmlTokenizer(null, 0, 0);
		if (tokens == null) {
			tokens = Tokens.load(repDir + "/tokens", true);
		}
		HuffmanCoder coder = new HuffmanCoder(tokens);
		BufferedOutputChannel boc = new BufferedOutputChannel((new FileOutputStream(
				repDir + "/xml.data")).getChannel());
		BitDataOutput xmlOut = new BitDataOutput((DataOutput) boc);
		EncoderCntHandler handler = new EncoderCntHandler(tokenizer, tokens,
				coder, (BitOutput) xmlOut, totalNodeCount);
		XmlEncoder indexer = new XmlEncoder(handler, "Pass 2 XmlEncoder",
				LogStream.DEFAULT_STREAM);
		XmlEncoder.IndexerState state;

		StopWatch watch = new StopWatch();
		indexer.setInputSource(source);
		System.out.println("Starting pass II...");
		watch.start();
		indexer.run();
		System.out.println("XmlEncoder is done with pass II.");
		System.out.println("Time taken for pass II:  " + watch);
		state = indexer.getState();
		System.out.println("XmlEncoder state is " + state);
		if (state.hadException()) {
			System.out.println("There was an exception...");
			state.getException().printStackTrace();
			return false;
		} else {
			xmlOut.flush();
			boc.flush();
			boc.close();
			watch.stop();
			System.out.println("Complete time elapsed:  " + watch);
			return true;
		}

	}

/*
	static public boolean passThree(InputSource source, String repDir,
			DataGuide guide, Terms terms) throws IOException {
		EnglishTokenizer tokenizer = new EnglishTokenizer(null, 0, 0);
		DataFileOutputStreamSeq outSeq =
				new DataFileOutputStreamSeq(repDir + "/termdoc.data");
		IndexSeqOutputChannel docFile = IndexSeqOutputChannel.create(repDir + "/docs");
		TermDocCounters tdCounters =
				new TermDocCounters(2000000, terms.getTermCount(), outSeq);
		TermDocCntHandler handler = new TermDocCntHandler(tokenizer, guide, terms,
				tdCounters,	docFile, LogStream.DEFAULT_STREAM);
		Indexer indexer = new Indexer(handler, "Pass 3 Indexer",
				LogStream.DEFAULT_STREAM);
		Indexer.IndexerState state;

//		InputSource input =	new InputSource("F:/Data/FT.xml");
//				new TrecInputStream("D:/FT", "FinancialTimes",
//						new TrecInputStream.FinancialTimesFilter()));
		StopWatch watch = new StopWatch();
		indexer.setInputSource(source);
		System.out.println("Starting pass II indexer...");
		watch.start();
		indexer.run();
		watch.stop();
		outSeq.close();
		System.out.println("Indexer is done with pass II.");
		System.out.println("Time for indexing:  " + watch);
		state = indexer.getState();
		if (state.hadException()) {
			System.out.println("There was an exception during indexing...");
			state.getException().printStackTrace();
			return false;
		}
		return true;
	}
	*/

	static public void merge(String repDir) throws IOException {
		StopWatch watch = new StopWatch();
		// The final output...
		// Merge the multiple inverted files into one
		System.out.println("Merging inverted files...");
		watch.start();
		DfMerger.merge(repDir, "termdf.data");
		watch.stop();
		System.out.println("Done with merging.");
		System.out.println("Time for merging:  " + watch);
	}


//	static public Tokens tokens;

	static public void main(String[] args) throws Exception {
		final boolean PASS_TWO_ONLY = false;  // Enable for only exec. 2nd pass
		IdxrConfig config = (IdxrConfig) IdxrConfig.load(args);
		String cfgId = args[0];
		String compression = config.getProperty(cfgId, "SourceFileCompression");
		String sourceHome = config.getProperty(cfgId, "SourceHome");
		String fileName = config.getProperty(cfgId, "SourceFileName");
		InputSource inp;
		Tokens tokens = new Tokens();
		StopWatch watch = new StopWatch();
		long totalNodeCount = -1;

		System.out.println("XML Source Encoding");
		System.out.println("Source file name...... " + fileName);
		System.out.println("Target directory...... " + sourceHome);
		System.out.println("Input compression..... " + compression);

		if (!PASS_TWO_ONLY) {
			// (I)
			// Create input source
			if (compression.compareToIgnoreCase("gzip") == 0
				&& fileName.endsWith(".xml")) {
				// GZip compressed input
				inp = new InputSource(new GZIPInputStream(
						new FileInputStream(fileName + ".gz")));
				System.out.println("..Gzip compressed direct file input...");
			} else if (cfgId.compareToIgnoreCase("LATimes") == 0
					   && !(fileName.indexOf(".xml") > 0)) {
				// LA Times
				inp = new InputSource(TrecInputStream.create("/cdrom/latimes",
						"LA_Times", TrecInputStream.LA_TIMES));
				//	OLD: new TrecInputStream.LaTimesFilter(),
				//	OLD:			new TrecInputStream.LaTimesFilter()));
				System.out.println("..LA Times CD input...");
			} else if (cfgId.compareToIgnoreCase("Reuters") == 0
					   && !(fileName.indexOf(".xml") > 0)) {
				// Reuters
				inp = new InputSource(ReutersInputStream.create(
						"/cdrom", "Reuters"));
				System.out.println("..Reuters CD input...");
			} else {
				// Regular (for uncompressed plain XML)
				inp = new InputSource(new BufferedInputStream(
						new FileInputStream(fileName)));
				System.out.println("..uncompressed direct file input...");
			}
			// Collect statistics, tokens, and terms
			totalNodeCount = passOne(inp, sourceHome, tokens);
			if (totalNodeCount < 0) {
				// Some error must have occurred
				return;
			}
		} else {
			// Do this when executing only the second step:
			tokens = null;
			DataGuide guide = DataGuide.load(config, cfgId);
			totalNodeCount = guide.getTotalNodeCount();
		}

		// (II)
		// Same as above, create input source
		if (compression.compareToIgnoreCase("gzip") == 0
				&& fileName.endsWith(".xml")) {
			// GZip compressed input
			inp = new InputSource(new GZIPInputStream(
				new FileInputStream(fileName + ".gz")));
			System.out.println("..Gzip compressed direct file input...");
		} else if (cfgId.compareToIgnoreCase("LATimes") == 0
				&& !(fileName.indexOf(".xml") > 0)) {
			// LA Times
			inp = new InputSource(TrecInputStream.create("/cdrom/latimes",
					"LA_Times", TrecInputStream.LA_TIMES));
			//	OLD: new TrecInputStream.LaTimesFilter(),
			//	OLD:			new TrecInputStream.LaTimesFilter()));
			System.out.println("..LA Times CD input...");
		} else if (cfgId.compareToIgnoreCase("Reuters") == 0
					   && !(fileName.indexOf(".xml") > 0)) {
			// Reuters
			inp = new InputSource(ReutersInputStream.create(
					"/cdrom", "Reuters"));
			System.out.println("..Reuters CD input...");
		} else {
			// Regular (for uncompressed plain XML)
			inp = new InputSource(new BufferedInputStream(
					new FileInputStream(fileName)));
			System.out.println("..uncompressed direct file input...");
		}
		// Now, generate compressed XML source
		passTwoAChannel(inp, sourceHome, tokens, totalNodeCount);

		// (III)
		// Generate text versions of tokens/terms/Data Guide
		System.out.println("Dumping token file...");
		Tokens.dumpTokenFile(config, args[0]);
		System.out.println("Dumping term file...");
		Terms.dumpTermFile(config, args[0], false);
		System.out.println("Dumping DataGuide file...");
		DataGuide.dumpDataGuide(config, args[0], false);
	}
}