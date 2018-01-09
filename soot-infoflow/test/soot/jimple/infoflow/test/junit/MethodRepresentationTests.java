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
package soot.jimple.infoflow.test.junit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
/**
 * check the conversion of Soot's String representation into our internal data format. 
 */
public class MethodRepresentationTests {

	@Test(timeout=300000)
	public void testParser(){
		String s = "<soot.jimple.infoflow.test.TestNoMain: java.lang.String function1()>";
		
		SootMethodRepresentationParser parser = SootMethodRepresentationParser.v();
		SootMethodAndClass result = parser.parseSootMethodString(s);
		
		assertEquals("soot.jimple.infoflow.test.TestNoMain", result.getClassName());
		assertEquals("function1", result.getMethodName());
		assertEquals("java.lang.String", result.getReturnType());
	}
	
}
