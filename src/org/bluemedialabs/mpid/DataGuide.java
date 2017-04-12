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
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import org.bluemedialabs.io.Storable;
import org.bluemedialabs.util.Configuration;
import org.bluemedialabs.util.FlagMatrix;
import org.bluemedialabs.util.MutableInteger;
import org.bluemedialabs.util.MyMath;
import org.bluemedialabs.util.ListFormat;
import org.bluemedialabs.util.Quack;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class DataGuide implements Codeables, Storable, Iterable<GuideNode> {
	private GuideNode root = new GuideNode(/*this,*/ null, "DataGuide");
	private TagMap tagMap = null;   // not needed any more !?
	private GuideNode[] nodeEnum = null;
	private FlagMatrix ancestors;
	private GuideNode[] nodes;
	private int totalNodeCount;
	private int nodeCount;
	private int nodeNoBits; // # of bits needed to encode a node number
	private int maxDepth;
	// NEW:
	private NodeLabel[] nodeLabels = null;
	private HashMap<String, MutableInteger> labelHash = null;


	/*+**********************************************************************
	 * Class Functions
	 ************************************************************************/

	static public DataGuide load(String fileName) throws IOException {
		System.out.print("Loading data guide from file '" + fileName
				+ "'...");
		DataInputStream in = new DataInputStream(new FileInputStream(fileName));
		DataGuide guide = new DataGuide();
		guide.load(in);
		System.out.println("done.");
		return guide;
	}

	static public DataGuide load(Configuration config, String id)
			throws IOException {
		return load(config.getProperty(id, "SourceHome") + "/"
					+ config.getProperty(id, "DataGuideFileName"));
	}
	
	
	/**
     * Iterator over the whole data guide in post (pre!?!) left order.
     */
    static private class GuideIterator implements Iterator<GuideNode> {
        LinkedList<Iterator<GuideNode>> itStack = 
                new LinkedList<Iterator<GuideNode>>();  // ..to store iterators
        Iterator<GuideNode> it;            // Iterator currently processed

        GuideIterator(DataGuide guide) {
            Iterator<GuideNode> it = guide.getRoot().iterator();
            if (it != null) {
                itStack.add(it);
            }
        }

        public boolean hasNext() {
            // As long as we still have an open iteration here there
            // must be more elements!
            return (itStack.size() > 0);
        }

        public GuideNode next() {
            Iterator<GuideNode> it;
            GuideNode node;

            if (!hasNext()) {
                throw new NoSuchElementException("There are no more elements in "
                    + "this data guide");
            }
            it = (Iterator<GuideNode>) itStack.getLast();
            if (!it.hasNext()) {
                throw new IllegalStateException("Found empty iterator in "
                    + "iterator stack while trying to get next node; this is "
                    + "not supposed to ever happen");
            }
            node = (GuideNode) it.next();
            if (!it.hasNext()) {
                itStack.removeLast();
            }
            it = node.iterator();
            if (it != null && it.hasNext()) {   // Can a returned it. be null??
                itStack.add(it);
            }
            return node;
        }

        public void remove() {
            throw new UnsupportedOperationException("Removing of nodes from a "
                + "data guide by means of an iterator is not yet supported");
        }
    } // GuideIterator


	/*+**********************************************************************
	 * Object Functions
	 ************************************************************************/

	public DataGuide() {
	}

	public Object clone() {
		DataGuide guide = new DataGuide();
		guide.root = (GuideNode) root.clone();
		return guide;
	}

	public void assignNumbers() {
		Iterator<GuideNode> it = iterator();
		GuideNode node, parent;
		int no = 0;
//		Quack stack = new Quack();
		int depth;
//		PathId pid = new PathId();

		/*
		 * NEW! Sort nodes by their node type before doing anything of the rest.
		 */
		sortByNodeType();

		nodeCount = countNodes();
		root.setNo(0);
		ancestors = new FlagMatrix(nodeCount + 1);
		nodes = new GuideNode[nodeCount + 1];
		nodeNoBits = (int) Math.ceil(MyMath.log2(nodeCount));
		nodes[0] = root;
		while (it.hasNext()) {
			node = (GuideNode) it.next();
			// Assign next number to node
			node.setNo(++no);
			nodes[node.getNo()] = node;
			// Create matrix of ancestor relationships
			parent = node.getParent();
			// ..and in between set numCount
			if (parent != null ) {
				node.setPosPartCount(parent.getPosPartCount());
			}
			if (node.getPosBitLen() > 0) {
				node.incPosPartCount();
			}
			depth = 0;
			while (parent != null) {
				ancestors.set(parent.getNo(), node.getNo());
				parent = parent.getParent();
				depth++;
			}
			node.setDepth(depth);
			if (depth > maxDepth) {
				maxDepth = depth;
			}
		}

		/*
		 * All the above should be replaced by Ival id scheme in the future.
		 * For the Ival scheme, we need the "endNo," i.e., the second
		 * part of a node-related Ival id. The first part of the Ival id
		 * is the node# itself as it is generated through a pre-order run
		 * through the XDG tree.
		 */

		createIvalIds(root);
	}

	protected int createIvalIds(GuideNode node) {
		if (node.isLeaf()) {
			// Leaf node (start == end)
			node.setEndNo(node.getNo());
			return node.getNo();
		} else {
			int no = -1;
			Iterator<GuideNode> it = node.getChildren().iterator();
			GuideNode n;
			while (it.hasNext()) {
				n = (GuideNode) it.next();
				no = createIvalIds(n);
			}
			node.setEndNo(no);
			return no;
		}
	}

	void sortByNodeType() {
		root.sortChildrenByType();
	}


	public void resetNodeCounters() {
		Iterator<GuideNode> it = iterator();
		GuideNode node;

		while (it.hasNext()) {
			node = (GuideNode) it.next();
			node.resetCounters();
		}
	}

	public boolean isAncestor(int anc, int desc) {
//		try {
//			return ancestors.test(anc, desc);
//		} catch (ArrayIndexOutOfBoundsException e) {
//			System.out.println("anc=" + anc + ", desc=" + desc);
//			throw e;
//		}
		return (nodes[anc].isAncOf(desc));
	}

	public boolean isDescendant(int anc, int desc) {
		return (nodes[anc].isAncOf(desc));
	}

	public boolean isChild(int parent, int child) {
		return (nodes[child].isChildOf(nodes[parent]));
	}

	public boolean isOnSamePath(int a, int b) {
		return (a == b || nodes[a].isAncOf(b) || nodes[b].isAncOf(a));
	}


	public int computeMaxPidLength() {
		// And right before that...
		// compute total number of bits required for each node's pid
		// number part...
		return (nodeNoBits + compute(root, 0));
	}

	private int compute(GuideNode node, int numLen) {
		int maxChildLen = 0;
		if (node.getChildren() != null) {
			Iterator<GuideNode> it = node.getChildren().iterator();
			while (it.hasNext()) {
				GuideNode n = (GuideNode) it.next();
				n.setTotalPosBitLen(numLen + n.getPosBitLen());
				int l = compute(n, numLen + n.getPosBitLen());
				if (l > maxChildLen) {
					maxChildLen = l;
				}
			}
		}
		return (node.getPosBitLen() + maxChildLen);
	}


	public GuideNode getRoot() {
		return root;
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public int getNodeNoBits() {
		return nodeNoBits;
	}

	public int getTotalNodeCount() {
		return totalNodeCount;
	}


	/**
	 * Rather expensive. Should only be used for debugging or output.
	 */
	public GuideNode getNode(int no) {
		/*
		Iterator it = iterator();
		GuideNode node = null;
		while (it.hasNext() && (node = (GuideNode) it.next()).no != no);
		return node;
		*/
		if (no >= nodes.length /*|| no <= 0*/) {
			throw new IllegalArgumentException("Invalid node# " + no
					+ ", valid node#s n are 1 <= n <= " + (nodes.length - 1));
		}
		return nodes[no];
	}

	public int getNodeCount() {
		return nodeCount;
	}

	public int getNodeCount(int nodeNo) {
		return getNode(nodeNo).getCount();
	}


	public NodeLabel getNodeLabel(String label) {
		return nodeLabels[((MutableInteger) labelHash.get(label)).getValue()];
	}

	public NodeLabel getNodeLabel(int index) {
		return nodeLabels[index];
	}

	public int getNodeLabelNo(String label) {
		return ((MutableInteger) labelHash.get(label)).getValue();
	}


	/**
	 * Returns a random node out of all guide tree nodes using the number of
	 * occurrences of each node as probability distribution.
	 */
	public GuideNode getRandomNode() {
		int rand = MyMath.random(1, totalNodeCount);
		int i = 0;
		Iterator<GuideNode> it = iterator();
		GuideNode n = (GuideNode) it.next();

		while (i + n.getParentCount() < rand) {
			i += n.getParentCount();
			try {
				n = (GuideNode) it.next();
			} catch (NoSuchElementException e) {
				throw new IllegalStateException("There seems to be something "
					+ "wrong with the random node selection (rand=" + rand
					+ ", totalNodeCount=" + totalNodeCount + ")");
			}
		}
		return n;
	}


	public Iterator<GuideNode> iterator() {
		return new GuideIterator(this);
	}

	public GuideNode[] getEnumeration() {
		if (nodeEnum == null) {
			nodeEnum = new GuideNode[countNodes() + 1];
			nodeEnum[0] = null;
			Quack q = new Quack();
			Iterator<GuideNode> it = getRoot().iterator();
			// Enqueue all the top-level nodes
			while (it.hasNext()) {
				q.enqueue(it.next());
			}
			createEnum(1, q);
		}
		return nodeEnum;
	}

	private void createEnum(int nextNo, Quack q) {
		// Dequeue first node...
		GuideNode node = (GuideNode) q.dequeue();
		if (node == null) return;   // Done.
		nodeEnum[nextNo++] = node;
		// ..and append its children to the queue
		Iterator<GuideNode> it = node.iterator();
		while (it.hasNext()) {
			q.enqueue(it.next());
		}
		createEnum(nextNo, q);
	}


	public int countNodes() {
		Iterator<GuideNode> it = iterator();
		int count = 0;
		GuideNode n;

		totalNodeCount = 0;
		while (it.hasNext()) {
			n = (GuideNode) it.next();
			count++;
			totalNodeCount += n.getCount();
		}
		return count;
	}
//	private int countNodes(GuideNode node) {
//		if (node.isLeaf()) {
//			return 1;
//		} else {
//			Iterator it = node.iterator();
//			int sum = 0;
//			while (it.hasNext()) {
//				sum += countNodes(it.next());
//			}
//			return sum;
//		}
//	}

	public int countUniqueNames(HashMap<String, MutableInteger> map) {
		Iterator<GuideNode> it = iterator();
		HashMap<String, MutableInteger> tagNames;
		GuideNode node;
		MutableInteger mi;
		int count = 0;

		if (map != null) {
			tagNames = map;
			tagNames.clear();
		} else {
			tagNames = new HashMap<String, MutableInteger>(countNodes());
		}
		while (it.hasNext()) {
			node = (GuideNode) it.next();
			mi = (MutableInteger) tagNames.get(node.getName());
			if (mi == null) {
				tagNames.put(node.getName(), new MutableInteger(1));
				count++;
			} else {
				mi.inc();
			}
		}
		return count;
	}


	private void generateNodeLabels() {
		HashMap<String, MutableInteger> labels =
		        new HashMap<String, MutableInteger>(getNodeCount());
		NodeLabel label;
		Iterator<GuideNode> it = iterator();
		GuideNode node;
		MutableInteger mi;
		int count = 0;

		// Determine basic parameters
		while (it.hasNext()) {
			node = (GuideNode) it.next();
			mi = (MutableInteger) labels.get(node.getName());
			if (mi == null) {
				labels.put(node.getName(), new MutableInteger(1));
				count++;
			} else {
				mi.inc();
			}
		}
		// Now, use the derived knwoledge to generate labels' array
		// and hash table
		labelHash = new HashMap<String, MutableInteger>(count * 2);
		nodeLabels = new NodeLabel[count + 1];
		nodeLabels[0] = null;  // unused
		count = 0;
		it = iterator();
		while (it.hasNext()) {
			node = (GuideNode) it.next();
			mi = (MutableInteger) labelHash.get(node.getName());
			if (mi == null) {
				mi = new MutableInteger(++count);  // Start with # 1
				label = new NodeLabel(node.getName());
				nodeLabels[mi.getValue()] = label;
				labelHash.put(node.getName(), mi);
			} else {
				label = nodeLabels[mi.getValue()];
			}
			label.addNode(node);
		}
		// Sort labels by name
		Arrays.sort(nodeLabels, 1, nodeLabels.length);
		for (int i = 1; i < nodeLabels.length; i++) {
			mi = (MutableInteger) labelHash.get(nodeLabels[i].getLabel());
			mi.setValue(i);
		}
	}


	public TagMap getTagMap() {
		if (tagMap == null) {
			tagMap = new TagMap(this);
		}
		return tagMap;
	}



	/*+**********************************************************************
	 * To string conversion functions
	 ************************************************************************/

	/**
	 * Returns a list-style string representation of this
	 */
	public String toString() {
		return toString(false);
	}

	private String toString(boolean justNames) {
		StringBuffer buf = new StringBuffer(4096);

		buf.append("(nodeCount=");
		buf.append(countNodes());
		buf.append(", totalNodeCount=");
		buf.append(totalNodeCount);
		buf.append(", uniqueNameCount=");
		buf.append(countUniqueNames(null));
		buf.append(", nodeNoBits=");
		buf.append(nodeNoBits);
		buf.append(", maxDepth=");
		buf.append(maxDepth);
		buf.append(", maxPidLen=");
		buf.append(computeMaxPidLength());
		buf.append(", nodeNoBits=");
		buf.append(getNodeNoBits());
		buf.append(", labels=(");
		buf.append(labelsToString());
		buf.append("), tree=(");
		if (justNames) {
			buf.append(root.toNameString());
		} else {
			buf.append(root.toString());
		}
		buf.append("))");
		return buf.toString();
	}

	public String toNameString() {
		return toString(true);
	}

	private String labelsToString() {
		NodeLabel label;
		StringBuffer buf = new StringBuffer(1000);
//		Iterator<GuideNode> it;
//		Map.Entry entry;
//		String name;

		if (nodeLabels == null) {
			return null;
		} else {
			buf.append("\n");
			for (int j = 1; j < nodeLabels.length; j++) {
				label = nodeLabels[j];
				buf.append("\t");
				buf.append(label.getLabel());
				if (label.getNodeCount() > 1) {
					buf.append(" [");
					buf.append(label.getNodeCount());
					buf.append("]");
				}
				buf.append("\t - ");
				if (label.getNodeCount() > 0) {
					buf.append(label.getNode(0).getNo());
					for (int i = 1; i < label.getNodeCount(); i++) {
						buf.append("; ");
						buf.append("" + label.getNode(i).getNo());
					}
					buf.append("\n");
				} else {
					buf.append("<no instances!??>\n");
				}
			}
		}
		return buf.toString();
	}

	/**
	 * Prints this data guide in tree-like format.
	 */
	public String toXML() {
		return null;
	}


	public String toTreeString() {
		GuideNode n;
		// Determine how long node# will get in terms of characters
		int numLen = (int) Math.ceil(Math.log(getNodeCount()) / Math.log(10));
		// Get actual root (considered as depth 0)
		n = (GuideNode) root.getChildren().getFirst();
		return toTreeString(n, 0, numLen);

	}

	private String toTreeString(GuideNode n, int depth, int noLen) {
		String PREFIX = "|   ";
		StringBuffer buf = new StringBuffer(100);
		List<GuideNode> children;
		Iterator<GuideNode> it;

		if (n != null) {
			// Construct prefix
			for (int d = 0; d < depth; d++) {
				buf.append(PREFIX);
			}
			// Append node name and number
			if (n.isAttrib()) {
				buf.append('-');
			} else {
				buf.append('+');
			}
			buf.append(n.getName());
			buf.append("  ");
			buf.append(n.getNo());

			// Iteratively do the same for all children, unless
			// they are attributes
			children = n.getChildren();
			if (children != null && (it = children.iterator()).hasNext()) {
				n = (GuideNode) it.next();
				if (n.isAttrib()) {
					while (n != null && n.isAttrib()) {
						buf.append("  -");
						buf.append(n.getName());
						buf.append(" ");
						buf.append(n.getNo());
						if (it.hasNext()) {
							n = (GuideNode) it.next();
						} else {
							n = null;
						}
					}
				}
				// All other children are regular elements, thus...
				buf.append("\n");
				buf.append(toTreeString(n, depth + 1, noLen));
				while (it.hasNext()) {
					n = (GuideNode) it.next();
					buf.append(toTreeString(n, depth + 1, noLen));
				}
			} else { // otherwise, there is not much to do
				buf.append("\n");
			}
			return buf.toString();
		} else {
			return "";
		}
	}



	/*+**********************************************************************
	 * Storable implementation
	 ************************************************************************/

	/**
	 * Stores itself and its child nodes in a preorder manner.
	 */
	public void store(DataOutput out) throws IOException {
		root.store(out);
		if (nodeLabels == null) {
			generateNodeLabels();
		}
		out.writeInt(nodeLabels.length);
		for (int i = 1; i < nodeLabels.length; i++) {
			nodeLabels[i].store(out);
		}
	}

	/**
	 * Does the store procedure in reverse order.
	 */
	public void load(DataInput in) throws IOException {
		root.load(in);
		assignNumbers();
		computeMaxPidLength();
		// And now also (NEW!)
		try {
			nodeLabels = new NodeLabel[in.readInt() + 1];
			for (int i = 1; i < nodeLabels.length; i++) {
				nodeLabels[i] = new NodeLabel(null);
				nodeLabels[i].load(in, this);
			}
			labelHash = 
			        new HashMap<String, MutableInteger>(nodeLabels.length * 2);
			for (int i = 1; i < nodeLabels.length; i++) {
				NodeLabel label = nodeLabels[i];
				labelHash.put(label.getLabel(), new MutableInteger(i));
			}
		} catch (IOException e) {
			// This probably means we are just loading an old data guide
			// which does not have this part yet, so, generate the information
			// instead to make sure the next store writes it out.
			// THIS PART CAN BE REMOVED SOMETIME IN THE FUTURE.
			generateNodeLabels();
		}
//		generateNodeLabels();
	}

	public int byteSize() {
		return -1;
	}


	/*+**********************************************************************
	 * Codeables implementation
	 ************************************************************************/

	public int getUniqueCount() {
		return (nodeEnum.length - 1);
	}

	public int getTotalCount() {
		return 0;
	}

	public void sort() {

	}

	public Codeable getCodeable(int idx) {
		return nodeEnum[idx];
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		boolean justNames = false;

		if (args.length != 2 && args.length != 3) {
			printUsage();
		}
		if (args.length == 3) {
			// Only print node names leading to a shorter string representation
			justNames = true;
		}
		// Load configuration
		IdxrConfig config = (IdxrConfig) IdxrConfig.load(args[1]);
		dumpDataGuide(config, args[0], justNames);
//		loadStore();
//		DataGuide guide = DataGuide.load(config, args[0]);
//		System.out.println(guide.toTreeString());
	}

	static private void printUsage() {
		System.out.println();
		System.out.println("Expecting 2 or 3 arguments for idxr.DataGuide:");
		System.out.println(" (1) The configuration name");
		System.out.println(" (2) The configuration file name");
		System.out.println("[(3) <Anything> to just print node names]");
	}


	// Just loads and stores the DG to add some new info.
	static public void loadStore() throws IOException {
//		DataGuide guide = DataGuide.load(Config.REP_HOME + "/guide.tree");
//		DataOutputStream out = new DataOutputStream(new FileOutputStream(
//				Config.REP_HOME + "/guide.tree2"));
//		guide.store(out);
//		out.flush();
//		out.close();
	}


	static public void dumpDataGuide(Configuration config, String cfgName,
									 boolean justNames) throws IOException {
		String sourceHome = config.getProperty(cfgName, "SourceHome");
		String fileName = config.getProperty(cfgName, "DataGuideFileName");
		DataGuide guide = DataGuide.load(sourceHome + "/" + fileName);
		String dumpBase = config.getProperty(cfgName, "DumpBase");
		String fileEnding = config.getProperty(cfgName, "DumpFileEnding");
		int pos = fileName.lastIndexOf('.');
		String dumpFile;
		if (pos > 0) {
			dumpFile = fileName.substring(0, pos) + fileEnding;
		} else {
			dumpFile = fileName + fileEnding;
		}
		String dumpFileName = dumpBase + "/" + cfgName;
		File file = new File(dumpFileName);
		if (!file.exists()) {
			file.mkdir();
		}
		dumpFileName += "/" + dumpFile;
		PrintWriter pw = new PrintWriter(new FileWriter(dumpFileName));

		pw.println("DataGuide file dump for '" + sourceHome + "/" + fileName + "'");
		pw.println("Generated on " + new java.sql.Timestamp(new java.util.Date().getTime()));
		pw.println();
		pw.println("Data guide is:\n");
		ListFormat.prettyPrint(guide.toString(justNames), pw);
		pw.println("\nAncestor relationships:");
		int nodeCount = guide.countNodes();
		int oldX = -1;
		for (int x = 0; x <= nodeCount; x++) {
			for (int y = 0; y <= nodeCount; y++) {
				if (guide.isAncestor(x, y)) {
					if (oldX < x) {
						oldX = x;
						pw.print("\n" + guide.getNode(x).getName() + ": \t");
					}
					pw.print("(" + x + "," + y + ") ");
				}
			}
		}
		pw.flush();
		pw.close();
	}

}