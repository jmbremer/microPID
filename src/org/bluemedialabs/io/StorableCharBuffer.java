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
import java.nio.CharBuffer;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class StorableCharBuffer implements CharSequence, Storable {
	private CharBuffer buffer = null;

	public StorableCharBuffer(CharBuffer buf) {
//		assert (buf != null);
		buffer = buf;
	}

	public StorableCharBuffer() {}


	public void setBuffer(CharBuffer buf) {
		buffer = buf;
	}

	public CharBuffer getBuffer() {
		return buffer;
	}

  /*+**********************************************************************
   * CharSequence implementation
   ************************************************************************/

	public char charAt(int index) {
		return buffer.charAt(index);
	}

	public int length() {
		return buffer.length();
	}

	public CharSequence subSequence(int start, int end) {
		return buffer.subSequence(start, end);
	}

	public String toString() {
		return buffer.toString();
	}


  /*+**********************************************************************
   * Storable implementation
   ************************************************************************/

	public void store(DataOutput out) throws IOException {
		if (out instanceof WritableDataChannel) {
			((WritableDataChannel) out).writeCharBuffer(buffer);
		} else {
			out.writeUTF(buffer.toString());
		}
	}

	public void load(DataInput in) throws IOException {
		if (in instanceof ReadableDataChannel) {
			((ReadableDataChannel) in).readCharBuffer(buffer);
		} else {
			buffer = CharBuffer.wrap(in.readUTF());
		}
		// Caller has to make sure buffer is prepared!
	}

	public int byteSize() {
		return -1;
	}

}