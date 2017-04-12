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

import java.util.Iterator;
import org.bluemedialabs.util.MutableLong;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class DFS {
	static public final int DEFAULT_INITIAL_CAPACITY = 131072;
	private int[] nodeNos;
	private int[] start;
	private int startCount;
	private long[] elems;
	private int elemCount;
	private DataGuide guide;
	private int nodeCount;

	private int currentNodeNo = 0;  // Node# of pos# currently treated/added



	/*+**********************************************************************
	 * Base functionality
	 ************************************************************************/

	public DFS(DataGuide guide, int initialCapacity) {
		this.guide = guide;
		nodeCount = guide.getNodeCount();
		nodeNos = new int[nodeCount + 1];
		nodeNos[0] = -1;
		start = new int[nodeCount + 1];
		start[0] = -1;
		startCount = 0;
		elems = new long[initialCapacity];
		elemCount = 0;
	}

	public DFS(DataGuide guide) {
		this(guide, DEFAULT_INITIAL_CAPACITY);
	}


	public int size() { return elemCount; }

	public int size(int nodeNo) {
		int i = 0;
		while (i < startCount && nodeNos[i] != nodeNo) { i++; }
		if (i == startCount) {
			return 0;
		} else if (i == startCount - 1) {
			// The node#-related sublist is at the end
			return (elemCount - start[i]);
		} else {
			return (start[i+1] - start[i]);
		}
	}


	public boolean contains(int nodeNo) {
		int i = 0;
		while (i < startCount && start[i] != nodeNo) { i++; }
		if (i < startCount) {
			// Node# found
			return true;
		} else {
			return false;
		}
	}


	public void clear() {
		startCount = 0;
		elemCount = 0;
		currentNodeNo = 0;
	}


	public void nextNodeNo(int nodeNo) {
		assert (nodeNo > currentNodeNo):
			"Next node# needs to be greater than current node#";
		start[startCount++] = elemCount;
		currentNodeNo = nodeNo;
	}
	public int currNodeNo() {
		return currentNodeNo;
	}


	public void add(long posNo) {
		elems[elemCount++] = posNo;
	}

	public void add(Iterator it) {
		MutableLong ml;

		while (it.hasNext()) {
			ml = (MutableLong) it.next();
			add(ml.getValue());
		}
	}



	/*+**********************************************************************
	 * Index list loading (and joining)
	 ************************************************************************/

//	public void join(nodeNo,




	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) {
	}
}