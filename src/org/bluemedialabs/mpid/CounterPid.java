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
public class CounterPid extends PathId {

	private int count;


	public CounterPid(short nodeNo, long posBits, int count) {
		super(nodeNo, posBits);
		this.count = count;
	}

	public CounterPid() {
		super();
	}


	public Object clone() {
		CounterPid cpid = new CounterPid(getNodeNo(), getPosBits(), count);
		return cpid;
	}

	public void copy(Object obj) {
		CounterPid pid = (CounterPid) obj;
		pid.setNodeNo(getNodeNo());
		pid.setPosBits(getPosBits());
		pid.setCount(count);
	}

	public void setCount(int count) { this.count = count; }
	public int getCount() { return count; }


	public static void main(String[] args) {
	}
}