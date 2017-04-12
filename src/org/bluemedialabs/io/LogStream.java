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
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * A default {@link Logable} implementation for classes that do not need to
 * have any other base class and thus can inherit from LogStream for
 * conveniency. Can also be used stand-alone.
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class LogStream implements Logable {
	static public LogStream NULL_STREAM = new LogStream(null, null);
	static public LogStream DEFAULT_STREAM = new LogStream(null, System.out);

	protected PrintStream logStream = null;
	protected PrintStream printStream = null;
	private SimpleDateFormat dateFormat;


	public LogStream(OutputStream lStream, OutputStream pStream) {
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
		setLogStream(lStream);
		setPrintStream(pStream);
	}

	public LogStream(OutputStream lStream) {
		this(lStream, null);
	}

	public LogStream(LogStream ls) {
		this(ls.logStream, ls.printStream);
	}

	public LogStream() {
		this(null, System.out);
	}


	public synchronized void setLogStream(OutputStream out) {
		if (out != null) {
			if (out instanceof PrintStream) {
				logStream = (PrintStream) out;
			} else {
				logStream = new PrintStream(out);
			}
		}
	}

	public synchronized void setPrintStream(OutputStream out) {
		if (out != null) {
			if (out instanceof PrintStream) {
				printStream = (PrintStream) out;
			} else {
				printStream = new PrintStream(out);
			}
		}
	}

	public synchronized void setStreams(LogStream log) {
		logStream = log.logStream;
		printStream = log.printStream;
	}


	public synchronized void log(String msg) {
		if (logStream != null) {
			logStream.println(getLogPrefix() + msg);
			logStream.flush();
		}
		if (printStream != null) {
			printStream.println(msg);
		}
	}

	public synchronized void log(Object obj) {
		log(obj.toString());
	}


	protected String getLogPrefix() {
		return "[" + dateFormat.format(new Date()) + "]  ";
	}
}