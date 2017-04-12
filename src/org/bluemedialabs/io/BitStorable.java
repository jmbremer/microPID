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
 * <p>This interface serves the same function as the {@link Storable} interface.
 * The difference is that here data is stored bit by bit thus objects can be
 * effectively encoded. An object may implement both the Storable and the
 * BitStorable interface with the understanding that all in- and output of
 * Storable is byte-aligned. Therefore, the same underlying storage routine
 * might be used for both interfaces. However, a flush() is required for
 * Storable(s) for every object in that case.</p>
 *
 * @author J. Marco Bremer
 * @version 0.1
 */
public interface BitStorable extends Storable {

	/**
	 * Writes the implementing object to the supplied {@link BitOutput}
	 * stream. The object <em>must</em> assume that more data will be written
	 * to the supplied stream thus, the stream may not be flushed.
	 */
	public void store(BitOutput out) throws IOException;

	/**
	 * Loads the implementing objects state from the given input source.
	 */
	public void load(BitInput in) throws IOException;

	/**
	 * Returns the number of bits the implementing object's state is encoded
	 * into when storing the object, or -1 if the object is of variable size.
	 */
	public long bitSize();

}