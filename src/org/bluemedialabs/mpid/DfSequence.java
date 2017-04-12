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

import java.util.*;
import org.bluemedialabs.util.MyPrint;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class DfSequence extends MyPrint {
	private ArrayList dfs = new ArrayList();
	private int matchCount = 0;
	private int termOccCount = 0;
	private DataGuide guide;

	// Some statistics
	public int totalMatchedCounters = 0;
	public int positiveMatches = 0;


	public DfSequence(DataGuide guide) {
		this.guide = guide;
	}

	public Object clone() {
		DfSequence d = new DfSequence(guide);
		d.dfs.addAll(dfs);
		return d;
	}

	public void add(DocFragment df) {
		dfs.add(df);
	}

	public DocFragment get(int i) {
		if (i < 0 || i >= dfs.size()) {
			throw new IndexOutOfBoundsException("Invalid index");
		}
		return (DocFragment) dfs.get(i);
	}

	public void remove(int i) {
		dfs.remove(i);
	}

	public int length() {
		return dfs.size();
	}

	public void resetCounters() {
		Iterator it = dfs.iterator();
		DocFragment df;

		while (it.hasNext()) {
			df = (DocFragment) it.next();
			df.resetCounters();
		}
		matchCount = 0;
		termOccCount = 0;
	}

	public void sort() {
		Collections.sort(dfs);
	}


	/**
	 * Adjust the DF's counters if there is the given counter relates to a
	 * (sub-)fragment of one of the DFs in the list.
	 */
	public void matchCounter(Counter c) {
		DocFragment df;
		try {

		for (int i = 0; i < dfs.size();  i++) {
			df = (DocFragment) dfs.get(i);
			if (df.getPid().contains(c.getPid(), guide)) {
				df.incTermOccCount(c.getCount());
				positiveMatches++;
//				if (positiveMatches % 1 == 0) {
//					p(df.getPid() + "\t" + c);
//					if (df.getPid().equals(c.getPid())) {
//						p(" ***");
//					}
//					pl();
//				}
			}
		}
		totalMatchedCounters += dfs.size();

		} catch (ArrayIndexOutOfBoundsException e) {
			pl("counter=" + c);
			throw e;
		}
	}

	public void matchInvList(InvertedDfList li) {
		Iterator it = li.iterator();
		Counter c;

		try {

		while (it.hasNext()) {
			c = (Counter) it.next();
			matchCounter(c);
		}

		} catch (ArrayIndexOutOfBoundsException e) {
			pl("invLi=" + li);
			throw e;
		}
	}


	public void matchInvList2(InvertedDfList li) {
		Iterator it = li.iterator();
		Counter c;
		DocFragment df;
		int dfStartPos = 0, liStartPos = 0;
		int liPos;
		PathId dfPid, cPid;
		long dfNumBits, adjNumBits, cNumBits;
		int dfNumLen;

		if (dfs.size() == 0) return;

		// Outer loop: walk through DFS elements
		while (dfStartPos < dfs.size()) {
			df = (DocFragment) dfs.get(dfStartPos);
			dfPid = df.getPid();
			dfNumBits = dfPid.getPosBits();
			dfNumLen = dfPid.getPosLen(guide);
//			p("\nSkipping list pos...");
			while (liStartPos < li.length()
					&& li.get(liStartPos).getPid().getNodeNo()
						< df.getPid().getNodeNo()) {
//				p(liStartPos + "("
//						+ li.get(liStartPos).getPid().getNodeNo() + ")..");
				liStartPos++;
			}
//			pl();
			// Now the inv. list's node# at least matches the current
			// DFS's element
			if (liStartPos < li.length()) {
				// There is at least on element left
				liPos = liStartPos;
				c = li.get(liPos);
				cPid = c.getPid();
//				pl("Processing DF node# " + df.getPid().getNodeNo() + "...");
				while (liPos < li.length()
						&& (guide.isAncestor(dfPid.getNodeNo(), cPid.getNodeNo())
							|| dfPid.getNodeNo() == cPid.getNodeNo())) {
					adjNumBits = cPid.getPosBits() >>>
											(cPid.getPosLen(guide) - dfNumLen);
					if ((dfNumBits - adjNumBits) == 0) {
						df.incTermOccCount(c.getCount());
						positiveMatches++;
//						if (positiveMatches % 1 == 0) {
//							p("[" + dfPid + "\t" + c);
//							if (dfPid.equals(cPid)) {
//								p(" ***");
//							}
//							pl("]");
//						}
					}
					totalMatchedCounters++;
					// Prepare next list element unless we handeled all already
					liPos++;
					if (liPos < li.length()) {
						c = li.get(liPos);
						cPid = c.getPid();
					}
				}
			} // if (liStartPos...
			dfStartPos++;
		}
	}



	public String toString() {
		StringBuffer buf = new StringBuffer(dfs.size() * 30);

		buf.append("(");
		if (dfs.size() > 0) {
			buf.append(dfs.get(0));
			for (int i = 1; i < dfs.size(); i++) {
//				buf.append(",\n ");
//				buf.append(dfs.get(i));
				buf.append("  ");
				buf.append(dfs.get(i));
			}
		}
		buf.append(")");
		return buf.toString();
	}




}