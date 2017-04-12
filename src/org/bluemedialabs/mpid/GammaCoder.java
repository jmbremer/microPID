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
import org.bluemedialabs.io.BitInput;
import org.bluemedialabs.io.BitOutput;
import org.bluemedialabs.util.MyMath;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class GammaCoder {
	static private final int ALMOST_ALL_ONE = 0xFFFFFFFE;

	// Position zero is invalid!!!
	static private final int[] CODE = {
		0xFFFFFFFF, 0x0, 0x4, 0x5, 0x18, 0x19, 0x1A, 0x1B, 0x70, 0x71, 0x72,
		0x73, 0x74, 0,75, 0x76, 0x77
	};
	static private final int[] CODE_LEN = {
		-1, 1, 3, 3, 5, 5, 5, 5, 7, 7, 7, 7, 7, 7, 7, 7
	};
	static private final int PRECOMPUTED = 15;

	static public int codeLength(int x) {
		if (x == 1) {
			return 1;
		} else if (x <= 3) {
			return 3;
		} else if (x <= 7) {
			return 5;
		} else {
			// Actually compute length
			return (1 + 2 * (int) Math.floor(MyMath.log2(x)));
		}
	}


	static public int encode(int x, BitOutput out) throws IOException {
		assert x > 0: "Gamma code only for x > 0";
		if (x <= 15) {
			out.write(CODE[x], CODE_LEN[x]);
			return CODE_LEN[x];
		} else {
			// Well, too bad, we have to compute this code manually...
			// (Is can still be improved by doing some more precalculation, etc.
			int logX = (int) Math.floor(MyMath.log2(x));
			int len = 1 + (logX << 1);
			// The quicker solution...
			if (logX <= 31) {
				// Write length part including final zero in one write...
				out.write(ALMOST_ALL_ONE, logX + 1);
			} else {
				// This can be improved...
				for (int i = 0; i < logX; i++) {
					out.write(1, 1);
				}
				out.write(0, 1);
			}
			// So much for the unary length prefix.
			x -= (1 << logX);
			out.write(x, logX);
			return len;
		}
	}

	static public int decode(BitInput in) throws IOException {
		int bitCount = 1;

		if (in.read() == 0) {
			// That was easy ;-)
			return 1;
		}
		while (in.read() != 0) {
			bitCount++;
		}
		return ((1 << bitCount) + in.read(bitCount));  // 2^(bitCount) + code
	}


	static public class Code {
		int bits;
		// ...
	}

}