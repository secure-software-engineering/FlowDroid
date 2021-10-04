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
package soot.jimple.infoflow.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import soot.SootMethod;
import soot.Type;

/**
 * Data container which stores the string representation of a SootMethod and its
 * corresponding class
 */
public class SootMethodAndClass extends AbstractMethodAndClass {

	private int hashCode = 0;

	public SootMethodAndClass(String methodName, String className, String returnType, List<String> parameters) {
		super(methodName, className, returnType, parameters);
	}

	public SootMethodAndClass(String methodName, String className, String returnType, String parameters) {
		super(methodName, className, returnType, parameterFromString(parameters));
	}

	private static List<String> parameterFromString(String parameters) {
		if (parameters != null && !parameters.isEmpty()) {
			return Arrays.asList(parameters.split(","));
		}
		return new ArrayList<>();
	}

	public SootMethodAndClass(SootMethod sm) {
		super(sm.getName(), sm.getDeclaringClass().getName(), sm.getReturnType().toString(), parameterFromMethod(sm));
	}

	private static List<String> parameterFromMethod(SootMethod sm) {
		ArrayList<String> parameters = new ArrayList<String>();
		for (Type p : sm.getParameterTypes())
			parameters.add(p.toString());
		return parameters;
	}

	public SootMethodAndClass(SootMethodAndClass methodAndClass) {
		super(methodAndClass.methodName, methodAndClass.className, methodAndClass.returnType,
				new ArrayList<String>(methodAndClass.parameters));
	}

	@Override
	public boolean equals(Object another) {
		if (super.equals(another))
			return true;
		if (!(another instanceof SootMethodAndClass))
			return false;
		SootMethodAndClass otherMethod = (SootMethodAndClass) another;

		if (!this.methodName.equals(otherMethod.methodName))
			return false;
		if (!this.parameters.equals(otherMethod.parameters))
			return false;
		if (!this.className.equals(otherMethod.className))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		if (this.hashCode == 0)
			this.hashCode = this.methodName.hashCode() + this.className.hashCode() * 5;
		// The parameter list is available from the outside, so we can't cache it
		return this.hashCode + this.parameters.hashCode() * 7;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		sb.append(className);
		sb.append(": ");
		sb.append(returnType);
		sb.append(" ");
		sb.append(methodName);
		sb.append("(");
		boolean isFirst = true;
		for (String param : parameters) {
			if (!isFirst)
				sb.append(",");
			sb.append(param);
			isFirst = false;
		}
		sb.append(")>");
		return sb.toString();
	}

}
