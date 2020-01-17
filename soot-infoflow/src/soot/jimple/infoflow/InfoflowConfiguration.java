package soot.jimple.infoflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central configuration class for FlowDroid
 * 
 * @author Steven Arzt
 *
 */
public class InfoflowConfiguration {

	protected final static Logger logger = LoggerFactory.getLogger(InfoflowConfiguration.class);

	/**
	 * Enumeration containing the callgraph algorithms supported for the use with
	 * the data flow tracker
	 */
	public static enum CallgraphAlgorithm {
		AutomaticSelection, CHA, VTA, RTA, SPARK, GEOM, OnDemand
	}

	/**
	 * Enumeration containing the aliasing algorithms supported by FlowDroid
	 */
	public static enum AliasingAlgorithm {
		/**
		 * A fully flow-sensitive algorithm based on Andromeda
		 */
		FlowSensitive,
		/**
		 * A flow-insensitive algorithm based on Soot's point-to-sets
		 */
		PtsBased,
		/**
		 * Do not perform any alias analysis
		 */
		None,
		/**
		 * Perform lazy aliasing. Propagate every taint everywhere to on-demand check
		 * whether it aliases with any value access
		 */
		Lazy
	}

	/**
	 * Enumeration containing all possible modes of dead and irrelevant code
	 * elimination
	 */
	public static enum CodeEliminationMode {
		/**
		 * Do not perform any code elimination before running the taint analysis
		 */
		NoCodeElimination,
		/**
		 * Perform an inter-procedural constant propagation and folding and then remove
		 * all code that is unreachable
		 */
		PropagateConstants,
		/**
		 * In addition to the inter-procedural constant propagation and folding, also
		 * remove live code that cannot potentially influence the outcome of the taint
		 * analysis
		 */
		RemoveSideEffectFreeCode
	}

	/**
	 * Enumeration containing the supported data flow solvers
	 */
	public static enum DataFlowSolver {
		/**
		 * Use a flow- and context-sensitive solver
		 */
		ContextFlowSensitive,

		/**
		 * Use a context-sensitive, but flow-insensitive solver
		 */
		FlowInsensitive
	}

	/**
	 * Enumeration containing the supported modes how the data flow analyzer shall
	 * handle implicit flows
	 */
	public static enum ImplicitFlowMode {
		/**
		 * Implicit flows will not be tracked at all
		 */
		NoImplicitFlows,
		/**
		 * Create a new taint when a non-tainted array is accessed using a tainted
		 * index, but do not flow other control-flow dependencies
		 */
		ArrayAccesses,
		/**
		 * Follow all control flow dependencies that involve tainted data
		 */
		AllImplicitFlows;

		/**
		 * Gets whether the current mode tracks array accesses on tainted indices
		 * 
		 * @return True if the current mode tracks array accesses on tainted indices,
		 *         otherwise false
		 */
		public boolean trackArrayAccesses() {
			return this == AllImplicitFlows || this == ArrayAccesses;
		}

		/**
		 * Gets whether the current mode tracks control flow dependencies
		 * 
		 * @return True if the current mode tracks control flow dependencies, otherwise
		 *         false
		 */
		public boolean trackControlFlowDependencies() {
			return this == AllImplicitFlows;
		}

	}

	/**
	 * Supported modes for reconstructing paths between sources and sinks. This mode
	 * does not influence the association between sources and sinks, only the
	 * processing of the path in between.
	 */
	public enum PathReconstructionMode {
		/**
		 * Do not reconstruct paths, only associate sources and sinks
		 */
		NoPaths,
		/**
		 * Reconstruct the path between source and sink, but allow for simplifications
		 * to improve performance
		 */
		Fast,
		/**
		 * Reconstruct the precise path between source and sink, do not simplify
		 * anything. This is the most precise option, but may also take the longest.
		 */
		Precise;

		/**
		 * Gets whether this configuration reconstructs the data flow paths between
		 * source and sink
		 * 
		 * @return True if this configuration reconstructs the data flow paths between
		 *         source and sink, otherwise (i.e., if only the source-to-sink mappings
		 *         are reconstructed) false
		 */
		public boolean reconstructPaths() {
			return this != NoPaths;
		}
	}

	/**
	 * Enumeration containing the supported path builders
	 */
	public enum PathBuildingAlgorithm {
		/**
		 * Simple context-insensitive, single-threaded, recursive approach to path
		 * reconstruction. Low overhead for small examples, but does not scale.
		 */
		Recursive,
		/**
		 * Highly precise context-sensitive path reconstruction approach. For a large
		 * number of paths or complex programs, it may be slow.
		 */
		ContextSensitive,
		/**
		 * A context-insensitive path reconstruction algorithm. It scales well, but may
		 * introduce false positives.
		 */
		ContextInsensitive,
		/**
		 * Very fast context-insensitive implementation that only finds source-to-sink
		 * connections, but no paths.
		 */
		ContextInsensitiveSourceFinder,
		/**
		 * An empty implementation that not reconstruct any paths and always returns an
		 * empty set. For internal use only.
		 */
		None
	}

	/**
	 * Enumeration containing the supported techniques for handling taints on static
	 * fields
	 */
	public enum StaticFieldTrackingMode {
		/**
		 * Track taints on static fields as normal taint abstraction. This approach is
		 * context- and flow-sensitive, but also very costly (time and memory).
		 */
		ContextFlowSensitive,

		/**
		 * Track taints on static fields as field-based annotations. This approach is
		 * neither context-, nor flow-sensitive, but very efficient.
		 */
		ContextFlowInsensitive,

		/**
		 * Do not track any taints on static fields
		 */
		None
	}

	/**
	 * The configuration that defines how FlowDroid shall handle between sources and
	 * sinks
	 * 
	 * @author Steven Arzt
	 *
	 */
	public static class PathConfiguration {

		private boolean sequentialPathProcessing = false;
		private PathReconstructionMode pathReconstructionMode = PathReconstructionMode.NoPaths;
		private PathBuildingAlgorithm pathBuildingAlgorithm = PathBuildingAlgorithm.ContextSensitive;
		private int maxCallStackSize = 30;
		private int maxPathLength = 75;
		private int maxPathsPerAbstraction = 15;
		private long pathReconstructionTimeout = 0;
		private int pathReconstructionBatchSize = 5;

		/**
		 * Copies the settings of the given configuration into this configuration object
		 * 
		 * @param pathConfig The other configuration object
		 */
		public void merge(PathConfiguration pathConfig) {
			this.sequentialPathProcessing = pathConfig.sequentialPathProcessing;
			this.pathReconstructionMode = pathConfig.pathReconstructionMode;
			this.pathBuildingAlgorithm = pathConfig.pathBuildingAlgorithm;
			this.maxCallStackSize = pathConfig.maxCallStackSize;
			this.maxPathLength = pathConfig.maxPathLength;
			this.maxPathsPerAbstraction = pathConfig.maxPathsPerAbstraction;
			this.pathReconstructionTimeout = pathConfig.pathReconstructionTimeout;
			this.pathReconstructionBatchSize = pathConfig.pathReconstructionBatchSize;
		}

		/**
		 * Gets whether FlowDroid shall perform sequential path reconstruction instead
		 * of running all reconstruction tasks concurrently. This can reduce the memory
		 * consumption, but will likely take longer when memory is not an issue.
		 * 
		 * @return True if the path reconstruction tasks shall be run sequentially,
		 *         false for running them in parallel
		 */
		public boolean getSequentialPathProcessing() {
			return this.sequentialPathProcessing;
		}

