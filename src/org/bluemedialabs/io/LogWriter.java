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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * A default {@link Logable} implementation for classes that do not need to
 * have any other base class and thus can inherit from LogWriter for
 * convenience.
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class LogWriter implements Logable {
	protected PrintWriter logStream;
	protected PrintWriter printStream = null;
	private SimpleDateFormat dateFormat;

	public LogWriter(LogWriter lw) {
		logStream = lw.logStream;
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
	}

	public LogWriter(OutputStream out) {
		logStream = new PrintWriter(new OutputStreamWriter(out));
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
	}

	public LogWriter() {
		this(System.out);
	}

	public void setLogStream(OutputStream out) {
		if (out != null) {
			synchronized(logStream) {
				logStream = new PrintWriter(new OutputStreamWriter(out));
			}
		}
	}

	public void setLogStream(LogWriter logWriter) {
		logStream = logWriter.logStream;
	}

	public void setPrintStream(OutputStream out) {}

	public void log(String msg) {
		synchronized(logStream) {
			logStream.println(getLogPrefix() + msg);
			logStream.flush();
		}
		if (printStream != null) {
			synchronized(printStream) {
				printStream.println(msg);
			}
		}
	}

	public void log(Object obj) {
		log(obj.toString());
	}


	public void setPrintLog(PrintWriter pw) {
		printStream = pw;
	}

	protected String getLogPrefix() {
		return "[" + dateFormat.format(new Date()) + "]  ";
	}
}