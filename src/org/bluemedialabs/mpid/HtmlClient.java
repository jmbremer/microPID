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
import org.bluemedialabs.io.DbIterator;
import org.bluemedialabs.util.Configuration;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class HtmlClient {
	static private final String DEFAULT_CFG_FILENAME =
			"F:/Java/project/idxr/idxr.cfg";
	static private final String ERROR_DUMP_FILE = "F:/Java/HtmlClientError.txt";
	static private HtmlClient client;

	static private Configuration config;
	static private HashMap queryEngines = new HashMap();


	static {
		try {
			config = Configuration.load(DEFAULT_CFG_FILENAME);
		} catch (Exception e) {
			PrintStream out = null;
			try {
				out = new PrintStream(new FileOutputStream(ERROR_DUMP_FILE));
			} catch (IOException e2) {
				System.out.print("HELP! SOMETHING'S GONE TERRIBLY WRONG!\r\n");
			}
			out.print("HELP! SOMETHING'S GONE TERRIBLY WRONG!\r\n");
			e.printStackTrace(out);
			out.flush();
			out.close();
			// There should be some better means of handling errors
			// in the future ;-)
		}
	}

	static public void init() throws Exception {
		config = Configuration.load(DEFAULT_CFG_FILENAME);
	}

//	static public HtmlClient getClient() { return client; }

//	public HtmlClient(String cfgFileName) throws Exception {
//		config = Configuration.load(cfgFileName);
//	}


	static public QueryEngine getQueryEngine(String repId) throws IOException {
		QueryEngine qe = (QueryEngine) queryEngines.get(repId);

		if (qe == null) {
			// First time this QE has been requested
			qe = QueryEngine.create(config, repId);
			queryEngines.put(repId, qe);
		}
		return qe;
	}


	static public void main(String[] args) throws Exception {
		QueryEngine qe = HtmlClient.getQueryEngine("Religion");
		DbIterator it = qe.content(qe.contains(143, 33), new String[]{"jesus"});
		if (it != null) {
		   while (it.hasNext()) {
			  StringBuffer buf = (StringBuffer) it.next();
			  System.out.print("<p>" + buf.toString() + "</p>");
		   }
		} else {
		   System.out.println("No results");
		 }
	}

}