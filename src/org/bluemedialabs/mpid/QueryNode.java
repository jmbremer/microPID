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
package org.bluemedialabs.mpid;

/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */

public class QueryNode {
	private boolean child;      // Is this node a child node or a descendant?
	private String labelOrTerm; // The node label, if present
	private boolean nodeLabel;      // The term, if present (only one of label, child!)


	public QueryNode(boolean child) {
		this.child = child;
		nodeLabel = false;
		// ...

	}
}