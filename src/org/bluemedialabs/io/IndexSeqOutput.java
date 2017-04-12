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
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public interface IndexSeqOutput {

	/**
	 *
	 * @return
	 * @throws IOException
	 */
	public float getAvgRecLen() throws IOException;

	/**
	 *
	 * @return
	 */
	public int getRecordCount();

	/**
	 *
	 * @param obj
	 * @return
	 * @throws IOException
	 */
	public int write(Storable obj) throws IOException;

	/**
	 * Returns the underlying
	 */
	public DataOutput getDataOutput();

	/**
	 * Finishes writing to the current record by flushing the underlying
	 * data output and then writing the current position in the data file to
	 * the index file.
	 *
	 * @throws IOException
	 */
	public int nextRecord() throws IOException;

	/**
	 *
	 * @throws IOException
	 */
	public void close() throws IOException;

}