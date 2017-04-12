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
import org.bluemedialabs.util.Configuration;


/**
 * <p> </p>
 * @author J. Marco Bremer
 * @version 1.0
 */
public class IdxrConfig extends Configuration {


	/*+**********************************************************************
	 * Class functions
	 ************************************************************************/

	/**
	 * Constructs and loads an indexer configuration from the file with the
	 * given fully-qualified name.
	 *
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	static public Configuration load(String fileName)
			throws IOException, ParseException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		IdxrConfig config = new IdxrConfig();
		config.load(br);
		return config;
	}

	/**
	 *
	 * @param args args[0] is supposed to be the configuration name, args[1]
	 *   the file name of the configuration
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	static public Configuration load(String[] args)
			throws IOException, ParseException {
		if (args.length < 2) {
			System.out.println("Expecting at least two arguments to identify "
							   + "a configuration:");
			System.out.println("1. The configuration identifier");
			System.out.println("2. The configuration file name");
			System.exit(1);
		}
		return load(args[1]);
	}


	/*+**********************************************************************
	 * Object functions
	 ************************************************************************/

	public IdxrConfig() {
		super();
	}

	public IdxrConfig(Configuration config) {
		super(config);
	}


	public String getTermFileBaseName(String cfgName) {
		String sourceHome = getProperty(cfgName, "SourceHome");
		String fileName = getProperty(cfgName, "TermFileBaseName");
		return (sourceHome + "/" + fileName);
	}

	public String getTokenFileBaseName(String cfgName) {
		String sourceHome = getProperty(cfgName, "SourceHome");
		String fileName = getProperty(cfgName, "TokenFileBaseName");
		return (sourceHome + "/" + fileName);
	}

	public String getDataGuideFileName(String cfgName) {
		String sourceHome = getProperty(cfgName, "SourceHome");
		String fileName = getProperty(cfgName, "DataGuideFileName");
		return (sourceHome + "/" + fileName);
	}

	public String getDecoderFileName(String cfgName) {
		String sourceHome = getProperty(cfgName, "SourceHome");
		String fileName = getProperty(cfgName, "DecoderFileName");
		return (sourceHome + "/" + fileName);
	}

	public String getIndexHome(String cfgName) {
		return getProperty("IndexHome");
	}

	public String getSourceHome(String cfgName) {
		return getProperty("SourceHome");
	}

	public String getPidTermFileBaseName(String cfgName) {
		String indexHome = getProperty(cfgName, "IndexHome");
		String fileName = getProperty(cfgName, "PidTermFileBaseName");
		return (indexHome + "/" + fileName);
	}

	public String getGroupedPidTermFileBaseName(String cfgName) {
		String indexHome = getProperty(cfgName, "IndexHome");
		String fileName = getProperty(cfgName, "GroupedTermFileBaseName");
		return (indexHome + "/" + fileName);
	}

	public String getTermDfMappingFileName(String cfgName) {
		String indexHome = getProperty(cfgName, "IndexHome");
		String fileName = getProperty(cfgName, "TermDfMappingFileName");
		return (indexHome + "/" + fileName);
	}

	public String getPidNodeFileBaseName(String cfgName) {
		String indexHome = getProperty(cfgName, "IndexHome");
		String fileName = getProperty(cfgName, "PidNodeFileBaseName");
		return (indexHome + "/" + fileName);
	}

	public String getIidTermFileBaseName(String cfgName) {
		String indexHome = getProperty(cfgName, "IndexHome");
		String fileName = getProperty(cfgName, "IidTermFileBaseName");
		return (indexHome + "/" + fileName);
	}

	public String getIidNodeFileBaseName(String cfgName) {
		String indexHome = getProperty(cfgName, "IndexHome");
		String fileName = getProperty(cfgName, "IidNodeFileBaseName");
		return (indexHome + "/" + fileName);
	}

}