		/**
		 * Sets whether FlowDroid shall perform sequential path reconstruction instead
		 * of running all reconstruction tasks concurrently. This can reduce the memory
		 * consumption, but will likely take longer when memory is not an issue.
		 * 
		 * @param sequentialPathProcessing True if the path reconstruction tasks shall
		 *                                 be run sequentially, false for running them
		 *                                 in parallel
		 */
		public void setSequentialPathProcessing(boolean sequentialPathProcessing) {
			this.sequentialPathProcessing = sequentialPathProcessing;
		}

		/**
		 * Gets the mode that defines how the paths between sources and sinks shall be
		 * reconstructed
		 * 
		 * @return The mode that defines how the paths between sources and sinks shall
		 *         be reconstructed
		 */
		public PathReconstructionMode getPathReconstructionMode() {
			return pathReconstructionMode;
		}

		/**
		 * Sets the mode that defines how the paths between sources and sinks shall be
		 * reconstructed
		 * 
		 * @param pathReconstructionMode The mode that defines how the paths between
		 *                               sources and sinks shall be reconstructed
		 */
		public void setPathReconstructionMode(PathReconstructionMode pathReconstructionMode) {
			this.pathReconstructionMode = pathReconstructionMode;
		}

		/**
		 * Gets the algorithm that shall be used for reconstructing the propagation
		 * paths between source and sink
		 * 
		 * @return The algorithm that shall be used for reconstructing the propagation
		 *         paths between source and sink
		 */
		public PathBuildingAlgorithm getPathBuildingAlgorithm() {
			return pathBuildingAlgorithm;
		}

		/**
		 * Sets the algorithm that shall be used for reconstructing the propagation
		 * paths between source and sink
		 * 
		 * @param pathBuildingAlgorithm The algorithm that shall be used for
		 *                              reconstructing the propagation paths between
		 *                              source and sink
		 */
		public void setPathBuildingAlgorithm(PathBuildingAlgorithm pathBuildingAlgorithm) {
			this.pathBuildingAlgorithm = pathBuildingAlgorithm;
		}

		/**
		 * Sets the maximum call stack size. If the call stack grows longer than this
		 * amount of entries, the respective path will no longer be followed.
		 * 
		 * @param maxCallStackSize The maximum call stack size
		 */
		public void setMaxCallStackSize(int maxCallStackSize) {
			this.maxCallStackSize = maxCallStackSize;
		}

		/**
		 * Gets the maximum call stack size. If the call stack grows longer than this
		 * amount of entries, the respective path will no longer be followed.
		 * 
		 * @return The maximum call stack size
		 */
		public int getMaxCallStackSize() {
			return maxCallStackSize;
		}

		/**
		 * Gets the maximum size for taint propagation paths. If a path is growing
		 * longer than this limit, the path reconstruction is aborted and the respective
		 * path is skipped.
		 * 
		 * @return The maximum length of a taint propagtation path3
		 */
		public int getMaxPathLength() {
			return maxPathLength;
		}

		/**
		 * Sets the maximum size for taint propagation paths. If a path is growing
		 * longer than this limit, the path reconstruction is aborted and the respective
		 * path is skipped.
		 * 
		 * @param maxPathLenfgth The maximum length of a taint propagtation path3
		 */
		public void setMaxPathLength(int maxPathLength) {
			this.maxPathLength = maxPathLength;
		}

		/**
		 * Gets the maximum number of paths that shall be recorded per abstraction. If
		 * this threshold is reached, all further paths will be discarded.
		 * 
		 * @return The maximum number of paths that shall be recorded per abstraction.
		 */
		public int getMaxPathsPerAbstraction() {
			return maxPathsPerAbstraction;
		}

		/**
		 * Sets the maximum number of paths that shall be recorded per abstraction. If
		 * this threshold is reached, all further paths will be discarded.
		 * 
		 * @param maxPathsPerAbstraction The maximum number of paths that shall be
		 *                               recorded per abstraction.
		 */
		public void setMaxPathsPerAbstraction(int maxPathsPerAbstraction) {
			this.maxPathsPerAbstraction = maxPathsPerAbstraction;
		}

		/**
		 * Gets the timeout in seconds after which path reconstruction shall be aborted.
		 * This timeout is applied after the data flow analysis has been completed. If
		 * incremental path reconstruction is used, it is applied for the remaining path
		 * reconstruction after the data flow analysis has been completed. If
		 * incremental path reconstruction is not used, the timeout is applied to the
		 * complete path reconstruction phase, because it does not overlap with the data
		 * flow analysis phase in this case.
		 * 
		 * @return The timeout in seconds after which the path reconstruction shall be
		 *         aborted
		 */
		public long getPathReconstructionTimeout() {
			return this.pathReconstructionTimeout;
		}

		/**
		 * Sets the timeout in seconds after which path reconstruction shall be aborted.
		 * This timeout is applied after the data flow analysis has been completed. If
		 * incremental path reconstruction is used, it is applied for the remaining path
		 * reconstruction after the data flow analysis has been completed. If
		 * incremental path reconstruction is not used, the timeout is applied to the
		 * complete path reconstruction phase, because it does not overlap with the data
		 * flow analysis phase in this case.
		 * 
		 * @param timeout The timeout in seconds after which the path reconstruction
		 *                shall be aborted
		 */
		public void setPathReconstructionTimeout(long timeout) {
			this.pathReconstructionTimeout = timeout;
		}

		/**
		 * Gets the number of paths that shall be reconstructed in one batch. Reduce
		 * this value to lower memory pressure during path reconstruction.
		 * 
		 * @return The number of paths that shall be reconstructed in one batch
		 */
		public int getPathReconstructionBatchSize() {
			return pathReconstructionBatchSize;
		}

		/**
		 * Sets the number of paths that shall be reconstructed in one batch. Reduce
		 * this value to lower memory pressure during path reconstruction.
		 * 
		 * @param pathReconstructionBatchSize The number of paths that shall be
		 *                                    reconstructed in one batch
		 */
		public void setPathReconstructionBatchSize(int pathReconstructionBatchSize) {
			this.pathReconstructionBatchSize = pathReconstructionBatchSize;
		}

		/**
		 * Gets whether the analysis must keep statements along the path
		 * 
		 * @return True if the analysis must keep statements along th path, false
		 *         otherwise
		 */
		public boolean mustKeepStatements() {
			return pathReconstructionMode.reconstructPaths()
					|| pathBuildingAlgorithm == PathBuildingAlgorithm.ContextSensitive;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + maxCallStackSize;
			result = prime * result + maxPathLength;
			result = prime * result + maxPathsPerAbstraction;
			result = prime * result + ((pathBuildingAlgorithm == null) ? 0 : pathBuildingAlgorithm.hashCode());
			result = prime * result + pathReconstructionBatchSize;
			result = prime * result + ((pathReconstructionMode == null) ? 0 : pathReconstructionMode.hashCode());
			result = prime * result + (int) (pathReconstructionTimeout ^ (pathReconstructionTimeout >>> 32));
			result = prime * result + (sequentialPathProcessing ? 1231 : 1237);
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
			PathConfiguration other = (PathConfiguration) obj;
			if (maxCallStackSize != other.maxCallStackSize)
				return false;
			if (maxPathLength != other.maxPathLength)
				return false;
			if (maxPathsPerAbstraction != other.maxPathsPerAbstraction)
				return false;
			if (pathBuildingAlgorithm != other.pathBuildingAlgorithm)
				return false;
			if (pathReconstructionBatchSize != other.pathReconstructionBatchSize)
				return false;
			if (pathReconstructionMode != other.pathReconstructionMode)
				return false;
			if (pathReconstructionTimeout != other.pathReconstructionTimeout)
				return false;
			if (sequentialPathProcessing != other.sequentialPathProcessing)
				return false;
			return true;
		}

	}

