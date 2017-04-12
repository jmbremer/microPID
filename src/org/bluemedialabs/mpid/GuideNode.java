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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import org.bluemedialabs.io.Storable;
import org.bluemedialabs.util.MyMath;


/**
 * <p>A single node within the data guide tree.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class GuideNode implements Codeables.Codeable, Storable, 
        Comparable<GuideNode> {
	/**
	 * Maximum number of parts the position bits may consist of, i.e.,
	 * the maximum number of nodes on any path in a DataGuide that may require
	 * any position bits for their PID encoding. This number is smaller or at
	 * most equal to the maximum depth of the source tree.
	 */
	static private final int MAX_POSITION_PARTS = 60;

//		DataGuide guide;
	private GuideNode parent;
	private LinkedList<GuideNode> children =  null;
	private boolean attrib  = false;  // Is this an attribute?
	private int no;                   // Assigned unique number
	private int endNo;                // Ival id end number
	private String name;              // Node (XML element/attribute) name
	private int depth;  // Depth of this node within DataGuide (root depth = 1)
	/**
	 * How many nodes with this name are there? Initially zero because
	 * incCount() increments all counters regardless of them being
	 * newly constructed or not.
	 */
	private int count            = 0;
	private int parentCount      = 0; // In how many nodes does this node occur in?
	private int minChildCount    = Integer.MAX_VALUE;  // Min & max counts for this...
	private int maxChildCount    = 0; // ..node as a child of its parent
	private int currentChildCount= 0; // Current count for this child label
	private int posBitLen        = 0; // # of bits required to encode maxChildCount
	private int totalPosBitLen   = 0; // # of bits needed for complete PID position bits
	/**
	 * Number of non-zero bitCount(s) in this node and its ancestors
	 * (= # of numbers that are part of the path id position part).
	 */
	private int posPartCount;

	private int tokenCount   = 0; // # of tokens in the related doc, fragment
