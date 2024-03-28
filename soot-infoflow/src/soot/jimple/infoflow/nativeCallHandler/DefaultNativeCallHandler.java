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
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;

public class DefaultNativeCallHandler extends AbstractNativeCallHandler {

	private static final String SIG_ARRAYCOPY = "<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>";
	private static final String SIG_NEW_ARRAY = "<java.lang.reflect.Array: java.lang.Object newArray(java.lang.Class,int)>";
	private static final String SIG_COMPARE_AND_SWAP_OBJECT = "<sun.misc.Unsafe: boolean compareAndSwapObject(java.lang.Object,long,java.lang.Object,java.lang.Object)>";
	private static final String SIG_MAKE_CONCAT_WITH_CONSTANTS = "<soot.dummy.InvokeDynamic: java.lang.String makeConcatWithConstants(java.lang.String,java.lang.String)>";

	@Override
	public Set<Abstraction> getTaintedValues(Stmt call, Abstraction source, Value[] params) {
		// check some evaluated methods:
		if (source.isAbstractionActive()) {
			Value taintedValue = source.getAccessPath().getPlainValue();
			switch (call.getInvokeExpr().getMethodRef().getSignature()) {
			case SIG_ARRAYCOPY:
				if (params[0].equals(taintedValue)) {
					if (manager.getTypeUtils().checkCast(source.getAccessPath(), params[2].getType())) {
						AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
								params[2], source.getAccessPath().getBaseType(), false);
						Abstraction abs = source.deriveNewAbstraction(ap, call);
						abs.setCorrespondingCallSite(call);
						return Collections.singleton(abs);
					}
				}
				break;
			case SIG_NEW_ARRAY:
				if (params[1].equals(taintedValue) && call instanceof DefinitionStmt) {
					DefinitionStmt defStmt = (DefinitionStmt) call;
					if (manager.getTypeUtils().checkCast(source.getAccessPath(), params[1].getType())) {
						AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
								defStmt.getLeftOp(), null, false, true, ArrayTaintType.Length);
						Abstraction abs = source.deriveNewAbstraction(ap, call);
						abs.setCorrespondingCallSite(call);
						return Collections.singleton(abs);
					}
				}
				break;
			case SIG_COMPARE_AND_SWAP_OBJECT:
				if (params[3].equals(taintedValue)) {
					if (manager.getTypeUtils().checkCast(source.getAccessPath(), params[3].getType())) {
						// The offset in params[1] defines the concrete field to taint. We
						// over-approximate and taint all fields (base object.*).
						AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
								params[0], null, false, true, ArrayTaintType.Length);
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
		if (!call.containsInvokeExpr())
			return false;
		String sig = call.getInvokeExpr().getMethod().getSignature();
		switch (sig) {
		case SIG_ARRAYCOPY:
		case SIG_COMPARE_AND_SWAP_OBJECT:
		case SIG_NEW_ARRAY:
			return true;

		default:
			return false;
		}
	}

}
