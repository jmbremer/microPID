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

import java.io.IOException;
import org.bluemedialabs.io.*;
import org.bluemedialabs.util.Configuration;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class XmlSource {
	private BitDataChannel source;
	private Tokens tokens;
	private XmlDecoderStream xds;


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public XmlSource create(Configuration config, String cfgName,
								   Tokens tokens) throws IOException {
		// Determine all required configuration strings
		String sourceHome = config.getProperty(cfgName, "SourceHome");
		String comprSourceFileName =
				config.getProperty(cfgName, "ComprSourceFileName");
		// Construct basic objects from files
		BitDataChannel ch = new BitDataChannel(
			RandomAccessChannel.create(sourceHome + "/" + comprSourceFileName));


		return new XmlSource(ch, tokens);
	}

	static public XmlSource create(Configuration config, String cfgName)
			throws IOException {
		String sourceHome = config.getProperty(cfgName, "SourceHome");
		String tokenFileBaseName =
				config.getProperty(cfgName, "TokenFileBaseName");
		Tokens tokens = Tokens.load(sourceHome + "/" + tokenFileBaseName, false);
		return create(config, cfgName, tokens);
	}


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public XmlSource(BitDataChannel source, Tokens tokens) {
		this.source = source;
		this.tokens = tokens;
		xds = new XmlDecoderStream(source, tokens);
	}


	public void getDocFragment(long pos, StringBuffer buf) throws IOException {
		source.bitPosition(pos);
		xds.readFragment(null, buf);
	}

	public void getDocFragment(long pos, String attrib, StringBuffer buf)
			throws IOException {
		source.bitPosition(pos);
		xds.readFragment(attrib, buf);
	}


	public void printDocFragment() {}



	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		Configuration cfg = Configuration.load(args[1]);
		XmlSource source = XmlSource.create(cfg, args[0]);
		StringBuffer buf = new StringBuffer(4096);

		for (int i = 2; i < args.length; i++) {
			System.out.println();
			System.out.println("________DF at Position " + args[i] + "_______");
			source.getDocFragment(Long.parseLong(args[i]), buf);
			System.out.println(buf);
			System.out.println();
		}
	}
}