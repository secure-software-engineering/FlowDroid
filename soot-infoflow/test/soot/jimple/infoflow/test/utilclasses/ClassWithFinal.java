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
package soot.jimple.infoflow.test.utilclasses;

public class ClassWithFinal<E> {
	 public final E[] a;
	 final String b;
	 
	 public ClassWithFinal(String c, @SuppressWarnings("unused") boolean e){
			if (c==null)
	            throw new NullPointerException();
			 b = c;
			 a = null;
		 }
	
	public ClassWithFinal(E[] value){
		 if (value==null)
             throw new NullPointerException();
		a = value;
		b = "";
	}
	
	public E[] getArray(){
		return a;
	}
	
	public String getString(){
		return b;
	}
}
