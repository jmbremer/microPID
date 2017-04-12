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
import javax.swing.JTextArea;


/**
 * A writer that writes to a {@link JTextArea} instead of a stream.
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TextAreaWriter extends PrintWriter {
	private JTextArea textArea;

	public TextAreaWriter(JTextArea textArea) {
		super(System.out);
		this.textArea = textArea;
	}

	public void println(String str) {
		textArea.append(str + "\n");
	}

	public void print(String str) {
		textArea.append(str);
	}
}