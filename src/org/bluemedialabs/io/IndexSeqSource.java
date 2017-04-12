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
 * <p>The general interface to an arbitrary but read-only index-sequential
 * data source (ISAM source).</p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public interface IndexSeqSource {

	/**
	 * Returns the total number of records found in the source.
	 *
	 * @return The total number of records in this source.
	 */
	public int getRecordCount();

	/**
	 * Returns the current source pointer position. The position is based on
	 * the assumption that the last get() operation obtained the complete
	 * record and thus, after reading the object the source position is
	 * automatically at the beginning of the next record .(Should be deprecated
	 * because it is incompatible with partial object reads!??)
	 *
	 * @return
	 */
	public int getCurrentRecNo();

	/**
	 * Loads the object found at the supplied record position into the given
	 * destination object.
	 *
	 * @param recNo The record number the object is to be found at.
	 * @param obj The object to be loaded.
	 * @throws IOException Whenever the underlying source throws such.
	 */
	public void get(int recNo, Storable obj) throws IOException;

	/**
	 * Returns an open input stream (or channel, or ...) starting at the given
	 * record position. The stream is only valid until the next get() operation
	 * is executed at which point the stream position is undefined.
	 *
	 * @param recNo The record number to start the stream at.
	 * @return An open stream starting at the supplied record position.
	 * @throws IOException Whenever the underlying source throws such.
	 */
	public DataInput getDataInput(int recNo) throws IOException;

	/**
	 * CLoses the index-sequential data source.
	 *
	 * @throws IOException Whenever the underlying source throws such.
	 */
	public void close() throws IOException;
}