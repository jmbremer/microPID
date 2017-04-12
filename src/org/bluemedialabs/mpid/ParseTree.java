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
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import org.bluemedialabs.util.*;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class ParseTree {
	private ParseTreeNode root;
	private int nodeCount;

	private DataGuide guide;
	private TermNodeMap map;
	private Terms terms;


	/**
	 * Constructs a new parse tree with a root node of the given label and
	 * type. A label of '*' of type child represents the root node of a source.
	 *
	 * @param label
	 * @param child
	 */
	public ParseTree(ParseTreeNode root, DataGuide guide, TermNodeMap map,
					 Terms terms) {
		this.root = root;
		// count nodes...
		this.guide = guide;
		this.map = map;
		this.terms = terms;
	}


	public boolean isPathPattern() {
		ParseTreeNode node = root;
		while (node != null && node.hasSingleChild()) {
			node = node.getFirstChild();
		}
		return (node == null);
	}

	public boolean isTreePattern() {
		return !isPathPattern();
	}


	public ParseTreeNode getRoot() { return root; }


	/**
	 * Matches this parse tree against the supplied DataGuide.
	 *
	 * @param guide
	 */
	protected void match() {
		matchNode(getRoot());
	}


	private void matchNode(ParseTreeNode ptNode) {
		Iterator it;
		int[] termNos;
		String label = ptNode.getLabel(), str;
		Term term;
		StringTokenizer tok;

		// Match this parse tree node with its parent node
		if (ptNode.getParent() == null) {
			// The root node of the parse tree
			if (ptNode.isTermNode()) {
				throw new IllegalStateException("The root node is a term node!?");
			}

			if (ptNode.isChildNode()) {
				if (ptNode.isStar()) {
					// This must be the root node
					ptNode.addMatch(guide.getRoot());
				} else {
					// A regular label node
					if(label.compareTo(guide.getRoot().getName()) != 0) {
						throw new NoSuchElementException(
								"There will be no matches, because "
							+ "the path's root label ("
							+ guide.getRoot().getName() + ")doesn't match!");
					} else {
						ptNode.addMatch(guide.getRoot());
					}
				}
			} else {
				// A descendant node
				if (ptNode.isStar()) {
					matchAllNodes(ptNode);
				} else {
					// All nodes with the given label match
					matchLabelNodes(ptNode);
				}
			}

		} else {
			// An inner node, so,...
			// ..fetch nodes for this node...

			// Now, we know all the matches for the parent-related path
			if (ptNode.isTermNode()) {
				matchTermNodes(ptNode);
			} else if (ptNode.isStar()) {
				matchAllNodes(ptNode);
			} else {
				matchLabelNodes(ptNode);
			}
			// ..and eliminate all this and this parent's nodes that do not
			// satisfy the structural relationship
//			joinNodes(ptNode.getParent(), ptNode);

		}
	}



	private void matchTermNodes(ParseTreeNode ptNode) {
		Iterator it;
		int[] termNos;
		String label, str;
		Term term;
		StringTokenizer tok;
		int count = 0;
		Queue q = new Queue(), q2 = new Queue(), tempQ;
		GuideNode guideNode;

		label = ptNode.getLabel();
		if (label.charAt(0) == '\'' || label.indexOf(' ') == -1) {
			// Single term
			str = label.substring(1, label.length() - 1);
			System.out.print("  Term '" + str + "'...");
			term = terms.get(str);
			if (term == null) {
				System.out.println("unknown.");
				throw new NoSuchElementException("There cannot be any "
						+ "match because the term '" + term + "' does "
								+ "not occur in the source");
			}
			termNos = new int[1];
			termNos[0] = term.getNo();
			System.out.println("known under #" + termNos[0]);
		} else {
			// Multiple terms delimited by spaces
			str = label.substring(1, label.length() - 1);
			tok = new StringTokenizer(str, " \t\r\n", false);
			termNos = new int[tok.countTokens()];
			count = 0;
			while (tok.hasMoreElements()) {
				count++;
				str = tok.nextToken();
				System.out.print("  Term '" + str + "'...");
				term = terms.get(str);
				if (term == null) {
					System.out.println("unknown.");
					throw new NoSuchElementException("There cannot be any "
							+ "match because the term '" + term + "' does "
							+ "not occur in the source");
				}
				termNos[count] = term.getNo();
				System.out.println("known under #" + termNos[0]);
			}
		}
		// Now that we have valid numbers for all terms in the condition...
		// ..determine all node numbers the terms occur in
		count = 0;
		// Fetch list for first term
		it = map.iterator(termNos[0]);
		while (it.hasNext()) {
			guideNode = guide.getNode(((MutableInteger) it.next()).getValue());
			q.enqueue(guideNode);
		}
		while (++count < termNos.length) {
			// Now fetch...
			it = map.iterator(termNos[count]);
			while (it.hasNext()) {
				guideNode = guide.getNode(((MutableInteger) it.next()).getValue());
				q2.enqueue(guideNode);
			}
			// ..and match nodes for all other terms
			andMatchNodes(q, q2);
			q2.clear();
		}
		// At this point "q" should contain all guide nodes that contain all
		// requested terms
		if (q.isEmpty()) {
			throw new NoSuchElementException("No match possible because terms "
					+ "do not occur together in any fragment");
		}
		// If everything went ok and we actually have some result,
		// then add all the result elements to the parse tree node
		while (!q.isEmpty()) {
			ptNode.addMatch((GuideNode) q.dequeue());
		}
		// Whow, done :-)
	}

	/**
	 * <p> Eliminates all guide nodes from the first queue that do not have a
	 * match in the second queue.
	 *
	 * @param anc
	 * @param desc
	 * @param child
	 */
	private void andMatchNodes(Queue anc, Queue desc) {
		int qElems = anc.getSize();
		GuideNode a, b = null;

		while (qElems > 0) {
			a = (GuideNode) anc.dequeue();
			b = (GuideNode) desc.dequeue();
			while (b != null && b.getNo() < a.getNo()) {
				b = (GuideNode) desc.dequeue();
			}
			if (b != null && b.getNo() == a.getNo()) {
				anc.enqueue(a);
			}
			qElems--;
		}
	}


	private void matchLabelNodes(ParseTreeNode ptNode) {
		NodeLabel nodeLabel = guide.getNodeLabel(ptNode.getLabel());
		for (int i = 0; i < nodeLabel.getNodeCount(); i++) {
			ptNode.addMatch(nodeLabel.getNode(i));
		}
	}


	private void matchAllNodes(ParseTreeNode ptNode) {
		// All nodes match
		for (int i = 1; i <= guide.getNodeCount(); i++) {
			ptNode.addMatch(guide.getNode(i));
		}
	}

/*
	private void matchWithParent(ParseTreeNode anc, ParseTreeNode desc,
								 boolean child) {
		Iterator ancIt = anc.getMatches(), descIt = desc.getMatches();
		GuideNode a = (GuideNode) ancIt.next();
		GuideNode d = (GuideNode) descIt.next();
		Quack stack = new Quack();
		Quack result = new Quack();
		boolean match = false;

		while (ancIt.hasNext() && descIt.hasNext()) {
//			?????


			while (descIt.hasNext()) {
				d = (GuideNode) descIt.next();
				if (d.getNo() < a.getNo()) {
					// d cannot be an ancestor of a
					descIt.remove();
				}
			}


//				d.getNo() > a.getEndNo() && ancIt.hasNext()) {
				a = ancIt.next();
			}
			if (!ancIt.hasNext()) {
				// There can't be any more matches
				descIt.remove();
				while (descIt.hasNext()) {
					descIt.remove();
				}
			}
			while (ancIt.hasNext()) {

			}
			ancIt = anc.getMatches();
			while (
		}
*/


	// Stack-tree
	private void joinNodes(ParseTreeNode anc, ParseTreeNode desc, boolean child) {
		Iterator ancIt = anc.getMatches(), descIt = desc.getMatches();
		GuideNode a, d;
		Quack stack = new Quack();
		Quack result = new Quack();

		if (child) {
			// Join based on parent-child relationships

		}
		a = (GuideNode) ancIt.next();
		d = (GuideNode) descIt.next();
		// (Hopefully, we have made sure earlier that both list have at
		//  least one element!)
		while (d != null) {
//			if (a.getNo() <
		}

	}

	static class GuideNodePair {
		GuideNode a;
		GuideNode b;
	}


	private int skipWhiteSpace(String str, int pos) {
		char ch = str.charAt(pos);

		while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
			pos++;
			ch = str.charAt(pos);
		}
		return pos;
	}


	public String toString() {
		if (!root.hasChildren() && root.isStar() && root.isChildNode()) {
			return "/";
		} else {
			return root.toString();
		}
	}




	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) {

	}
}