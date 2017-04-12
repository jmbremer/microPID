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

import java.text.ParseException;
import org.bluemedialabs.util.MutableInteger;


/**
 * <p>Unfinished. Needs to be adopted to work with modified ParseTree class...
 * </p>
 *
 * @author J. Marco Bremer
 * @version 1.0
 */
public class PathExpr {
	private char[] str;
	private int pos;
	private int len;
	private ParseTree parseTree = null;


	public PathExpr(String str) {
		assert (str != null);
		this.str = str.toCharArray();
		len = str.length();
	}

	public ParseTree parse() throws ParseException {
		ParseTreeNode node;

		if (len == 1 && str[0] == '/') {
			parseTree = null; // new ParseTree(new ParseTreeNode()); -- doesn't work anymore!
		} else {
			pos = 0;
			node = parsePath(null, true);
			parseTree = null; // new ParseTree(node); -- doesn't work anymore!
		}
		return parseTree;
	}

	public int getParsePosition() { return pos; }


	/*+**********************************************************************
	 * NEXPath Grammar Implementation
	 ************************************************************************/

	ParseTreeNode parsePath(ParseTreeNode parent, boolean select)
			throws ParseException {
		ParseTreeNode first, curr, next;

		if (pos >= len) {
			// Unable to parse a single path fragment
			throw new ParseException("Expecting root AtomicPath", pos);
		} else  {
			first = parseAtomicPath(parent);
			first.setSelectNode(select);
			curr = first;
			while (pos < len && str[pos] == '/') {
				next = parseAtomicPath(curr);
				next.setSelectNode(select);
				curr.addChild(next);
				curr = next;
			}
			return first;
		}
	}

	ParseTreeNode parseAtomicPath(ParseTreeNode parent)
			throws ParseException {
		ParseTreeNode node, cond;
		boolean child = parsePathModif();
		String label = parseLabel();

		node = new ParseTreeNode(label, child, parent);
		if (pos < len && str[pos] == '[') {
			// There is a condition
			pos++;
			cond = parseCondition(node);
			if (pos >= len || str[pos] != ']') {
				throw new ParseException("Expecting ']' as end of condition", pos);
			}
			if (cond != null) {
				// (Position conditions don't create an extra node!
				node.addChild(cond);
			}
			pos++;
		}
		return node;
	}

	boolean parsePathModif() throws ParseException {
		boolean child;

		if (pos >= len || str[pos] != '/') {
			throw new ParseException("Expecting '/'", pos);
		}
		pos++;
		if (str[pos] == '/') {
			// descendant
			child = false;
			pos++;
		} else {
			// child
			child = true;
		}
		return child;
	}


	ParseTreeNode parseCondition(ParseTreeNode parent)
			throws ParseException {
		ParseTreeNode node;
		String term, termSeq;
		boolean child = true;

		if (pos >= len) {
			throw new ParseException("Expecting condition but no input left", pos);
		}
		// Check for descendant term condition (vs. child term condition)
		if (str[pos] == '[') {
			child = false;
			pos++;
		}
		switch (str[pos]) {
			case '.':   // Path condition
				pos++;
				node = parsePath(parent, false);
				break;
			case '\'':  // Single term
				pos++;
				term = parseTerm();
				if (pos >= len || str[pos] != '\'') {
					throw new ParseException("Expecting \"'\" as end of term", pos);
				}
				pos++;
				node = new ParseTreeNode("'" + term + "'", child, parent);
				break;
			case '"':  // Term sequence
				pos++;
				termSeq = parseTermSeq();
				if (pos >= len || str[pos] != '"') {
					throw new ParseException("Expecting '\"' as end of term sequence", pos);
				}
				pos++;
				node = new ParseTreeNode("\"" + termSeq + "\"", child, parent);
				break;
			default:
				if (isDigit(str[pos])) {
					// This must be a position specification
					parsePosCond(parent);
					node = null;
				} else {
					throw new ParseException("Invalid start of a condition", pos);
				}
		}
		if (!child) {
			if (str[pos] != ']') {
				throw new ParseException("Missing end of descendant term condition", pos);
			} else {
				pos++;
			}
		}
		return node;
	}


	void parsePosCond(ParseTreeNode parent) throws ParseException {
		String from = parseNumber();
		String to = from;

		nextPos();
		if (pos >= len) {
			throw new ParseException("Cannot check for position range as input "
									 + "ends here", pos);
		}
		if (str[pos] == '-') {
			// Have a true range
			pos++;
			to = parseNumber();
		}
		parent.setPosRange(Integer.parseInt(from), Integer.parseInt(to));
	}


	String parseLabel() throws ParseException {
		int start = pos;
		char ch = str[pos];

		if (pos >= len || !isLetter(ch) && ch != '*') {
			throw new ParseException("Expecting a letter or '*' as the start "
									 + "of a node label", pos);
		}
		if (ch != '*') {
			pos++;
			while (pos < len && (isLetter(str[pos]) || str[pos] == '_')) {
				pos++;
			}
		} else {
			pos++;
		}
		return new String(str, start, pos - start);
	}


	String parseTermSeq() throws ParseException {
		StringBuffer buf = new StringBuffer(100);
		while (pos < len && isLetter(str[pos])) {
			if (buf.length() > 0) {
				buf.append(' ');
			}
			buf.append(parseTerm());
			// Get past whitespace!
			nextPos();
		}
		return buf.toString();
	}

	String parseTerm() throws ParseException {
		String word = parseWord();
		String number = null;
		if (pos < len && isDigit(str[pos])) {
			 number = parseNumber();
			 word = word + number;
		}
		return word;
	}

	String parseWord() throws ParseException {
		int start = pos;
		while (pos < len && isLetter(str[pos])) {
			pos++;
		}
		if (start == pos) {
			throw new ParseException("Expecting word, but cannot find a letter "
									 + "at the current position", pos);
		}
		return new String(str, start, pos - start);
	}


	String parseNumber() throws ParseException {
		int start = pos;
		while (isDigit(str[pos])) {
			pos++;
		}
		if (start == pos) {
			throw new ParseException("Expecting number, but cannot find a digit "
									 + "at the current position", pos);
		}
		return new String(str, start, pos - start);
	}


	boolean isLetter(char ch) {
		return (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z');
	}

	boolean isDigit(char ch) {
		return (ch >= '0' && ch <= '9');
	}


	void nextPos() {
//		pos++;
		while (pos < len && (str[pos] == ' ' || str[pos] == '\t'
			   || str[pos] == '\r' || str[pos] == '\n')) {
			pos++;
		}
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	static public void main(String[] args) throws Exception {
		String EMPTY = "                                              ";
		String input = "/A[./B]";
		PathExpr pathExpr;
		ParseTree tree = null;

		if (args.length > 0) {
			input = args[0];
		}
		System.out.println("Parsing expression " + input + " ...");
		try {
			pathExpr = new PathExpr(input);
			tree = pathExpr.parse();
		} catch (ParseException e) {
			System.out.println("Parse exception at marked position:");
			System.out.println("   " + input);
			while (EMPTY.length() < e.getErrorOffset() + 3) { EMPTY += "     "; }
			System.out.print(EMPTY.substring(0, 3 + e.getErrorOffset()));
			System.out.println("^");
			System.out.println();
			e.printStackTrace();
		}
		System.out.println(input + " successfully parsed.");
		System.out.flush();
		System.out.println("Parse tree: " + tree);
	}
}