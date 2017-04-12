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
 * <p>Currently, only good for look up of information related to a single
 * term.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class DocFragment implements Comparable {
//	private DocFragment parent = null;
//	private List children;

	/** Related path id */
	private PathId pid;
	/** Is this DF actually part of the DFS or is it just scaffolding? */
//	private boolean include = true;
	private int termOccCount;  // How often does this term occur here?
//    private int termCount;  // Number of unique terms in this DF. Not used yet
	private int wordCount;  // What's the total number of words?


	public DocFragment(PathId pid) {
		this.pid = pid;
//		this.parent = parent;
	}

	public DocFragment() {}


//	public DocFragment getParent() { return parent; }

	/**
	 * Adds the given child somewhere down from this node, creating all
	 * neccessary scaffolding fragments in between.
	 */
//	public void addChild(DocFragment df) {
//		if (children
//
//	}


	public void setPid(PathId pid) { this.pid = pid; }
	public PathId getPid() { return pid; }

	public void setTermOccCount(int count) { termOccCount = count; }
	public void incTermOccCount(int count) { termOccCount += count; }
	public int getTermOccCount() { return termOccCount; }

	public void setWordCount(int count) { wordCount = count; }
	public void incWordCount(int count) { wordCount += count; }
	public int getWordCount() { return wordCount; }

	public void resetCounters() {
		termOccCount = 0;
		wordCount = 0;
	}


	public String toString() {
		StringBuffer buf = new StringBuffer(60);

//		buf.append("(pid=");
//		buf.append(pid);
//		buf.append(", count=");
//		buf.append(termOccCount);
//		buf.append(")");
		buf.append(pid);
		return buf.toString();
	}


	/************************************************************************
	 * Comparable implementation
	 ************************************************************************/

	public boolean equals(Object obj) {
		DocFragment d = (DocFragment) obj;
		return pid.equals(d.getPid());
	}

	public int compareTo(Object obj) {
		DocFragment d = (DocFragment) obj;
		return (pid.compareTo(d.getPid()));
	}


}