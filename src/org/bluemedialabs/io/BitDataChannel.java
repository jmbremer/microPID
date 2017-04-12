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
import java.io.IOException;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class BitDataChannel extends BitDataInput implements BitChannel {
	private DataChannel channel;

	public BitDataChannel(DataChannel in) {
		super(in);
		channel = in;
	}

	public BitDataChannel() {
		super();
		channel =null;
	}

	public BitDataInput reattach(DataInput in) {
		throw new UnsupportedOperationException("Must reattach to a DataChannel, "
				+ "not a DataInput as in the super class");
	}

	public BitDataChannel reattach(DataChannel in) throws IOException {
		super.reattach(in);
		channel = in;
		return this;
	}

	public void bitPosition(long pos) throws IOException {
		int bits;
		long bytes;

		if (pos != bitPosition()) {
			bits = (int) (pos % 8);
			bytes = pos / 8;
			channel.position(bytes);
			if (bits > 0) {
				buf = channel.readUnsignedByte();
				used = 8 - bits;
			} else {
				used = 0;
			}
			this.bitPos = pos;
		}
	}

}