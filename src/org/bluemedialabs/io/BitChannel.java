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
public interface BitChannel extends BitInput {

	/**
	 * Positions the internal pointer to the given Bit position.
	 *
	 * @param pos
	 * @throws IOException
	 */
	public void bitPosition(long pos) throws IOException;
}