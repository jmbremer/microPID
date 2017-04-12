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
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class PercentPrinter {
	private long totalCount;
//	private long onePercent;
	private int currentPercent;

	public PercentPrinter(long totalCount) {
		reuse(totalCount);
	}
	
	public void reuse(long totalCount) {
	    this.totalCount = totalCount;
//        onePercent = totalCount / 100;
        currentPercent = -1;
	}

	public void reset() {
		currentPercent = -1;
	}

	public void notify(long count) {
		long p;

		if (currentPercent < 0) {
			System.out.print("|");
			currentPercent = 0;
		}
		// Do we have to advance by one or more percent?..
		if (count == totalCount) {
			p = 100;
		} else {
			p = (100 * count) / totalCount;
		}
		if (p > currentPercent) {
			// ...yes!
			for (int i = currentPercent + 1; i <= (int) p; i++) {
				if (p == 100) {
					System.out.print("|");
				} else if (i % 10 == 0) {
					System.out.print((i / 10) + "|0");
				} else if (i % 5 == 0) {
					System.out.print(";");
				} else {
					System.out.print(".");
				}
			}
			currentPercent = (int) p;
		}
	}

	/**
	 * Returns the percentage divided by 100 of the amount of work already
	 * processed.
	 *
	 * @return
	 */
	public int getPercent() {
		return currentPercent;
	}
}