	/**
	 * Class that defines settings for writing out the FlowDroid results
	 * 
	 * @author Steven Arzt
	 *
	 */
	public static class OutputConfiguration {

		private boolean noPassedValues = false;
		private boolean noCallGraphFraction = false;
		private int maxCallersInOutputFile = 5;
		private long resultSerializationTimeout = 0;

		/**
		 * Copies the settings of the given configuration into this configuration object
		 * 
		 * @param outputConfig The other configuration object
		 */
		public void merge(OutputConfiguration outputConfig) {
			this.noPassedValues = outputConfig.noPassedValues;
			this.noCallGraphFraction = outputConfig.noCallGraphFraction;
			this.maxCallersInOutputFile = outputConfig.maxCallersInOutputFile;
			this.resultSerializationTimeout = outputConfig.resultSerializationTimeout;
		}

		/**
		 * Gets whether FlowDroid shall exclude the passed values to sources and sinks
		 * from the xml output from the analysis
		 * 
		 * @return True if FlowDroid shall exclude the passed values to sources and
		 *         sinks, otherwise false
		 */
		public boolean getNoPassedValues() {
			return this.noPassedValues;
		}

		/**
		 * Sets whether to exclude the call graph fraction from the entry points to the
		 * source in the xml output
		 * 
		 * @param noCallGraphFraction True if the call graph fraction from the entry
		 *                            points to the source shall be excluded from the
		 *                            xml output
		 */
		public void setNoCallGraphFraction(boolean noCallGraphFraction) {
			this.noCallGraphFraction = noCallGraphFraction;
		}

		/**
		 * Gets whether to exclude the call graph fraction from the entry points to the
		 * source in the xml output
		 * 
		 * @return True if the call graph fraction from the entry points to the source
		 *         shall be excluded from the xml output
		 */
		public boolean getNoCallGraphFraction() {
			return noCallGraphFraction;
		}

		/**
		 * Specifies the maximum number of callers that shall be considered per node
		 * when writing out the call graph fraction from the entry point to the source.
		 * 
		 * @param maxCallers The maximum number of callers to consider when writing out
		 *                   the call graph fraction between entry point and source
		 */
		public void setMaxCallersInOutputFile(int maxCallers) {
			this.maxCallersInOutputFile = maxCallers;
		}

		/**
		 * Gets the maximum number of callers that shall be considered per node when
		 * writing out the call graph fraction from the entry point to the source.
		 * 
		 * @return The maximum number of callers to consider when writing out the call
		 *         graph fraction between entry point and source
		 */
		public int getMaxCallersInOutputFile() {
			return this.maxCallersInOutputFile;
		}

		/**
		 * Sets the timeout in seconds for the result serialization process. Writing out
		 * the results is aborted if it takes longer than the given amount of time.
		 * 
		 * @param timeout The maximum time for writing out the results in seconds
		 */
		public void setResultSerializationTimeout(long timeout) {
			this.resultSerializationTimeout = timeout;
		}

		/**
		 * Gets the timeout for the result serialization process in seconds. Writing out
		 * the results is aborted if it takes longer than the given amount of time.
		 * 
		 * @result The maximum time for writing out the results in seconds
		 */
		public long getResultSerializationTimeout() {
			return this.resultSerializationTimeout;
		}

		/**
		 * Sets the option for exclusion of the passed values to sources and sinks in
		 * the xml output
		 * 
		 * @param noPassedValues the boolean value whether passed values should be
		 *                       excluded from the xml output
		 */
		public void setNoPassedValues(boolean noPassedValues) {
			this.noPassedValues = noPassedValues;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + maxCallersInOutputFile;
			result = prime * result + (noCallGraphFraction ? 1231 : 1237);
			result = prime * result + (noPassedValues ? 1231 : 1237);
			result = prime * result + (int) (resultSerializationTimeout ^ (resultSerializationTimeout >>> 32));
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
			OutputConfiguration other = (OutputConfiguration) obj;
			if (maxCallersInOutputFile != other.maxCallersInOutputFile)
				return false;
			if (noCallGraphFraction != other.noCallGraphFraction)
				return false;
			if (noPassedValues != other.noPassedValues)
				return false;
			if (resultSerializationTimeout != other.resultSerializationTimeout)
				return false;
			return true;
		}

	}

	/**
	 * Class containing the configuration options for the IFDS data flow solver
	 * 
	 * @author Steven Arzt
	 *
	 */
	public static class SolverConfiguration {

		private DataFlowSolver dataFlowSolver = DataFlowSolver.ContextFlowSensitive;
		private int maxJoinPointAbstractions = 10;
		private int maxCalleesPerCallSite = 75;
		private int maxAbstractionPathLength = 100;

		/**
		 * Copies the settings of the given configuration into this configuration object
		 * 
		 * @param solverConfig The other configuration object
		 */
		public void merge(SolverConfiguration solverConfig) {
			this.dataFlowSolver = solverConfig.dataFlowSolver;
			this.maxJoinPointAbstractions = solverConfig.maxJoinPointAbstractions;
			this.maxCalleesPerCallSite = solverConfig.maxCalleesPerCallSite;
			this.maxAbstractionPathLength = solverConfig.maxAbstractionPathLength;
		}

		/**
		 * Gets the data flow solver to be used for the taint analysis
		 * 
		 * @return The data flow solver to be used for the taint analysis
		 */
		public DataFlowSolver getDataFlowSolver() {
			return this.dataFlowSolver;
		}

		/**
		 * Sets the data flow solver to be used for the taint analysis
		 * 
		 * @param solver The data flow solver to be used for the taint analysis
		 */
		public void setDataFlowSolver(DataFlowSolver solver) {
			this.dataFlowSolver = solver;
		}

		/**
		 * Gets the maximum number of abstractions that shall be recorded per join
		 * point. In other words, enabling this option disables the recording of
		 * neighbors beyond the given count. This greatly reduces the memory
		 * requirements of the analysis. On the other hand, if data is tainted from two
		 * different sources, only some of them will be reported.
		 * 
		 * @return The maximum number of abstractions per join point, or -1 to record an
		 *         arbitrary number of join point abstractions
		 */
		public int getMaxJoinPointAbstractions() {
			return this.maxJoinPointAbstractions;
		}

		/**
		 * Sets the maximum number of abstractions that shall be recorded per join
		 * point. In other words, enabling this option disables the recording of
		 * neighbors beyond the given count. This greatly reduces the memory
		 * requirements of the analysis. On the other hand, if data is tainted from two
		 * different sources, only some of them will be reported.
		 * 
		 * @param maxJoinPointAbstractions The maximum number of abstractions per join
		 *                                 point, or -1 to record an arbitrary number of
		 *                                 join point abstractions
		 */
		public void setMaxJoinPointAbstractions(int maxJoinPointAbstractions) {
			this.maxJoinPointAbstractions = maxJoinPointAbstractions;
		}

		/**
		 * Gets the maximum number of callees permitted per call site. If a call site
		 * has more callees than this limit, the call site is ignored completely. Use
		 * this limit to reduce the impact of imprecise call graphs.
		 * 
		 * @return The maximum number of callees per call site
		 */
		public int getMaxCalleesPerCallSite() {
			return maxCalleesPerCallSite;
		}

