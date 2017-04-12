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
package org.bluemedialabs.util;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class FlagSet {
	/**
	 * The maximum number of 32bit buckets a flag set may consist of
	 * considering only 16 bit indices.
	 */
	static private final int MAX_BUCKET_COUNT_16BIT = 2048; // 2^16 / 32
	/**
	 * Maximum number of flags supported for 16bit indices.
	 */
	static private final int MAX_FLAG_COUNT_16BIT = 65536;
	/**
	 * Mask for the five least significant bits that determine a particular
	 * flag's position within a bucket. Note that for changing the BUCKET_SIZE
	 * a different mask has to be used, e.g. six bits for a uint64. The design
	 * right now requires a change in the implementation for doing this for now.
	 */
	static private final int FIVE_BIT_MASK = 0x0001F; // least signif. 5 bits set

	int[] bit = new int[32]; // array of ints, each having exactly one bit set
	int[] flags;
	int bucketCount;
	int size;    // actual number of flags used

	/**
	 * Creates a new flag set with the supplied size (=number of single flags).
	 */
	public FlagSet(int size) {
		// What was the following for!??? (maybe from C++ implementation?)
		/*
		if (size > MAX_FLAG_COUNT_16BIT) {
			throw new IllegalArgumentException("Maximum number of flags in "
				+ "flag set is " + MAX_FLAG_COUNT_16BIT
				+ ", but given size is " + size);
		}
		*/
		bucketCount = (int) (size / 32);    // or:  size >>> 5
		if (bucketCount * 32 < size) {      // or: size & FIVE_BIT_MASK
			bucketCount++;
		}
		flags = new int[bucketCount];
		// set all flags to zero
		clearAll();
		// initialize single bit masks
		int flag = 1;
		bit[0] = flag;
		for (int pos = 1; pos < 32; pos ++) {
			flag = flag * 2;
			bit[pos] = flag;
			//cout << "Bit at position " << pos << " is " << bit[pos] << endl;
		}
	}

	public FlagSet() {
		this(MAX_FLAG_COUNT_16BIT);
	}

	/**
	 * Sets the flag at the given position.
	 */
	public void set(int pos) {
		int rest = pos & FIVE_BIT_MASK;
		pos >>>= 5;  // is this unsigned shift!?
		int bucket = flags[pos];
		bucket = (bucket | bit[rest]);
		flags[pos] = bucket;
	}

	/**
	 * Clears the flag at the given position.
	 */
	public void clear(int pos) {
		int rest = pos & FIVE_BIT_MASK;
		pos >>>= 5;
		int bucket = flags[pos];
		bucket = (bucket & (~bit[rest]));
		flags[pos] = bucket;
	}

	/**
	 * Clears all flags.
	 */
	public void clearAll() {
		for (int i = 0; i < bucketCount; i++) {
			flags[i] = 0;
		}
	}

	/**
	 * Checks whether the flag at the given position is set.
	 *
	 * @param pos
	 */
	public boolean test(int pos) {
		int rest = pos & FIVE_BIT_MASK;
		pos >>>= 5;
		int bucket = flags[pos];
		return ((bucket & bit[rest]) != 0);
	}


	private void printFlag(int pos) {
		System.out.println("Flag " + pos + " is "
			+ (test(pos)? "set": "not set"));
	}

	public static void main(String[] args) {
		FlagSet fs = new FlagSet();
		fs.set(17);
		fs.set(1038);
		fs.set(65535);
		fs.printFlag(0);
		fs.printFlag(1);
		fs.printFlag(16);
		fs.printFlag(17);
		fs.printFlag(18);
		fs.printFlag(1038);
		fs.printFlag(65535);
		fs.clear(65535);
		fs.printFlag(65534);
		fs.printFlag(65535);
	}

}