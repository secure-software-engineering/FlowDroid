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
package soot.jimple.infoflow.solver.fastSolver;

import heros.InterproceduralCFG;

import java.lang.ref.WeakReference;

import soot.Unit;

/**
 * A path edge as described in the IFDS/IDE algorithms.
 * The source node is implicit: it can be computed from the target by using the {@link InterproceduralCFG}.
 * Hence, we don't store it.
 *
 * @param <N> The type of nodes in the interprocedural control-flow graph. Typically {@link Unit}.
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 * 
 * @author Eric Bodden
 * @author Steven Arzt
 */
class WeakPathEdge<N,D> {

	protected final WeakReference<N> target;
	protected final WeakReference<D> dSource, dTarget;
	protected final int hashCode;

	/**
	 * @param dSource The fact at the source.
	 * @param target The target statement.
	 * @param dTarget The fact at the target.
	 */
	public WeakPathEdge(D dSource, N target, D dTarget) {
		super();
		this.target = target == null ? null : new WeakReference<N>(target);
		this.dSource = dSource == null ? null : new WeakReference<D>(dSource);
		this.dTarget = dTarget == null ? null : new WeakReference<D>(dTarget);
		
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dSource == null) ? 0 : dSource.hashCode());
		result = prime * result + ((dTarget == null) ? 0 : dTarget.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		this.hashCode = result;
	}
	
	public N getTarget() {
		return target.get();
	}

	public D factAtSource() {
		return dSource.get();
	}

	public D factAtTarget() {
		return dTarget.get();
	}
	
	public boolean isDead() {
		return dSource == null || dTarget == null;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		WeakPathEdge other = (WeakPathEdge) obj;
		if (dSource.get() == null) {
			if (other.dSource.get() != null)
				return false;
		} else if (!dSource.get().equals(other.dSource.get()))
			return false;
		if (dTarget.get() == null) {
			if (other.dTarget.get() != null)
				return false;
		} else if (!dTarget.get().equals(other.dTarget.get()))
			return false;
		if (target.get() == null) {
			if (other.target.get() != null)
				return false;
		} else if (!target.get().equals(other.target.get()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("<");
		result.append(dSource.get());
		result.append("> -> <");
		result.append(target.get().toString());
		result.append(",");
		result.append(dTarget.get());
		result.append(">");
		return result.toString();
	}

}
