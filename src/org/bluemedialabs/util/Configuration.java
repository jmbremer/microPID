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

import java.io.*;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;


/**
 * <p>A set of key value pairs called properties grouped under so-called
 * categories. A default category is used to look up values not found in a
 * category-specific property. This class is a generalization of the
 * java.util.Properties class. Furthermore, this class is closely related
 * to Windows(TM) .ini files.</p>
 * <p><em>Copyright (c) 2002 by J. Marco Bremer</em></p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class Configuration implements CloneableObject<Configuration> {
	/**
	 * The initial number of elements of all categories' property hash tables.
	 */
	static public final int DEFAULT_SIZE = 20;

	private LinkedList<Category> categories = new LinkedList<Category>();
	private Category defaultCategory = new Category("default");


	/*+**********************************************************************
	 * Class functions
	 ************************************************************************/

	static public Configuration load(String[] args, int index)
			throws IOException, ParseException {
		if (args.length < index + 2) {
			// We expect at least the configuration name and file name!
			System.out.println();
			System.out.println("Missing the configuration name or file name "
							   + "as the command line arguments " + index
							   + " and " + (index + 1));
			return null;
		}
		return load(args[1]);
	}


	static public Configuration load(String[] args)
			throws IOException, ParseException {
		return load(args, 0);
	}


	/**
	 * Constructs and loads a configuration from the file with the given,
	 * fully-qualified name.
	 *
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	static public Configuration load(String fileName)
			throws IOException, ParseException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		Configuration config = new Configuration();
		config.load(br);
		return config;
	}


	/**
	 * <p>A small wrapper around a category name and its attached property
	 * hash table.</p>
	 *
	 * @author J. Marco Bremer
	 * @version 1.0
	 */
	static private class Category implements Cloneable {
		String name = null;
		HashMap<String, String> properties =
		        new HashMap<String, String>(DEFAULT_SIZE);

		Category(String name) {
			this.name = name;
		}

		public Category clone() {
			Category cat = new Category(name);
			cat.properties = new HashMap<String, String>(properties);
			return cat;
		}

		public boolean isEmpty() {
			return (properties.size() == 0);
		}
	}


	/*+**********************************************************************
	 * Object functions
	 ************************************************************************/

	/**
	 * Constructs an empty configuration to be initialized manually or loaded
	 * from a source.
	 */
	public Configuration() {}

	/**
	 * Constructs a new configuration object as a copy of the supplied
	 * configuration.
	 *
	 * @param config The configuration to be copied in this configuration.
	 */
	public Configuration(Configuration config) {
		this();
		config.copy(this);
	}


	public Configuration clone() {
		Configuration config = new Configuration();
		copy(config);
		return config;
	}


	public void copy(Configuration config) {
		List<Category> dest = config.categories;
		Category cat;

		dest.clear();
		Iterator<Category> it = categories.listIterator();
		while (it.hasNext()) {
			cat = it.next();
			dest.add(cat.clone());
		}
	}


	/**
	 * Sets the property with the supplied key and category to the given value.
	 *
	 * @param category
	 * @param key
	 * @param value
	 */
	public void setProperty(String category, String key, String value) {
		Category cat = findCategory(category);
		if (cat == null) {
			cat = new Category(category);
			categories.add(cat);
		}
		cat.properties.put(key, value);
	}

	/**
	 * Finds the category object with the given name.
	 *
	 * @param name The category name.
	 * @return The category object with the given name, or null, if no such
	 *   object is found.
	 */
	protected Category findCategory(String name) {
		ListIterator<Category> it = categories.listIterator();
		Category cat = null;
		boolean found = false;

		if (name.compareToIgnoreCase("default") == 0) {
			cat = defaultCategory;
		} else {
			while (it.hasNext() && !found) {
				cat = it.next();
				found = (cat.name.compareToIgnoreCase(name) == 0);
			}
		}
		return (found? cat: null);
	}


	/**
	 * Returns the property string for the given key within the given
	 * category.
	 *
	 * @param category
	 * @param key
	 * @return
	 */
	public String getProperty(String category, String key) {
		Category cat;
		String prop = null;

		if (category == null || category.compareToIgnoreCase("default") == 0) {
			if (defaultCategory != null) {
				return (String) defaultCategory.properties.get(key);
			} else {
				throw new NullPointerException("Key '" + key + "' does not "
						+ "exist for category '" + category + "' in this "
						+ "configuration");
			}

		} else {
			cat = findCategory(category);
			if (cat != null) {
				prop = (String) cat.properties.get(key);
			}
			if (prop == null && defaultCategory != null) {
				// Haven't found the category or the property within
				// the category, thus, fall back to default category
				prop = (String) defaultCategory.properties.get(key);
			}
			if (prop == null) {
				throw new NullPointerException("Key '" + key + "' does not "
						+ "exist for category '" + category + "' in this "
						+ "configuration");
			}
			return prop;
		}
	}


	/**
	 * Returns the global default value for the given key, if present.
	 *
	 * @param key
	 * @return
	 */
	public String getProperty(String key) {
		return getProperty(null, key);
	}


	/**
	 *
	 * @param category
	 * @param key
	 * @return
	 */
	public int getIntProperty(String category, String key) {
		String str = getProperty(category, key);
		int value = -1;

		try {
			value = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			throwNumberFormatException(category, key, str, e);
		}
		return value;
	}

	/**
	 *
	 * @param category
	 * @param key
	 * @param value
	 * @param cause
	 * @throws NumberFormatException
	 */
	private void throwNumberFormatException(String category, String key,
			String value, NumberFormatException cause)
			throws NumberFormatException {
		NumberFormatException e = new NumberFormatException(
				"Cannot convert the value '" + value + "' of property "
					+ category + "/" + key + " to the requested number "
					+ "type");
		e.initCause(cause);
		throw e;
	}


	/**
	 * Returns false, if the property value is empty or equal to 'false' ignoring
	 * lower/upper case, otherwise true.
	 *
	 * @param category
	 * @param key
	 * @return
	 */
	public boolean getBooleanProperty(String category, String key) {
		String str = getProperty(category, key);

		if (str.length() == 0 || str.compareToIgnoreCase("false") == 0) {
			return false;
		} else {
			return true;
		}
	}



	/**
	 *
	 * @param out
	 */
	public void store(PrintWriter out) {
        for (Category cat: categories) {
			out.println("[" + cat.name + "]");
//			out.println();
			storeCategory(cat, out);
			out.println();
			out.println();
		}
		if (!defaultCategory.isEmpty()) {
			out.println("[DEFAULT]");
//			out.println();
			storeCategory(defaultCategory, out);
		}
	}

	/**
	 *
	 * @param cat
	 * @param out
	 */
	protected void storeCategory(Category cat, PrintWriter out) {
	    for (Map.Entry<String, String> entry: cat.properties.entrySet()) {
	        out.println(entry.getKey() + "\t= " + entry.getValue());
	    }
	}


	/**
	 *
	 * @param in
	 * @throws IOException
	 * @throws ParseException
	 */
	public void load(BufferedReader in) throws IOException, ParseException {
		String line, catName, name;
		StringBuffer value = new StringBuffer(120);
		Category cat = defaultCategory;
		categories.clear();
		defaultCategory.properties.clear();
		int pos;

		while ((line = in.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0) {

			} else if (line.charAt(0) == '[') {
				// A category
				pos = line.indexOf(']');
				catName = line.substring(1, pos);
				if (catName.compareToIgnoreCase("default") == 0) {
					cat = defaultCategory;
				} else {
					cat = findCategory(catName);
					if (cat == null) {
						cat = new Category(catName);
						categories.add(cat);
					}
				}
			} else if (line.charAt(0) != '#') {
				// A property
				name = parseProperty(line, value);
				cat.properties.put(name, value.toString());
			}
		}

		// Replace variables with their values
		String key, val;
		for (Category cat2: categories) {
			for (Map.Entry<String, String> entry: cat2.properties.entrySet()) {
				key = entry.getKey();
				val = entry.getValue();
				if (val.indexOf('$') >= 0) {
					expandVariable(cat2, key, val);
				}
			}
		}
	}

	private void expandVariable(Category cat, String key, String value)
			throws ParseException {
		int pos = value.indexOf('$');
		int end;
		StringBuffer id = new StringBuffer();
		String var;

		while (pos >= 0) {
			// While there are more potential variables...
			if (pos > 0 && value.charAt(pos - 1) == '\\') {
				// This is meant to be a regular $ and not a variable
				value = value.substring(0, pos - 1)
					+ value.substring(pos, value.length());
				end = pos;
			} else {
				// A variable that needs to be replaced
				end = parseVarIdentifier(value, pos, id);
				// Expand variable
				var = getProperty(cat.name, id.toString());
				if (var == null) {
					throw new IllegalStateException("Variable $(" + id.toString()
							+ ") cannot be expanded as it is undefined");
				}
				value = value.substring(0, Math.max(0, pos - 1)) + var +
					 value.substring(end, value.length());
				end = pos - 1 + var.length();
			}
			// Find more variables/$'s in the rest of the value string
			pos = value.indexOf('$', end);
		}
		setProperty(cat.name, key, value);
	}

	private int parseVarIdentifier(String value, int pos, StringBuffer id)
			throws ParseException {
		int len = value.length();
		int start, end;
		char ch;

		if (pos + 1 >= len) {
			throw new ParseException("Variable without name in property value '"
									  + value + "'", len);
		}
		if (value.charAt(pos + 1) == '(') {
			start = pos + 2;
			end = value.indexOf(')', start);
		} else {
			start = pos + 1;
			end = start;
			ch = value.charAt(end);
			while (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z'
					|| ch >= '0' && ch <= '9' || ch == '_') {
				end++;
				ch = value.charAt(end);
			}
		}
		id.append(value.substring(start, end));
		if (value.charAt(pos + 1) == '(') {
			end++;
		}
		return end;
	}


	/**
	 *
	 * @param line
	 * @param value
	 * @return
	 * @throws ParseException
	 */
	protected String parseProperty(String line, StringBuffer value)
			throws ParseException {
		int pos = line.indexOf('='), endPos;
		if (pos == -1) {
			throw new ParseException("Cannot find '=' in property line \""
			   + line + "\"", 0);
		}
		String name = line.substring(0, pos).trim();
		// Clear buffer
		value.delete(0, value.length());
		pos++;
		while (pos < line.length()
			   && (line.charAt(pos) == ' ' || line.charAt(pos) == '\t')) pos++;
		if (pos < line.length()) {
			if (line.charAt(pos) == '\"') {
				endPos = line.lastIndexOf('\"');
				value.append(line.substring(pos + 1, endPos));
			} else {
				value.append(line.substring(pos).trim());
			}
		} // otherwise the value is empty which is valid
		return name;
	}


	/**
	 *
	 * @return
	 */
	public String toString() {
		StringWriter sw = new StringWriter();
		store(new PrintWriter(sw));
		return sw.getBuffer().toString();
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	/**
	 *
	 * @param args
	 * @throws Exception
	 */
	static public void main(String[] args) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(
				"F:/Java/test.cfg"));
		Configuration config = new Configuration();
		config.load(br);
		System.out.println(config);
		System.out.println();
		System.out.println("TermFileBaseName property is: "
						   + config.getProperty("Religion", "TermFileBaseName"));
		System.out.println("SourceHomeBase is: "
						   + config.getProperty("SourceHomeBase"));
	}
}