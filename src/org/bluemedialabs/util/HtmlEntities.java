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
import java.util.LinkedList;
import java.util.Iterator;


/**
 * <p></p>
 * 
 * @author J. Marco Bremer
 * @version 1.0
 */
public class HtmlEntities {
    
	static private String[] ADDITIONAL_ENTITIES = {
		"cacute", "c", "Federal Register entity",
		"cir", "o", "Federal Register entity",
		"blank", " ", "Federal Register entity",
		"Gacute", "G", "Federal Register entity",
		"hyph", "-", "Federal Register entity",
		"Iuml", "I", "Federal Register entity",
		"Kuml", "K", "Federal Register entity",
		"lacute", "l", "Federal Register entity",
		"nacute", "n", "Federal Register entity",
		"ncirc", "n", "Federal Register entity",
		"pacute", "p", "Federal Register entity",
		"racute", "r", "Federal Register entity",
		"sacute", "s", "Federal Register entity",
		"utilde", "u", "Federal Register entity"
	};

	private LinkedList<Entity> entities = new LinkedList<Entity>();

    static protected class Entity {
        public String name;
        public String number;
        public String text;
    };


    /**
     * Construct a new HTML entities object from the given file.
     * 
     * @param fileName
     * @throws IOException
     */
	public HtmlEntities(String fileName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line;
		String entity;
		String num;
		String text;
		int pos;
		Entity en;

		while ((line = br.readLine()) != null) {
			pos = line.indexOf(' ');
			entity = line.substring(0, pos);
			pos = line.indexOf('&');
			num = line.substring(pos, line.indexOf(';', pos) + 1);
			text = line.substring(line.indexOf('-') + 3);
			en = new Entity();
			en.name = entity;
			en.number = num;
			en.text = text;
			entities.add(en);
		}
	}


	public String toString() {
		StringBuffer buf = new StringBuffer(entities.size() * 60);
		Iterator<Entity> it = entities.iterator();
		Entity en;
		int pos;

		while (it.hasNext()) {
			en = it.next();
			buf.append(en.name + "\t" + en.number + "\t-- " + en.text + "\r\n");
		}
		pos = 0;
		en = new Entity();
		while (pos < ADDITIONAL_ENTITIES.length) {
			en.name = ADDITIONAL_ENTITIES[pos++];
			en.number = ADDITIONAL_ENTITIES[pos++];
			en.text = ADDITIONAL_ENTITIES[pos++];
			buf.append(en.name + "\t" + en.number + "\t-- " + en.text + "\r\n");
		}
		return buf.toString();
	}

	public String toXml(String dtdRoot) {
		StringBuffer buf = new StringBuffer(entities.size() * 60);
		Iterator<Entity> it = entities.iterator();
		Entity en;
		int pos;

		if (dtdRoot != null) {
			buf.append("<!DOCTYPE " + dtdRoot + " [\r\n");
		}
		while (it.hasNext()) {
			en = it.next();
			buf.append("    <!ENTITY " + en.name + " \"" + en.number 
			        + "\">\r\n");
		}
		pos = 0;
		en = new Entity();
		while (pos < ADDITIONAL_ENTITIES.length) {
			en.name = ADDITIONAL_ENTITIES[pos++];
			en.number = ADDITIONAL_ENTITIES[pos++];
			pos++;
			buf.append("    <!ENTITY " + en.name + " \"" + en.number 
			        + "\">\r\n");
		}
		if (dtdRoot != null) {
			buf.append("]>\r\n");
		}
		return buf.toString();
	}

	public String toXml() {
		return toXml(null);
	}


	/*+**********************************************************************
	 * TEST
	 ************************************************************************/

	public static void main(String[] args) throws Exception {
		HtmlEntities ent = new HtmlEntities("/aramis/Data/Source/entities.txt");
		System.out.println(ent.toXml("FedReg"));
//		extract();
	}

	static public void extract() throws IOException {
		String fileName = "/aramis/HtmlEntities.txt";
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line;
		int pos, next;
		String text;
		String entity;
		String num;

		while ((line = br.readLine()) != null) {
			if (line.length() > 0) {
				pos = line.indexOf('&');
				text = line.substring(0, pos - 1);
				next = line.indexOf(';', pos);
				entity = line.substring(pos + 1, next);
				pos = line.indexOf('&', next);
				pos = line.indexOf('&', pos + 1);
				next = line.indexOf(';', pos);
				num = line.substring(pos, next + 1);
				System.out.println(entity + " \t" + num + " \t-- " + text);
			}
		}
	}
}