		/**
		 * Sets the maximum number of callees permitted per call site. If a call site
		 * has more callees than this limit, the call site is ignored completely. Use
		 * this limit to reduce the impact of imprecise call graphs.
		 * 
		 * @param maxCalleesPerCallSite The maximum number of callees per call site
		 */
		public void setMaxCalleesPerCallSite(int maxCalleesPerCallSite) {
			this.maxCalleesPerCallSite = maxCalleesPerCallSite;
		}

		/**
		 * Sets the maximum number of join point abstractions to a single abstraction.
		 * 
		 * @param singleJointAbstraction True to configure the solver to register only a
		 *                               single join point abstraction
		 */
		public void setSingleJoinPointAbstraction(boolean singleJointAbstraction) {
			this.maxJoinPointAbstractions = singleJointAbstraction ? 1 : 10;
		}

		/**
		 * Gets the maximum length over which an abstraction may be propagated before
		 * the abstractions is dropped
		 * 
		 * @return The maximum length over which an abstraction may be propagated
		 */
		public int getMaxAbstractionPathLength() {
			return maxAbstractionPathLength;
		}

		/**
		 * Sets the maximum length over which an abstraction may be propagated before
		 * the abstractions is dropped
		 * 
		 * @param maxAbstractionPathLength The maximum length over which an abstraction
		 *                                 may be propagated
		 */
		public void setMaxAbstractionPathLength(int maxAbstractionPathLength) {
			this.maxAbstractionPathLength = maxAbstractionPathLength;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((dataFlowSolver == null) ? 0 : dataFlowSolver.hashCode());
			result = prime * result + maxCalleesPerCallSite;
			result = prime * result + maxJoinPointAbstractions;
			result = prime * result + maxAbstractionPathLength;
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
			SolverConfiguration other = (SolverConfiguration) obj;
			if (dataFlowSolver != other.dataFlowSolver)
				return false;
			if (maxCalleesPerCallSite != other.maxCalleesPerCallSite)
				return false;
			if (maxJoinPointAbstractions != other.maxJoinPointAbstractions)
				return false;
			if (maxAbstractionPathLength != other.maxAbstractionPathLength)
				return false;
			return true;
		}

	}

	/**
	 * Class containing the configuration options for dealing with access paths
	 * (maximum length, reduction options, etc.)
	 * 
	 * @author Steven Arzt
	 *
	 */
	public static class AccessPathConfiguration {

		private int accessPathLength = 5;
		private boolean useRecursiveAccessPaths = true;
		private boolean useThisChainReduction = true;
		private boolean useSameFieldReduction = true;

		/**
		 * Merges the given configuration options into this configuration object
		 * 
		 * @param config The configuration data to merge in
		 */
		public void merge(AccessPathConfiguration config) {
			this.accessPathLength = config.accessPathLength;
			this.useRecursiveAccessPaths = config.useRecursiveAccessPaths;
			this.useThisChainReduction = config.useThisChainReduction;
			this.useSameFieldReduction = config.useSameFieldReduction;
		}

		/**
		 * Gets the maximum depth of the access paths. All paths will be truncated if
		 * they exceed the given size.
		 * 
		 * @param accessPathLength the maximum value of an access path.
		 */
		public int getAccessPathLength() {
			return accessPathLength;
		}

		/**
		 * Sets the maximum depth of the access paths. All paths will be truncated if
		 * they exceed the given size.
		 * 
		 * @param accessPathLength the maximum value of an access path. If it gets
		 *                         longer than this value, it is truncated and all
		 *                         following fields are assumed as tainted (which is
		 *                         imprecise but gains performance) Default value is 5.
		 */
		public void setAccessPathLength(int accessPathLength) {
			this.accessPathLength = accessPathLength;
		}

		/**
		 * Gets whether recursive access paths shall be reduced, e.g. whether we shall
		 * propagate a.[next].data instead of a.next.next.data.
		 * 
		 * @return True if recursive access paths shall be reduced, otherwise false
		 */
		public boolean getUseRecursiveAccessPaths() {
			return useRecursiveAccessPaths;
		}

		/**
		 * Sets whether recursive access paths shall be reduced, e.g. whether we shall
		 * propagate a.[next].data instead of a.next.next.data.
		 * 
		 * @param useRecursiveAccessPaths True if recursive access paths shall be
		 *                                reduced, otherwise false
		 */
		public void setUseRecursiveAccessPaths(boolean useRecursiveAccessPaths) {
			this.useRecursiveAccessPaths = useRecursiveAccessPaths;
		}

		/**
		 * Gets whether access paths pointing to outer objects using this$n shall be
		 * reduced, e.g. whether we shall propagate a.data instead of a.this$0.a.data.
		 * 
		 * @return True if access paths including outer objects shall be reduced,
		 *         otherwise false
		 */
		public boolean getUseThisChainReduction() {
			return this.useThisChainReduction;
		}

		/**
		 * Sets whether access paths pointing to outer objects using this$n shall be
		 * reduced, e.g. whether we shall propagate a.data instead of a.this$0.a.data.
		 * 
		 * @param useThisChainReduction True if access paths including outer objects
		 *                              shall be reduced, otherwise false
		 */
		public void setUseThisChainReduction(boolean useThisChainReduction) {
			this.useThisChainReduction = useThisChainReduction;
		}

		/**
		 * Gets whether access paths that repeat the same field shall be reduced, i.e.,
		 * whether a.next.obj shall be propagated instead of a.next.next.obj.
		 * 
		 * @return True if access paths with repeating fields shall be reduced to the
		 *         last occurrence of that field
		 */
		public boolean getUseSameFieldReduction() {
			return useSameFieldReduction;
		}

		/**
		 * Sets whether access paths that repeat the same field shall be reduced, i.e.,
		 * whether a.next.obj shall be propagated instead of a.next.next.obj.
		 * 
		 * @param useSameFieldReduction True if access paths with repeating fields shall
		 *                              be reduced to the last occurrence of that field
		 */
		public void setUseSameFieldReduction(boolean useSameFieldReduction) {
			this.useSameFieldReduction = useSameFieldReduction;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + accessPathLength;
			result = prime * result + (useRecursiveAccessPaths ? 1231 : 1237);
			result = prime * result + (useSameFieldReduction ? 1231 : 1237);
			result = prime * result + (useThisChainReduction ? 1231 : 1237);
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
			AccessPathConfiguration other = (AccessPathConfiguration) obj;
			if (accessPathLength != other.accessPathLength)
				return false;
			if (useRecursiveAccessPaths != other.useRecursiveAccessPaths)
				return false;
			if (useSameFieldReduction != other.useSameFieldReduction)
				return false;
			if (useThisChainReduction != other.useThisChainReduction)
				return false;
			return true;
		}

	}

	private static boolean pathAgnosticResults = true;
	private static boolean oneResultPerAccessPath = false;
	private static boolean mergeNeighbors = false;

	private int stopAfterFirstKFlows = 0;
	private ImplicitFlowMode implicitFlowMode = ImplicitFlowMode.NoImplicitFlows;
	private boolean enableExceptions = true;
	private boolean enableArrays = true;
	private boolean enableArraySizeTainting = true;
	private boolean flowSensitiveAliasing = true;
	private boolean enableTypeChecking = true;
	private boolean ignoreFlowsInSystemPackages = false;
	private boolean excludeSootLibraryClasses = false;
	private int maxThreadNum = -1;
	private boolean writeOutputFiles = false;
	private boolean logSourcesAndSinks = false;
	private boolean enableReflection = false;
	private boolean enableLineNumbers = false;
	private boolean enableOriginalNames = false;

	private boolean inspectSources = false;
	private boolean inspectSinks = false;

