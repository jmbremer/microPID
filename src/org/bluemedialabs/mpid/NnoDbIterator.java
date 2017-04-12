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
import org.bluemedialabs.io.DbIterator;
//import org.bluemedialabs.util.MutableLong;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public abstract class NnoDbIterator implements DbIterator {
	private int nodeNo;
	private DataGuide guide;


	public NnoDbIterator(int nodeNo, DataGuide guide) {
		this.nodeNo = nodeNo;
		this.guide = guide;
	}


//	void setNodeNo(int nno) { nodeNo = nno; }
	public int getNodeNo() { return nodeNo; }


	public DataGuide getDataGuide() { return guide; }


	public int skip(int n) throws IOException {
		int skipped = 0;
		while (hasNext() && skipped < n) {
			next();
			skipped++;
		}
		return skipped;
	}


	public void remove() {
		throw new UnsupportedOperationException("This iterator is read-only! "
				+ "Cannot remove any elements!");
	}


	// This is just a work around to allow PIndex position iterators to return
	// AIndex address positions:
	public int getCurrAddrIdx() { return -1; }

}