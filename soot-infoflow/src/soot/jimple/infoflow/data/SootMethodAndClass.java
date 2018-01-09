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
import java.util.List;

import soot.SootMethod;
import soot.Type;

/**
 * Data container which stores the string representation of a SootMethod and its corresponding class
 */
public class SootMethodAndClass {
	private final String methodName;
	private final String className;
	private final String returnType;
	private final List<String> parameters;

	private String subSignature = null;
	private String signature = null;
	private int hashCode = 0;
	
	public SootMethodAndClass
			(String methodName,
			String className,
			String returnType,
			List<String> parameters){
		this.methodName = methodName;
		this.className = className;
		this.returnType = returnType;
		this.parameters = parameters;
	}
	
	public SootMethodAndClass(SootMethod sm) {
		this.methodName = sm.getName();
		this.className = sm.getDeclaringClass().getName();
		this.returnType = sm.getReturnType().toString();
		this.parameters = new ArrayList<String>();
		for (Type p: sm.getParameterTypes())
			this.parameters.add(p.toString());
	}
	
	public SootMethodAndClass(SootMethodAndClass methodAndClass) {
		this.methodName = methodAndClass.methodName;
		this.className = methodAndClass.className;
		this.returnType = methodAndClass.returnType;
		this.parameters = new ArrayList<String>(methodAndClass.parameters);
	}

	public String getMethodName() {
		return this.methodName;
	}
	
	public String getClassName() {
		return this.className;
	}
	
	public String getReturnType() {
		return this.returnType;
	}
	
	public List<String> getParameters() {
		return this.parameters;
	}
	
	public String getSubSignature() {
		if (subSignature != null)
			return subSignature;
		
		StringBuilder sb = new StringBuilder(10 + this.returnType.length() + this.methodName.length() + (this.parameters.size() * 30));
		if (!this.returnType.isEmpty()) {
			sb.append(this.returnType);
			sb.append(" ");
		}
		sb.append(this.methodName);
		sb.append("(");
		
		for (int i = 0; i < this.parameters.size(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(this.parameters.get(i).trim());
		}
		sb.append(")");
		this.subSignature = sb.toString();
		
		return this.subSignature;
	}

	public String getSignature() {
		if (signature != null)
			return signature;
		
		StringBuilder sb = new StringBuilder(10 + this.className.length() + this.returnType.length() + this.methodName.length() + (this.parameters.size() * 30));
		sb.append("<");
		sb.append(this.className);
		sb.append(": ");
		if (!this.returnType.isEmpty()) {
			sb.append(this.returnType);
			sb.append(" ");
		}
		sb.append(this.methodName);
		sb.append("(");
		
		for (int i = 0; i < this.parameters.size(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(this.parameters.get(i).trim());
		}
		sb.append(")>");
		this.signature = sb.toString();
		
		return this.signature;
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
		}
		sb.append(")>");
		return sb.toString();
	}

}
