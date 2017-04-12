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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.bluemedialabs.io.Storable;


/**
 * <p>A Data Guide node label and all of its node instances within the data
 * guide.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class NodeLabel implements Comparable /* Pseudo-Storable */ {
	private String label;
	private GuideNode[] nodes = null;


	public NodeLabel(String label) {
		this.label = label;
	}

	public String getLabel() { return label; }

	public void addNode(GuideNode node) {
		GuideNode[] newNodes;

		if (node.getName().compareTo(label) != 0) {
			throw new IllegalArgumentException("Cannot add node " + node
					+ " as an instance to this node label '" + label
					+ "' because the labels don't match");
		}
		if (nodes == null) {
			// This is the first instance to be added
			nodes = new GuideNode[1];
			nodes[0] = node;
		} else {
			// Make sure this label is not registered yet
			for (int i = 0; i < nodes.length; i++) {
				if (nodes[i].getNo() == node.getNo()) {
					throw new IllegalArgumentException("GuideNode " + node
							+ " is already registered as an instance of this "
							+ " label '" + label + "'");
				}
			}
			newNodes = new GuideNode[nodes.length + 1];
			System.arraycopy(nodes, 0, newNodes, 0, nodes.length);
			newNodes[nodes.length] = node;
			nodes = newNodes;
			// Now, make sure the nodes are still sorted
			sort();
		}
	}

	private void sort() {
		if (nodes.length > 1) {
			GuideNode n;
			int i = nodes.length - 2;
			boolean smaller = (nodes[i].getNo() > nodes[i + 1].getNo());
			while (smaller) {
				// ...meaning, while new, last element is smaller than
				// the elements to its left..
				// swap elements
				n = nodes[i];
				nodes[i] = nodes[i + 1];
				nodes[i + 1] = n;
				i--;
			}
		} // ...otherwise there is nothing to sort
	}


	public GuideNode getNode(int index) {
		if (nodes == null || index >= nodes.length) {
			throw new IndexOutOfBoundsException("There are only "
					+ (nodes != null? nodes.length: 0)
					+ " nodes, but node " + index + " is requested");
		}
		return nodes[index];
	}

	public GuideNode[] getNodes() { return nodes; }

	public int getNodeCount() { return nodes.length; }


	/*+**********************************************************************
	 * Comparable implementation
	 ************************************************************************/

	public boolean equals(Object obj) {
		NodeLabel l = (NodeLabel) obj;

		return (l.getLabel().compareTo(label) == 0);
	}

	public int compareTo(Object obj) {
		NodeLabel l = (NodeLabel) obj;

		return label.compareTo(l.getLabel());
	}


	/*+**********************************************************************
	 * Storable implementation
	 ************************************************************************/

	public void store(DataOutput out) throws IOException {
		out.writeUTF(label);
		out.writeInt(nodes.length);
		for (int i = 0; i < nodes.length; i++) {
			out.writeInt(nodes[i].getNo());
		}
	}

	/**
	 *
	 */
	public void load(DataInput in, DataGuide guide) throws IOException {
		label = in.readUTF();
		int len = in.readInt();
		nodes = new GuideNode[len];
		int no;
		for (int i = 0; i < len; i++) {
			no = in.readInt();
			nodes[i] = guide.getNode(no);
		}
	}

	public int byteSize() {
		return -1;
	}

}