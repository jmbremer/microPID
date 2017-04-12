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

import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import org.bluemedialabs.util.Quack;
import org.bluemedialabs.io.BitDataChannel;
import org.bluemedialabs.io.RandomAccessChannel;


/**
 * <p>Maintains a dynamic pool of BitDataChannels for a specific file. Lets
 * clients request channels that are positioned at a given bit position from
 * this pool.<em>(The latter being disabled right now.</em></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class BitDataChannelPool {
    
    // All channels ever created
	private LinkedList<Pair> allChannels = new LinkedList<Pair>();
	// Currently unused channels
	private Quack channelStack = new Quack();
	private String fileName;
	
	
	/*+**********************************************************************
	 * CLASS
	 ************************************************************************/

    /**
     * <p></p>
     *
     * @author J. Marco Bremer
     * @version 1.0
     */
    static class Pair {
        public RandomAccessChannel rac;
        public BitDataChannel bdc;

        public Pair(RandomAccessChannel rac, BitDataChannel bdc) {
            this.rac = rac;
            this.bdc = bdc;
        }
    }
	
	
	/*+**********************************************************************
	 * INSTANCE
	 ************************************************************************/

    /**
     * Construct a new channel pool for the file with the given name.
     */
	public BitDataChannelPool(String fileName) {
		this.fileName = fileName;
	}


	public void finalize() {
		// The following should be protected against multiple calls!
		try {
			closeAll();
		} catch (IOException e) {
			// Ok, ok, if you don't want...
		}
	}


	public void closeAll() throws IOException {
		ListIterator<Pair> it;
		Pair pair;

		synchronized (channelStack) {
			channelStack.clear();
			it = allChannels.listIterator();
			while (it.hasNext()) {
				pair = (Pair) it.next();
				pair.rac.close();
				it.remove();
			}
		}
	}


	public BitDataChannel claim() throws IOException {
		RandomAccessChannel rac;
		BitDataChannel bdc;

		if (channelStack.isEmpty()) {
			rac = RandomAccessChannel.create(fileName);
			bdc = new BitDataChannel(rac);
//			bdc.bitPosition(bitPos);
			synchronized (allChannels) {
				allChannels.add(new Pair(rac, bdc));
			}
			return bdc;
		} else {
			synchronized (channelStack) {
				bdc = (BitDataChannel) channelStack.pop();
			}
//			bdc.bitPosition(bitPos);
			return bdc;
		}
	}

//	public BitDataChannel claim() throws IOException {
//		return claim(0);
//	}


	public void release(BitDataChannel ch) {
		assert (ch != null);
		synchronized (channelStack) {
			channelStack.push(ch);
		}
	}


	public int capacity() { return allChannels.size(); }
	public int available() { return channelStack.size(); }


	public void compact(int leaveN) {
		BitDataChannel bdc;
		RandomAccessChannel rac;

		assert (leaveN >= 0);
		synchronized (channelStack) {
			if (leaveN < channelStack.size()) {
				// Only then, there is really something to do
				while (channelStack.size() > leaveN) {
					bdc = (BitDataChannel) channelStack.pop();
					rac = findAndRemove(bdc);
					try {
						rac.close();
					} catch (IOException e) {
						System.out.println("Problems closing random access "
								+ "channel in BitDataChannelPool");
					}
					rac = null;
				}
			}
		}
	}

	private RandomAccessChannel findAndRemove(BitDataChannel bdc) {
		ListIterator<Pair> it = allChannels.listIterator();
		Pair pair;
		RandomAccessChannel rac = null;

		while (it.hasNext()) {
			pair = (Pair) it.next();
			if (pair.bdc == bdc) {
				it.remove();
				rac = pair.rac;
				pair = null;
			}
		}
		if (rac == null) {
			throw new IllegalStateException("Cannot find channel to be "
					+ "removed");
		}
		return rac;
	}


	public void compact() {
		compact(0);
	}


}