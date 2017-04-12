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

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.Map;
import org.bluemedialabs.util.MutableDouble;
import org.bluemedialabs.util.MutableInteger;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class RankedDoc extends Document implements Cloneable, Comparable {
	static private final DecimalFormat WEIGHT_FORMATTER;

	private float weight;        // Just temporary here...


	static {
		WEIGHT_FORMATTER = new DecimalFormat();
		WEIGHT_FORMATTER.setMaximumFractionDigits(6);
		WEIGHT_FORMATTER.setGroupingSize(1000);
	}


	protected RankedDoc(int no) {
		super(no);
		weight = 0;
	}
	public RankedDoc() {
		this(0);
	}

	public Object clone() {
		RankedDoc doc = new RankedDoc();
		clone(doc);
		return doc;
	}

	void clone(RankedDoc doc) {
		super.clone(doc);
		doc.weight = weight;
	}

	void setWeight(float w) { weight = w; }
	void addWeight(float w) { weight += w; }
	public float getWeight() { return weight; }

	public String toString() {
		StringBuffer buf = new StringBuffer(getId().length() + 90);

		buf.append("(doc=");
		buf.append(super.toString());
		buf.append(", weight=");
		buf.append(WEIGHT_FORMATTER.format(weight));
		buf.append(")");
		return buf.toString();
	}

	public String toTrecString(int queryId, int rank, String runName) {
		StringBuffer buf = new StringBuffer(100);

		buf.append(queryId);
		buf.append("  0  ");
		buf.append(getId());
		buf.append("  ");
		buf.append(rank);
		buf.append("  ");
		buf.append(weight);
		buf.append("  ");
		buf.append(runName);
		return buf.toString();
	}


	/*+**********************************************************************
	 * Comparable implementation
	 ************************************************************************/

	/**
	 * <p>Compares this and the supplied object based on their </p>
	 *
	 * @param obj The object to compare us to.
	 * @returns the String comparisons result between the two objects
	 */
	public int compareTo(Object obj) {
		// throws a NullPointerException (intentionally) if obj is null
		if (obj instanceof RankedDoc) {
			RankedDoc doc = (RankedDoc) obj;
			float diff = doc.getWeight() - weight;
			if (diff < 0) {
				return -1;
			} else if (diff > 0) {
				return 1;
			} else {
				return 0;
			}
		} else if (obj instanceof Document) {
			// ranked documents should always appear before unranked once
			return -1;
		} else {
			throw new IllegalArgumentException("Supplied object for RankedDoc "
				+ "comparison is not of document type");
		}
	}


	/************************************************************************
	 * A Comparator that compares simple key-value entries within a HashMap
	 * to be compared. The assumed key type is MutableInteger (for the document
	 * number). The expected value type is MutableDouble (for the weight).
	 ************************************************************************/
	static public class MapEntryComparator implements Comparator {

		public int compare(Object o1, Object o2) {
			Map.Entry e1, e2;
			int no1, no2;
			double w1, w2;
			double diff;

			e1 = (Map.Entry) o1;
			e2 = (Map.Entry) o2;
			w1 = ((MutableDouble) e1.getValue()).getValue();
			w2 = ((MutableDouble) e2.getValue()).getValue();
			diff = w2 - w1;
			if (diff < 0) {
				return -1;
			} else if (diff > 0) {
				return 1;
			} else {
				// ..let the document numbers decide (smaller wins here)
				no1 = ((MutableInteger) e1.getKey()).getValue();
				no2 = ((MutableInteger) e2.getKey()).getValue();
				return (no1 - no2);
			}
		}

		public boolean equals(Object obj) {
			return (obj instanceof MapEntryComparator);
		}
	}

}