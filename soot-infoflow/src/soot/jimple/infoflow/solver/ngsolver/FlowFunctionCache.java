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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import soot.jimple.infoflow.solver.functions.SolverCallFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverCallToReturnFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverNormalFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverReturnFlowFunction;

public class FlowFunctionCache<N, D, M> implements FlowFunctions<N, D, M> {

	protected final FlowFunctions<N, D, M> delegate;

	protected final LoadingCache<NNKey, SolverNormalFlowFunction<N, D>> normalCache;

	protected final LoadingCache<CallKey, SolverCallFlowFunction<N, D>> callCache;

	protected final LoadingCache<ReturnKey, SolverReturnFlowFunction<N, D>> returnCache;

	protected final LoadingCache<NNKey, SolverCallToReturnFlowFunction<N, D>> callToReturnCache;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@SuppressWarnings("unchecked")
	public FlowFunctionCache(final FlowFunctions<N, D, M> delegate,
			@SuppressWarnings("rawtypes") CacheBuilder builder) {
		this.delegate = delegate;

		normalCache = builder.build(new CacheLoader<NNKey, SolverNormalFlowFunction<N, D>>() {
			public SolverNormalFlowFunction<N, D> load(NNKey key) throws Exception {
				return delegate.getNormalFlowFunction(key.getCurr(), key.getSucc());
			}
		});

		callCache = builder.build(new CacheLoader<CallKey, SolverCallFlowFunction<N, D>>() {
			public SolverCallFlowFunction<N, D> load(CallKey key) throws Exception {
				return delegate.getCallFlowFunction(key.getCallStmt(), key.getDestinationMethod());
			}
		});

		returnCache = builder.build(new CacheLoader<ReturnKey, SolverReturnFlowFunction<N, D>>() {
			public SolverReturnFlowFunction<N, D> load(ReturnKey key) throws Exception {
				return delegate.getReturnFlowFunction(key.getCallStmt(), key.getDestinationMethod(), key.getExitStmt(),
						key.getReturnSite());
			}
		});

		callToReturnCache = builder.build(new CacheLoader<NNKey, SolverCallToReturnFlowFunction<N, D>>() {
			public SolverCallToReturnFlowFunction<N, D> load(NNKey key) throws Exception {
				return delegate.getCallToReturnFlowFunction(key.getCurr(), key.getSucc());
			}
		});
	}

	public SolverNormalFlowFunction<N, D> getNormalFlowFunction(N curr, N succ) {
		return normalCache.getUnchecked(new NNKey(curr, succ));
	}

	public SolverCallFlowFunction<N, D> getCallFlowFunction(N callStmt, M destinationMethod) {
		return callCache.getUnchecked(new CallKey(callStmt, destinationMethod));
	}

	public SolverReturnFlowFunction<N, D> getReturnFlowFunction(N callSite, M calleeMethod, N exitStmt, N returnSite) {
		return returnCache.getUnchecked(new ReturnKey(callSite, calleeMethod, exitStmt, returnSite));
	}

	public SolverCallToReturnFlowFunction<N, D> getCallToReturnFlowFunction(N callSite, N returnSite) {
		return callToReturnCache.getUnchecked(new NNKey(callSite, returnSite));
	}

	private class NNKey {
		private final N curr, succ;

		private NNKey(N curr, N succ) {
			this.curr = curr;
			this.succ = succ;
		}

		public N getCurr() {
			return curr;
		}

		public N getSucc() {
			return succ;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((curr == null) ? 0 : curr.hashCode());
			result = prime * result + ((succ == null) ? 0 : succ.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			NNKey other = (NNKey) obj;
			if (curr == null) {
				if (other.curr != null)
					return false;
			} else if (!curr.equals(other.curr))
				return false;
			if (succ == null) {
				if (other.succ != null)
					return false;
			} else if (!succ.equals(other.succ))
				return false;
			return true;
		}
	}

	private class CallKey {
		private final N callStmt;
		private final M destinationMethod;

		private CallKey(N callStmt, M destinationMethod) {
			this.callStmt = callStmt;
			this.destinationMethod = destinationMethod;
		}

		public N getCallStmt() {
			return callStmt;
		}

		public M getDestinationMethod() {
			return destinationMethod;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((callStmt == null) ? 0 : callStmt.hashCode());
			result = prime * result + ((destinationMethod == null) ? 0 : destinationMethod.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			CallKey other = (CallKey) obj;
			if (callStmt == null) {
				if (other.callStmt != null)
					return false;
			} else if (!callStmt.equals(other.callStmt))
				return false;
			if (destinationMethod == null) {
				if (other.destinationMethod != null)
					return false;
			} else if (!destinationMethod.equals(other.destinationMethod))
				return false;
			return true;
		}
	}

	private class ReturnKey extends CallKey {

		private final N exitStmt, returnSite;

		private ReturnKey(N callStmt, M destinationMethod, N exitStmt, N returnSite) {
			super(callStmt, destinationMethod);
			this.exitStmt = exitStmt;
			this.returnSite = returnSite;
		}

		public N getExitStmt() {
			return exitStmt;
		}

		public N getReturnSite() {
			return returnSite;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((exitStmt == null) ? 0 : exitStmt.hashCode());
			result = prime * result + ((returnSite == null) ? 0 : returnSite.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			ReturnKey other = (ReturnKey) obj;
			if (exitStmt == null) {
				if (other.exitStmt != null)
					return false;
			} else if (!exitStmt.equals(other.exitStmt))
				return false;
			if (returnSite == null) {
				if (other.returnSite != null)
					return false;
			} else if (!returnSite.equals(other.returnSite))
				return false;
			return true;
		}
	}

	public void printStats() {
		logger.debug(
				"Stats for flow-function cache:\n" + "Normal:         {}\n" + "Call:           {}\n"
						+ "Return:         {}\n" + "Call-to-return: {}\n",
				normalCache.stats(), callCache.stats(), returnCache.stats(), callToReturnCache.stats());
	}

}
