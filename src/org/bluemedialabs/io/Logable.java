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

import java.io.OutputStream;


/**
 * Interface for classes that write some information to a <em>log stream</em>.
 * A log can be written to permanent storage and to the screen at the same time
 * by means of an additional <em>print stream</em>.
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public interface Logable {
	/**
	 * Sets the stream this Logable is writing its logs to. The initial
	 * default stream should be null, which means, no log is written.
	 *
	 * @param stream The new log stream.
	 */
	public void setLogStream(OutputStream stream);

	/**
	 * Sets the stream this Logable is printing to. The initial
	 * default stream should be {@link System.out}.
	 *
	 * @param stream The new print stream.
	 */
	public void setPrintStream(OutputStream stream);


	/**
	 * Writes a log message to the current log and print streams.
	 *
	 * @param msg The message to be written.
	 */
	public void log(String msg);

	/**
	 * Writes the string representation of the given object to the log
	 * and print streams.
	 *
	 * @param obj The object to write the string representation of.
	 */
	public void log(Object obj);
}