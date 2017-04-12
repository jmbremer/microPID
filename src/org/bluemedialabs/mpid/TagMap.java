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
import java.util.HashMap;
import java.util.Map;
import org.bluemedialabs.io.Storable;


/**
 * <p>Maintains and stores mappings between tag names and internal tag
 * numbers. Not used any more as functionality is already included in DataGuide.
 * But kept for possible future used.</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TagMap implements Storable {
    
    static private final int INITIAL_SIZE   = 100;
	private String[] tagNames = null;  // Maps element#s to element names
	private HashMap<String, Integer> tagNumbers = null;

	/**
	 * Constructs an optimal tag mapping from the information found in
	 * the supplied data guide.
	 */
	TagMap(DataGuide guide) {
		computeTagNumbers(guide);
	}

	TagMap() {
		// data still needs to be loaded...
	}

	void computeTagNumbers(DataGuide guide) {
		int no = 0;
		// Initialize element mapping data structures
		if (tagNumbers == null) {
			tagNumbers = new HashMap<String, Integer>(INITIAL_SIZE);
		} else {
			tagNumbers.clear();
		}
		// Walk through the elements
		for (GuideNode node: guide) {
		    Integer n = tagNumbers.get(node.getName());
            if (n == null) {
                // First time we see this element name
                tagNumbers.put(node.getName(), new Integer(++no));
            }
            // ..otherwise, there is nothing to do for us
		}
		// Now we mapped every element name to a number, it remains to...
		tagNames = new String[no + 1];  // don't use index 0!
		for (Map.Entry<String, Integer> entry: tagNumbers.entrySet()) {
		    tagNames[(entry.getValue()).intValue()] = entry.getKey();
		}
	}

	public int getNumber(String name) {
		Integer num;

		if (tagNumbers == null) {
			throw new IllegalStateException("Must call computeElementNumbers() "
				+ "before trying to obtain an element number, but that "
				+ "has not been called yet");
		}
		num = (Integer) tagNumbers.get(name);
		if (num == null) {
			return -1;
		} else {
			return num.intValue();
		}
	}

	public String getName(int number) {
		if (tagNames == null) {
			throw new IllegalStateException("Must call computeElementNumbers() "
				+ "before trying to obtain an element number, but that "
				+ "has not been called yet");
			// This might be overkill, as getElementNumber() will certainly
			// have been called previously
		}
		if (number <= 0 || number >= tagNames.length) {
			throw new IndexOutOfBoundsException("Invalid element number "
				+ number + ", valid numbers are 1.." + (tagNames.length - 1));
		}
		return tagNames[number];
	}


	public String toString() {
		StringBuffer buf = new StringBuffer(tagNames.length * 25);

		buf.append("(");
		if (tagNames != null && tagNames.length > 1) {
			buf.append(tagNames[1]);
			buf.append("/");
			buf.append(1);
			for (int i = 2; i < tagNames.length; i++) {
				buf.append(", ");
				buf.append(tagNames[i]);
				buf.append("/");
				buf.append(i);
			}
		}
		buf.append(")");
		return buf.toString();
	}

	
	/********************************************************************
	 * Storable implementation
	 ********************************************************************/

	public void store(DataOutput out) throws IOException {
		out.writeInt(tagNames.length - 1);
		for (int i = 1; i < tagNames.length; i++) {
			out.writeUTF(tagNames[i]);
			out.writeInt(i);     // Could spare this part (#s are sequential)
		}
	}

	public void load(DataInput in) throws IOException {
		int count = in.readInt();
		if (tagNames == null || tagNames.length != count + 1) {
			tagNames = new String[count + 1];
		}
		if (tagNumbers == null) {
			tagNumbers = new HashMap<String, Integer>(INITIAL_SIZE);
		}
		for (int i = 1; i < count; i++) {
			tagNames[i] = in.readUTF();
			tagNumbers.put(tagNames[i], new Integer(i));
		}
		// Now we *should* check the numbers (anything missing, etc. ...)
	}

	public int byteSize() {
		return -1;
	}

}
