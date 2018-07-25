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
package soot.jimple.infoflow.solver.ngsolver;

import java.util.Map;
import java.util.Set;

import heros.InterproceduralCFG;
import heros.solver.IDESolver;
import heros.solver.IFDSSolver;
import soot.SootMethod;
import soot.Unit;

/**
 * A tabulation problem for solving in an {@link IFDSSolver} as described by the
 * Reps, Horwitz, Sagiv 1995 (RHS95) paper.
 *
 * @param <N> The type of nodes in the interprocedural control-flow graph.
 *        Typically {@link Unit}.
 * @param <D> The type of data-flow facts to be computed by the tabulation
 *        problem.
 * @param <M> The type of objects used to represent methods. Typically
 *        {@link SootMethod}.
 * @param <I> The type of inter-procedural control-flow graph being used.
 */
public interface IFDSTabulationProblem<N, D, M, I extends InterproceduralCFG<N, M>> {

	/**
	 * Returns a set of flow functions. Those functions are used to compute
	 * data-flow facts along the various kinds of control flows.
	 *
	 * <b>NOTE:</b> this method could be called many times. Implementations of this
	 * interface should therefore cache the return value!
	 */
	FlowFunctions<N, D, M> flowFunctions();

	/**
	 * Returns the interprocedural control-flow graph which this problem is computed
	 * over.
	 * 
	 * <b>NOTE:</b> this method could be called many times. Implementations of this
	 * interface should therefore cache the return value!
	 */
	I interproceduralCFG();

	/**
	 * Returns initial seeds to be used for the analysis. This is a mapping of
	 * statements to initial analysis facts.
	 */
	Map<N, Set<D>> initialSeeds();

	/**
	 * This must be a data-flow fact of type {@link D}, but must <i>not</i> be part
	 * of the domain of data-flow facts. Typically this will be a singleton object
	 * of type {@link D} that is used for nothing else. It must holds that this
	 * object does not equals any object within the domain.
	 *
	 * <b>NOTE:</b> this method could be called many times. Implementations of this
	 * interface should therefore cache the return value!
	 */
	D zeroValue();

	/**
	 * If true, the analysis will compute a partially unbalanced analysis problem in
	 * which function returns are followed also further up the call stack than where
	 * the initial seeds started.
	 * 
	 * If this is enabled, when reaching the exit of a method that is <i>nowhere</i>
	 * called, in order to avoid not at all processing the exit statement, the
	 * {@link IDESolver} will call the <i>return</i> flow function with a
	 * <code>null</code> call site and return site.
	 */
	boolean followReturnsPastSeeds();

}
