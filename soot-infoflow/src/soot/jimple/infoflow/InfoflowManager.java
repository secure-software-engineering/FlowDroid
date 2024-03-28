package soot.jimple.infoflow;

import soot.FastHierarchy;
import soot.Scene;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.AccessPathFactory;
import soot.jimple.infoflow.globalTaints.GlobalTaintManager;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.river.IUsageContextProvider;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.typing.TypeUtils;
import soot.jimple.toolkits.callgraph.VirtualEdgesSummaries;

/**
 * Manager class for passing internal data flow objects to interface
 * implementors
 * 
 * @author Steven Arzt
 *
 */
public class InfoflowManager {

	private final InfoflowConfiguration config;
	private IInfoflowSolver mainSolver;
	private IInfoflowSolver aliasSolver;
	private final IInfoflowCFG icfg;
	private final IInfoflowCFG originalIcfg;
	private final ISourceSinkManager sourceSinkManager;
	private final ITaintPropagationWrapper taintWrapper;
	private final TypeUtils typeUtils;
	private final FastHierarchy hierarchy;
	private final AccessPathFactory accessPathFactory;
	private final GlobalTaintManager globalTaintManager;
	private final VirtualEdgesSummaries virtualEdgeSummaries = new VirtualEdgesSummaries();
	private Aliasing aliasing;
	// The infoflow manager for the on-demand analysis that computes additional flows
	public InfoflowManager additionalManager;

	private IUsageContextProvider usageContextProvider;

	public InfoflowManager(InfoflowConfiguration config) {
		this.config = config;
		this.mainSolver = null;
		this.icfg = null;
		this.originalIcfg = null;
		this.sourceSinkManager = null;
		this.taintWrapper = null;
		this.typeUtils = null;
		this.hierarchy = null;
		this.accessPathFactory = null;
		this.globalTaintManager = null;
		this.additionalManager = null;
		this.usageContextProvider = null;
	}

	protected InfoflowManager(InfoflowConfiguration config, IInfoflowSolver mainSolver, IInfoflowCFG icfg,
			ISourceSinkManager sourceSinkManager, ITaintPropagationWrapper taintWrapper, FastHierarchy hierarchy,
			GlobalTaintManager globalTaintManager) {
		this.config = config;
		this.mainSolver = mainSolver;
		this.icfg = icfg;
		this.originalIcfg = null;
		this.sourceSinkManager = sourceSinkManager;
		this.taintWrapper = taintWrapper;
		this.typeUtils = new TypeUtils(this);
		this.hierarchy = hierarchy;
		this.accessPathFactory = new AccessPathFactory(config, typeUtils);
		this.globalTaintManager = globalTaintManager;
		this.usageContextProvider = null;
	}

	protected InfoflowManager(InfoflowConfiguration config, IInfoflowSolver mainSolver, IInfoflowCFG icfg,
			ISourceSinkManager sourceSinkManager, ITaintPropagationWrapper taintWrapper, FastHierarchy hierarchy,
			InfoflowManager existingManager) {
		this.config = config;
		this.mainSolver = mainSolver;
		this.icfg = icfg;
		this.originalIcfg = existingManager.getICFG();
		this.sourceSinkManager = sourceSinkManager;
		this.taintWrapper = taintWrapper;
		this.typeUtils = existingManager.getTypeUtils();
		this.hierarchy = hierarchy;
		this.accessPathFactory = existingManager.getAccessPathFactory();
		this.globalTaintManager = existingManager.getGlobalTaintManager();
		this.usageContextProvider = null;
	}

	public InfoflowManager(InfoflowConfiguration config, IInfoflowSolver mainSolver, IInfoflowCFG icfg) {
		this.config = config;
		this.mainSolver = mainSolver;
		this.icfg = icfg;
		this.originalIcfg = null;
		this.sourceSinkManager = null;
		this.taintWrapper = null;
		this.typeUtils = new TypeUtils(this);
		this.hierarchy = Scene.v().getOrMakeFastHierarchy();
		this.accessPathFactory = new AccessPathFactory(config, typeUtils);
		this.globalTaintManager = null;
		this.usageContextProvider = null;
	}

	/**
	 * Gets the configuration for this data flow analysis
	 * 
	 * @return The configuration for this data flow analysis
	 */
	public InfoflowConfiguration getConfig() {
		return this.config;
	}

