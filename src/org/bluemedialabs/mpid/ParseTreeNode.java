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
import java.util.LinkedList;
import java.util.NoSuchElementException;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class ParseTreeNode {
	private String label;
	private ParseTreeNode parent = null;
	private ParseTreeNode firstChild = null;
	private ParseTreeNode nextSibling = null;
	private boolean child;
	// Is this node on the selection path (or a condition path?)
	private boolean select = false;
	private boolean term = false;
	private int from = -1, to = -1;

	private LinkedList matches = null;   // Matching nodes in the DataGuide


	ParseTreeNode(String str, boolean child, ParseTreeNode parent) {
		if (str.charAt(0) == '*') {
			if (str.length() > 1) {
				throw new IllegalStateException("Invalid ('*') label '"
						+ str + "'");
			}
			label = str;
		} else {
			// Let's assume for now that the label is valid
			label = str;
		}
		if (label.charAt(0) == '"' || label.charAt(0) == '\'') {
			term = true;
		}
		this.child = child;
		this.parent = parent;
	}

	/**
	 * Constructs the root node of a source. Identical to ParseTreeNode("*", true).
	 */
	ParseTreeNode() {
		this("*", true, null);
	}


	/**
	 * Adds a sibling node at the end of the sibling list regardless of
	 * the position of this node in the list of siblings.
	 *
	 * @param sibling
	 */
	public void addSibling(ParseTreeNode sibling) {
		assert (sibling != null);
		if (nextSibling == null) {
			nextSibling = sibling;
		} else {
			ParseTreeNode s = nextSibling;
			while (s.nextSibling != null) {
				s = s.nextSibling;
			}
			s.nextSibling = sibling;
		}
	}

	public ParseTreeNode getNextSibling() {
		return nextSibling;
	}

	// This is neat, isn't it! .-)
	public void addChild(ParseTreeNode child) {
		if (firstChild == null) {
			firstChild = child;
		} else {
			firstChild.addSibling(child);
		}
	}

	public ParseTreeNode getParent() { return parent; }

	public boolean isLabel() { return !isStar() && !isTermNode(); }
	public String getLabel() { return label; }
	public boolean isStar() { return label.charAt(0) == '*'; }
	public boolean hasChildren() { return (firstChild != null); }
	public boolean hasSingleChild() { return (firstChild != null
										&& firstChild.getNextSibling() == null); }
	public boolean isLeaf() { return !hasChildren(); }

	public void setSelectNode(boolean b) { select = b; }
	public boolean isSelectNode() { return select; }
	public boolean isTermNode() { return term; }
	public boolean isChildNode() { return child; }
	public boolean isDescNode() { return !child; }
//	public boolean isRootNode() { return (isChildNode() && isStar()); }
	public boolean isBranch() { return (firstChild != null
										&& firstChild.getNextSibling() != null); }


	public void setPosRange(int from, int to) {
		assert (from <= to);
		this.from = from;
		this.to = to;
	}
	public int getFromPos() { return from; }
	public int getToPos() { return to; }
	public boolean hasPosRange() { return (from == -1); }


	public Iterator getChildren() {
		assert (firstChild != null);
		return new NodeIterator(firstChild);
	}

	public ParseTreeNode getFirstChild() { return firstChild; }

//	public String toParentString(StringBuffer buf) {
//		ParseTreeNode parent = getParent();
//
//		if (parent != null) {
//			parent.toParentString(buf);
//		}
//		if (term) {
//			buf.append('[');
//		} else if (child) {
//			buf.append('/');
//		} else {
//			buf.append("//");
//		}
//		buf.append(label);
//		if (hasChildren()) {
//			it = getChildren();
//			while (it.hasNext()) {
//				node = (ParseTreeNode) it.next();
//				if (node.isSelectNode()) {
//					buf.append(node.toString());
//				} else if (node.isTermNode()) {
//					if (!node.isChildNode()) {
//						buf.append('[');
//					}
//					buf.append(node.toString());
//					if (!node.isChildNode()) {
//						buf.append(']');
//					}
//				} else if (isSelectNode() && !node.isSelectNode()) {
//					buf.append("[.");
//					buf.append(node.toString());
//					buf.append(']');
//				}
//			}
//		}
//		if (term) {
//			buf.append(']');
//		}
//	}


	public void addMatch(GuideNode node) {
		matches.add(node);
	}
	public Iterator getMatches() {
		return matches.iterator();
	}
	public int getMatchCount() {
		return matches.size();
	}


	public String toString() {
		StringBuffer buf = new StringBuffer(256);
		Iterator it;
		ParseTreeNode node;

		if (term) {
			buf.append('[');
		} else if (child) {
			buf.append('/');
		} else {
			buf.append("//");
		}
		buf.append(label);
		if (hasChildren()) {
			it = getChildren();
			while (it.hasNext()) {
				node = (ParseTreeNode) it.next();
				if (node.isSelectNode()) {
					buf.append(node.toString());
				} else if (node.isTermNode()) {
					if (!node.isChildNode()) {
						buf.append('[');
					}
					buf.append(node.toString());
					if (!node.isChildNode()) {
						buf.append(']');
					}
				} else if (isSelectNode() && !node.isSelectNode()) {
					buf.append("[.");
					buf.append(node.toString());
					buf.append(']');
				}
			}
		}
		if (term) {
			buf.append(']');
		}
		return buf.toString();
	}

	/**
	 * <p></p>
	 *
	 * @author J. Marco Bremer
	 * @version 1.0
	 */
	static public class NodeIterator implements Iterator {
		private ParseTreeNode head;

		public NodeIterator(ParseTreeNode head) {
			assert (head != null);
			this.head = head;
		}

		public boolean hasNext() {
			return (head != null);
		}

		public Object next() {
			ParseTreeNode tmp;

			if (!hasNext()) {
				throw new NoSuchElementException("There are no nodes left "
						+ "in this node iterator");
			}
			tmp = head;
			head = head.nextSibling;
			return tmp;
		}

		public void remove() {
			throw new UnsupportedOperationException("Removing of elements "
					+ "from node lists is not supported");
		}
	} // NodeIterator




	static public void main(String[] args) {

	}
}