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
 * <p>This interface is mainly just an indicator that an implementing class is
 * reuseable. Reuseable means, it can be re-initialized into a state equivalent
 * to a just constructed instance without creating a totally new object
 * (e.g., a copy). By convention, an implementing class should
 * provide a set of <em>reuse()</em> methods taking the same arguments as the
 * constructors.</p>
 * <p>No method is actually required by the interface to be implemented. Even a
 * default no-argument method cannot be required as reusable objects might not
 * want to provide a related constructor. An additional method,
 * reset(), clears an object and sets all values to their default.
 * By this means, an object's data cannot accidentially be made available to
 * a reuser of that object, e.g., in a general pool of reuseable objects.</p>
 * <p>As another convention, all the reuse() methods should be defined close
 * to the constructor definition in the code. The constructor might also just
 * call the related reuse method and not contain any other code itself.</p>
 * <p>In some instances, a single reuse() call might not be able to set a
 * completely new object state. This is the case, when a super class is not
 * reuseable and some instance variables can only be accessed by the constructor
 * or some getter and setter methods. In that case, the documentation should
 * clearly state, which state is reused automatically, and which properties
 * have to be manually set afterwards.</p>
 * <p>If the super class' state is partially not accessible within a subclass
 * (immutable), the subclass cannot be reuseable.</p>
 * <p>If the only constructor is the default constructor, reuse can be implicit
 * without explicitly calling any reuse() method.</p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public interface Reuseable {
	
    // Re-initializes objects of an implementing class.
    // public void reuse();
    // Same as reuse, ain't it?
    // public void reset();
}