	private PathConfiguration pathConfiguration = new PathConfiguration();
	private OutputConfiguration outputConfiguration = new OutputConfiguration();
	private SolverConfiguration solverConfiguration = new SolverConfiguration();
	private AccessPathConfiguration accessPathConfiguration = new AccessPathConfiguration();

	private CallgraphAlgorithm callgraphAlgorithm = CallgraphAlgorithm.AutomaticSelection;
	private AliasingAlgorithm aliasingAlgorithm = AliasingAlgorithm.FlowSensitive;
	private CodeEliminationMode codeEliminationMode = CodeEliminationMode.PropagateConstants;
	private StaticFieldTrackingMode staticFieldTrackingMode = StaticFieldTrackingMode.ContextFlowSensitive;

	private boolean taintAnalysisEnabled = true;
	private boolean incrementalResultReporting = false;
	private long dataFlowTimeout = 0;
	private double memoryThreshold = 0.9d;
	private boolean oneSourceAtATime = false;

	/**
	 * Merges the given configuration options into this configuration object
	 * 
	 * @param config The configuration data to merge in
	 */
	public void merge(InfoflowConfiguration config) {
		this.stopAfterFirstKFlows = config.stopAfterFirstKFlows;
		this.implicitFlowMode = config.implicitFlowMode;
		this.enableExceptions = config.enableExceptions;
		this.enableArrays = config.enableArrays;
		this.enableArraySizeTainting = config.enableArraySizeTainting;
		this.flowSensitiveAliasing = config.flowSensitiveAliasing;
		this.enableTypeChecking = config.enableTypeChecking;
		this.ignoreFlowsInSystemPackages = config.ignoreFlowsInSystemPackages;
		this.excludeSootLibraryClasses = config.excludeSootLibraryClasses;
		this.maxThreadNum = config.maxThreadNum;
		this.writeOutputFiles = config.writeOutputFiles;
		this.logSourcesAndSinks = config.logSourcesAndSinks;
		this.enableReflection = config.enableReflection;
		this.enableLineNumbers = config.enableLineNumbers;
		this.enableOriginalNames = config.enableOriginalNames;

		this.pathConfiguration.merge(config.pathConfiguration);
		this.outputConfiguration.merge(config.outputConfiguration);
		this.solverConfiguration.merge(config.solverConfiguration);
		this.accessPathConfiguration.merge(config.accessPathConfiguration);

		this.callgraphAlgorithm = config.callgraphAlgorithm;
		this.aliasingAlgorithm = config.aliasingAlgorithm;
		this.codeEliminationMode = config.codeEliminationMode;

		this.inspectSources = config.inspectSources;
		this.inspectSinks = config.inspectSinks;

		this.callgraphAlgorithm = config.callgraphAlgorithm;
		this.aliasingAlgorithm = config.aliasingAlgorithm;
		this.codeEliminationMode = config.codeEliminationMode;
		this.staticFieldTrackingMode = config.staticFieldTrackingMode;

		this.taintAnalysisEnabled = config.writeOutputFiles;
		this.incrementalResultReporting = config.incrementalResultReporting;
		this.dataFlowTimeout = config.dataFlowTimeout;
		this.memoryThreshold = config.memoryThreshold;
		this.oneSourceAtATime = config.oneSourceAtATime;
	}

	/**
	 * Sets whether results (source-to-sink connections) that only differ in their
	 * propagation paths shall be merged into a single result or not.
	 * 
	 * @param pathAgnosticResults True if two results shall be regarded as equal if
	 *                            they connect the same source and sink, even if
	 *                            their propagation paths differ, otherwise false
	 */
	public static void setPathAgnosticResults(boolean pathAgnosticResults) {
		InfoflowConfiguration.pathAgnosticResults = pathAgnosticResults;
	}

	/**
	 * Gets whether results (source-to-sink connections) that only differ in their
	 * propagation paths shall be merged into a single result or not.
	 * 
	 * @return True if two results shall be regarded as equal if they connect the
	 *         same source and sink, even if their propagation paths differ,
	 *         otherwise false
	 */
	public static boolean getPathAgnosticResults() {
		return InfoflowConfiguration.pathAgnosticResults;
	}

	/**
	 * Gets whether different results shall be reported if they only differ in the
	 * access path the reached the sink or left the source
	 * 
	 * @return True if results shall also be distinguished based on access paths
	 */
	public static boolean getOneResultPerAccessPath() {
		return oneResultPerAccessPath;
	}

	/**
	 * Gets whether different results shall be reported if they only differ in the
	 * access path the reached the sink or left the source
	 * 
	 * @param oneResultPerAP True if results shall also be distinguished based on
	 *                       access paths
	 */
	public static void setOneResultPerAccessPath(boolean oneResultPerAP) {
		oneResultPerAccessPath = oneResultPerAP;
	}

	/**
	 * Gets whether neighbors at the same statement shall be merged into a single
	 * abstraction
	 * 
	 * @return True if equivalent neighbor shall be merged, otherwise false
	 */
	public static boolean getMergeNeighbors() {
		return mergeNeighbors;
	}

	/**
	 * Sets whether neighbors at the same statement shall be merged into a single
	 * abstraction
	 * 
	 * @param value True if equivalent neighbor shall be merged, otherwise false
	 */
	public static void setMergeNeighbors(boolean value) {
		InfoflowConfiguration.mergeNeighbors = value;
	}

	/**
	 * Sets after how many flows the information flow analysis shall stop.
	 * 
	 * @param stopAfterFirstKFlows number of flows after which to stop
	 */
	public void setStopAfterFirstKFlows(int stopAfterFirstKFlows) {
		this.stopAfterFirstKFlows = stopAfterFirstKFlows;
	}

	/**
	 * Gets after how many flows the information flow analysis shall stop.
	 * 
	 * @return number of flows after which to stop
	 */
	public int getStopAfterFirstKFlows() {
		return stopAfterFirstKFlows;
	}

	/**
	 * Sets whether the information flow analysis shall stop after the first flow
	 * has been found
	 * 
	 * @param stopAfterFirstFlow True if the analysis shall stop after the first
	 *                           flow has been found, otherwise false.
	 */
	public void setStopAfterFirstFlow(boolean stopAfterFirstFlow) {
		this.stopAfterFirstKFlows = stopAfterFirstFlow ? 1 : 0;
	}

	/**
	 * Gets whether the information flow analysis shall stop after the first flow
	 * has been found
	 * 
	 * @return True if the information flow analysis shall stop after the first flow
	 *         has been found, otherwise false
	 */
	public boolean getStopAfterFirstFlow() {
		return stopAfterFirstKFlows == 1;
	}

	/**
	 * Sets whether the implementations of source methods shall be analyzed as well
	 * 
	 * @param inspect True if the implementations of source methods shall be
	 *                analyzed as well, otherwise false
	 */
	public void setInspectSources(boolean inspect) {
		inspectSources = inspect;
	}

	/**
	 * Gets whether the implementations of source methods shall be analyzed as well
	 * 
	 * @return True if the implementations of source methods shall be analyzed as
	 *         well, otherwise false
	 */
	public boolean getInspectSources() {
		return inspectSources;
	}

	/**
	 * Sets whether the implementations of sink methods shall be analyzed as well
	 * 
	 * @param inspect True if the implementations of sink methods shall be analyzed
	 *                as well, otherwise false
	 */
	public void setInspectSinks(boolean inspect) {
		inspectSinks = inspect;
	}

	/**
	 * Sets whether the implementations of sink methods shall be analyzed as well
	 * 
	 * @return True if the implementations of sink methods shall be analyzed as
	 *         well, otherwise false
	 */
	public boolean getInspectSinks() {
		return inspectSinks;
	}

