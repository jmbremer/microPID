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
import java.util.Iterator;
import org.bluemedialabs.util.*;


/**
 * <p>/p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Queries {
	static private String CFG_ID = "XMark1Gb";
	static private StopWatch watch = new StopWatch();


	static void dump(QueryEngine qe) throws Exception {
		PIndex pIndex = qe.getPIndex();
		TIndex tIndex = qe.getTIndex();
		NnoDbIterator it;
		Terms terms= qe.getTerms();
		MutableLong posNo;
		String term = "macht";
		int tno = terms.get(term).getNo();
		int nno = 12;
		int count = 0;

		it = tIndex.posIterator(tno, nno);
//		it = pIndex.posIterator(nno, 0, -1);
		while (it.hasNext()) {
			posNo = (MutableLong) it.next();
			count++;
		}
		System.out.println("\nCount = " + count);
		System.exit(0);
	}


	/**
	 * DBLP
	 *
	 * //article[contains(./author,"Abiteboul")]/title
	 * //article[./author]/title
	 */
	static void query1(QueryEngine qe) throws Exception {
		PIndex pIndex = qe.getPIndex();
		TIndex tIndex = qe.getTIndex();
		AIndex aIndex = qe.getAIndex();
		XmlSource source = qe.getSource();
		NnoDbIterator pIt, tIt;
		Terms terms= qe.getTerms();
		PrefixJoinIterator it;
		MutableLong posNo;
		String term = "abiteboul";
		int tno = terms.get(term).getNo();
		int nno = 32;    // '/DBLP/ARTICLE/AUTHOR' = 32
		int nno2 = 13;   // '/DBLP/ARTICLE/TITLE' = 13
		int prefixLen = 17;
		int count = 0;
		int addrIdx;
		long addr;
		StringBuffer buf = new StringBuffer(1024);

//		System.out.println("Term '" + term + "' has # " + tno);
		tIt = pIndex.posIterator(nno, 0, -1);
//		tIt = tIndex.posIterator(tno, nno);
		pIt = pIndex.posIterator(nno2, 0, -1);
		it = new PrefixJoinIterator(pIt, tIt, prefixLen);
		while (it.hasNext()) {
			posNo = (MutableLong) it.next();
			addrIdx = pIt.getCurrAddrIdx();
			addr = aIndex.getAddress(nno2, addrIdx);
//			source.getDocFragment(addr, buf);
			count++;
//			System.out.println("\n(" + count + ")");
//			System.out.println(buf);
		}
		System.out.println("There are " +  count + " 'Abiteboul' titles.");
	}


	/**
	 * Shakespeare
	 *
	 * //play[contains(./title, "Cleopatra")]/personae/persona
	 * //article[./author]/title
	 */
	static void query2(QueryEngine qe) throws Exception {
		PIndex pIndex = qe.getPIndex();
		TIndex tIndex = qe.getTIndex();
		AIndex aIndex = qe.getAIndex();
		XmlSource source = qe.getSource();
		NnoDbIterator pIt, tIt;
		Terms terms= qe.getTerms();
		PrefixJoinIterator it;
		MutableLong posNo;
		String term = "cleopatra";
		int tno = terms.get(term).getNo();
		int nno = 3;     // '.../PLAY/TITLE' = 3
		int nno2 = 8;    // '.../PLAY/PERSONAE/PERSONA = 8
		int nno2b = 10;  // '.../PLAY/PERSONAE/PGROUP/PERSONA = 10
		int prefixLen = 6;
		int count = 0;
		int addrIdx;
		long addr;
		StringBuffer buf = new StringBuffer(1024);

//		System.out.println("Term '" + term + "' has # " + tno);
//		tIt = pIndex.posIterator(nno, 0, -1);
		tIt = tIndex.posIterator(tno, nno);
		pIt = pIndex.posIterator(nno2, 0, -1);
		it = new PrefixJoinIterator(pIt, tIt, prefixLen);
		while (it.hasNext()) {
			posNo = (MutableLong) it.next();
			addrIdx = pIt.getCurrAddrIdx();
			addr = aIndex.getAddress(nno2, addrIdx);
			source.getDocFragment(addr, buf);
			count++;
//			System.out.println("\n(" + count + ")");
//			System.out.println(buf);
		}
		System.out.println("There are " +  count + " 'cleopatra' persons.");
//		/*
		count = 0;
		tIt = tIndex.posIterator(tno, nno);
		pIt = pIndex.posIterator(nno2b, 0, -1);
		it = new PrefixJoinIterator(pIt, tIt, prefixLen);
		while (it.hasNext()) {
			posNo = (MutableLong) it.next();
			addrIdx = pIt.getCurrAddrIdx();
			addr = aIndex.getAddress(nno2, addrIdx);
//			source.getDocFragment(addr, buf);
			count++;
//			System.out.println("\n(" + count + ")");
//			System.out.println(buf);
		}
//		*/
		System.out.println("There are " +  count + " 'cleopatra' persons.");
	}


	/**
	 * Reuters
	 *
	 * (1) //newsitem[contains(./text, "stockmarket")]  -- 270 matches
	 * (2) //newsitem[contains(./text, "market")] -- 27434
	 * (3) //newsitem[contains(./text, "macht")] -- 4
	 */
	static void query3(QueryEngine qe) throws Exception {
		final boolean ITERATE_OVER_AINDEX = false;
		PIndex pIndex = qe.getPIndex();
		TIndex tIndex = qe.getTIndex();
		AIndex aIndex = qe.getAIndex();
		XmlSource source = qe.getSource();
		NnoDbIterator pIt, tIt, aIt;
		Terms terms= qe.getTerms();
		PrefixJoinIterator it;
		MutableLong posNo;
		String term = "macht";
		int tno = terms.get(term).getNo();
		int nno = 12;     // '.../PLAY/TITLE' = 3
		int prefixLen = 29;
		int count = 0;
		int addrIdx;
		long addr;
		StringBuffer buf = new StringBuffer(1024);
		MutableLong maddr = null;
		int aIdxPos = -1;

//		System.out.println("Term '" + term + "' has # " + tno);
		tIt = tIndex.posIterator(tno, nno);
		pIt = pIndex.posIterator(nno, 0, -1);//185645058, 415704068);
		aIt = aIndex.addrIterator(nno);
		it = new PrefixJoinIterator(pIt, tIt, prefixLen);
		while (it.hasNext()) {
			addrIdx = pIt.getCurrAddrIdx();
			posNo = (MutableLong) it.next();
			if (!ITERATE_OVER_AINDEX) {
			addr = aIndex.getAddress(nno, addrIdx);
//			source.getDocFragment(addr, buf);
//			count++;
//			System.out.println("\n(" + count + ")");
//			System.out.println(buf);
			} else {
				if (aIt.hasNext()) {
					while (aIdxPos < addrIdx) {
						maddr = (MutableLong) aIt.next();
						aIdxPos++;
					}
//					source.getDocFragment(maddr.getValue(), buf);
					count++;
//					System.out.println("\n(" + count + ")");
//					System.out.println(buf);
				}
			}
		}
		System.out.println("There are " +  count + " occurences of the term.");
	}


	/**
	 * SwissProt (also used for Big10!)
	 *
	 * //entry[./features/metal]/ref/author
	 */
	static void query4(QueryEngine qe) throws Exception {
		PIndex pIndex = qe.getPIndex();
		AIndex aIndex = qe.getAIndex();
		XmlSource source = qe.getSource();
		NnoDbIterator pIt, pIt2, aIt;
		PrefixJoinIterator it;
		MutableLong posNo;
		int nno1 = 530;//= 21;     // /root/entry/ref/author   (530 in Big10)
		int nno2 = 666;//= 157;    // /root/entry/features/metal  (666 in Big10)
		int prefixLen = 16;
		int count = 0;
		int addrIdx;
		MutableLong addr = null;
		StringBuffer buf = new StringBuffer(1024);
		int aIdxPos;

		pIt = pIndex.posIterator(nno1, 0, -1);
		pIt2 = pIndex.posIterator(nno2, 0, -1);
		aIt = aIndex.addrIterator(nno1);
		it = new PrefixJoinIterator(pIt, pIt2, prefixLen);

//		addr = (MutableLong) aIt.next();
		aIdxPos = -1;
		while (it.hasNext()) {
			addrIdx = pIt.getCurrAddrIdx();
			posNo = (MutableLong) it.next();
//			if (aIt.hasNext()) {
				while (aIdxPos < addrIdx) {
					addr = (MutableLong) aIt.next();
					aIdxPos++;
				}
//			addr = aIndex.getAddress(nno1, addrIdx);
				source.getDocFragment(addr.getValue(), buf);
				count++;
//				System.out.println("\n(" + count + ")");
//				System.out.println(buf);
//			}
		}
		System.out.println("There are " +  count + " authors.");
	}


	/**
	 * XMark1Gb
	 *
	 * (1) /site/regions/asia/item[./mailbox/mail/text/keyword]
	 * (2)
	 */
	static void query5(QueryEngine qe) throws Exception {
		PIndex pIndex = qe.getPIndex();
		AIndex aIndex = qe.getAIndex();
		XmlSource source = qe.getSource();
		NnoDbIterator pIt, aIt;
		PrefixIterator it;
		MutableLong posNo;
		int nno = 116;
		int prefixLen = 15;
		int count = 0;
		int addrIdx;
		MutableLong addr = null;
		StringBuffer buf = new StringBuffer(1024);
		int aIdxPos = -1;

		// (1)
//		/*
//		System.out.println("*** Starting query XMark 1Gb query ***");
		pIt = pIndex.posIterator(nno, 0, -1);
		it = new PrefixIterator(pIt, prefixLen);
		aIt = aIndex.addrIterator(nno);
		addr = (MutableLong) aIt.next();
		aIdxPos++;
		while (it.hasNext()) {
			addrIdx = pIt.getCurrAddrIdx();
			posNo = (MutableLong) it.next();
//			if (aIt.hasNext()) {
				while (aIdxPos < addrIdx) {
					addr = (MutableLong) aIt.next();
					aIdxPos++;
				}
				source.getDocFragment(addr.getValue(), buf);
				count++;
//				System.out.println("\n(" + count + ")");
//				System.out.println(buf);
//			}
		}
//		System.out.println("There are " +  count + " result elements.");
//		*/

		// (2)
		/*
		DataGuide guide = pIndex.getDataGuide();
		TIndex tIndex = qe.getTIndex();
		NnoDbIterator tIt;
		Terms terms = qe.getTerms();
		TermNodeMap map = tIndex.getMap();
		int tno = (terms.get("money")).getNo();
		MutableInteger mno;
		Iterator mit = map.iterator(tno);
		PrefixJoinIterator pit;

		count = 0;
		int mcount = 0;
		while (mit.hasNext()) {
			mcount++;
			mno = (MutableInteger) mit.next();
			tIt = tIndex.posIterator(tno, mno.getValue());
			pIt = pIndex.posIterator(mno.getValue());
			pit = new PrefixJoinIterator(pIt, tIt, guide.getNode(
					mno.getValue()).getTotalPosBitLen());
			aIt = aIndex.addrIterator(mno.getValue());
			addr = (MutableLong) aIt.next();
			aIdxPos = 0;
			while (pit.hasNext()) {
				addrIdx = pIt.getCurrAddrIdx();
				posNo = (MutableLong) pit.next();

				while (aIdxPos < addrIdx) {
					addr = (MutableLong) aIt.next();
					aIdxPos++;
				}

//				source.getDocFragment(addr.getValue(), buf);
				count++;
//				System.out.println("\n(" + count + ")");
//				System.out.println(buf);
			}
		}
		System.out.println("Total DF count is " + count);
		System.out.println("Node count        " + mcount);
		*/
	}


	/**
	 * XMark1Gb Counter Accumulation
	 * Terms and their DF count:
	 *  innovation - 1000; fujisawa - 500; 1504 - 400; malt - 300; 8150 - 200; alwan - 100; achille - 50;
	 *
	 */
	static void query6(QueryEngine qe) throws Exception {
		TIndex tIndex = qe.getTIndex();
		DataGuide guide = tIndex.getDataGuide();
		NnoDbIterator tIt;
		Terms terms = qe.getTerms();
		TermNodeMap map = tIndex.getMap();
		MutableLong posNo;
		MutableInteger mno;
		String term = "innovation";
		int tno = (terms.get(term)).getNo();
		int count = 0, mcount = 0;
		StopWatch watch = new StopWatch();
		long firstTime;
		int repeat = 1, r = 0;
		int[] counters = new int[10000];
		GuideNode node;
		GuideNode dummy = guide.getNode(3);

		System.out.println("Term count\t Time [ms]");
		tno = terms.getTermCount();
		int tcount = 0;
		int inc = 100;
		int k = 0;
		do {

		r = 0;
		watch.start();
		while (r++ < repeat) {
			Iterator mit = map.iterator(tno);

			count = 0;
			mcount = 0;
			while (mit.hasNext()) {
				mcount++;
				mno = (MutableInteger) mit.next();
				tIt = tIndex.posIterator(tno, mno.getValue());
				node = guide.getNode(mno.getValue());

				while (tIt.hasNext()) {
					posNo = (MutableLong) tIt.next();
					count++;
					for (int i = 0; i < 10000; i++) {
						// Is current pos# a descendant of list element?
						if (node.isDescOf(dummy)) {
							counters[i]++;
						}
					}
				}
				tIt.close();
			}
		}
		watch.stop();
		System.out.println(tcount + "\t " + watch.getAverage(repeat));
		watch.reset();

		// Compute next term#
		tno--;
		k++;
		while (tno > 0 && terms.get(tno).getCount() < (tcount + (k * inc))) tno--;
		int c = terms.get(tno).getCount();
		if (tno > 0) tcount = c;
		if (k == 10) {
			inc = 10 * inc;
			k = 0;
		}



		} while (tno >= 1);
		System.out.println("Total DF count is " + count);
		System.out.println("Node count is     " + mcount);
		System.out.println("Total time for " + repeat + " repeats: " + watch.getTime());
		System.out.println("Average time per accumulation:  " + watch.getAverage(repeat));
		System.out.println();
	}


