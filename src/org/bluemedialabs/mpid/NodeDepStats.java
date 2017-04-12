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

//import java.util.List;
import java.text.DecimalFormat;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class NodeDepStats {
	static final DecimalFormat probability = new DecimalFormat("#.##");
	static final int MAX_PRINT = 100;

	short[] descA = null;
	short[] descB = null;

	int[] both = null;
	int[] aOnly = null;
	int[] bOnly = null;
	int[] none = null;

	int currentCount = 0;



	static public NodeDepStats[] init(DataGuide guide) {
		int nodeCount = guide.getNodeCount();
		GuideNode node = guide.getRoot();

		return null;
	}


	public NodeDepStats(int nodePairCount) {
		descA = new short[nodePairCount];
		descB = new short[nodePairCount];
		both = new int[nodePairCount];
		aOnly = new int[nodePairCount];
		bOnly = new int[nodePairCount];
		none = new int[nodePairCount];
	}

	public void addNodePair(short a, short b) {
		if (currentCount >= descA.length) {
			throw new IllegalStateException("Cannot add another node pair " +
					"as only " + descA.length + " pairs where allocated");
		}
		descA[currentCount] = a;
		descB[currentCount] = b;
		currentCount++;
	}

	public int getPairCount() { return currentCount; }

	public void addStats(int[] counts) {
		int a, b;

		for (int i = 0; i < currentCount; i++) {
			a = counts[descA[i]];
			b = counts[descB[i]];
			if (a > 0) {
				if (b > 0) {
					both[i]++;
				} else {
					aOnly[i]++;
				}
			} else {
				if (b > 0) {
					bOnly[i]++;
				} else {
					none[i]++;
				}
			}
		}
	}

	public int getBothCount(int i) { return both[i]; }
	public boolean isEquivalent(int i) { return (aOnly[i] == 0 && bOnly[i] == 0); }
	public int getNoneCount(int i) { return none[i]; }
	public boolean isWeakContradict(int i) { return (both[i] == 0); }
	public boolean isStrongContradict(int i) {
		return (isWeakContradict(i) && none[i] == 0);
	}
	public boolean isAimpliesB(int i) { return (aOnly[i] == 0);	}
	public boolean isBimpliesA(int i) { return (bOnly[i] == 0);	}
	public int getAonlyCount(int i) { return aOnly[i]; }
	public int getBonlyCount(int i) { return bOnly[i]; }


	public String toString(DataGuide guide, int nodeNo) {
		StringBuffer buf = new StringBuffer(currentCount * 40);
		int totalCount = guide.getNodeCount(nodeNo);
		int equivPairs = 0, weakContrPairs = 0, strongContrPairs = 0;
		int aImplB = 0, bImplA = 0;
		int total = currentCount;
		DecimalFormat percent = DepIndexer.percent;
		int maxPrint;

		// Print aggregated statistics
		for (int j = 0; j < currentCount; j++) {
			if (isEquivalent(j)) equivPairs++;
			if (isWeakContradict(j)) weakContrPairs++;
			if (isStrongContradict(j)) strongContrPairs++;
			if (isAimpliesB(j)) aImplB++;
			if (isBimpliesA(j)) bImplA++;
		}
		buf.append("Total pairs............. " + currentCount + "\n");
		buf.append("  ...equivalent......... " + equivPairs + " (");
		buf.append(percent.format(equivPairs / (double) total) + ")\n");
		buf.append("  ...purely implied..... " + (aImplB + bImplA - 2 * equivPairs) + " (");
		buf.append(percent.format((aImplB + bImplA - 2 * equivPairs) / (double) total) + ")\n");
		buf.append("    ...A implies B...... " + aImplB + " (");
		buf.append(percent.format(aImplB / (double) total) + ")\n");
		buf.append("    ...B implies A...... " + bImplA + " (");
		buf.append(percent.format(bImplA / (double) total) + ")\n");
		buf.append("  ...weak contradict.... " + weakContrPairs + " (");
		buf.append(percent.format(weakContrPairs / (double) total) + ")\n");
		buf.append("  ...strong contradict.. " + strongContrPairs + " (");
		buf.append(percent.format(strongContrPairs / (double) total) + ")\n");
		// Print full statistics unless there are too many elements for this
		buf.append("\nFull statistics.........\n");
		if (currentCount <= MAX_PRINT) {
			maxPrint = currentCount;
		} else {
			maxPrint = 25;
		}
		for (int i = 0; i < maxPrint; i++) {
			buf.append("(");
			buf.append(guide.getNode(descA[i]).getName());
			buf.append(",");
			buf.append(guide.getNode(descB[i]).getName());
			buf.append(") ");
			if ((both[i] + none[i]) == totalCount) {
				buf.append("<->");
			} else if (aOnly[i] == 0) {
				buf.append("->");
			} else if (bOnly[i] == 0) {
				buf.append("<-");
			} else {
				// Print full distribution
				buf.append(probability.format(both[i] / (double) totalCount));
				buf.append("/");
				buf.append(probability.format(aOnly[i] / (double) totalCount));
				buf.append("/");
				buf.append(probability.format(bOnly[i] / (double) totalCount));
				buf.append("/");
				buf.append(probability.format(none[i] / (double) totalCount));
			}
			buf.append("   ");
		}
		if (currentCount <= MAX_PRINT) {
			buf.append("\n <");
			buf.append(currentCount);
			buf.append(" pairs>");
		} else {
			buf.append("\n <");
			buf.append(currentCount - 25);
			buf.append(" more pairs>");
		}
		return buf.toString();
	}

}