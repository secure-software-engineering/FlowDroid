/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package soot.jimple.infoflow.solver.ngsolver.flowFunctions;

import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.Set;

import soot.jimple.infoflow.solver.functions.SolverCallFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverCallToReturnFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverNormalFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverReturnFlowFunction;
import soot.jimple.infoflow.solver.ngsolver.SolverState;

public class Identity<N, D> implements SolverCallFlowFunction<N, D>, SolverReturnFlowFunction<N, D>,
		SolverCallToReturnFlowFunction<N, D>, SolverNormalFlowFunction<N, D> {

	@SuppressWarnings("rawtypes")
	private final static Identity instance = new Identity();

	private Identity() {
	} // use v() instead

	public Set<D> computeTargets(D source) {
		return singleton(source);
	}

	@SuppressWarnings("unchecked")
	public static <N, D> Identity<N, D> v() {
		return instance;
	}

	@Override
	public Set<D> computeTargets(SolverState<N, D> state, Collection<D> callerD1s) {
		return singleton(state.getTargetVal());
	}

	@Override
	public Set<D> computeTargets(SolverState<N, D> state) {
		return singleton(state.getTargetVal());
	}

}