//	private int minTokens    = 0;
//	private int maxTokens    = 0;
	private int currentTokens= 0;

	private byte codeLen     = 0; // Only for Codeable implementation

	// IR term-related statistical values...
	// ...per node basis for words
	private int totalWordCount   = 0;
	private int minWordCount     = Integer.MAX_VALUE;
	private int maxWordCount     = 0;
	private int expWordCount  = 0;
	private double varWordSum    = 0; // Sum over (X- E(X)) values for variance
	// ..per DF basis for words
	private int totalDfWordCount = 0;
	private int minDfWordCount   = Integer.MAX_VALUE;
	private int maxDfWordCount   = 0;
	private int expDfWordCount= 0;
	private double varDfWordSum  = 0;
	// ..per node basis for terms
	private int totalTermCount   = 0;
	private int minTermCount     = Integer.MAX_VALUE;
	private int maxTermCount     = 0;
	private int expTermCount  = 0;
	private double varTermSum    = 0;


	/**
	 * Temporary data used for generating PIDs for a certain node
	 */
	private GuideNode[] nodesWithBits = new GuideNode[MAX_POSITION_PARTS];


	/*+**********************************************************************
	 * Class functions
	 ************************************************************************/



	/*+**********************************************************************
	 * Object functions
	 ************************************************************************/

	 /**
	  * Constructs a new DataGuide node.
	  *
	  * @param parent
	  * @param name
	  */
	GuideNode(/*DataGuide guide,*/ GuideNode parent, String name) {
//			this.guide = guide;
		this.parent = parent;
		this.name = name;
	}

	GuideNode(/*DataGuide guide,*/ GuideNode parent) {
		this(/*guide,*/ parent, null);
	}

	public Object clone() {
		GuideNode n = new GuideNode(null);
		ListIterator<GuideNode> it;

		n.name = name;
		n.count = count;
		n.tokenCount = tokenCount;
		n.attrib = attrib;
		if (children != null) {
			// If we have children, clone them too (ok it's not allowed,
			// but is going to detect it here ;-)
			n.children = new LinkedList<GuideNode>();
			it = children.listIterator();
			while (it.hasNext()) {
				GuideNode m = (GuideNode) ((GuideNode) it.next()).clone();
				m.parent = n;
				n.children.add(m);
			}
		}
		return n;
	}



	/*+**********************************************************************
	 * Structure-related and general functions
	 ************************************************************************/

	protected void setNo(int n) { no = n; }
	public int getNo() { return no; }

	protected void setEndNo(int n) { endNo = n; }
	public int getEndNo() { return endNo; }

	public void incCurrentChildCount() { currentChildCount++; }
	public int getCurrentChildCount() { return currentChildCount; }

	public int getPosBitLen() { return posBitLen; }

	protected void setTotalPosBitLen(int bitLen) { totalPosBitLen = bitLen; }
	public int getTotalPosBitLen() { return totalPosBitLen; }

	protected void setPosPartCount(int count) { posPartCount = count; }
	protected void incPosPartCount() { posPartCount++; }
	public int getPosPartCount() { return posPartCount; }


	public long getPosBits() {
		GuideNode n = this;
		int len = 0;
		long bits = 0;

		// Obtain all parents (and own) child counts
		for (int i = 0; i < posPartCount; i++) {
			while (n.posBitLen <= 0) {
				n = n.parent;
			}
			nodesWithBits[i] = n;
			n = n.parent;
		}
		// Store them in order, parents' first
		for (int i = posPartCount - 1; i >= 0; i--) {
			bits <<= nodesWithBits[i].posBitLen;
			bits |= (long) (nodesWithBits[i].currentChildCount);  //NO! # = count - 1 !!!
			len += nodesWithBits[i].posBitLen;
		}
		if (len > 64 || len != totalPosBitLen) {
			throw new IllegalStateException("PosBits for node " + this
					+ "requires " + len + " (>64) bits or computed length ("
					+ len + ") is unequal precomputed length (" + totalPosBitLen
					+ ")!");
		}
		return bits;
	}


	public void getPid(PathId id) {
		GuideNode n = this;
		int len = 0;
		long bits = 0;
			// FOR DEBUGGING
//			int posBitLen = -1;
//			int currentChildCount = -1;

		// set node number
		id.setNodeNo(no);
		// obtain all parents (and own) child counts
		for (int i = 0; i < posPartCount; i++) {
			while (n.posBitLen <= 0) {
				n = n.parent;
			}
			nodesWithBits[i] = n;
			n = n.parent;
		}
		// store them in order, parents' first
		for (int i = posPartCount - 1; i >= 0; i--) {
			// FOR DEBUGGING
//				posBitLen = nodesWithBits[i].posBitLen;
//				currentChildCount = nodesWithBits[i].currentChildCount;

			bits <<= nodesWithBits[i].posBitLen;
			bits |= (nodesWithBits[i].currentChildCount);  //NO! # = count - 1 !!!
			len += nodesWithBits[i].posBitLen;
		}
		if (len > 64 || len != totalPosBitLen) {
			/*
			  DISABLE ONLY FOR LA TIMES!!! (Or anything else with PIDs
			  longer than 32 bits)
			  => NOT REQUIRED ANYMORE AS POSBITS ARE NOW LONG!
			 */
			throw new IllegalStateException("NumBits for node " + this
					+ "requires " + len + " (>32) bits or computed length ("
					+ len + ") is unequal precomputed length (" + totalPosBitLen
					+ ")!");
		}
		id.setPosBits(bits);
//			try {
//				id.setPosLen(len);
//			} catch (IllegalStateException e) {
//				System.out.println("Bug");
//			}
	}


	public void resetChildCounts() {
		if (children != null) {
			Iterator<GuideNode> it = children.listIterator();
			GuideNode n;
			while (it.hasNext()) {
				n = (GuideNode) it.next();
				n.currentChildCount = 0;
				// Do the same thing recursively to the child's children,
				// because all their counters should be reset!
				n.resetChildCounts();
			}
		}
	}


	/**
	 * ;-) Generates a pid to this node but with random (but valid)
	 * number path.
	 */
	public void getRandomPid(PathId id) {
		GuideNode n = this;
		int len = 0;
		int bits = 0;

		// set node number
		id.setNodeNo(no);
		// obtain all parents (and own) child counts
		for (int i = 0; i < posPartCount; i++) {
			while (n.posBitLen <= 0) {
				n = n.parent;
			}
			nodesWithBits[i] = n;
			n = n.parent;
		}
		// store them in order, parents' first
		for (int i = posPartCount - 1; i >= 0; i--) {
			bits <<= nodesWithBits[i].posBitLen;
			bits |= MyMath.random(0, nodesWithBits[i].maxChildCount - 1);
			len += nodesWithBits[i].posBitLen;
		}
		if (len > 32) {
			throw new IllegalStateException("NumBits for node " + this
					+ "requires " + len + " (>32) bits!");
		}
		id.setPosBits(bits);
		if (len != totalPosBitLen) {
			System.out.println("Mayday!");
		}
	}

	/**
	 * Sorts child nodes as follows. All attribute nodes come before element
	 * nodes. The order among attributes and among elements is preserved.
	 */
	public void sortChildrenByType() {
		if (!isLeaf()) {
			// Sort own children
			Collections.sort(getChildren(), new NodeTypeComparator());
			// Let all children know to do the same
			Iterator<GuideNode> it = iterator();
			GuideNode n;
			while (it.hasNext()) {
				n = (GuideNode) it.next();
				n.sortChildrenByType();
			}
		}
	}

	static class NodeTypeComparator implements Comparator<GuideNode> {
		 public int compare(GuideNode a, GuideNode b) {
			 if (b.isAttrib() && !a.isAttrib()) {
				 return 1;
			 } else {
				 return -1;
			 }
			 // The equal case can never occur
		 }

		 public boolean equals(Object obj) {
			 return (obj instanceof NodeTypeComparator);
		 }
	} // LabelComparator


	public String getName() { return name; }

	public String getLabelPath() {
		StringBuffer path = new StringBuffer(256);
		getLabelPath(this, path);
		return path.toString();
	}

	private void getLabelPath(GuideNode node, StringBuffer buf) {
		if (node.getDepth() > 0) {
			getLabelPath(node.getParent(), buf);
			buf.append("/");
			if (node.isAttrib()) {
				buf.append('@');
			}
			buf.append(node.getName());
		}
	}

	public void setAttrib(boolean a) { attrib = a; }
	public boolean isAttrib() { return attrib; }

	public boolean isLeaf() { return (children == null); }

	protected void setDepth(int d) { depth = d; }
	public int getDepth() { return depth; }

	public boolean isChildOf(GuideNode node) {
		return (isDescOf(node) && depth == node.getDepth() + 1);
	}
