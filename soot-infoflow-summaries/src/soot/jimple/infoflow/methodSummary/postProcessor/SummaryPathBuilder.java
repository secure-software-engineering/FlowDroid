package soot.jimple.infoflow.methodSummary.postProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.SourceContextAndPath;
import soot.jimple.infoflow.data.pathBuilders.ContextSensitivePathBuilder;
import soot.jimple.infoflow.methodSummary.util.AliasUtils;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;

/**
 * Extended path reconstruction algorithm for StubDroid
 * 
 * @author Steven Arzt
 */
class SummaryPathBuilder extends ContextSensitivePathBuilder {

	private Set<SummaryResultInfo> resultInfos = new ConcurrentHashSet<SummaryResultInfo>();
	private Set<Abstraction> visitedAbstractions = Collections
			.newSetFromMap(new IdentityHashMap<Abstraction, Boolean>());
	private final SummaryPathBuilderContext context;

	/**
	 * Extended version of the {@link SourceInfo} class that also allows to store
	 * the abstractions along the path.
	 * 
	 * @author Steven Arzt
	 */
	public static class SummarySourceInfo extends ResultSourceInfo {

		private final AccessPath sourceAP;
		private final boolean isAlias;
		private final boolean isInCallee;

		public SummarySourceInfo() {
			this.sourceAP = null;
			this.isAlias = false;
			this.isInCallee = false;
		}

		public SummarySourceInfo(AccessPath source, Stmt context, Object userData, AccessPath sourceAP, boolean isAlias,
				boolean isInCallee) {
			super(null, source, context, userData, null, null);
			this.sourceAP = sourceAP;
			this.isAlias = isAlias;
			this.isInCallee = isInCallee;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + (isAlias ? 1231 : 1237);
			result = prime * result + ((sourceAP == null) ? 0 : sourceAP.hashCode());
			result = prime * result + (isInCallee ? 1231 : 1237);
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
			SummarySourceInfo other = (SummarySourceInfo) obj;
			if (isAlias != other.isAlias)
				return false;
			if (sourceAP == null) {
				if (other.sourceAP != null)
					return false;
			} else if (!sourceAP.equals(other.sourceAP))
				return false;
			if (isInCallee != other.isInCallee)
				return false;
			return true;
		}

		public AccessPath getSourceAP() {
			return this.sourceAP;
		}

		public boolean getIsAlias() {
			return this.isAlias;
		}

		public boolean getIsInCallee() {
			return this.isInCallee;
		}

	}

	/**
	 * Data class containing a single source-to-sink connection produced by
	 * FlowDroid
	 * 
	 * @author Steven Arzt
	 */
	public static class SummaryResultInfo {

		private final SummarySourceInfo sourceInfo;
		private final ResultSinkInfo sinkInfo;

		/**
		 * Creates a new instance of the {@link SummaryResultInfo} class
		 * 
		 * @param sourceInfo The source information object
		 * @param sinkInfo   The sink information object
		 */
		public SummaryResultInfo(SummarySourceInfo sourceInfo, ResultSinkInfo sinkInfo) {
			this.sourceInfo = sourceInfo;
			this.sinkInfo = sinkInfo;
		}

		/**
		 * Gets the source information for this source-to-sink connection
		 * 
		 * @return The source information for this source-to-sink connection
		 */
		public SummarySourceInfo getSourceInfo() {
			return this.sourceInfo;
		}

		/**
		 * Gets the sink information for this source-to-sink connection
		 * 
		 * @return The sink information for this source-to-sink connection
		 */
		public ResultSinkInfo getSinkInfo() {
			return this.sinkInfo;
		}

		@Override
		public String toString() {
			return "Source: " + sourceInfo + " -> Sink: " + sinkInfo;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((sinkInfo == null) ? 0 : sinkInfo.hashCode());
			result = prime * result + ((sourceInfo == null) ? 0 : sourceInfo.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SummaryResultInfo other = (SummaryResultInfo) obj;
			if (sinkInfo == null) {
				if (other.sinkInfo != null)
					return false;
			} else if (!sinkInfo.equals(other.sinkInfo))
				return false;
			if (sourceInfo == null) {
				if (other.sourceInfo != null)
					return false;
			} else if (!sourceInfo.equals(other.sourceInfo))
				return false;
			return true;
		}

	}

	/**
	 * Creates a new instance of the SummaryPathBuilder class
	 * 
	 * @param manager  The data flow manager that gives access to the icfg and other
	 *                 objects
	 * @param executor The executor in which to run the path reconstruction tasks
	 */
	public SummaryPathBuilder(InfoflowManager manager) {
		super(manager);
		this.context = new SummaryPathBuilderContext(manager.getTaintWrapper());
	}

	@Override
	protected boolean checkForSource(Abstraction abs, SourceContextAndPath scap) {
		// Record the abstraction
		visitedAbstractions.add(abs);

		// Source abstractions do not have predecessors
		if (abs.getPredecessor() != null)
			return false;

		// Save the abstraction path
		SummarySourceContextAndPath sscap = (SummarySourceContextAndPath) scap;
		SummarySourceInfo ssi = new SummarySourceInfo(abs.getSourceContext().getAccessPath(),
				abs.getSourceContext().getStmt(), abs.getSourceContext().getUserData(), sscap.getCurrentAccessPath(),
				sscap.getIsAlias(), !scap.isCallStackEmpty() || sscap.getDepth() != 0);
		ResultSinkInfo rsi = new ResultSinkInfo(null, scap.getAccessPath(), scap.getStmt());

		this.resultInfos.add(new SummaryResultInfo(ssi, rsi));
		return true;
	}

	/**
	 * Clears all results computed by this path reconstruction algorithm so far
	 */
	public void clear() {
		super.getResults().clear();
		resultInfos.clear();
		visitedAbstractions.clear();
		pathCache.clear();
	}

	/**
	 * Gets the source information and the reconstructed paths
	 * 
	 * @return The found source-to-sink connections and the respective propagation
	 *         paths
	 */
	public Set<SummaryResultInfo> getResultInfos() {
		return this.resultInfos;
	}

	@Override
	public InfoflowResults getResults() {
		throw new RuntimeException("Not implemented, use getResultInfos() instead");
	}

	@Override
	protected Runnable getTaintPathTask(AbstractionAtSink abs) {
		SourceContextAndPath scap = new SummarySourceContextAndPath(manager, abs.getAbstraction().getAccessPath(),
				abs.getSinkStmt(), AliasUtils.canAccessPathHaveAliases(abs.getAbstraction().getAccessPath()),
				abs.getAbstraction().getAccessPath(), new ArrayList<SootMethod>(), context);
		scap = scap.extendPath(abs.getAbstraction());

		if (scap != null) {
			if (pathCache.put(abs.getAbstraction(), scap))
				if (!checkForSource(abs.getAbstraction(), scap))
					return new SourceFindingTask(abs.getAbstraction());
		}
		return null;
	}

	@Override
	protected void onTaintPathsComputed() {
		// Don't shut down the executor, because we reset it and run several iterations
		// on the same path builder.
	}

	/**
	 * Terminates the path builder. Afterwards, no new tasks can be scheduled.
	 */
	public void shutdown() {
		executor.shutdown();
	}

}
