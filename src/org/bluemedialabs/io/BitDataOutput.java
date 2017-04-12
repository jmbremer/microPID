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

import java.io.DataOutput;
import java.io.IOException;


/**
 * <p></p>
 * <p>Note in particualr the special flush() handling to ensure proper writing
 * of the last output before closing of the underlying stream (data sink)!</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class BitDataOutput implements BitOutput {
	static long LONG_BIT_MASK_31 = 0x7FFFFFFF;
	static int[] X_BIT_MASK; // Masks for X lower order bits (X times 1)
	static private int EIGHT_BIT_MASK;  // Mask for 8 least significant bits
	private int buf = 0;     // Bits yet to be written (at most 7 !!)
	private int used = 0;    // Bits in use in buf (always < 8 !!)
	private long pos = 0;
	private DataOutput out;  // Underlying output stream/channel/...


	static {
		X_BIT_MASK = new int[33];
		X_BIT_MASK[0] = 0;
		X_BIT_MASK[1] = 1;
		for (int i = 2; i <= 32; i++) {
			X_BIT_MASK[i] = X_BIT_MASK[i-1] * 2 + 1;
		}
		EIGHT_BIT_MASK = X_BIT_MASK[8];
	}


	public BitDataOutput(DataOutput out) {
		this.out = out;
	}

	public BitDataOutput() {
		out = null;
	}


	/**
	 * Exchanges the underlying data output stream flushing all unwritten bits
	 * before. Notice that the bit stream may become inconsistent as holes
	 * may be introduced if more bits are later written to the same stream!
	 *
	 * @param out
	 * @return
	 * @throws IOException
	 */
	public BitDataOutput reattach(DataOutput out) throws IOException {
		if (out != null) {
			flush();
		}
		this.out = out;
		// Leave old channel unharmed!!
		return this;
   }


   /**
	* Writes the supplied number of bits from the given code integer to the
	* underlying stream. Notice that a length of zero does not break anything
	* even though there is no explicit check.
	*
	* @param code
	* @param len
	* @throws IOException
	*/
   public void write(int code, int len) throws IOException {
	   int completeLen = len + used;   // total # of bits left to be written

	   if (completeLen < 8) {
		   // Everything is a little easier as nothing has to be written
		   // Append to the existing rest
		   buf <<= len;
		   buf |= code;
		   used = completeLen;
		   pos += len;
	   } else {
		   int restLen = completeLen & X_BIT_MASK[3];  // completeLen % 8
		   // Buffer overflow bits
		   int rest = code & X_BIT_MASK[restLen];
		   int fullLen = len;
		   // Elminate overflow bits from code
		   code >>>= restLen;
		   // Determine new code length
		   len -= restLen;
		   // OR buffer in (knowing that the result will be exactly 32 bits)
		   code |= (buf << len);
		   len += used;
		   // Write rest back to buffer
		   buf = rest;
		   used = restLen;
		   /*
			* At this point, the byte aligned data of length <len> is in
			* <code> and the rest to remain in <buf> lateron is in <rest>.
			*/
		   // Write aligned bytes
		   while (len > 0) {
			   // Determine shift bit length
			   len -= 8;
			   out.write((code >>> len) & EIGHT_BIT_MASK);
			   // The "& EIGHT_BIT_MASK" isn't really necessary!?!
			   // (Because of the write() function.)
		   }
		   pos += fullLen;
	   }
   }

   // works only for up to 63, 62 bits!!?!!
   public void write(long code, int len) throws IOException {
	   if (len > 31) {
			write((int) (code >>> 31), len - 31);
			len = 31;
	   }
	   write((int) (code & LONG_BIT_MASK_31), len);

   }

   // What was this?? Doesn't make sense!
//   public void position(long pos) { this.pos = pos; }
   public long bitPosition() { return pos; }


/* OLD VERSION
	public void write(int code, int len) throws IOException {
	int rest = 0;
	int completeLen = len + used;   // total # of bits left to be written
	int byteCount = completeLen >> 3;   // = / 8
	int restLen = completeLen % 8;

	int freeBits = 8 - used;
	// NEW
	while (completeLen >= 8) {
		// ..as long as there are more full bytes...
		// Fill buffer byte
		buf <<= freeBits;
	}
	// OLD
	if (completeLen > 32) {
		// extract remaining bytes first
		try {
			rest = code & X_BIT_MASK[restLen];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw e;
		}
		code = code >>> restLen;
		len -= restLen;
		buf <<= len;
		code |= buf;
		len = 32;
		//			byteCount = 4;
	} else {
		// append buf and code first
		buf <<= len;
		code |= buf;
		len += used;
		if (restLen > 0) {
			rest = code & X_BIT_MASK[restLen];
			code = code >>> restLen;
			len -= restLen;
		}
	}
	// Now, code contains all the data of len bits and rest contains a
	// (potential) rest of restLen bits
	byteCount = len >> 3;   // redundant???
	writeBytes(code, byteCount);
	buf = rest;
	used = restLen;
	//		System.out.print("  " + used);
}


	private void writeBytes(int code, int byteCount) throws IOException {
		int c;
		for (int i = byteCount - 1; i >= 0; i--) {
			c = (code >> (8 * i)) & EIGHT_BIT_MASK;
			out.write(c);
			//			String str = Integer.toBinaryString(c);
			//			System.out.println("0000000000".substring(0, 8 - str.length()) + str);
		}
	}
*/

	/**
	 * Flushes this output stream and forces any buffered output bytes to be
	 * written out to the stream. Notice that flushing a bit stream makes
	 * writing any further output impossible!
	 * <p><em>Also VERY important to note: As a BitDataOutput is based on a
	 * DataOutput (interface), flushing the underlying stream is not
	 * possible. Thus, the user has to make sure to flush the underlying
	 * stream manually right after flushing this bit output!</em></p>
	 */
	public void flush() throws IOException {
		if (used > 0) {
			buf <<= (8 - used);
			out.write(buf);
			buf = 0;
			used = 0;
		}
	}

	/**
	 * Flushes the remaining bits and closes the underlying stream.
	 *
	 * @throws IOException
	 */
//	public void close() throws IOException {
	   // Write contents of this int buffer
//	   flush();
//   }


   static public void main(String[] args) throws Exception {
	   java.io.DataOutputStream dos = new java.io.DataOutputStream(
				new java.io.FileOutputStream("F:/Java/huff.tmp"));
		BitDataOutput bdo = new BitDataOutput(dos);
		int bits = 1657;
		int sum = 0;
		for (int i = 1; i <= 32; i++) {
			bdo.write((bits & X_BIT_MASK[i]), i);
			sum += i;
		}
		bdo.flush();
		dos.close();
		System.out.println("\nBits = " + sum + ",  bytes = "
				+ (sum >>> 3));
	}
}