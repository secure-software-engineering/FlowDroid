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
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.CategoryDefinition;
import soot.jimple.infoflow.android.data.CategoryDefinition.CATEGORY;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;

/**
 * Parser of the permissions to method map from the University of Toronto
 * (PScout)
 * 
 * @author Siegfried Rasthofer
 */
public class PScoutPermissionMethodParser implements ISourceSinkDefinitionProvider {
	private static final int INITIAL_SET_SIZE = 10000;

	private Set<ISourceSinkDefinition> sourceList = null;
	private Set<ISourceSinkDefinition> sinkList = null;
	private Set<ISourceSinkDefinition> neitherList = null;

	private Map<String, CategoryDefinition> categories = new HashMap<>();

	private String fileName;
	private final String regex = "^<(.+):\\s*(.+)\\s+(.+)\\s*\\((.*)\\)>.+?(->.+)?$";
	private final boolean SET_IMPLICIT_SOURCE_TO_SOURCE = false;
	private final boolean SET_INDIRECT_SINK_TO_SINK = false;

	private Reader reader;

	public PScoutPermissionMethodParser(String filename) {
		this.fileName = filename;

		initializeCategoryMap();
	}

	public PScoutPermissionMethodParser(Reader reader) {
		this.reader = reader;

		initializeCategoryMap();
	}

	private void initializeCategoryMap() {
		categories.put("_NO_CATEGORY_", new CategoryDefinition(CATEGORY.NO_CATEGORY));
		categories.put("_HARDWARE_INFO_", new CategoryDefinition(CATEGORY.HARDWARE_INFO));
		categories.put("_NFC_", new CategoryDefinition(CATEGORY.NFC));
		categories.put("_PHONE_CONNECTION_", new CategoryDefinition(CATEGORY.PHONE_CONNECTION));
		categories.put("_INTER_APP_COMMUNICATION_", new CategoryDefinition(CATEGORY.INTER_APP_COMMUNICATION));
		categories.put("_VOIP_", new CategoryDefinition(CATEGORY.VOIP));
		categories.put("_CONTACT_INFORMATION_", new CategoryDefinition(CATEGORY.CONTACT_INFORMATION));
		categories.put("_UNIQUE_IDENTIFIER_", new CategoryDefinition(CATEGORY.UNIQUE_IDENTIFIER));
		categories.put("_PHONE_STATE_", new CategoryDefinition(CATEGORY.PHONE_STATE));
		categories.put("_SYSTEM_SETTINGS_", new CategoryDefinition(CATEGORY.SYSTEM_SETTINGS));
		categories.put("_LOCATION_INFORMATION_", new CategoryDefinition(CATEGORY.LOCATION_INFORMATION));
		categories.put("_NETWORK_INFORMATION_", new CategoryDefinition(CATEGORY.NETWORK_INFORMATION));
		categories.put("_EMAIL_", new CategoryDefinition(CATEGORY.EMAIL));
		categories.put("_SMS_MMS_", new CategoryDefinition(CATEGORY.SMS_MMS));
		categories.put("_CALENDAR_INFORMATION_", new CategoryDefinition(CATEGORY.CALENDAR_INFORMATION));
		categories.put("_ACCOUNT_INFORMATION_", new CategoryDefinition(CATEGORY.ACCOUNT_INFORMATION));
		categories.put("_BLUETOOTH_", new CategoryDefinition(CATEGORY.BLUETOOTH));
		categories.put("_ACCOUNT_SETTINGS_", new CategoryDefinition(CATEGORY.ACCOUNT_SETTINGS));
		categories.put("_VIDEO_", new CategoryDefinition(CATEGORY.VIDEO));
		categories.put("_AUDIO_", new CategoryDefinition(CATEGORY.AUDIO));
		categories.put("_SYNCHRONIZATION_DATA_", new CategoryDefinition(CATEGORY.SYNCHRONIZATION_DATA));
		categories.put("_NETWORK_", new CategoryDefinition(CATEGORY.NETWORK));
		categories.put("_EMAIL_SETTINGS_", new CategoryDefinition(CATEGORY.EMAIL_SETTINGS));
		categories.put("_EMAIL_INFORMATION_", new CategoryDefinition(CATEGORY.EMAIL_INFORMATION));
		categories.put("_IMAGE_", new CategoryDefinition(CATEGORY.IMAGE));
		categories.put("_FILE_INFORMATION_", new CategoryDefinition(CATEGORY.FILE_INFORMATION));
		categories.put("_BLUETOOTH_INFORMATION_", new CategoryDefinition(CATEGORY.BLUETOOTH_INFORMATION));
		categories.put("_BROWSER_INFORMATION_", new CategoryDefinition(CATEGORY.BROWSER_INFORMATION));
		categories.put("_FILE_", new CategoryDefinition(CATEGORY.FILE));
		categories.put("_VOIP_INFORMATION_", new CategoryDefinition(CATEGORY.VOIP_INFORMATION));
		categories.put("_DATABASE_INFORMATION_", new CategoryDefinition(CATEGORY.DATABASE_INFORMATION));
		categories.put("_PHONE_INFORMATION_", new CategoryDefinition(CATEGORY.PHONE_INFORMATION));
		categories.put("_LOG_", new CategoryDefinition(CATEGORY.LOG));
	}