//	public boolean isParentOf(int nodeNo) {
//		return (isAncOf(nodeNo) && ...
//	}

	public boolean isDescOf(GuideNode node) {
		return (no > node.getNo() && no <= node.getEndNo());
	}

	public boolean isAncOf(int nodeNo) {
		return (nodeNo > getNo() && nodeNo <= getEndNo());
	}



	public int getChildCount() {
		return (children == null? 0: children.size());
	}
	public int getDescCount() {
		return (endNo - no);
	}

	/**
	 * Called at the end of an element DURING INDEXING (!) to obtain parent
	 * pointer. Can thus be used...
	 */
	public GuideNode returnToParent() {
		tokenCount += currentTokens;
		currentTokens = 0;
		return parent;
	}

	public GuideNode getParent() {
		return parent;
	}

	public boolean isEquivToParent() {
		if (getDepth() <= 1) {
			return false;
		} else {
			return (getTotalPosBitLen() == getParent().getTotalPosBitLen()
					&& getCount() == getParent().getCount());
		}
	}

	/**
	 * Determines and returns the closest ancestor node that is not (storage-)
	 * equivalent to this node, or this node if not even the parent is
	 * equivalent.
	 *
	 * @return
	 */
	public GuideNode getNonEquivAncOrSelf() {
		if (!isEquivToParent()) {
			return this;
		} else {
			return getParent().getNonEquivAncOrSelf();
		}
	}

	public void addChild(GuideNode child) {
		children.add(child);
	}

	public GuideNode getOrCreateChild(String name) {
		GuideNode child = getChild(name);
		if (child == null) {
			child = new GuideNode(/*guide,*/ this, name);
			if (children == null) {
				children = new LinkedList<GuideNode>();
			}
			children.add(child);
		}
		return child;
	}

	public GuideNode getChild(String name) {
		if (children == null) {
			return null;
		}
		ListIterator<GuideNode> it = children.listIterator();
		GuideNode child = null;
		boolean found = false;
		while (it.hasNext() && !found) {
			child = (GuideNode) it.next();
			found = (child.getName().compareTo(name) == 0);
		}
		if (found) {
			return child;
		} else {
			return null;
		}
	}


	public LinkedList<GuideNode> getChildren() {
		return children;
	}


	/**
	 * Notification of the element node being seen as child of current
	 * (parent) node.
	 */
	public void notifyNewChild() {
		incCount();
	}

	/**
	 * Executes notification of parent's end to all children.
	 */
	public void notifyParentEnd() {
		if (children != null) {
			Iterator<GuideNode> it = children.iterator();
			GuideNode child;
			while (it.hasNext()) {
				child = (GuideNode) it.next();
				child.notifyLastChild();
			}
		}
		// otherwise, there is nothing to do
	}

	/**
	 * <p>Notification routine to let this node know that a sequence of
	 * it (nodes with the label of this node) under a single parent has
	 * just come to an end. Only called by notifyParentEnd() !</p>
	 * <p>The initial values of minChildCount and maxChildCount ensure
	 * proper processing.</p>
	 */
	protected void notifyLastChild() {
		if (currentChildCount > 0) {
			// Adjust...
			// ..min/max child counter
			if (currentChildCount < minChildCount) {
				minChildCount = currentChildCount;
			}
			if (currentChildCount > maxChildCount) {
				maxChildCount = currentChildCount;
			}
			currentChildCount = 0;
			parentCount++;
		}
		// ..otherwise, this element has not been seen recently
	}

	public Iterator<GuideNode> iterator() {
		return (children == null? null: children.iterator());
	}

	protected void incCount() {
		count++;
		currentChildCount++;
	}
	protected void incCount(int incr) {
		count += incr;
		currentChildCount += incr;
	}
	public int getCount() { return count; }

	public int getParentCount() { return parentCount; }

	protected void setMinChildCount(int count) { minChildCount = 0; }
	public int getMinChildCount() { return minChildCount; }
	public int getMaxChildCount() { return maxChildCount; }



	/*+**********************************************************************
	 * Term statistics-related functions
	 ************************************************************************/

	public void incTokenCount() { currentTokens++; /*tokenCount++;*/ }
	public void incTokenCount(int incr) { currentTokens += incr; }


	protected void setTotalWordCount(int count) { totalWordCount = count; }
	protected void incTotalWordCount(int inc) { totalWordCount += inc; }
	public int getTotalWordCount() { return totalWordCount; }

	protected void setMinWordCount(int count) { minWordCount = count; }
	protected void incMinWordCount(int inc) { minWordCount += inc; }
	public int getMinWordCount() { return minWordCount; }

	protected void setMaxWordCount(int count) { maxWordCount = count; }
	protected void incMaxWordCount(int inc) { maxWordCount += inc; }
	public int getMaxWordCount() { return maxWordCount; }

	protected void setExpWordCount(int count) { expWordCount = count; }
	public int getExpWordCount() { return expWordCount; }

	protected void setVarWordSum(int sum) { varWordSum = sum; }
	protected void addVarWordSum(double value) { varWordSum += value; }
	public double getVarWordSum() { return varWordSum; }
	public double getWordCountVar() { return (getVarWordSum() / count); }
	public double getWordCountStdDev() { return Math.sqrt(getWordCountVar()); }
	public double getWordCountRelStdDev() { return (getWordCountStdDev() / getExpWordCount()); }


	protected void setTotalDfWordCount(int count) { totalDfWordCount = count; }
	protected void incTotalDfWordCount(int inc) { totalDfWordCount += inc; }
	public int getTotalDfWordCount() { return totalDfWordCount; }

	protected void setMinDfWordCount(int count) { minDfWordCount = count; }
	protected void incMinDfWordCount(int inc) { minDfWordCount += inc; }
	public int getMinDfWordCount() { return minDfWordCount; }

	protected void setMaxDfWordCount(int count) { maxDfWordCount = count; }
	protected void incMaxDfWordCount(int inc) { maxDfWordCount += inc; }
	public int getMaxDfWordCount() { return maxDfWordCount; }

	public int getDfWordCountBitLen() {
		int diff = getMaxDfWordCount() - getMinDfWordCount();
		// There is no point in storing counters n times, if all n counters
		// are the same length!
		switch (diff) {
			case 0: return 0;
			default: return (int) Math.ceil(MyMath.log2(diff + 1));
		}
	}

	// ~ log_2(nodeCount)
	public int getIndexBitLen() {
		return (count <= 1? 1: (int) Math.ceil(MyMath.log2(count + 1)));
	}

	protected void setExpDfWordCount(int count) { expDfWordCount = count; }
	public int getExpDfWordCount() { return expDfWordCount; }

	protected void setVarDfWordSum(int sum) { varDfWordSum = sum; }
	protected void addVarDfWordSum(double value) { varDfWordSum += value; }
	public double getVarDfWordSum() { return varDfWordSum; }
	public double getDfWordCountVar() { return (getVarDfWordSum() / count); }
	public double getDfWordCountStdDev() { return Math.sqrt(getDfWordCountVar()); }
	public double getDfWordCountRelStdDev() { return (getDfWordCountStdDev() / getExpDfWordCount()); }


	protected void setTotalTermCount(int count) { totalTermCount = count; }
	protected void incTotalTermCount(int inc) { totalTermCount += inc; }
	public int getTotalTermCount() { return totalTermCount; }

	protected void setMinTermCount(int count) { minTermCount = count; }
	protected void incMinTermCount(int inc) { minTermCount += inc; }
	public int getMinTermCount() { return minTermCount; }

	protected void setMaxTermCount(int count) { maxWordCount = count; }
	protected void incMaxTermCount(int inc) { maxWordCount += inc; }
	public int getMaxTermCount() { return maxWordCount; }

	protected void setExpTermCount(int count) { expTermCount = count; }
	public int getExpTermCount() { return expTermCount; }

	protected void setVarTermSum(int sum) { varTermSum = sum; }
	protected void addVarTermSum(double value) { varTermSum += value; }
	public double getVarTermSum() { return varTermSum; }
	public double getTermCountVar() { return (getVarTermSum() / count); }
	public double getTermCountStdDev() { return Math.sqrt(getTermCountVar()); }
	public double getTermCountRelStdDev() { return (getTermCountStdDev() / getExpTermCount()); }


	public void resetCounters() {
		totalWordCount   = 0;
		minWordCount     = Integer.MAX_VALUE;
		maxWordCount     = 0;
		expWordCount  = 0;
		varWordSum    = 0; // Sum over (X- E(X)) values for variance
		// ..per DF basis for words
		totalDfWordCount = 0;
		minDfWordCount   = Integer.MAX_VALUE;
		maxDfWordCount   = 0;
		expDfWordCount= 0;
		varDfWordSum  = 0;
		// ..per node basis for terms
		totalTermCount   = 0;
		minTermCount     = Integer.MAX_VALUE;
		maxTermCount     = 0;
		expTermCount  = 0;
		varTermSum    = 0;
	}


	/*+**********************************************************************
	 * To string conversion functions
	 ************************************************************************/

	public String toString() {
		return toString(false);
	}

	public String toString(boolean justNames) {
		StringBuffer buf = new StringBuffer(128);
		ListIterator<GuideNode> it;

		if (justNames) {
			buf.append("(");
			buf.append(no + "_" +  name);
			if (children != null) {
				buf.append(" (");
				it = children.listIterator();
				if (it.hasNext()) {
					buf.append(((GuideNode) it.next()).toNameString());
				}
				while (it.hasNext()) {
					buf.append(", ");
					buf.append(((GuideNode) it.next()).toNameString());
				}
				buf.append(")");
			}
			buf.append(")");
			return buf.toString();
		}
		buf.append("(" + no + "." + endNo + " name=");
		buf.append(name);
//		buf.append(", no=");
//		buf.append(no);
		buf.append(" [");
		if (isAttrib()) {
			buf.append("attrib. at ");
		} else if (isLeaf()) {
			buf.append("leaf at ");
		}
		buf.append("depth ");
		buf.append(depth);
		buf.append("], count [./parent/minChild/maxChild/token]=");
		buf.append(count);
		buf.append("/");
		buf.append(parentCount);
		buf.append("/");
		buf.append(minChildCount);
		buf.append("/");
		buf.append(maxChildCount);
		buf.append("/");
		buf.append(tokenCount);
		buf.append(", pos [bitLen/totalBitLen/partCount]=");
		buf.append(posBitLen);
		buf.append("/");
		buf.append(totalPosBitLen);
		buf.append("/");
		buf.append(posPartCount);
		// Term and word-related statistics
		buf.append(", wordCount [total/min/max/exp/var/stddev]=");
		buf.append(getTotalWordCount());
		buf.append("/");
		buf.append(getMinWordCount());
		buf.append("/");
		buf.append(getMaxWordCount());
		buf.append("/");
		buf.append(getExpWordCount());
		buf.append("/");
		buf.append(getWordCountVar());
		buf.append("/");
		buf.append(getWordCountStdDev());
		buf.append(", dfWordCount [total/min/max/exp/var/stddev]=");
		buf.append(getTotalDfWordCount());
		buf.append("/");
		buf.append(getMinDfWordCount());
		buf.append("/");
		buf.append(getMaxDfWordCount());
		buf.append("/");
		buf.append(getExpDfWordCount());
		buf.append("/");
		buf.append(getDfWordCountVar());
		buf.append("/");
		buf.append(getDfWordCountStdDev());
		buf.append(", termCount [total/min/max/exp/var/stddev]=");
		buf.append(getTotalTermCount());
		buf.append("/");
		buf.append(getMinTermCount());
		buf.append("/");
		buf.append(getMaxTermCount());
		buf.append("/");
		buf.append(getExpTermCount());
		buf.append("/");
		buf.append(getTermCountVar());
		buf.append("/");
		buf.append(getTermCountStdDev());

		// Same for all the children...
		if (children != null) {
			buf.append(", children=(");
			it = children.listIterator();
			if (it.hasNext()) {
				buf.append(it.next().toString());
			}
			while (it.hasNext()) {
				buf.append(", ");
				buf.append(it.next().toString());
			}
			buf.append(")");
		}
		buf.append(")");
		return buf.toString();
	}

	public String toNameString() {
		return toString(true);
	}

	public String toXML() {
		return null;
	}



	/*+******************************************************************
	 * Codeable implementation
	 ********************************************************************/

	public void setCodeLen(byte len) {
		codeLen = len;
	}
		public byte getCodeLen() {
			return codeLen;
		}
		// getCount() has already been defined earlier...


		/*+******************************************************************
		 * Storable implementation
		 ********************************************************************/

		/**
		 * Stores itself and its child nodes in a preorder manner.
		 */
		public void store(DataOutput out) throws IOException {
			ListIterator<GuideNode> it;
			// store own node information
			out.writeUTF(name);
			out.writeInt(count);
			out.writeInt(parentCount);
			out.writeInt(minChildCount);
			out.writeInt(maxChildCount);
			out.writeInt(tokenCount);
			out.writeBoolean(attrib);
			// *** NEW ***
			out.writeInt(totalWordCount);
			out.writeInt(minWordCount);
			out.writeInt(maxWordCount);
			out.writeInt(expWordCount);
			out.writeDouble(varWordSum);
			out.writeInt(totalDfWordCount);
			out.writeInt(minDfWordCount);
			out.writeInt(maxDfWordCount);
			out.writeInt(expDfWordCount);
			out.writeDouble(varDfWordSum);
			out.writeInt(totalTermCount);
			out.writeInt(minTermCount);
			out.writeInt(maxTermCount);
			out.writeInt(expTermCount);
			out.writeDouble(varTermSum);
			// *** END NEW ***
			if (children == null) {
				out.writeInt(0);
			} else {
				out.writeInt(children.size());
				it = children.listIterator();
				while (it.hasNext()) {
					((GuideNode) it.next()).store(out);
				}
			}
		}

		/**
		 * Does the store procedure in reverse order.
		 */
		public void load(DataInput in) throws IOException {
			int size;
			GuideNode child;

			name = in.readUTF();
			count = in.readInt();
			parentCount  = in.readInt();
			minChildCount  = in.readInt();
			maxChildCount = in.readInt();
			// Compute and set bits necessary to encode up to maxChildCount
			// children identifiers
			if (maxChildCount <= 1) {
				posBitLen = 0;
			} else {
				posBitLen = (int) Math.ceil(MyMath.log2(maxChildCount));
			}
			tokenCount = in.readInt();
			attrib = in.readBoolean();
			// *** NEW ***
			totalWordCount = in.readInt();
			minWordCount = in.readInt();
			maxWordCount = in.readInt();
			expWordCount = in.readInt();
			varWordSum = in.readDouble();
			totalDfWordCount = in.readInt();
			minDfWordCount = in.readInt();
			maxDfWordCount = in.readInt();
			expDfWordCount = in.readInt();
			varDfWordSum = in.readDouble();
			totalTermCount = in.readInt();
			minTermCount = in.readInt();
			maxTermCount = in.readInt();
			expTermCount = in.readInt();
			varTermSum = in.readDouble();
			// *** END NEW ***
			size = in.readInt();
			if (size > 0) {
				if (children == null) {
					children = new LinkedList<GuideNode>();
				} else {
					children.clear();
				}
				for (int i = 0; i < size; i++) {
					child = new GuideNode(/*guide,*/ this);
					child.load(in);
					children.add(child);
				}
			} else {
				children = null;
			}
		}

		public int byteSize() {
			return -1;
		}


	/*+**********************************************************************
	 * Comparable implementation
	 ************************************************************************/

	public int compareTo(GuideNode node) {
		return (getNo() - node.getNo());
	}

	public boolean equals(GuideNode node) {
		return (node.getNo() == getNo());
	}

} // GuideNode