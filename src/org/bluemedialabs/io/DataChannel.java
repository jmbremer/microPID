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


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public interface DataChannel extends WritableDataChannel, ReadableDataChannel {
	/**
	 * Returns the current position, i.e., the starting point for the next
	 * read or write operation, within this data channel.
	 *
	 * @return The current channel position.
	 */
//	public long position() throws IOException;  -- now in Read/Writable...

	/**
	 * Sets a new starting position for the next read or write operation.
	 *
	 * @param pos The new channel position.
	 */
	public void position(long pos) throws IOException;

	/**
	 *
	 * @return
	 * @throws IOException
	 */
	public long size() throws IOException;

}