	/**
	 * Sets the mode that defines whether and how the solver shall handle implicit
	 * flows
	 * 
	 * @param implicitFlowMode The mode that defines whether and how the solver
	 *                         shall handle implicit flows
	 */
	public void setImplicitFlowMode(ImplicitFlowMode implicitFlowMode) {
		this.implicitFlowMode = implicitFlowMode;
	}

	/**
	 * Gets the mode that defines whether and how the solver shall handle implicit
	 * flows
	 * 
	 * @return The mode that defines whether and how the solver shall handle
	 *         implicit flows
	 */
	public ImplicitFlowMode getImplicitFlowMode() {
		return implicitFlowMode;
	}

	/**
	 * Sets how the data flow solver shall treat assignments to static fields
	 * 
	 * @param staticFieldTrackingMode The mode that specifies the precision with
	 *                                which assignments to static fields shall be
	 *                                handled
	 */
	public void setStaticFieldTrackingMode(StaticFieldTrackingMode staticFieldTrackingMode) {
		this.staticFieldTrackingMode = staticFieldTrackingMode;
	}

	/**
	 * Gets how the data flow solver shall treat assignments to static fields
	 * 
	 * @return The mode that specifies the precision with which assignments to
	 *         static fields shall be handled
	 */
	public StaticFieldTrackingMode getStaticFieldTrackingMode() {
		return staticFieldTrackingMode;
	}

	/**
	 * Sets whether a flow sensitive aliasing algorithm shall be used
	 * 
	 * @param flowSensitiveAliasing True if a flow sensitive aliasing algorithm
	 *                              shall be used, otherwise false
	 */
	public void setFlowSensitiveAliasing(boolean flowSensitiveAliasing) {
		this.flowSensitiveAliasing = flowSensitiveAliasing;
	}

	/**
	 * Gets whether a flow sensitive aliasing algorithm shall be used
	 * 
	 * @return True if a flow sensitive aliasing algorithm shall be used, otherwise
	 *         false
	 */
	public boolean getFlowSensitiveAliasing() {
		return flowSensitiveAliasing;
	}

	/**
	 * Sets whether the solver shall track taints of thrown exception objects
	 * 
	 * @param enableExceptions True if taints associated with exceptions shall be
	 *                         tracked over try/catch construct, otherwise false
	 */
	public void setEnableExceptionTracking(boolean enableExceptions) {
		this.enableExceptions = enableExceptions;
	}

	/**
	 * Gets whether the solver shall track taints of thrown exception objects
	 * 
	 * @return True if the solver shall track taints of thrown exception objects,
	 *         otherwise false
	 */
	public boolean getEnableExceptionTracking() {
		return enableExceptions;
	}

	/**
	 * Sets whether the solver shall track tainted arrays and array elements
	 * 
	 * @param enableArrays True if taints associated with arrays or array elements
	 *                     shall be tracked over try/catch construct, otherwise
	 *                     false
	 */
	public void setEnableArrayTracking(boolean enableArrays) {
		this.enableArrays = enableArrays;
	}

	/**
	 * Gets whether the solver shall track taints of arrays and array elements
	 * 
	 * @return True if the solver shall track taints of arrays and array elements,
	 *         otherwise false
	 */
	public boolean getEnableArrayTracking() {
		return enableArrays;
	}

	public void setEnableArraySizeTainting(boolean arrayLengthTainting) {
		this.enableArraySizeTainting = arrayLengthTainting;
	}

	public boolean getEnableArraySizeTainting() {
		return this.enableArraySizeTainting;
	}

	/**
	 * Sets the callgraph algorithm to be used by the data flow tracker
	 * 
	 * @param algorithm The callgraph algorithm to be used by the data flow tracker
	 */
	public void setCallgraphAlgorithm(CallgraphAlgorithm algorithm) {
		this.callgraphAlgorithm = algorithm;
	}

	/**
	 * Gets the callgraph algorithm to be used by the data flow tracker
	 * 
	 * @return The callgraph algorithm to be used by the data flow tracker
	 */
	public CallgraphAlgorithm getCallgraphAlgorithm() {
		return callgraphAlgorithm;
	}

	/**
	 * Sets the aliasing algorithm to be used by the data flow tracker
	 * 
	 * @param algorithm The aliasing algorithm to be used by the data flow tracker
	 */
	public void setAliasingAlgorithm(AliasingAlgorithm algorithm) {
		this.aliasingAlgorithm = algorithm;
	}

	/**
	 * Gets the aliasing algorithm to be used by the data flow tracker
	 * 
	 * @return The aliasing algorithm to be used by the data flow tracker
	 */
	public AliasingAlgorithm getAliasingAlgorithm() {
		return aliasingAlgorithm;
	}

	/**
	 * Sets whether type checking shall be done on casts and method calls
	 * 
	 * @param enableTypeChecking True if type checking shall be performed, otherwise
	 *                           false
	 */
	public void setEnableTypeChecking(boolean enableTypeChecking) {
		this.enableTypeChecking = enableTypeChecking;
	}

	/**
	 * Gets whether type checking shall be done on casts and method calls
	 * 
	 * @return True if type checking shall be performed, otherwise false
	 */
	public boolean getEnableTypeChecking() {
		return enableTypeChecking;
	}

	/**
	 * Sets whether flows starting or ending in system packages such as Android's
	 * support library shall be ignored.
	 * 
	 * @param ignoreFlowsInSystemPackages True if flows starting or ending in system
	 *                                    packages shall be ignored, otherwise
	 *                                    false.
	 */
	public void setIgnoreFlowsInSystemPackages(boolean ignoreFlowsInSystemPackages) {
		this.ignoreFlowsInSystemPackages = ignoreFlowsInSystemPackages;
	}

	/**
	 * Gets whether flows starting or ending in system packages such as Android's
	 * support library shall be ignored.
	 * 
	 * @return True if flows starting or ending in system packages shall be ignored,
	 *         otherwise false.
	 */
	public boolean getIgnoreFlowsInSystemPackages() {
		return ignoreFlowsInSystemPackages;
	}

	/**
	 * Sets whether classes that are declared library classes in Soot shall be
	 * excluded from the data flow analysis, i.e., no flows shall be tracked through
	 * them
	 * 
	 * @param excludeSootLibraryClasses True to exclude Soot library classes from
	 *                                  the data flow analysis, otherwise false
	 */
	public void setExcludeSootLibraryClasses(boolean excludeSootLibraryClasses) {
		this.excludeSootLibraryClasses = excludeSootLibraryClasses;
	}

	/**
	 * Gets whether classes that are declared library classes in Soot shall be
	 * excluded from the data flow analysis, i.e., no flows shall be tracked through
	 * them
	 * 
	 * @return True to exclude Soot library classes from the data flow analysis,
	 *         otherwise false
	 */
	public boolean getExcludeSootLibraryClasses() {
		return this.excludeSootLibraryClasses;
	}

	/**
	 * Sets the maximum number of threads to be used by the solver. A value of -1
	 * indicates an unlimited number of threads, i.e., there will be as many threads
	 * as there are CPU cores on the machine.
	 * 
	 * @param threadNum The maximum number of threads to be used by the solver, or
	 *                  -1 for an unlimited number of threads.
	 */
	public void setMaxThreadNum(int threadNum) {
		this.maxThreadNum = threadNum;
	}

	/**
	 * Gets the maximum number of threads to be used by the solver. A value of -1
	 * indicates an unlimited number of threads, i.e., there will be as many threads
	 * as there are CPU cores on the machine.
	 * 
	 * @return The maximum number of threads to be used by the solver, or -1 for an
	 *         unlimited number of threads.
	 */
	public int getMaxThreadNum() {
		return this.maxThreadNum;
	}