	/**
	 * Sets the IFDS solver that propagates edges in the main direction
	 * 
	 * @param solver The IFDS solver that propagates edges in the main direction
	 */
	public void setMainSolver(IInfoflowSolver solver) {
		this.mainSolver = solver;
	}

	/**
	 * Gets the IFDS solver that propagates edges forward
	 * 
	 * @return The IFDS solver that propagates edges forward
	 */
	public IInfoflowSolver getMainSolver() {
		return this.mainSolver;
	}

	/**
	 * Gets the IFDS solver that propagates alias edges
	 *
	 * @return The IFDS solver that propagates alias edges
	 */
	public IInfoflowSolver getAliasSolver() {
		return this.aliasSolver;
	}

	/**
	 * Sets the IFDS solver that propagates edges forward
	 *
	 * @param solver The IFDS solver that propagates edges forward
	 */
	public void setAliasSolver(IInfoflowSolver solver) {
		this.aliasSolver = solver;
	}

	/**
	 * Gets the interprocedural control flow graph
	 * 
	 * @return The interprocedural control flow graph
	 */
	public IInfoflowCFG getICFG() {
		return this.icfg;
	}

	/**
	 * Gets the interprocedural control flow graph for the other direction. Only
	 * available in the alias search.
	 *
	 * @return The inversed interprocedural control flow graph
	 */
	public IInfoflowCFG getOriginalICFG() {
		return this.originalIcfg;
	}

	/**
	 * Gets the SourceSinkManager implementation
	 * 
	 * @return The SourceSinkManager implementation
	 */
	public ISourceSinkManager getSourceSinkManager() {
		return this.sourceSinkManager;
	}

	/**
	 * Gets the taint wrapper to be used for handling library calls
	 * 
	 * @return The taint wrapper to be used for handling library calls
	 */
	public ITaintPropagationWrapper getTaintWrapper() {
		return this.taintWrapper;
	}

	/**
	 * Gets the utility class for type checks
	 * 
	 * @return The utility class for type checks
	 */
	public TypeUtils getTypeUtils() {
		return this.typeUtils;
	}

	/**
	 * Gets the Soot type hierarchy that was constructed together with the
	 * callgraph. In contrast to Scene.v().getFastHierarchy, this object is
	 * guaranteed to be available.
	 * 
	 * @return The fast hierarchy
	 */
	public FastHierarchy getHierarchy() {
		return hierarchy;
	}

	/**
	 * Gets the factory object for creating new access paths
	 * 
	 * @return The factory object for creating new access paths
	 */
	public AccessPathFactory getAccessPathFactory() {
		return this.accessPathFactory;
	}

	/**
	 * Checks whether the analysis has been aborted
	 * 
	 * @return True if the analysis has been aborted, otherwise false
	 */
	public boolean isAnalysisAborted() {
		if (mainSolver instanceof IMemoryBoundedSolver)
			return ((IMemoryBoundedSolver) mainSolver).isKilled();
		return false;
	}

	/**
	 * Releases all resources that are no longer required after the main step of the
	 * data flow analysis
	 */
	public void cleanup() {
		mainSolver = null;
		aliasing = null;
	}

	public void setAliasing(Aliasing aliasing) {
		this.aliasing = aliasing;
	}

	public Aliasing getAliasing() {
		return aliasing;
	}

	/**
	 * Gets the manager object for handling global taints outside of the IFDS solver
	 * 
	 * @return The manager object for handling global taints outside of the IFDS
	 *         solver
	 */
	public GlobalTaintManager getGlobalTaintManager() {
		return globalTaintManager;
	}

	/**
	 * Set the additional flow manager
	 *
	 * @param usageContextProvider The manager object for usage contexts
	 */
	public void setUsageContextProvider(IUsageContextProvider usageContextProvider) {
		this.usageContextProvider = usageContextProvider;
	}

	/**
	 * Get the additional information flow manager
	 *
	 * @return The manager object for usage contexts
	 */
	public IUsageContextProvider getUsageContextProvider() {
		return this.usageContextProvider;
	}

	/**
	 * Returns the virtual edge summaries
	 * @return the virtual edge summaries
	 */
	public VirtualEdgesSummaries getVirtualEdgeSummaries() {
		return virtualEdgeSummaries;
	}

}
