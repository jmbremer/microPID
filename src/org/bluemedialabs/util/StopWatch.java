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
 * <p>A little stop watch. This class implements a stop watch's features: start,
 * get current time, stop. It returns a nice string representation for the
 * current time (if the watch is running) or the last stopped time.<p>
 * In addition, it calculates an average time for a process that ran n times
 * while the watch was running.</p>
 * <p><em>Is the synchronization still meant to be in here? Don't think so.
 * </em></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class StopWatch {
	private long startTime; // current start-time, 0 if stopped
	private long stopTime;  // last stopped time, 0 if never run or reset()
	private boolean running;// is the watch runnig currenly?

	/**
	 * Constructs a new stop-watch, resetting the internal time to zero and the
	 * internal state to "stopped".
	 */
	public StopWatch() {
		super();
		startTime = 0;
		stopTime = 0;
		running = false;
	}

	/**
	 * Starts the watch. If the watch is running it will be
	 * {@link #reset reset} before.
	 */
	public synchronized void start() {
		stopTime = 0;
		startTime  = System.currentTimeMillis();
		running = true;
	}

	/**
	 * Stops the watch, if it is running. Nothing is changed if it is not
	 * running.
	 */
	public synchronized void stop() {
		if (running) {
			stopTime = System.currentTimeMillis();
			running = false;
		}
	}

	/**
	 * Is the watch running?
	 *
	 * @return boolean, representing the watch's current running state
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Resets the watch as if it was just new constructed: it is not running
	 * and the internal time is zero.
	 */
	public synchronized void reset() {
		running = false;
		startTime = 0;
		stopTime = 0;
	}

	public synchronized void subtract(long time) {
		if (running) {
			startTime += time;
		} else {
			stopTime -= time;
		}
	}

	/**
	 * Gets the current time in milliseconds, if the watch is running
	 * ({@link #isRunning} is true), the last stopped time otherwise (may be
	 * zero if already reset).
	 *
	 * @return the current internal time in milliseconds
	 */
	public long getTime() {
		long time;

		time = System.currentTimeMillis();
		if (isRunning()) {
			// watch is still running
			time = time - startTime;
		} else {
			// watch is stopped
			time = stopTime - startTime;
		}

		return time;
	}

	/**
	 * Calculates an average time for a process, runnig the given counter times
	 * based on {@link #getTime()}.
	 *
	 * @param counter the number of times the process to be averaged has been
	 *  run
	 * @return {@link #getTime()} / counter (in milliseconds)
	 */
	public double getAverage(int counter) {
//		System.out.print(" **" + getTime() + "** ");
		return (double) getTime() / counter;
	}

	/**
	 * Returns a string representation of this stop-watch's current internal
	 * time in minutes using the format 2:12,89 (for two minutes, twelve
	 * seconds,..).
	 *
	 * @return a nice string representation of the stop-watch's current time
	 */
	public String toString() {
		int time;
		long currentTime;
		int min;
		int sec;
		int ms;
		String secStr;
		String msStr;

		currentTime = System.currentTimeMillis();
		if (!isRunning()) {
			time = (int) (stopTime - startTime);
		} else {
			time = (int) (currentTime - startTime);
		}
		ms = (int) (time - ((int) (time / 1000)) * 1000);
		min = (int) time / 60000;
		sec = (int) (((int) (time / 1000)) - 60 * min);

		secStr = String.valueOf(sec);
		if (secStr.length() < 2) {
			secStr = "0" + secStr;
		}

		msStr = String.valueOf(ms);
		if (msStr.length() < 2) {
			msStr = "00" + msStr;
		} else if (msStr.length() < 3) {
			msStr = "0" + msStr;
		}

		return min + ":" + secStr + "." + msStr
			+ ((isRunning())?"(still running)":"");
	}
}