/****************************************************************************/
/****************************************************************************/
/****************************************************************************/

	/**
	 * M_A_I_N
	 *
	 * @param args
	 * @throws Exception
	 */
	static public void main(String[] args) throws Exception {
		// Load configuration and construct respective query engine
		Configuration config = Configuration.load("/F/Java/project/idxr/idxr.cfg");
		String cfgId = CFG_ID;
		QueryEngine qe = QueryEngine.create(config, cfgId);

//		dump(qe);

		int repeat = 100, r = 0;
		StopWatch watch = new StopWatch();
		StopWatch firstWatch = new StopWatch();

		watch.start();
		firstWatch.start();
		while (++r <= repeat) {
			query5(qe);
			if (r == 1) {
				firstWatch.stop();
			}
		}
		watch.stop();
		System.out.println("Average time:  " + watch.getAverage(repeat));
		System.out.println("First time:    " + firstWatch);
	}

/****************************************************************************/
/****************************************************************************/
/****************************************************************************/


	/**
	 * <p>Iterator over prefixes of position numbers from the two given
	 * iterators.</p>
	 *
	 * @author J. Marco Bremer
	 * @version 1.0
	 */
	static protected class PrefixJoinIterator extends NnoDbIterator {
		private XmlSource source;
		private PIndex pIndex;
		private NnoDbIterator mainIt, joinIt;
		private MutableLong posNo;
		private int prefixLen;
		private DataGuide guide;
		long nextMain, nextJoin;
		int mainShift, joinShift;
		int addrIndex;


		public PrefixJoinIterator(NnoDbIterator mainPosIt, NnoDbIterator joinPosIt,
								  int prefixLen) {
			super(mainPosIt.getNodeNo(), mainPosIt.getDataGuide());
			this.mainIt = mainPosIt;
			this.joinIt = joinPosIt;
			this.guide = getDataGuide();
			assert (guide.isOnSamePath(mainIt.getNodeNo(), joinIt.getNodeNo())):
				"The node numbers are not on the same rooted label path!";
			this.source = source;
			this.pIndex = pIndex;
			this.prefixLen = prefixLen;
			mainShift = guide.getNode(mainIt.getNodeNo()).getTotalPosBitLen() - prefixLen;
			joinShift = guide.getNode(joinIt.getNodeNo()).getTotalPosBitLen() - prefixLen;
			// Get first position numbers
			if (mainIt.hasNext()) {
				posNo = (MutableLong) mainIt.next();
				nextMain = posNo.getValue();
			} else {
				nextMain = -1;
			}
			if (joinIt.hasNext()) {
				posNo = (MutableLong) joinIt.next();
				nextJoin = posNo.getValue();
//				System.out.println(" ++" + nextJoin);
			} else {
				nextJoin = -1;
			}
			goToNext();
		}


		private void goToNext() {
			long mainPrefix = nextMain >>> mainShift;
			long joinPrefix = nextJoin >>> joinShift;

			while (nextMain >= 0 && nextJoin >= 0 && mainPrefix != joinPrefix) {
				if (mainPrefix < joinPrefix) {
					nextMain = nextElement(mainIt);
					mainPrefix = (nextMain >>> mainShift);
				} else {
					nextJoin = nextElement(joinIt);
//					System.out.println(" **" + nextJoin);
					joinPrefix = (nextJoin >>> joinShift);
				}
			}
//			addrIdx = mainIt.getCu
		}

		private long nextElement(NnoDbIterator it) {
			long next = -1;
			if (it.hasNext()) {
				next = ((MutableLong) it.next()).getValue();
			}
			return next;
		}


		public boolean hasNext() {
			return (nextMain >= 0 && nextJoin >= 0);
		}


		public Object next() {
			if (!hasNext()) {
				throw new IllegalStateException();
			}
			posNo.setValue(nextMain);
			nextMain = nextElement(mainIt);
			goToNext();
			return posNo;
		}

		public void close() throws IOException {
			mainIt.close();
			joinIt.close();
		}
	}


	/**
	 * <p>Iterator over prefix of position numbers from the given iterator.</p>
	 *
	 * @author J. Marco Bremer
	 * @version 1.0
	 */
	static protected class PrefixIterator extends NnoDbIterator {
		private NnoDbIterator it;
		private MutableLong posNo;
		private int prefixLen;
		private DataGuide guide;
		private int shift;


		public PrefixIterator(NnoDbIterator it, int prefixLen) {
			super(it.getNodeNo(), it.getDataGuide());
			this.it = it;
			this.guide = getDataGuide();
			this.shift = guide.getNode(it.getNodeNo()).getTotalPosBitLen()
							- prefixLen;
			assert (shift < 0):
				"Position# length is below requested prefix length!";
			this.prefixLen = prefixLen;
		}


		public boolean hasNext() {
			return (it.hasNext());
		}


		public Object next() {
			if (!hasNext()) {
				throw new IllegalStateException();
			}
			posNo = (MutableLong) it.next();
			posNo.setValue(posNo.getValue() >>> shift);
			return posNo;
		}


		public void close() throws IOException {
			it.close();
		}
	}

}