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


/**
 * <p>Standard configuration parameter that in part have to be adjusted
 * from platform (Windows) to platform (Linux).</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public interface Config {

	/*
	 * Linux
	 */
//	static public final String JAVA_HOME = "/F/Java";
//	static public final String DATA_HOME = "/F/Data";


	/*
	 * Linux, Lab (Cassiopeia)
	 */
//	static public final String JAVA_HOME = "/F/Java";
//	static public final String DATA_HOME = "/cassiopeia/Data";


	/*
	 * Windoofs
	 */
//	static public final String JAVA_HOME = "F:/Java";
//	static public final String DATA_HOME = "F:/Data";




	/*
	 * General data about current repository being worked on
	 */
//	static public final String REP_HOME  = DATA_HOME + "/xml1g"; //"/home/bremer/Data/Reuters";
//	static public final String FILE_NAME = "/I/Data/Source/giga.xml";



	/*
	 * General file name conventions and specific file names
	 */
//	static public final String DATAGUIDE_FILENAME    = "guide.tree";
//	static public final String DECODER_FILENAME      = "decoder.data";
//	static public final String TERMDF_MAPPING_FILENAME  = "termdf-pp.map";


//	static public final String TERMFILE_BASENAME     = "terms";
//	static public final String TOKENFILE_BASENAME    = "tokens";


//	static public final String IID_TERMFILE_BASENAME = "termdf-i.data";
//	static public final String PID_TERMFILE_BASENAME = "termdf-p.data";

//	static public final String IID_NODEFILE_BASENAME = "nodedf-i.data";
//	static public final String PID_NODEFILE_BASENAME = "nodedf-p.data";



	/**
	 * The number of pid/counter pairs that will be held in memory before
	 * storing them to a temporary file that will have to be merged with
	 * the rest of the indexing data lateron.
	 */


	/**
	 *
	 */
//	static public final int IN_MEM_NODEDF_LIST_ELEMS = 500000;

//	static public final int IN_MEM_PID_NODEDF_LIST_ELEMS = 1000000; // 4,000,000

	/**
	 *
	 */
//	static public final int IN_MEM_TERMDF_LIST_ELEMS = 2000000;


}