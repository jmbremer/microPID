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

import java.io.IOException;
import java.io.DataInput;


/**
 * <p></p>
 * <p>Copyright (c) 2002 by J. Marco Bremer</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public abstract class IndexSeqBitSource implements IndexSeqSource {
	/**
	 * Do objects written to the data file expect a bit output or
	 * a (byte-aligned) data output?
	 */
	static public final boolean DEFAULT_BIT_INPUT = true;

	/** Used in the index file to indicate non-existing objects. */
//	static protected final MutableLong MINUS_ONE = new MutableLong(-1);

	private BitDataInput bitDataIn = null;


	public IndexSeqBitSource(boolean bitInput) throws IOException {
		if (bitInput) {
			bitDataIn = new BitDataInput(null);
		}
	}

	protected BitInput getBitInput(int recNo) throws IOException {
		if (!isBitInput()) {
			throw new IllegalStateException("This source has not been "
					+ "created as bit source, thus, getting a bit input "
					+ "is not supported");
		}
		return bitDataIn.reattach(getDataInput(recNo));
	}

	public boolean isBitInput() {
		return (bitDataIn != null);
	}

}