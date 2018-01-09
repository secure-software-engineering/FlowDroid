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

import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.UnopExpr;

/**
 * BaseSelector removes unnecessary information from a value such as casts
 */
public class BaseSelector {
	
	/**
	 * the operations that are not relevant for analysis like "not" or casts
	 * are removed - array refs are only removed if explicitly stated
	 * @param val the value which should be pruned
	 * @param keepArrayRef if false then array refs are pruned to the base array object
	 * @return the value (possibly pruned to base object)
	 */
	public static Value selectBase(Value val, boolean keepArrayRef){
		//we taint base of array instead of array elements
		if (val instanceof ArrayRef && !keepArrayRef) {
			return ((ArrayRef) val).getBase();
		}
		else if (val instanceof CastExpr) {
			return ((CastExpr) val).getOp();
		}
		else if (val instanceof NewArrayExpr) {
			return ((NewArrayExpr) val).getSize();
		}
		else if (val instanceof InstanceOfExpr) {
			return ((InstanceOfExpr) val).getOp();
		}
		else if (val instanceof UnopExpr)
			return ((UnopExpr) val).getOp();
		
		return val;
	}
	
	/**
	 * the operations that are not relevant for analysis like "not" or casts
	 * are removed - array refs are only removed if explicitly stated
	 * BinOpExpr are divided into two values
	 * @param val the value which should be pruned
	 * @param keepArrayRef if false then array refs are pruned to the base array object
	 * @return one or more values 
	 */
	public static Value[] selectBaseList(Value val, boolean keepArrayRef){
		if (val instanceof BinopExpr) {
			Value[] set = new Value[2];
			BinopExpr expr = (BinopExpr) val;
			set[0] = expr.getOp1();
			set[1] = expr.getOp2();
			return set;
		}
		else
			return new Value[] { selectBase(val, keepArrayRef) };
	}
	
}