	private void parse() {
		sourceList = new HashSet<>(INITIAL_SET_SIZE);
		sinkList = new HashSet<>(INITIAL_SET_SIZE);
		neitherList = new HashSet<>(INITIAL_SET_SIZE);

		BufferedReader rdr = readFile();

		String line = null;
		Pattern p = Pattern.compile(regex);
		String currentPermission = null;

		try {
			while ((line = rdr.readLine()) != null) {
				if (line.startsWith("Permission:"))
					currentPermission = line.substring(11);
				else {
					Matcher m = p.matcher(line);
					if (m.find()) {
						parseMethod(m, currentPermission);
					}
				}
			}

			if (rdr != null)
				rdr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void addToList(Set<ISourceSinkDefinition> sourceList, MethodSourceSinkDefinition def,
			String currentPermission) {
		if (!sourceList.add(def)) {
			for (ISourceSinkDefinition ssdef : sourceList) {
				if (ssdef instanceof MethodSourceSinkDefinition) {
					MethodSourceSinkDefinition mssdef = (MethodSourceSinkDefinition) ssdef;
					SootMethodAndClass singleMethod = def.getMethod();
					if (singleMethod instanceof AndroidMethod && mssdef.getMethod().equals(singleMethod)) {
						((AndroidMethod) singleMethod).addPermission(currentPermission);
						break;
					}
				}
			}
		}
	}

	@Override
	public Set<ISourceSinkDefinition> getSources() {
		if (sourceList == null || sinkList == null)
			parse();
		return this.sourceList;
	}

	@Override
	public Set<ISourceSinkDefinition> getSinks() {
		if (sourceList == null || sinkList == null)
			parse();
		return this.sinkList;
	}

	private BufferedReader readFile() {
		Reader r = null;
		BufferedReader br = null;
		try {
			if (reader != null)
				r = reader;
			else
				r = new FileReader(fileName);
			br = new BufferedReader(r);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}

		return br;
	}

	private MethodSourceSinkDefinition parseMethod(Matcher m, String currentPermission) {
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
		Set<String> permissions = null;
		if (currentPermission != null) {
			permissions = new HashSet<>();
			permissions.add(currentPermission);
		}
		AndroidMethod singleMethod = new AndroidMethod(methodName, methodParameters, returnType, className,
				permissions);
		MethodSourceSinkDefinition sourceSinkDef = new MethodSourceSinkDefinition(singleMethod);

		if (m.group(5) != null) {
			String targets = m.group(5).substring(3);

			for (String target : targets.split(" "))
				if (target.startsWith("_SOURCE_")) {
					singleMethod.setSourceSinkType(SourceSinkType.Source);
					if (target.contains("|")) {
						String cat = target.substring(target.indexOf('|') + 1);
						sourceSinkDef.setCategory(returnCorrectCategory(cat));
					}
				} else if (target.startsWith("_SINK_")) {
					singleMethod.setSourceSinkType(SourceSinkType.Sink);
					if (target.contains("|")) {
						String cat = target.substring(target.indexOf('|') + 1);
						sourceSinkDef.setCategory(returnCorrectCategory(cat));
					}
				} else if (target.equals("_NONE_"))
					singleMethod.setSourceSinkType(SourceSinkType.Neither);
				else if (target.startsWith("_IMPSOURCE_")) {
					if (SET_IMPLICIT_SOURCE_TO_SOURCE) {
						singleMethod.setSourceSinkType(SourceSinkType.Source);
						if (target.contains("|")) {
							String cat = target.substring(target.indexOf('|') + 1);
							sourceSinkDef.setCategory(returnCorrectCategory(cat));
						}
					} else
						singleMethod.setSourceSinkType(SourceSinkType.Neither);
				} else if (target.startsWith("_INDSINK_")) {
					if (SET_INDIRECT_SINK_TO_SINK) {
						singleMethod.setSourceSinkType(SourceSinkType.Sink);
						if (target.contains("|")) {
							String cat = target.substring(target.indexOf('|') + 1);
							sourceSinkDef.setCategory(returnCorrectCategory(cat));
						}
					} else
						singleMethod.setSourceSinkType(SourceSinkType.Neither);
				} else if (target.equals("_IGNORE_"))
					return null;
				else if (target.startsWith("-")) {
					String cat = target.substring(target.indexOf('|') + 1);
					sourceSinkDef.setCategory(returnCorrectCategory(cat));
				} else
					throw new RuntimeException("error in target definition");
		}

		if (singleMethod != null) {
			if (singleMethod.getSourceSinkType().isSource())
				addToList(sourceList, sourceSinkDef, currentPermission);
			else if (singleMethod.getSourceSinkType().isSink())
				addToList(sinkList, sourceSinkDef, currentPermission);
			else if (singleMethod.getSourceSinkType() == SourceSinkType.Neither)
				addToList(neitherList, sourceSinkDef, currentPermission);
		}

		return sourceSinkDef;
	}

	private CategoryDefinition returnCorrectCategory(String category) {
		CategoryDefinition def = categories.get(category);
		if (def == null)
			throw new RuntimeException("The category -" + category + "- is not supported!");
		return def;
	}

	@Override
	public Set<ISourceSinkDefinition> getAllMethods() {
		if (sourceList == null || sinkList == null)
			parse();

		Set<ISourceSinkDefinition> sourcesSinks = new HashSet<>(
				sourceList.size() + sinkList.size() + neitherList.size());
		sourcesSinks.addAll(sourceList);
		sourcesSinks.addAll(sinkList);
		sourcesSinks.addAll(neitherList);
		return sourcesSinks;
	}
}
