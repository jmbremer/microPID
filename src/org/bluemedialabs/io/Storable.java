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


/**
 * Similar to {@link java.io.Serializable} and in particular
 * {@link java.io.Externalizable}, this interface is meant for objects to write
 * their internal state to a stream. However, Storable avoids the overhead
 * of the class information written by the foremost classes. Furthermore,
 * Storable only requires {@link DataInput} and {@link DataOuput} streams. This
 * allows {@link java.io.RandomAccessFile}s to be used for storing and loading
 * implementing classes.
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public interface Storable {
	/**
	 * Writes the implementing object to the supplied {@link WritableDataChannel}
	 * stream.
	 */
	public void store(DataOutput out) throws IOException;

	/**
	 * Adjusts the internal state of the implementing object according to the
	 * data read from the supplied channel.
	 */
	public void load(DataInput in) throws IOException;

	/**
	 * Returns the number of bytes the implementing object's state is encoded
	 * into when storing the object, or -1 if the object is of variable size.
	 * In the latter case, lastSize() must be determined for each store
	 * operation.
	 */
	public int byteSize();
}