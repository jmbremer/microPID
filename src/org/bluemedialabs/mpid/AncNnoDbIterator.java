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
import org.bluemedialabs.util.MutableLong;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class AncNnoDbIterator extends NnoDbIterator {
	private NnoDbIterator it;
	private int posShift;
//	private int ancNodeNo;


	public AncNnoDbIterator(int ancNodeNo, NnoDbIterator it) {
		super(ancNodeNo, it.getDataGuide());
//		this.ancNodeNo = ancNodeNo;
		this.it = it;
		checkNodeRelship();
	}

	public AncNnoDbIterator(GuideNode ancNode, NnoDbIterator it) {
		this(ancNode.getNo(), it);
	}

	private void checkNodeRelship() throws IllegalArgumentException {
		int nno = it.getNodeNo();
		int ancNno = getNodeNo();
		GuideNode node = getDataGuide().getNode(ancNno);
		if (!node.isAncOf(nno)) {
			throw new IllegalArgumentException("Cannot create ancestor db "
					+ "iterator with node# " + ancNno + " over iterator "
					+ "with node# " + nno + ", because the latter is not a "
					+ "descendant node");
		}
		// If everything is fine here, proceed by determining the nodes'
		// position number bit length difference
		posShift = getDataGuide().getNode(nno).getTotalPosBitLen()
		   - node.getTotalPosBitLen();
	}


	public boolean hasNext() {
		return it.hasNext();
	}

	public MutableLong next() {
		// Get
		MutableLong l = (MutableLong) it.next();
		l.setValue(l.getValue() >>> posShift);
		return l;
	}


	public int skip(int n) throws IOException {
		return it.skip(n);
	}

	public void close() throws IOException {
		it.close();
	}


	static public void main(String[] args) {
	}
}