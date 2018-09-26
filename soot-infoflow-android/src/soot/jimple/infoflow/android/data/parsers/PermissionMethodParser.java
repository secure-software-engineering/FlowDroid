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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;

/**
 * Parser for the permissions to method map of Adrienne Porter Felt.
 * 
 * @author Siegfried Rasthofer
 */
public class PermissionMethodParser implements ISourceSinkDefinitionProvider {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private Map<String, AndroidMethod> methods = null;
	private Set<SourceSinkDefinition> sourceList = null;
	private Set<SourceSinkDefinition> sinkList = null;
	private Set<SourceSinkDefinition> neitherList = null;

	private static final int INITIAL_SET_SIZE = 10000;

	private List<String> data;
	private final String regex = "^<(.+):\\s*(.+)\\s+(.+)\\s*\\((.*)\\)>\\s*(.*?)(\\s+->\\s+(.*))?$";
	// private final String regexNoRet =
	// "^<(.+):\\s(.+)\\s?(.+)\\s*\\((.*)\\)>\\s+(.*?)(\\s+->\\s+(.*))?+$";
	private final String regexNoRet = "^<(.+):\\s*(.+)\\s*\\((.*)\\)>\\s*(.*?)?(\\s+->\\s+(.*))?$";

	public static PermissionMethodParser fromFile(String fileName) throws IOException {
		PermissionMethodParser pmp = new PermissionMethodParser();
		pmp.readFile(fileName);
		return pmp;
	}

	public static PermissionMethodParser fromStream(InputStream input) throws IOException {
		PermissionMethodParser pmp = new PermissionMethodParser();
		pmp.readReader(new InputStreamReader(input));
		return pmp;
	}

	public static PermissionMethodParser fromStringList(List<String> data) throws IOException {
		PermissionMethodParser pmp = new PermissionMethodParser(data);
		return pmp;
	}

	private PermissionMethodParser() {
	}

	private PermissionMethodParser(List<String> data) {
		this.data = data;
	}

	private void readFile(String fileName) throws IOException {
		FileReader fr = null;
		try {
			fr = new FileReader(fileName);
			readReader(fr);
		} finally {
			if (fr != null)
				fr.close();
		}
	}

	private void readReader(Reader r) throws IOException {
		String line;
		this.data = new ArrayList<String>();
		BufferedReader br = new BufferedReader(r);
		try {
			while ((line = br.readLine()) != null)
				this.data.add(line);
		} finally {
			br.close();
		}

	}

	@Override
	public Set<SourceSinkDefinition> getSources() {
		if (sourceList == null || sinkList == null)
			parse();
		return this.sourceList;
	}

	@Override
	public Set<SourceSinkDefinition> getSinks() {
		if (sourceList == null || sinkList == null)
			parse();
		return this.sinkList;
	}

	private void parse() {
		methods = new HashMap<>(INITIAL_SET_SIZE);
		sourceList = new HashSet<>(INITIAL_SET_SIZE);
		sinkList = new HashSet<>(INITIAL_SET_SIZE);
		neitherList = new HashSet<>(INITIAL_SET_SIZE);

		Pattern p = Pattern.compile(regex);
		Pattern pNoRet = Pattern.compile(regexNoRet);

		for (String line : this.data) {
			if (line.isEmpty() || line.startsWith("%"))
				continue;
			Matcher m = p.matcher(line);
			if (m.find()) {
				createMethod(m);
			} else {
				Matcher mNoRet = pNoRet.matcher(line);
				if (mNoRet.find()) {
					createMethod(mNoRet);
				} else
					logger.warn(String.format("Line does not match: %s", line));
			}
		}

		// Create the source/sink definitions
		for (AndroidMethod am : methods.values()) {
			SourceSinkDefinition singleMethod = new MethodSourceSinkDefinition(am);

			if (am.getSourceSinkType().isSource())
				sourceList.add(singleMethod);
			if (am.getSourceSinkType().isSink())
				sinkList.add(singleMethod);
			if (am.getSourceSinkType() == SourceSinkType.Neither)
				neitherList.add(singleMethod);
		}
	}

	private AndroidMethod createMethod(Matcher m) {
		AndroidMethod am = parseMethod(m, true);
		AndroidMethod oldMethod = methods.get(am.getSignature());
		if (oldMethod != null) {
			oldMethod.setSourceSinkType(oldMethod.getSourceSinkType().addType(am.getSourceSinkType()));
			return oldMethod;
		} else {
			methods.put(am.getSignature(), am);
			return am;
		}
	}

	private AndroidMethod parseMethod(Matcher m, boolean hasReturnType) {
		assert (m.group(1) != null && m.group(2) != null && m.group(3) != null && m.group(4) != null);
		AndroidMethod singleMethod;
		int groupIdx = 1;

		// class name
		String className = m.group(groupIdx++).trim();

		String returnType = "";
		if (hasReturnType) {
			// return type
			returnType = m.group(groupIdx++).trim();
		}

		// method name
		String methodName = m.group(groupIdx++).trim();

		// method parameter
		List<String> methodParameters = new ArrayList<String>();
		String params = m.group(groupIdx++).trim();
		if (!params.isEmpty())
			for (String parameter : params.split(","))
				methodParameters.add(parameter.trim());

		// permissions
		String classData = "";
		String permData = "";
		Set<String> permissions = null;
		;
		if (groupIdx < m.groupCount() && m.group(groupIdx) != null) {
			permData = m.group(groupIdx);
			if (permData.contains("->")) {
				classData = permData.replace("->", "").trim();
				permData = "";
			}
			groupIdx++;
		}
		if (!permData.isEmpty()) {
			permissions = new HashSet<String>();
			for (String permission : permData.split(" "))
				permissions.add(permission);
		}

		// create method signature
		singleMethod = new AndroidMethod(methodName, methodParameters, returnType, className, permissions);

		if (classData.isEmpty())
			if (m.group(groupIdx) != null) {
				classData = m.group(groupIdx).replace("->", "").trim();
				groupIdx++;
			}
		if (!classData.isEmpty())
			for (String target : classData.split("\\s")) {
				target = target.trim();

				// Throw away categories
				if (target.contains("|"))
					target = target.substring(target.indexOf('|'));

				if (!target.isEmpty() && !target.startsWith("|")) {
					if (target.equals("_SOURCE_"))
						singleMethod.setSourceSinkType(SourceSinkType.Source);
					else if (target.equals("_SINK_"))
						singleMethod.setSourceSinkType(SourceSinkType.Sink);
					else if (target.equals("_NONE_"))
						singleMethod.setSourceSinkType(SourceSinkType.Neither);
					else if (target.equals("_BOTH_"))
						singleMethod.setSourceSinkType(SourceSinkType.Both);
					else
						throw new RuntimeException("error in target definition: " + target);
				}
			}
		return singleMethod;
	}

	@Override
	public Set<SourceSinkDefinition> getAllMethods() {
		if (sourceList == null || sinkList == null)
			parse();

		Set<SourceSinkDefinition> sourcesSinks = new HashSet<>(
				sourceList.size() + sinkList.size() + neitherList.size());
		sourcesSinks.addAll(sourceList);
		sourcesSinks.addAll(sinkList);
		sourcesSinks.addAll(neitherList);
		return sourcesSinks;
	}
}
