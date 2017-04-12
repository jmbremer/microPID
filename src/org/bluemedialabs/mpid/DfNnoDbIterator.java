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
import java.util.NoSuchElementException;
import org.bluemedialabs.util.MutableLong;


/**
 * <p>...Returns StringBuffers.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class DfNnoDbIterator extends NnoDbIterator {
	private final String PREFIXa  =
			"<b style=\"color:black;background-color:#";
	private final String PREFIXb    = "\">";
	private final String SUFFIX     = "</b>";
	private final String[] COLORS   = {
		"ff5555", "55ff55", "5555ff", "ee9933"
	};
	private final int PREFIX_LENGTH =
			PREFIXa.length() + COLORS[0].length() + PREFIXb.length();
	private final int SUFFIX_LENGTH = SUFFIX.length();
	private final int JOINT_LENGTH  = PREFIX_LENGTH + SUFFIX_LENGTH;

	private XmlSource source;
	private PIndex pIndex;
//	private int nodeNo;
	private NnoDbIterator posIt;
	private StringBuffer buf = new StringBuffer(1024);
	private long posNo;
	private String[] terms;


	public DfNnoDbIterator(NnoDbIterator posIt, XmlSource source, PIndex pIndex,
						   String[] terms) {
		super(posIt.getNodeNo(), posIt.getDataGuide());
		this.posIt = posIt;
		this.source = source;
		this.pIndex = pIndex;
		this.terms = terms;
	}

	public DfNnoDbIterator(NnoDbIterator posIt, XmlSource source, PIndex pIndex) {
		this(posIt, source, pIndex, null);
	}


	public boolean hasNext() {
		return posIt.hasNext();
	}


	public Object next() {
		String prefix;

		if (!hasNext()) {
			throw new NoSuchElementException("There are no more "
					+ "position numbers left");
		}
		posNo = ((MutableLong) posIt.next()).getValue();
		try {
			source.getDocFragment(pIndex.getAddress(getNodeNo(), posNo), buf);
			if (terms != null) {
				// Mark the supplied terms...
				for (int i = 0; i < terms.length; i++) {
					prefix = PREFIXa + COLORS[i] + PREFIXb;
					markTerms(buf, terms[i], prefix, SUFFIX);
				}
			}
		} catch (IOException e) {
			buf.setLength(0);
			buf.append("Unable to retrieve content, because:");
			buf.append(e);

		}
		return buf;
	}

	private int markTerms(StringBuffer buf, String term,
						  String prefix, String suffix) {
		String str = buf.toString().toLowerCase();
		int offset = str.indexOf(term), off;
		int len = term.length();
		int matchCount = 0;

		off = offset;
		while (off >= 0) {
			buf.insert(off + JOINT_LENGTH * matchCount, prefix);
			buf.insert(off + len + PREFIX_LENGTH
						+ JOINT_LENGTH * matchCount, SUFFIX);
			off = str.indexOf(term, off + 1);
			matchCount++;
		}
		return offset;
	}



	public void close() throws IOException {
		posIt.close();
	}

}