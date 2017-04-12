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
import java.io.DataOutput;
import org.bluemedialabs.util.MutableLong;


/**
 * <p>An index sequential output that supports single records be written
 * in an encoded format to a bit output. However, notice that whole records
 * are still byte-aligned. Thus, up to seven bits at the end of the record
 * may be garbage. This has to be appropriately handled by the user.</p>
 * <p><em>Copyright (c) 2002 by J. Marco Bremer</em></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public abstract class IndexSeqBitOutput implements IndexSeqOutput {
	/**
	 * Do objects written to the data file expect a bit output or
	 * a (byte-aligned) data output?
	 */
	static public final boolean DEFAULT_BIT_OUTPUT = true;

	/** Used in the index file to indicate non-existing objects. */
	static protected final MutableLong MINUS_ONE = new MutableLong(-1);

	private BitDataOutput bitDataOut = null;


	public IndexSeqBitOutput(boolean bitOutput) throws IOException {
		if (bitOutput) {
			bitDataOut = new BitDataOutput(null);
		}
	}

	protected BitOutput getBitOutput(DataOutput out) throws IOException {
		// Should check for not null exception here...
		return bitDataOut.reattach(out);
	}

	protected void flushBitOutput() throws IOException {
		// Should check for not null exception here...
		bitDataOut.flush();
	}

	public boolean isBitOutput() {
		return (bitDataOut != null);
	}

}