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

import org.bluemedialabs.util.Configuration;
import org.bluemedialabs.util.StopWatch;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TIndX {

	static public void main(String[] args) throws Exception {
		Configuration config = Configuration.load(args);
		String cfgName = args[0];
		StopWatch watch = new StopWatch();
		String[] xargs;
		int totalCounters = -1;

		if (args.length > 2) {
			totalCounters = Integer.parseInt(args[2]);
		}
		System.out.println("\n### Indexing >>>" + args[0] + "<<< ...");
		watch.start();
//		System.out.println("\n### Terms & term counters [T-index & TC-index]...");
//		PidIndexer.main(args);
		System.out.println("\n### Mingling term files...");
		xargs = new String[4];
		xargs[0] = args[0]; xargs[1] = args[1];
		xargs[2] = "t"; xargs[3] = "p";
		Mingler.main(xargs);
		System.out.println("\n### Deriving grouped file...");
		TermDfList.constructGroupedFile(config, cfgName, totalCounters);
//		System.out.println("    (NOT YET IMPLEMENTED)");
		watch.stop();
		System.out.println("\n### Complete elapsed time: " + watch);
	}

}
