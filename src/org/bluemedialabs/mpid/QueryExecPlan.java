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
import java.text.ParseException;
import java.util.*;
import org.bluemedialabs.util.Configuration;
import org.bluemedialabs.util.Queue;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class QueryExecPlan {
	private ParseTreeNode root;
	private DataGuide guide;
	private HyperNode planRoot;


	public QueryExecPlan(ParseTree tree, DataGuide guide) {
		this.root = tree.getRoot();
		this.guide = guide;
//		derivePlan();
	}

	void derivePlan() {
		planRoot = new HyperNode(null, root);
		derivePlan(root, planRoot);
		// Plan root now points to [...]
		eliminatePlans(planRoot);

	}


//	public HyperNode match(ParseTree tree) {
//		return matchTree(tree.getRoot(), guide.getRoot());
//	}
//
//
//	HyperNode matchTree(ParseTreeNode ptNode, GuideNode rootNode) {
//		if (ptNode == null)
//	}


	void derivePlan(ParseTreeNode ptNode, HyperNode hyperNode) {
		Queue q = new Queue();
		GuideNode guideNode;
		ParseTreeNode node;
		HyperNode hNode;
		Iterator it;
		boolean hasSelectChild = false, selectHyperNode = false;

		if (ptNode.isLeaf()) {
			matchPath(ptNode, q);
			if (ptNode.isSelectNode()) {
				hyperNode.setSelectNode(true);
			}
			while (!q.isEmpty()) {
				guideNode = (GuideNode) q.dequeue();
				// hyperNode.addNode(guideNode);  -- needs modification
			}
			hasSelectChild = false;  //..simply because there are no children
		} else if (ptNode.isBranch() || (ptNode.isSelectNode()
				   && ptNode.hasSingleChild()
				   && !ptNode.getFirstChild().isSelectNode()) ) {
			// DON'T MATCH ANYTHING HERE, BUT WAIT FOR THE CHILDREN TO DO IT!!!
			matchPath(ptNode, q);
			while (!q.isEmpty()) {
				guideNode = (GuideNode) q.dequeue();
				// hyperNode.addNode(guideNode);  -- needs modification
			}
			// Proceed with children of ptNode
			it = ptNode.getChildren();
			while (it.hasNext()) {
				// Get child node
				node = (ParseTreeNode) it.next();
				// Create new hyper node for each child
				hNode = new HyperNode(hyperNode, node);  // Is node here correct???
				// hyperNode.addChild(hNode);  -- needs modification
				hasSelectChild = (hasSelectChild || node.isSelectNode());
				// ???:
//				if (node.isSelectNode()) {
//					hNode.setSelectNode(true);
//				}
				derivePlan(node, hNode);
			}

		} else {
			// We are just somewhere in ptNode, so, just go to the child
			node = (ParseTreeNode) ptNode.getChildren().next();
			hasSelectChild = node.isSelectNode();
			derivePlan(node, hyperNode);
		}

		selectHyperNode = (ptNode.isSelectNode() && !hasSelectChild);
		hyperNode.setSelectNode(selectHyperNode);
	}


	void matchPath(ParseTreeNode ptNode, Queue matches) {
		ParseTreeNode parent = ptNode.getParent();
		int parentMatchCount;
		String label = ptNode.getLabel();
		GuideNode node, node2;

		if (parent == null) {
			// Find the initial matches..
			if (ptNode.isTermNode() || (ptNode.isStar() && ptNode.isDescNode())) {
				// All nodes match
				for (int i = 1; i <= guide.getNodeCount(); i++) {
					matches.enqueue(guide.getNode(i));
				}
			} else if (ptNode.isStar() && ptNode.isChildNode()) {
				// This must be the root node
				matches.enqueue(guide.getRoot());
			} else {
				// A regular labeled node
				if (ptNode.isChildNode()) {
					if(label.compareTo(guide.getRoot().getName()) != 0) {
						System.out.println("There will be no matches, because "
								+ "the path's root label ("
								+ guide.getRoot().getName() + ")doesn't match!");
					} else {
						matches.enqueue(guide.getRoot());
					}
				} else {
					// A descendant node, all nodes with the given label match
					NodeLabel nodeLabel = guide.getNodeLabel(label);
					for (int i = 0; i < nodeLabel.getNodeCount(); i++) {
						matches.enqueue(nodeLabel.getNode(i));
					}
				}

			}
		} else {
			matchPath(ptNode.getParent(), matches);

			parentMatchCount = matches.getSize();
			// Now, we know all the matches for the parent-related path
			if (!ptNode.isTermNode()) {
				// A term condition is not to be matched against the data guide.
				// Thus, when we see a term condition, we are done.

				// Replace all nodes by their child nodes/descandant
				for (int i = 0; i < parentMatchCount; i++) {
					node = (GuideNode) matches.dequeue();
					if (ptNode.isChildNode()) {
						// Replace by child nodes
						LinkedList li = node.getChildren();
						if (li != null) {
							Iterator it = li.iterator();
							while (it.hasNext()) {
								node2 = (GuideNode) it.next();
								if (ptNode.isStar()
									|| node2.getName().compareTo(label) == 0) {
									matches.enqueue(node2);
								}
							}
						}
					} else {
						// Replace by descendant nodes
						for (int n = node.getNo() + 1; n <= node.getEndNo(); n++) {
							node2 = guide.getNode(n);
							if (ptNode.isStar()
								|| node2.getName().compareTo(label) == 0) {
								matches.enqueue(node2);
							}
						}
					}
				} // FOR (int i = 0...
			} // if (!..isTermNode()...
		}
	}


	void eliminatePlans(HyperNode hyperNode) throws IllegalStateException {
		Iterator hypeIt, nodeIt;
		GuideNode node;
		HyperNode h;
//		int childCount = hyperNode.

		if (!hyperNode.isLeaf()) {
			// First do the elimination for all the children
			hypeIt = null; // hyperNode.getChildren();  -- needs modification
			while (hypeIt.hasNext()) {
				h = (HyperNode) hypeIt.next();
				eliminatePlans(h);
				nodeIt = hyperNode.getNodes();
				if (!nodeIt.hasNext()) {
					throw new IllegalStateException("There cannot be any match, "
							+ "because the path sub-expression "
							+ hyperNode.toPathExpr()
							+ " does not match the DataGuide");
				}
				while (nodeIt.hasNext()) {
					node = (GuideNode) nodeIt.next();
					if (!h.hasAncestorMatch(node)) {
						nodeIt.remove();
					}
				}
			}

		}
	}



	/**
	 * Determines and returns the leaf nodes in this parse tree. The first
	 * leaf node returned relates to the path containing the selection nodes.
	 *
	 * @return
	 */
	public ParseTreeNode[] getLeafs() {
		return null;
	}


	public String toString() {
		return planRoot.toString();
	}



	/*+**********************************************************************
	 * Subclasses
	 ************************************************************************/

	 /**
	  * <p></p>
	  *
	  * @author J. Marco Bremer
	  * @version 1.0
	  */
	static class HyperNode {
		private LinkedList nodes; // List of contained Nodes
		private HyperNode parent;                         // Parent Node(!)
//		private LinkedList children = null;
		private boolean selectNode = false;          // Is this the select node?
		private boolean leaf = true;
		private ParseTreeNode ptNode;                // Related parse tree node


		public HyperNode(HyperNode parent, ParseTreeNode associate) {
			this.parent = parent;
			this.ptNode = associate;
		}

		protected void setSelectNode(boolean b) { selectNode = b; }
		public boolean isSelectNode() { return selectNode; }


		// A hypernode is a leaf
		void setLeaf(boolean l) { leaf = l; }
		public boolean isLeaf() { return leaf; }

		public void addNode(Node node) {
			if (nodes == null) {
				nodes = new LinkedList();
			}
			nodes.add(node);
		}
		public Iterator getNodes() {
			return nodes.iterator();
		}
		public boolean hasNodes() {
			return (nodes != null);
		}


		// Children are now attached to contained nodes!
//		public void addChild(HyperNode node) {
//			if (children == null) {
//				children = new LinkedList();
//			}
//			children.add(node);
//		}
//		public Iterator getChildren() {
//			return children.iterator();
//		}


		// ???
		boolean hasAncestorMatch(GuideNode anc) {
			Iterator nodeIt = getNodes();
			boolean match = false;
			GuideNode desc;

			while (!match && nodeIt.hasNext()) {
				desc = (GuideNode) nodeIt.next();
				match = desc.isDescOf(anc);
			}
			return match;
		}


		public String toPathExpr() {
			return  ptNode.toString();
		}


		public String toString() {
			StringBuffer buf = new StringBuffer(512);
			Iterator it;
			Node node;

			buf.append("(select=");
			buf.append((isSelectNode()? "yes": "no"));
			buf.append(", nodes=(");
			it = getNodes();
			while (it.hasNext()) {
				node = (Node) it.next();
				buf.append(node);
				if (it.hasNext()) {
					buf.append(", ");
				}
			}
			buf.append("))");
			return buf.toString();
		}


		private void appendNodes(StringBuffer buf, LinkedList li) {
			Iterator it = li.iterator();
			GuideNode node;

			while (it.hasNext()) {
				node = (GuideNode) it.next();
//				node = node.getGuideNode();
				buf.append(node.getName());
				buf.append("(");
				buf.append(node.getNo());
				buf.append(")");
				if (it.hasNext()) {
					buf.append(", ");
				}
			}
		}
	} // HyperNode



	/**
	 * <p></p>
	 *
	 * @author J. Marco Bremer
	 * @version 1.0
	 */
	static class Node {
		protected GuideNode guideNode = null;  // Related DataGuide node
		private HyperNode hyperNode = null;    // Container node
		private LinkedList children = null;    // HyperNode children


		protected Node(GuideNode gNode, HyperNode hNode) {
			this.guideNode = gNode;
			this.hyperNode = hNode;
		}


		GuideNode getGuideNode() { return guideNode; }

		HyperNode getHyperNode() { return hyperNode; }


		void addChild(HyperNode child) {
			if (children == null) {
				// This is the first child added
				children = new LinkedList();
				hyperNode.setLeaf(false);
			}
			children.add(child);
		}

		Iterator getChildren() {
			return children.iterator();
		}

		boolean isLeaf() {
			return (children == null);
		}

		boolean isRoot() {
			return (hyperNode == null);
		}


		public String toString() {
			StringBuffer buf = new StringBuffer(256);
			Iterator it = children.iterator();
			HyperNode hNode;

			buf.append("(node#=");
			buf.append(guideNode.getNo());
			buf.append(", label=");
			buf.append(guideNode.getName());
			buf.append(", children=(");
			while (it.hasNext()) {
				hNode = (HyperNode) it.next();
				buf.append(hNode);
				if (it.hasNext()) {
					buf.append(", ");
				}
			}
			buf.append("))");
			return buf.toString();
		}

	} // Node



	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws IOException, ParseException {
		String EMPTY = "                                              ";
//		String input = "/home//author[./name[[\"bremer\"]]]/address/street[['fish']]";
//		String input = "//PROCEEDINGS[.//MONTH]/*";
		String input = "//*[./MONTH]/*";
		PathExpr pathExpr;
		ParseTree tree = null;
		Configuration config = Configuration.load("/F/Java/project/idxr/idxr.cfg");
		DataGuide guide = DataGuide.load(config, "Big10");
		QueryExecPlan plan = null;

		if (args.length > 0) {
			input = args[0];
		}
		System.out.println("Parsing expression " + input + " ...");
		try {

			pathExpr = new PathExpr(input);
			tree = pathExpr.parse();

		} catch (ParseException e) {
			System.out.println("Parse exception at marked position:");
			System.out.println("   " + input);
			while (EMPTY.length() < e.getErrorOffset() + 3) { EMPTY += "     "; }
			System.out.print(EMPTY.substring(0, 3 + e.getErrorOffset()));
			System.out.println("^");
			System.out.println();
			e.printStackTrace();
		}
		System.out.println(input + " successfully parsed.");
		System.out.flush();
		System.out.println("Parse tree: " + tree);
		System.out.println();
		plan = new QueryExecPlan(tree, guide);
		plan.derivePlan();
		System.out.println(plan);
	}
}