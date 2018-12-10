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

import java.util.Collection;
import java.util.Set;

import soot.jimple.infoflow.solver.functions.SolverCallFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverCallToReturnFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverNormalFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverReturnFlowFunction;
import soot.jimple.infoflow.solver.ngsolver.SolverState;

/**
 * The empty function, i.e. a function which returns an empty set for all points
 * in the definition space.
 * 
 * @param <D> The type of data-flow facts to be computed by the tabulation
 *        problem.
 */
public class KillAll<N, D> implements SolverCallFlowFunction<N, D>, SolverReturnFlowFunction<N, D>,
		SolverCallToReturnFlowFunction<N, D>, SolverNormalFlowFunction<N, D> {

	@SuppressWarnings("rawtypes")
	private final static KillAll instance = new KillAll();

	private KillAll() {
	} // use v() instead

	@SuppressWarnings("unchecked")
	public static <N, D> KillAll<N, D> v() {
		return instance;
	}

	@Override
	public Set<D> computeTargets(SolverState<N, D> source) {
		return null;
	}

	@Override
	public Set<D> computeTargets(SolverState<N, D> state, Collection<D> callerD1s) {
		return null;
	}

}
