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
package soot.jimple.infoflow.test;


public class InFunctionCode {
	
	public String infSourceCode1(String secret) {
		return secret;
	}

	public String infSourceCode2(@SuppressWarnings("unused") String foo) {
		String secret = "Hello World";
		return secret;
	}

	public String infSourceCode3(String foo) {
		String secret = copy(foo);
		return secret;
	}
	
	private String copy(String bar) {
		System.out.println("bar");
		return bar;
	}
	
	public String tmp = "";
	
	public void setTmp(String t ){
		tmp = t;
	}
	
	public String foo(String p1, String p2){
		String t = p2;
		p2 = p1;
		return tmp + p1 + t;
	}
	
	public class DataClass {
		public int i;
		public int j;
	}

	@SuppressWarnings("unused")
	public int paraToParaFlow(int a, int b, DataClass data, DataClass data2) {
		int c = a;
		data.i = c;
		return b;
	}

}
