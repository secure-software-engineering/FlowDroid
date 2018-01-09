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
package soot.jimple.infoflow;

import java.util.Arrays;
import java.util.List;

import soot.jimple.infoflow.util.ArgParser;
/**
 * cmdInfoflow offers a command-line interface to start the analysis.
 * However, it is not efficiently usable for large amounts of entry points, sources and sinks.
 * Therefore, most use-cases access the java classes directly.
 *
 */
public class cmdInfoflow {

	public static void main(String[] args) {
		ArgParser parser = new ArgParser();
		if (args.length > 0) {
			if (Arrays.asList(args).contains(ArgParser.METHODKEYWORD)) {
				List<List<String>> inputArgs = parser.parseClassArguments(args);

				if (inputArgs.get(0) == null
						|| inputArgs.size() < 3) {
					System.err.println("Arguments could not be parsed!");
					return;
				}
				IInfoflow infoflow = new Infoflow();
				infoflow.computeInfoflow(inputArgs.get(3).get(0), "", inputArgs.get(0), inputArgs.get(1), inputArgs.get(2));
			}
		}
	}

}
