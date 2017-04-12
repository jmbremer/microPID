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
import org.bluemedialabs.io.XmlSeqInputStream;
import org.bluemedialabs.util.Configuration;
import org.bluemedialabs.util.HtmlEntities;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class XmlSourceJoiner {


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/
	static public void main(String[] args) throws Exception {
		Configuration config;
		int argCount = args.length;
		PrintWriter out;
		String fileName;
		String root;
		HtmlEntities entities = new HtmlEntities("/aramis/Data/Source/entities.txt");

		if (argCount < 4) {
			printUsage();
			System.exit(1);
		}
		config = Configuration.load(args[argCount - 1]);
		root = config.getProperty(args[0], "RootNodeName");
		if (root == null) {
			// If no name for the root node is given explicitly, the default
			// name is the configuration name
			root = args[0];
		}
		fileName = config.getProperty(args[0], "SourceFileName");
		System.out.println("Creating XML source '" + args[0] + "' as file '"
						   + fileName + "'...");
		out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
		out.print(XmlSeqInputStream.HEADER);
		out.println();
		out.println(entities.toXml(root));
		out.println();
		out.println("<" + root + ">");
		for (int i = 1; i < argCount - 1; i++) {
			System.out.print("  ..appending source '" + args[i] + "'...");
			appendSource(args[i], config, out);
		}
		out.println();
		out.print("</" + root + ">");
		out.flush();
		out.close();
	}

	static private void printUsage() {
		System.out.println();
		System.out.println("Expecting at least n=4 arguments for XmlSourceJoiner:");
		System.out.println("(1) The configuration name of the target source");
		System.out.println("(2 to n-1) Configuration names to be joined");
		System.out.println("(n) The configuration file name");
	}

	static private void appendSource(String cfgName, Configuration config,
									 PrintWriter pw) throws IOException {
		String fileName = config.getProperty(cfgName, "SourceFileName");
		String compression = config.getProperty(cfgName, "SourceFileCompression");
		BufferedReader br;
		String line = null;
		int pos;

		if (fileName == null) {
			throw new IllegalArgumentException("Cannot determine "
					+ "SourceFileName for configuration '" + cfgName
					+ "' (SourceFileName="
					+ fileName + ")");
		}
		if (compression != null && compression.compareToIgnoreCase("gzip") == 0) {
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(
					new FileInputStream(fileName + ".gz"))));
			System.out.println(" (file '" + fileName + ".gz')");
		} else {
			br = new BufferedReader(new FileReader(fileName));
			System.out.println("(file '" + fileName + "')");
		}
		try {
			line = br.readLine();
			while (!(line.length() > 0 && (pos = line.indexOf('<')) >= 0
					 && isAlpha(line.charAt(pos + 1)))) {
				line = br.readLine();
			}
		} catch (NullPointerException e) {
			System.out.println("Failed to detect start of XML content for '"
							   + cfgName + "!' Exiting program.");
			System.exit(1);
		}
		// NOTE: The above assumes that the first two lines are global meta
		// information followed by nothing more than whitespace and the actual
		// XML content -> Now NEW!
		pw.println();
		while (line != null) {
			pw.println(line);
			line = br.readLine();
		}
		pw.flush();
	}


	private static boolean isAlpha(char ch) {
		return ('a' <= ch && ch <= 'z' || 'A' <= ch && ch <= 'Z');
	}

}