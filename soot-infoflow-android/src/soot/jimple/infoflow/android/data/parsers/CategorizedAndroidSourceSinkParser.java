/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.android.data.parsers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.CategoryDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;

/**
 * This parser parses the categorized sources and sink file (only one a time)
 * for specific categories.
 * 
 * @author Siegfried Rasthofer
 */
public class CategorizedAndroidSourceSinkParser {
	private Set<CategoryDefinition> categories;
	private final String fileName;
	private SourceSinkType sourceSinkType;

	private final String regex = "^<(.+):\\s*(.+)\\s+(.+)\\s*\\((.*)\\)>.+?\\((.+)\\)$";

	public CategorizedAndroidSourceSinkParser(Set<CategoryDefinition> categories, String filename,
			SourceSinkType sourceSinkType) {
		this.categories = categories;
		this.fileName = filename;
		this.sourceSinkType = sourceSinkType;
	}

	public Set<ISourceSinkDefinition> parse() throws IOException {
		Set<ISourceSinkDefinition> definitions = new HashSet<>();
		CategoryDefinition allCats = CategoryDefinition.ALL_CATEGORIES;
		boolean loadAllCategories = categories.contains(allCats);

		BufferedReader rdr = readFile();
		if (rdr == null)
			throw new RuntimeException("Could not read source/sink file");

		String line = null;
		Pattern p = Pattern.compile(regex);

		while ((line = rdr.readLine()) != null) {
			Matcher m = p.matcher(line);
			if (m.find()) {
				String strCat = m.group(5);
				CategoryDefinition cat = new CategoryDefinition(strCat);

				if (loadAllCategories || categories.contains(cat)) {
					AndroidMethod method = parseMethod(m);
					method.setSourceSinkType(sourceSinkType);
					MethodSourceSinkDefinition def = new MethodSourceSinkDefinition(method);
					def.setCategory(cat);
					definitions.add(def);
				}
			}
		}

		try {
			if (rdr != null)
				rdr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return definitions;
	}

	private BufferedReader readFile() {
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(fileName);
			br = new BufferedReader(fr);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}

		return br;
	}

	private AndroidMethod parseMethod(Matcher m) {
		assert (m.group(1) != null && m.group(2) != null && m.group(3) != null && m.group(4) != null);
		int groupIdx = 1;

		// class name
		String className = m.group(groupIdx++).trim();

		// return type
		String returnType = m.group(groupIdx++).trim();

		// method name
		String methodName = m.group(groupIdx++).trim();

		// method parameter
		List<String> methodParameters = new ArrayList<String>();
		String params = m.group(groupIdx++).trim();
		if (!params.isEmpty())
			for (String parameter : params.split(","))
				methodParameters.add(parameter.trim());

		// create method signature
		return new AndroidMethod(methodName, methodParameters, returnType, className);
	}

}
