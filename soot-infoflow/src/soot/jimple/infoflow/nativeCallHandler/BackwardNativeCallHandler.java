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
package soot.jimple.infoflow.nativeCallHandler;

import java.util.Collections;
import java.util.Set;

import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;

public class BackwardNativeCallHandler extends AbstractNativeCallHandler {

	private static final String SIG_ARRAYCOPY = "<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>";
	private static final String SIG_COMPARE_AND_SWAP_OBJECT = "<sun.misc.Unsafe: boolean compareAndSwapObject(java.lang.Object,long,java.lang.Object,java.lang.Object)>";

	@Override
	public Set<Abstraction> getTaintedValues(Stmt call, Abstraction source, Value[] params) {
		// check some evaluated methods:
		if (source.isAbstractionActive()) {
			Value taintedValue = source.getAccessPath().getPlainValue();
			switch (call.getInvokeExpr().getMethod().getSignature()) {
			case SIG_ARRAYCOPY:
				if (params[2].equals(taintedValue)) {
					if (manager.getTypeUtils().checkCast(source.getAccessPath(), params[0].getType())) {
						AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
								params[0], source.getAccessPath().getBaseType(), false);
						Abstraction abs = source.deriveNewAbstraction(ap, call);
						abs.setCorrespondingCallSite(call);
						return Collections.singleton(abs);
					}
				}
				break;
			case SIG_COMPARE_AND_SWAP_OBJECT:
				if (params[0].equals(taintedValue)) {
					if (manager.getTypeUtils().checkCast(source.getAccessPath(), params[0].getType())) {
						// The offset in params[1] defines the concrete field to taint. We
						// over-approximate and taint all fields (base object.*).
						AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
								params[3], null, false, true, ArrayTaintType.Length);
						Abstraction abs = source.deriveNewAbstraction(ap, call);
						abs.setCorrespondingCallSite(call);
						return Collections.singleton(abs);
					}
				}
				break;
			}
		}
		return null;
	}

	@Override
	public boolean supportsCall(Stmt call) {
		return call.containsInvokeExpr() && call.getInvokeExpr().getMethod().getSignature().equals(SIG_ARRAYCOPY);
	}

}
