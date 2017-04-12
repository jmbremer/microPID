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
 */package org.bluemedialabs.io;

import java.io.*;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.NoSuchElementException;


/**
 * <p></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class TrecInputStream extends XmlSeqInputStream {
	static public final int LA_TIMES          = 1;
	static public final int FINANCIAL_TIMES   = 2;
	static public final int FEDERAL_REGISTER  = 3;
	static public final int FOREIGN_BROADCAST = 4;
	static public final int CONGRESS_RECORD   = 5;

	static public InputStream create(String directory, String tag, int type)
			throws IOException {
		Object filterComp = null;

		switch (type) {
			case LA_TIMES:
				filterComp = new LaTimesFilter();
				break;
			case FINANCIAL_TIMES:
				filterComp = new FinancialTimesFilter();
				break;
			case FEDERAL_REGISTER:
				filterComp = new FedRegisterFilter();
				break;
			case FOREIGN_BROADCAST:
				filterComp = new ForeignBroadcastFilter();
				break;
			case CONGRESS_RECORD:
				filterComp = new CongressRecordFilter();
				break;
			default:
				throw new IllegalArgumentException("TREC data set type "
					 + type + " invalid");
		}
		return new TrecInputStream(directory, tag, (FilenameFilter) filterComp,
								   (Comparator) filterComp);
	}


	/**
	 *
	 */
	protected TrecInputStream(String mainDirectory, String collectionTag,
			FilenameFilter filter, Comparator fileSequencer)
			throws IOException {
		super(new FileMatcher(mainDirectory, filter, fileSequencer), collectionTag);
	}


	public InputStream createInputStream(Object source) throws IOException {
		InputStream is = null;

		try {
			is = new SequenceInputStream(
					new UncompressInputStream(new FileInputStream((File) source)),
					new ByteArrayInputStream(SPACE.getBytes()));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("File not found in FileMatcher "
					+ "enumeration, but more files should be there");
		} catch (IOException e) {
			throw new IllegalStateException("IO problem in FileMatcher "
					+ "enumeration, but more data should be there");
		}
		return is;

	}



	/************************************************************************
	 * TREC disc 4 Financial Times data set file filter.
	 ************************************************************************/
	static public class FinancialTimesFilter
			implements FilenameFilter, Comparator {
		public boolean accept(File dir, String name) {
			name = name.toLowerCase();
			if (name.regionMatches(true, 0, "ft9", 0, 3)
					&& name.charAt(name.length() - 2) == '.'
					&& name.charAt(name.length() - 1) == 'z') {
				return true;
			} else {
				return false;
			}
		}

		public int compare(Object obj1, Object obj2) {
			final String ZEROS = "0000";
			String s1, s2;
			int pos, r;
			String name1, name2;
			String num1, num2;
			int len1, len2;

			if (obj1 instanceof File) {
				s1 = (String) ((File) obj1).getName();
				s2 = (String) ((File) obj2).getName();
			} else {
				s1 = (String) obj1;
				s2 = (String) obj2;
			}
			// Check for equality
			if (s1.compareTo(s2) == 0) {
				return 0;
			}
			pos = s1.indexOf('_');
			name1 = s1.substring(0, pos);
			num1 = s1.substring(pos + 1, s1.indexOf('.'));
			pos = s2.indexOf('_');
			name2 = s2.substring(0, pos);
			num2 = s2.substring(pos + 1, s2.indexOf('.'));
			// Check based on main name part only
			r = name1.compareTo(name2);
			if (r != 0) {
				return r;
			}
			len1 = num1.length();
			len2 = num2.length();
			if (len1 < len2) {
				num1 = ZEROS.substring(0, len2 - len1) + num1;
			} else if (len2 < len1) {
				num2 = ZEROS.substring(0, len1 - len2) + num2;
			}
			return num1.compareTo(num2);
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj instanceof FinancialTimesFilter) {
				return true;
			} else {
				return false;
			}
		}
	}


	/************************************************************************
	 * TREC disc 4 Federal Register data set file filter.
	 ************************************************************************/
	static public class FedRegisterFilter implements FilenameFilter, Comparator {
		public boolean accept(File dir, String name) {
//			if (dir.getName().indexOf("aux") > 0
//					|| dir.getName().indexOf("AUX") > 0) {
//				return false;
//			}
			name = name.toLowerCase();
			if (name.charAt(name.length() - 3) == '.'
					&& name.charAt(name.length() - 1) == 'z') {
				return true;
			} else {
				return false;
			}
		}

		public int compare(Object obj1, Object obj2) {
			String s1;
			String s2;

			if (obj1 instanceof File) {
				s1 = (String) ((File) obj1).getName();
				s2 = (String) ((File) obj2).getName();
			} else {
				s1 = (String) obj1;
				s2 = (String) obj2;
			}
			// Here, simple lexicographical comparison surfices
			return (s1.compareTo(s2));
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj instanceof FedRegisterFilter) {
				return true;
			} else {
				return false;
			}
		}
	}


	/************************************************************************
	 * TREC disc 4 Congressional Record data set file filter.
	 ************************************************************************/
	static public class CongressRecordFilter implements FilenameFilter, Comparator {
		public boolean accept(File dir, String name) {
			name = name.toLowerCase();
			if (name.startsWith("cr")
				&& name.charAt(name.length() - 2) == '.'
				&& name.charAt(name.length() - 1) == 'z') {
				return true;
			} else {
				return false;
			}
		}

		public int compare(Object obj1, Object obj2) {
			final String ZEROS = "0000";
			String s1, s2;
			String num1, num2;
			int len1, len2;

			if (obj1 instanceof File) {
				s1 = (String) ((File) obj1).getName();
				s2 = (String) ((File) obj2).getName();
			} else {
				s1 = (String) obj1;
				s2 = (String) obj2;
			}
			// Check for equality
			if (s1.compareTo(s2) == 0) {
				return 0;
			} else if (s1.charAt(4) != s2.charAt(4)) {
				return ((int) (s1.charAt(4) - s2.charAt(4)));
			} else {
				// both file names are of the same type, so, let the #'s decide
				num1 = s1.substring(5, s1.indexOf('.'));
				num2 = s2.substring(5, s2.indexOf('.'));
				len1 = num1.length();
				len2 = num2.length();
				if (len1 < len2) {
					num1 = ZEROS.substring(0, len2 - len1) + num1;
				} else if (len2 < len1) {
					num2 = ZEROS.substring(0, len1 - len2) + num2;
				}
				return num1.compareTo(num2);
			}
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj instanceof FedRegisterFilter) {
				return true;
			} else {
				return false;
			}
		}
	}


	/************************************************************************
	 * TREC disc 5 LA Times data set file filter.
	 ************************************************************************/
	static public class LaTimesFilter implements FilenameFilter, Comparator {
		public boolean accept(File dir, String name) {
			name = name.toLowerCase();
			if (name.regionMatches(true, 0, "la", 0, 2)
					&& name.charAt(name.length() - 2) == '.'
					&& name.charAt(name.length() - 1) == 'z'
					&& name.length() == 10) {
				return true;
			} else {
				return false;
			}
		}

		public int compare(Object obj1, Object obj2) {
			String s1;
			String s2;

			if (obj1 instanceof File) {
				s1 = (String) ((File) obj1).getName();
				s2 = (String) ((File) obj2).getName();
			} else {
				s1 = (String) obj1;
				s2 = (String) obj2;
			}
//			System.out.println("Comparing " + s1 + " and " + s2);
			// Remove possible directory prefixes
			s1 = s1.substring(s1.lastIndexOf('/') + 1);
			s2 = s2.substring(s2.lastIndexOf('/') + 1);
			// Extract the date parts of both file name strings
			int year1 = Integer.parseInt(s1.substring(6, 8));
			int year2 = Integer.parseInt(s2.substring(6, 8));
			int date1 = Integer.parseInt(s1.substring(2, 6));
			int date2 = Integer.parseInt(s2.substring(2, 6));
			if (year1 < year2) {
				return -1;
			} else if (year1 > year2) {
				return 1;
			} else {
				return (date1 - date2);
			}
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj instanceof LaTimesFilter) {
				return true;
			} else {
				return false;
			}
		}
	}


	/************************************************************************
	 * TREC disc 5 Foreign Broadcast Information Service data set file filter.
	 ************************************************************************/
	static public class ForeignBroadcastFilter implements FilenameFilter, Comparator {
		public boolean accept(File dir, String name) {
			name = name.toLowerCase();
			if (name.endsWith(".z")) {
				return true;
			} else {
				return false;
			}
		}

		public int compare(Object obj1, Object obj2) {
			String s1;
			String s2;

			if (obj1 instanceof File) {
				s1 = (String) ((File) obj1).getName();
				s2 = (String) ((File) obj2).getName();
			} else {
				s1 = (String) obj1;
				s2 = (String) obj2;
			}
			// Here, simple lexicographical comparison surfices
			return (s1.compareTo(s2));
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj instanceof ForeignBroadcastFilter) {
				return true;
			} else {
				return false;
			}
		}
	}



	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		// LA Times
//		InputStream in = TrecInputStream.create(
//				"/cdrom/latimes", "LA_Times", LA_TIMES);
		// Financial Times
//		InputStream in = TrecInputStream.create(
//				"/cdrom/ft", "Financial_Times", FINANCIAL_TIMES);
		// Federal Register
//		InputStream in = TrecInputStream.create(
//				"/cdrom/fr94", "Federal_Register", FEDERAL_REGISTER);
//		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		// Federal Register
//		InputStream in = TrecInputStream.create(
//				"/cdrom/fbis", "Foreign_Broadcast", FOREIGN_BROADCAST);
//		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		// Congressional Record
		InputStream in = TrecInputStream.create(
				"/cdrom/cr", "Congress_Record", CONGRESS_RECORD);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line;

		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}
	}

}