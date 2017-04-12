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
public class MyPrint {
	static public final boolean DISABLED = false;

	static private final int[] POWER_10 = {
			1, 10, 100, 1000, 10000, 100000, 1000000
	};


	static public void p(String s) {
		if (!DISABLED) System.out.print(s);
	}
	static public void pl(String s) {
		if (!DISABLED) System.out.println(s);
	}
	static public void pl() {
		if (!DISABLED) System.out.println();
	}

	static public void pe(String s) {
		if (!DISABLED) System.err.print(s);
	}
	static public void ple(String s) {
		if (!DISABLED) System.err.println(s);
	}
	static public void ple() {
		if (!DISABLED) System.err.println();
	}
//	static public void pn(double d, int prec) {
//		int prec *= 10;
//
//		return Math.round(d * prec)
//					   / (double) 10000

	static public String toPercent(double d, int prec) {
		assert prec >= 0;
		double f = (prec <= 6? POWER_10[prec]: Math.pow(10.0, (double) prec));
		return String.valueOf(Math.round(d * f * 100) / f);
	}

	static public String toPercent(double d) {
		return toPercent(d, 0);
	}
}