	/**
	 * Gets whether FlowDroid shall write the Jimple files to disk after the data
	 * flow analysis
	 * 
	 * @return True if the Jimple files shall be written to disk after the data flow
	 *         analysis, otherwise false
	 */
	public boolean getWriteOutputFiles() {
		return this.writeOutputFiles;
	}

	/**
	 * Gets whether FlowDroid shall write the Jimple files to disk after the data
	 * flow analysis
	 * 
	 * @param writeOutputFiles True if the Jimple files shall be written to disk
	 *                         after the data flow analysis, otherwise false
	 */
	public void setWriteOutputFiles(boolean writeOutputFiles) {
		this.writeOutputFiles = writeOutputFiles;
	}

	/**
	 * Sets whether and how FlowDroid shall eliminate irrelevant code before running
	 * the taint propagation
	 * 
	 * @param Mode the mode of dead and irrelevant code eliminiation to be used
	 */
	public void setCodeEliminationMode(CodeEliminationMode mode) {
		this.codeEliminationMode = mode;
	}

	/**
	 * Gets whether and how FlowDroid shall eliminate irrelevant code before running
	 * the taint propagation
	 * 
	 * @return the mode of dead and irrelevant code elimination to be used
	 */
	public CodeEliminationMode getCodeEliminationMode() {
		return codeEliminationMode;
	}

	/**
	 * Gets whether the discovered sources and sinks shall be logged
	 * 
	 * @return True if the discovered sources and sinks shall be logged, otherwise
	 *         false
	 */
	public boolean getLogSourcesAndSinks() {
		return logSourcesAndSinks;
	}

	/**
	 * Sets whether the discovered sources and sinks shall be logged
	 * 
	 * @param logSourcesAndSinks True if the discovered sources and sinks shall be
	 *                           logged, otherwise false
	 */
	public void setLogSourcesAndSinks(boolean logSourcesAndSinks) {
		this.logSourcesAndSinks = logSourcesAndSinks;
	}

	/**
	 * Gets whether reflective method calls shall be supported
	 * 
	 * @return True if reflective method calls shall be supported, otherwise false
	 */
	public boolean getEnableReflection() {
		return this.enableReflection;
	}

	/**
	 * Sets whether reflective method calls shall be supported
	 * 
	 * @param enableReflections True if reflective method calls shall be supported,
	 *                          otherwise false
	 */
	public void setEnableReflection(boolean enableReflections) {
		this.enableReflection = enableReflections;
	}

	/**
	 * Gets whether line numbers associated with sources and sinks should be output
	 * in XML results
	 * 
	 * @return True if line number should be output, otherwise false
	 */
	public boolean getEnableLineNumbers() {
		return this.enableLineNumbers;
	}

	/**
	 * Sets whether line numbers associated with sources and sinks should be output
	 * in XML results
	 * 
	 * @param enableLineNumbers True if line numbers associated with sources and
	 *                          sinks should be output in XML results, otherwise
	 *                          false
	 */
	public void setEnableLineNumbers(boolean enableLineNumbers) {
		this.enableLineNumbers = enableLineNumbers;
	}

	/**
	 * Gets whether the usage of original variablenames (if available) is enabled
	 * 
	 * @return True if the usage is enabled, otherwise false
	 */
	public boolean getEnableOriginalNames() {
		return this.enableOriginalNames;
	}

	/**
	 * Sets whether the usage of original variablenames (if available) is enabled
	 * 
	 * @param enableOriginalNames True if the usage of original variablenames (if
	 *                            available) is enabled, otherwise false
	 */
	public void setEnableOriginalNames(boolean enableOriginalNames) {
		this.enableOriginalNames = enableOriginalNames;
	}

	/**
	 * Gets whether the taint analysis is enabled. If it is disabled, FlowDroid will
	 * initialize the Soot instance and then return immediately.
	 * 
	 * @return True if data flow tracking shall be performed, false otherwise
	 */
	public boolean isTaintAnalysisEnabled() {
		return taintAnalysisEnabled;
	}

	/**
	 * Sets whether the taint analysis is enabled. If it is disabled, FlowDroid will
	 * initialize the Soot instance and then return immediately.
	 * 
	 * @param taintAnalysisEnabled True if data flow tracking shall be performed,
	 *                             false otherwise
	 */
	public void setTaintAnalysisEnabled(boolean taintAnalysisEnabled) {
		this.taintAnalysisEnabled = taintAnalysisEnabled;
	}

	/**
	 * Gets whether the data flow results shall be reported incrementally instead of
	 * being only available after the full data flow analysis has been completed.
	 * 
	 * @return True if incremental data flow results shall be available, otherwise
	 *         false
	 */
	public boolean getIncrementalResultReporting() {
		return this.incrementalResultReporting;
	}

	/**
	 * Sets whether the data flow results shall be reported incrementally instead of
	 * being only available after the full data flow analysis has been completed.
	 * 
	 * @param incrementalReporting True if incremental data flow results shall be
	 *                             available, otherwise false
	 */
	public void setIncrementalResultReporting(boolean incrementalReporting) {
		this.incrementalResultReporting = incrementalReporting;
	}

	/**
	 * Gets the timeout in seconds after which the taint analysis shall be aborted.
	 * This timeout only applies to the taint analysis itself, not to the path
	 * reconstruction that happens afterwards.
	 * 
	 * @return The timeout in seconds after which the analysis shall be aborted
	 */
	public long getDataFlowTimeout() {
		return this.dataFlowTimeout;
	}

	/**
	 * Sets the timeout in seconds after which the analysis shall be aborted. This
	 * timeout only applies to the taint analysis itself, not to the path
	 * reconstruction that happens afterwards.
	 * 
	 * @param timeout The timeout in seconds after which the analysis shall be
	 *                aborted
	 */
	public void setDataFlowTimeout(long timeout) {
		this.dataFlowTimeout = timeout;
	}

	/**
	 * Gets the threshold at which the data flow analysis shall be terminated. If
	 * the JVM consumes more than this fraction of the heap, no more data flow edges
	 * are propagated, and the results obtained so far are returned.
	 * 
	 * @return The threshold at which to abort the workers
	 */
	public double getMemoryThreshold() {
		return memoryThreshold;
	}

	/**
	 * Sets the threshold at which the data flow analysis shall be terminated. If
	 * the JVM consumes more than this fraction of the heap, no more data flow edges
	 * are propagated, and the results obtained so far are returned.
	 * 
	 * @param memoryThreshold The threshold at which to abort the workers
	 */
	public void setMemoryThreshold(double memoryThreshold) {
		this.memoryThreshold = memoryThreshold;
	}

	/**
	 * Gets whether one source shall be analyzed at a time instead of all sources
	 * together
	 * 
	 * @return True if the analysis shall be run with one analysis at a time, false
	 *         if the analysis shall be run with all sources together
	 */
	public boolean getOneSourceAtATime() {
		return this.oneSourceAtATime;
	}

	/**
	 * Sets whether one source shall be analyzed at a time instead of all sources
	 * together
	 * 
	 * @param oneSourceAtATime True if the analysis shall be run with one analysis
	 *                         at a time, false if the analysis shall be run with
	 *                         all sources together
	 */
	public void setOneSourceAtATime(boolean oneSourceAtATime) {
		this.oneSourceAtATime = oneSourceAtATime;
	}

	/**
	 * Gets the configuration for dealing with the paths between source and sinks
	 * 
	 * @return The configuration for dealing with the paths between source and sinks
	 */
	public PathConfiguration getPathConfiguration() {
		return pathConfiguration;
	}

