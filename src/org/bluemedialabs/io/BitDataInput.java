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
import java.io.EOFException;
import java.io.IOException;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class BitDataInput implements BitInput {
	static private int[] BIT_MASK;
	static private int BIT_0_MASK;
	int buf;       // Buffer for the remaining bits (< 8)
	int used;      // # of bits in buf not yet returned (< 8)
	long bitPos;      // Current file position in Bits
	private DataInput in;

	static {
		BIT_MASK = new int[32];
		BIT_MASK[0] = 1;
		for (int i = 1; i < 32; i++) {
			BIT_MASK[i] = (BIT_MASK[i-1] << 1);
		}
		BIT_0_MASK = BIT_MASK[0];
	}


	public BitDataInput(DataInput in) {
		this.in = in;
		used = 0;
		bitPos = 0;
	}

	public BitDataInput() {
		in = null;
		used = 0;
		bitPos = 0;
	}

	public BitDataInput reattach(DataInput in) throws IOException {
		this.in = in;
		used = 0;
		bitPos = 0;
		return this;
   }


	public void close() throws IOException {
		// Nothing to be done here as DataInput cannot be closed.
	}


	/**
	 * Reads and returns the requested number of bits from the underlying input
	 * stream. (How should the end be handled?)
	 * <p></p>
	 */
	public int read(int len) throws IOException {
		/*
		if (bits > nextBit + 1) {
			fillBuffer();
		}
		nextBit -= bits;
		/*
		* The following may cause trouble at the end of the stream and
		* needs to be revised! There needs to be another check whether
		* there is really enough data left.
		*
		return ((buf >> (nextBit + 1)) & BitDataOutput.X_BIT_MASK[bits]);
		*/
		if (len <= used) {
			// Enough data left in our internal buffer
			used -= len;
			bitPos += len;
			return ((buf >>> used) & BitDataOutput.X_BIT_MASK[len]);
		} else {
			// Unfortunately, we have to get more data
			int result = buf & BitDataOutput.X_BIT_MASK[used];
			// int resultLen = used; -- not used anymore!?
			int fullLen = len;

			len -= used;
			used = 0; // At this point, no data in the buffer is used any more
			while (len >= 8) {
				// Here we can process whole bytes at once
				result <<= 8;
				result |= in.readUnsignedByte();
				len -= 8;
			}
			if (len > 0) {
				// Handle partial bytes to be returned and buffered
				// (BUT DO THIS PART ONLY, IF NOT ENOUGH DATA HAS BEEN READ
				//  ALREADY< BECAUSE OTHERWISE WE MIGHT ATTEMPT TO READ
				//  A BYTE THAT IS NOT THERE ANY MORE. THIS WILL LEAD TO
				//  A PREMATURE EOFEXCEPTION BEFORE THE LAST ELEMENT HAS BEEN
				//  RETURNED!!!)
				buf = in.readUnsignedByte();
				used = 8 - len;
				result <<= len;
				result |= (buf >>> used);
			}
			bitPos += fullLen;
			return result;
		}
	}

	public long readLong(int len) throws IOException {
		long l;
		if (len > 31) {
			l = (long) read(len - 31);
			l <<= 31;
			l |= (long) read(31);
		} else {
			l = (long) read(len);
		}
		return l;

	}

	/**
	 * Has a different semantics than the super class! The next bit is returned.
	 */
	public int read() throws IOException {
		/*
		if (nextBit < 0) {
			fillBuffer();
		}
		return ((buf >> nextBit--) & BIT_0_MASK);
		*/
		if (used == 0) {
			// Need to get more data
			buf = in.readUnsignedByte();
			used = 7;
			bitPos++;
			return (buf >>> 7);
		} else {
			bitPos++;
			return ((buf >>> --used) & BIT_0_MASK);
		}
	}


	public long bitPosition() { return bitPos; }


/* OLD
	private void fillBuffer() throws IOException {
		int b = 0;
		int bits = nextBit + 8;
		// bits in the while condition is always the number of bits -1 in the
		// buffer AFTER fetching the next byte
		while (bits < 32 && (b = in.readUnsignedByte()) >= 0) {
//			System.out.print("  0x" + Integer.toHexString(b) + "  ");
			buf <<= 8;
			buf |= b;
			bits += 8;
		}
		nextBit = bits - 8;
		if (b < 0) {
			if (nextBit < 0) {
				// We didn't even get a single more byte
				throw new EOFException("Regular end of BitInputStream");
			}
		} // else, we know for sure that we got at least some more data!
	}
*/


	static public void main(String[] args) throws Exception {
		  BitDataInput bdi = new BitDataInput(new java.io.DataInputStream(
				  new java.io.FileInputStream("F:/Java/huff.tmp")));
		  int bits, bit;
		  int len = 0;
		  int sum = 0;
		  try {
			  while(true) {
				  bit = bdi.read();
				  len++;
				  System.out.print(bit);
				  if (len % 64 == 0) {
					  System.out.println();
				  } else if (len % 8 == 0) {
					  System.out.print(" ");
				  }
			  }
		  } catch (EOFException e) {
			  System.out.println("\nEnd of file reached after " + len + " bits");
		  }
		  // Reopen input
		  bdi = new BitDataInput(new java.io.DataInputStream(
				  new java.io.FileInputStream("F:/Java/huff.tmp")));
		  for (int i = 1; i <= 32; i++) {
			  bits = bdi.read(i);
			  sum += i;
			  System.out.println("i = " + i + "\t" + Integer.toBinaryString(bits));
		  }
		  bdi.close();
		  System.out.println("\nBits = " + sum + ",  bytes = "
				  + (sum >>> 3));
	  }

}