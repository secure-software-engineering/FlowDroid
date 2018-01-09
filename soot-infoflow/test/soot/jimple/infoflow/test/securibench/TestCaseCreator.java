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
package soot.jimple.infoflow.test.securibench;


public class TestCaseCreator {

	public static void main(String[] args) {
		String pathAndClassWithoutNumber = "securibench.micro.basic.Basic";
		int numberOfTests = 42;
		
		String classname = pathAndClassWithoutNumber.substring(pathAndClassWithoutNumber.lastIndexOf(".")+1);

		for(int i=1; i<=numberOfTests; i++){
			System.out.println("@Test");
			System.out.println("public void "+classname.toLowerCase()+i+ "() {");
			System.out.println("Infoflow infoflow = initInfoflow();");
			System.out.println("List<String> epoints = new ArrayList<String>();");
			System.out.println("epoints.add(\"<"+pathAndClassWithoutNumber+i+ ": void doGet(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)>\");");
			System.out.println("infoflow.computeInfoflow(path, entryPointCreator, epoints, sources, sinks);");
			System.out.println("checkInfoflow(infoflow, 1);");
			System.out.println("}");
			System.out.println();
		}
	}
}