	/**
	 * Gets the configuration for writing the data flow results into files
	 * 
	 * @return The configuration for writing the data flow results into files
	 */
	public OutputConfiguration getOutputConfiguration() {
		return outputConfiguration;
	}

	/**
	 * Gets the configuration for the core IFDS data flow solver
	 * 
	 * @return The configuration for the core IFDS data flow solver
	 */
	public SolverConfiguration getSolverConfiguration() {
		return solverConfiguration;
	}

	/**
	 * Gets the access path configuration that defines, e.g. low long access paths
	 * may be before being truncated
	 * 
	 * @return The access path configuration
	 */
	public AccessPathConfiguration getAccessPathConfiguration() {
		return accessPathConfiguration;
	}

	/**
	 * Prints a summary of this data flow configuration
	 */
	public void printSummary() {
		if (staticFieldTrackingMode == StaticFieldTrackingMode.None)
			logger.warn("Static field tracking is disabled, results may be incomplete");
		if (!flowSensitiveAliasing)
			logger.warn("Using flow-insensitive alias tracking, results may be imprecise");
		switch (implicitFlowMode) {
		case AllImplicitFlows:
			logger.info("Implicit flow tracking is enabled");
			break;
		case ArrayAccesses:
			logger.info("Tracking of implicit array accesses is enabled");
			break;
		case NoImplicitFlows:
			logger.info("Implicit flow tracking is NOT enabled");
			break;
		}
		if (enableExceptions)
			logger.info("Exceptional flow tracking is enabled");
		else
			logger.info("Exceptional flow tracking is NOT enabled");
		logger.info("Running with a maximum access path length of {}", accessPathConfiguration.getAccessPathLength());
		if (pathAgnosticResults)
			logger.info("Using path-agnostic result collection");
		else
			logger.info("Using path-sensitive result collection");
		if (accessPathConfiguration.useRecursiveAccessPaths)
			logger.info("Recursive access path shortening is enabled");
		else
			logger.info("Recursive access path shortening is NOT enabled");
		logger.info("Taint analysis enabled: " + taintAnalysisEnabled);
		if (oneSourceAtATime)
			logger.info("Running with one source at a time");
		logger.info("Using alias algorithm " + aliasingAlgorithm);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessPathConfiguration == null) ? 0 : accessPathConfiguration.hashCode());
		result = prime * result + ((aliasingAlgorithm == null) ? 0 : aliasingAlgorithm.hashCode());
		result = prime * result + ((callgraphAlgorithm == null) ? 0 : callgraphAlgorithm.hashCode());
		result = prime * result + ((codeEliminationMode == null) ? 0 : codeEliminationMode.hashCode());
		result = prime * result + (int) (dataFlowTimeout ^ (dataFlowTimeout >>> 32));
		result = prime * result + (enableArraySizeTainting ? 1231 : 1237);
		result = prime * result + (enableArrays ? 1231 : 1237);
		result = prime * result + (enableExceptions ? 1231 : 1237);
		result = prime * result + (enableReflection ? 1231 : 1237);
		result = prime * result + (enableLineNumbers ? 1231 : 1237);
		result = prime * result + (enableOriginalNames ? 1231 : 1237);
		result = prime * result + (enableTypeChecking ? 1231 : 1237);
		result = prime * result + (excludeSootLibraryClasses ? 1231 : 1237);
		result = prime * result + (flowSensitiveAliasing ? 1231 : 1237);
		result = prime * result + (ignoreFlowsInSystemPackages ? 1231 : 1237);
		result = prime * result + ((implicitFlowMode == null) ? 0 : implicitFlowMode.hashCode());
		result = prime * result + (incrementalResultReporting ? 1231 : 1237);
		result = prime * result + (inspectSinks ? 1231 : 1237);
		result = prime * result + (inspectSources ? 1231 : 1237);
		result = prime * result + (logSourcesAndSinks ? 1231 : 1237);
		result = prime * result + maxThreadNum;
		long temp;
		temp = Double.doubleToLongBits(memoryThreshold);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (oneSourceAtATime ? 1231 : 1237);
		result = prime * result + ((outputConfiguration == null) ? 0 : outputConfiguration.hashCode());
		result = prime * result + ((pathConfiguration == null) ? 0 : pathConfiguration.hashCode());
		result = prime * result + ((solverConfiguration == null) ? 0 : solverConfiguration.hashCode());
		result = prime * result + ((staticFieldTrackingMode == null) ? 0 : staticFieldTrackingMode.hashCode());
		result = prime * result + stopAfterFirstKFlows;
		result = prime * result + (taintAnalysisEnabled ? 1231 : 1237);
		result = prime * result + (writeOutputFiles ? 1231 : 1237);
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
		InfoflowConfiguration other = (InfoflowConfiguration) obj;
		if (accessPathConfiguration == null) {
			if (other.accessPathConfiguration != null)
				return false;
		} else if (!accessPathConfiguration.equals(other.accessPathConfiguration))
			return false;
		if (aliasingAlgorithm != other.aliasingAlgorithm)
			return false;
		if (callgraphAlgorithm != other.callgraphAlgorithm)
			return false;
		if (codeEliminationMode != other.codeEliminationMode)
			return false;
		if (dataFlowTimeout != other.dataFlowTimeout)
			return false;
		if (enableArraySizeTainting != other.enableArraySizeTainting)
			return false;
		if (enableArrays != other.enableArrays)
			return false;
		if (enableExceptions != other.enableExceptions)
			return false;
		if (enableReflection != other.enableReflection)
			return false;
		if (enableLineNumbers != other.enableLineNumbers)
			return false;
		if (enableOriginalNames != other.enableOriginalNames)
			return false;
		if (enableTypeChecking != other.enableTypeChecking)
			return false;
		if (excludeSootLibraryClasses != other.excludeSootLibraryClasses)
			return false;
		if (flowSensitiveAliasing != other.flowSensitiveAliasing)
			return false;
		if (ignoreFlowsInSystemPackages != other.ignoreFlowsInSystemPackages)
			return false;
		if (implicitFlowMode != other.implicitFlowMode)
			return false;
		if (incrementalResultReporting != other.incrementalResultReporting)
			return false;
		if (inspectSinks != other.inspectSinks)
			return false;
		if (inspectSources != other.inspectSources)
			return false;
		if (logSourcesAndSinks != other.logSourcesAndSinks)
			return false;
		if (maxThreadNum != other.maxThreadNum)
			return false;
		if (Double.doubleToLongBits(memoryThreshold) != Double.doubleToLongBits(other.memoryThreshold))
			return false;
		if (oneSourceAtATime != other.oneSourceAtATime)
			return false;
		if (outputConfiguration == null) {
			if (other.outputConfiguration != null)
				return false;
		} else if (!outputConfiguration.equals(other.outputConfiguration))
			return false;
		if (pathConfiguration == null) {
			if (other.pathConfiguration != null)
				return false;
		} else if (!pathConfiguration.equals(other.pathConfiguration))
			return false;
		if (solverConfiguration == null) {
			if (other.solverConfiguration != null)
				return false;
		} else if (!solverConfiguration.equals(other.solverConfiguration))
			return false;
		if (staticFieldTrackingMode != other.staticFieldTrackingMode)
			return false;
		if (stopAfterFirstKFlows != other.stopAfterFirstKFlows)
			return false;
		if (taintAnalysisEnabled != other.taintAnalysisEnabled)
			return false;
		if (writeOutputFiles != other.writeOutputFiles)
			return false;
		return true;
	}

}
