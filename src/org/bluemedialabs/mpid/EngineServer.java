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
import java.text.ParseException;
import java.util.*;
import org.bluemedialabs.io.DbIterator;
import org.bluemedialabs.util.Configuration;


/**
 * <p>Constructs and maintains query engines and delivers some general
 * meta information.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class EngineServer {
	static private final String DEFAULT_CFG_FILENAME =
			"/F/Java/project/idxr/idxr.cfg";
	static private final String ERROR_DUMP_FILE =
			"/F/Java/EngineServerError.txt";

	static private Configuration config = null;
	static private HashMap queryEngines = new HashMap();


//	static {
//		try {
//			config = Configuration.load(DEFAULT_CFG_FILENAME);
//		} catch (Exception e) {
//			PrintStream out = null;
//			try {
//				out = new PrintStream(
//						new FileOutputStream(ERROR_DUMP_FILE));
//			} catch (IOException e2) {
//				System.out.print("HELP! SOMETHING'S GONE TERRIBLY WRONG!\r\n");
//			}
//			out.print("HELP! SOMETHING'S GONE TERRIBLY WRONG!\r\n");
//			e.printStackTrace(out);
//			out.flush();
//			out.close();
//			// There should be some better means of handling errors
//			// in the future ;-)
//		}
//	}

	static public void init() throws IOException, ParseException {
		if (config == null) {
			config = Configuration.load(DEFAULT_CFG_FILENAME);
		}
	}

//	static public EngineServer getClient() { return client; }

//	public EngineServer(String cfgFileName) throws Exception {
//		config = Configuration.load(cfgFileName);
//	}


	static public QueryEngine getQueryEngine(String repId) throws IOException {
		// Check wether the server has already been initialized...
		if (config == null) {
			// ..it doesn't seem so, so, do it now...
			try {
				init();
			} catch (ParseException e) {
				throw new IOException("Configuration file parsing exception: "
									  + e.toString());
			}
		}
		// Fetch exisiting query engine, or,...
		QueryEngine qe = (QueryEngine) queryEngines.get(repId);
		if (qe == null) {
			// ..if this is the first request for the engine, create it
			qe = QueryEngine.create(config, repId);
			queryEngines.put(repId, qe);
		}
		return qe;
	}


	static public SourceMetadata getMetadata(String repId) {
		return null;
	}


	static public String getTitle(String repId) throws Exception {
		String title;
		if (config == null) {
			init();
		}
//		try {
		title = config.getProperty(repId, "Title");
		if (title == null) {
			title = "<Source title for " + repId + " undefined>";
		}
		return title;
	}


	static public void main(String[] args) throws Exception {
		QueryEngine qe = EngineServer.getQueryEngine("Religion");
		DfNnoDbIterator it = qe.content(qe.contains(143, 33), new String[]{"jesus"});
		if (it != null) {
		   while (it.hasNext()) {
			   StringBuffer buf = (StringBuffer) it.next();
			  System.out.print("\n<p>" + buf.toString() + "</p>\n");
		   }
		} else {
		   System.out.println("No results");
		 }
	}


}