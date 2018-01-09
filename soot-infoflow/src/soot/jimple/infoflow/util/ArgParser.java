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
package soot.jimple.infoflow.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * argument parser which is used in {@link soot.jimple.infoflow.cmdInfoflow}. It parses the command line arguments.
 * @author Christian
 *
 */
public class ArgParser {
    private final Logger logger = LoggerFactory.getLogger(getClass());
	public static String METHODKEYWORD = "-entrypoints";
	public static String SOURCEKEYWORD = "-sources";
	public static String SINKKEYWORD = "-sinks";
	public static String PATHKEYWORD = "-path";
	
	public List<List<String>> parseClassArguments(String[] args){
		List<String> argList = Arrays.asList(args);
		List<String> ePointList;
		List<String> sourceList = new ArrayList<String>();
		List<String> sinkList = new ArrayList<String>();
		List<String> pathList = new ArrayList<String>();
	
		if(argList.contains(METHODKEYWORD)){
			ePointList = getListToAttribute(argList, METHODKEYWORD);
		} else{
			logger.error("parameter '"+ METHODKEYWORD+ "' is missing or has not enough arguments!");
			return null;
		}
		if(argList.contains(SOURCEKEYWORD)){
			sourceList = getListToAttribute(argList, SOURCEKEYWORD);
		}
		if(argList.contains(SINKKEYWORD)){
			sinkList = getListToAttribute(argList, SINKKEYWORD);
		}
			
		if(argList.contains(PATHKEYWORD)){
			pathList = getListToAttribute(argList, PATHKEYWORD);
		}
		
		 List<List<String>> resultlist = new ArrayList<List<String>>();
		 resultlist.add(ePointList);
		 resultlist.add(sourceList);
		 resultlist.add(sinkList);
		 resultlist.add(pathList);
		
		return resultlist;
		
	}

	private List<String> getListToAttribute(List<String> argList, String attr){
		List<String> result = new ArrayList<String>();
		if(argList.indexOf(attr)+1 < argList.size() && !argList.get(argList.indexOf(attr)+1).startsWith("-")){
			int position = argList.indexOf(attr);
			while(position +1 < argList.size() && !argList.get(position+1).startsWith("-")){
				result.add(argList.get(position+1));
				position++;
			}
		}
		
		return result;
	}

}
