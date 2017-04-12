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

import java.io.PrintWriter;


/**
 * <p>Writes objects according to an encoded format
 * to a stream. Should be subclassed for different kinds of textual outputs,
 * like plain text, HTML, or XML.</p>
 * <p>Very similar to the {@link java.text.Format} class in general, here, only
 * output is supported. Moreover, instead of giving back strings, the formatted
 * text is written to a supplied stream.</p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public interface StreamFormat {

	/**
	 * <p>Prints the given object to the given character stream.</p>
	 * <p>Note that no I/O exception is thrown in case anything goes wrong writing
	 * to the supplied writer. This is, because the PrintWriter class itself
	 * never throws such exception. Instead, the user may check the error status
	 * using the {@link PrintWriter#checkError} method.</p>
	 *
	 * @param obj The object to be formatted.
	 * @param writer The stream to write the formatted object to.
	 * @throws IllegalArgumentException If the type of the object given is not
	 *  supported by this formatter.
	 */
	public PrintWriter format(Object obj, PrintWriter writer)
			throws IllegalArgumentException;

}