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
package org.bluemedialabs.util;

/**
 * <p>Interface for all objects that can be cloned. An extra interface besides
 * Cloneable is required because Cloneable is a pure <em>flagging</em>
 * interface that does not actually contain any methods. In Object, where clone()
 * is defined, however, clone() is protected. Thus, clone() cannot be called if
 * nothing more about an object is known other than that it is an Object.</p>
 * <p><em>Is this still useful/useable in current versions of Java!? Need to
 * check.</em></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public interface CloneableObject<Type> extends Cloneable {

	/**
	 * Returns a (usually) deep copy of this object.
	 *
	 * @return A copy of the implementing object.
	 */
	public Type clone();

	/**
	 * Copies the implementing object <em>into</em> the supplied object of
	 * supposedly the same type. Does <em>not</em> copy the values of the
	 * supplied object into the implementing object!
	 *
	 * @param obj The object to copy this objects properties into.
	 * @throws ClassCastException If the supplied object is not of the same
	 *    class type as this object, in which case this object cannot know
	 *    how to copy itself
	 */
	public void copy(Type obj);

}