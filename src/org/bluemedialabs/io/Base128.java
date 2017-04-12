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
package org.bluemedialabs.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.bluemedialabs.util.MyPrint;


/**
 * <p>Supports the conversion from and to base 128-encoded numbers.</p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Base128 {
	static private final int SEVEN_BIT_MASK = 0x7F;
	static private final int BIT_EIGHT_MASK = 0x80;
	static private final int[] BIT_MASK = {
		0xF0000000, 0xFE000000, 0x1FC000, 0x3F80, 0x7F
	};
	static private final int[] SHIFT_LEN = {
		28, 21, 14, 7, -112
	};

	private int codeLen = 0;  // The code length of the last coding operation.


	/**
	 * Private constructor makes sure no one is instantiating this class.
	 */
	public Base128() {}


	/**
	 * Writes the given number to the given data destination in base 128
	 * encoding.
	 *
	 * @param n The integer to be encoded and written to the data output.
	 * @param out The output sink to write the encoded number to.
	 * @throws IOException Whenever such occurs.
	 */
	public void writeInt(int n, BitOutput out) throws IOException {
		int sevenBits;

		assert n >= 0: "Only positive integers for Base 128 numbers, please";
		// Extract 7 bits...
		if (n <= 127) {
			out.write(n, 8);
			codeLen = 8;
		} else { // ..otherwise, we know that there are some bits set
			int i = 0;
			while ((sevenBits = (n & BIT_MASK[i])) == 0) i++;
			// Now, i relates to the first group of seven bits unequal zero
			if (i < 4) {
				// More than 7 bits needed...
				for (int j = i + 1; j <= 4; j++) {
					sevenBits >>>= SHIFT_LEN[j - 1];
					sevenBits |= BIT_EIGHT_MASK;
					out.write(sevenBits, 8);
					sevenBits = n & BIT_MASK[j];
				}
			}
			// Write least significant seven bits (with cleared 8th bit)
			out.write(sevenBits, 8);
			codeLen = (40 - i) << 3;  // (40 - i) * 8
		}
	}


	/**
	 * Same as above but byte-aligned.
	 *
	 * @param n
	 * @param out
	 * @throws IOException
	 */
	public void writeInt(int n, DataOutput out) throws IOException {
		int sevenBits;

		assert n >= 0: "Only positive integers for Base 128 numbers, please";
		// Extract 7 bits...
		if (n <= 127) {
			out.writeByte(n);
			codeLen = 8;
		} else { // ..otherwise, we know that there are some bits set
			int i = 0;
			while ((sevenBits = (n & BIT_MASK[i])) == 0) i++;
			// Now, i relates to the first group of seven bits unequal zero
			if (i < 4) {
				// More than 7 bits needed...
				for (int j = i + 1; j <= 4; j++) {
					sevenBits >>>= SHIFT_LEN[j - 1];
					sevenBits |= BIT_EIGHT_MASK;
					out.writeByte(sevenBits);
					sevenBits = n & BIT_MASK[j];
				}
			}
			// Write least significant seven bits (with cleared 8th bit)
			out.writeByte(sevenBits);
			codeLen = (40 - i) << 3;
		}
	}


	/**
	 * Reads and returns the next base 128 encoded integer from the supplied
	 * bit input source.
	 *
	 * @param in The input data to obtain the next number from.
	 * @return The read number.
	 * @throws IOException Whenever such occurs.
	 */
	public int readInt(BitInput in) throws IOException {
		int n = in.read(8);
		int k;

		codeLen = 8;
		if ((n & BIT_EIGHT_MASK) != 0) {
			// There is more
			n &= SEVEN_BIT_MASK;
			while (((k = in.read(8)) & BIT_EIGHT_MASK) != 0) {
				n <<= 7;
				n |= (k & SEVEN_BIT_MASK);
				codeLen += 8;
			}
			n <<= 7;
			n |= k;
			codeLen += 8;
		} // ...otherwise, we are done
		return n;
	}


	/**
	 * Same as above, but byte-aligned.
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public int readInt(DataInput in) throws IOException {
		int n = in.readUnsignedByte();
		int k;

		codeLen = 8;
		if ((n & BIT_EIGHT_MASK) != 0) {
			// There is more
			n &= SEVEN_BIT_MASK;
			while (((k = in.readUnsignedByte()) & BIT_EIGHT_MASK) != 0) {
				n <<= 7;
				n |= (k & SEVEN_BIT_MASK);
				codeLen += 8;
			}
			n <<= 7;
			n |= k;
			codeLen += 8;
		} // ...otherwise, we are done
		return n;
	}


	/**
	 * Returns the code length in bits of the last coding operation executed.
	 *
	 * @return The number of bits of the last coding operation.
	 */
	public int getCodeLen() {
		return codeLen;
	}



  /*+**********************************************************************
   * TEST
   ************************************************************************/

	static public void main(String[] args) throws Exception {
		Base128 b128 = new Base128();

		// Test for bit outputs
		BufferedOutputChannel out =
				BufferedOutputChannel.create("/home/Java/base128.test");
		BitDataOutput bitOut = new BitDataOutput(out);
		int i, j;
		MyPrint.p("Writing test data...");
		for (i = 0; i < 1000000; i++) {
			b128.writeInt(i, bitOut);
		}
		bitOut.flush();
		out.flush();
		out.close();
		MyPrint.pl("completed.");

		BufferedInputChannel in =
				BufferedInputChannel.create("/home/Java/base128.test");
		BitDataInput bitIn = new BitDataInput(in);
		MyPrint.pl("Reading test data...");
		try {

			for (i = 0; i < 1000000; i++) {
				if ((j = b128.readInt(bitIn)) != i) {
					System.out.println(j + " != " + i);
				}
			}
			bitIn.close();
		} catch (Exception e) {
			MyPrint.pl("Exception at Base128 number " + i);
			e.printStackTrace();
		}
		MyPrint.pl("Non-byte-aligned test completed successfully.");

		// Test for byte-aligned output
		out = BufferedOutputChannel.create("/home/Java/base128.test");
		// Notice that BufferedOutputChannel's implement DataOutput
		MyPrint.p("Writing test data...");
		for (i = 0; i < 1000000; i++) {
			b128.writeInt(i, out);
		}
		out.flush();
		out.close();
		MyPrint.pl("completed.");

		in = BufferedInputChannel.create("/home/Java/base128.test");
		MyPrint.pl("Reading test data...");
		try {

			for (i = 0; i < 1000000; i++) {
				if ((j = b128.readInt(in)) != i) {
					System.out.println(j + " != " + i);
				}
			}
			in.close();
		} catch (Exception e) {
			MyPrint.pl("Exception at Base128 number " + i);
			e.printStackTrace();
		}
		MyPrint.pl("Byte-aligned test completed successfully.");

	}
}