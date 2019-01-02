package soot.jimple.infoflow;

import soot.FastHierarchy;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.AccessPathFactory;
import soot.jimple.infoflow.globalTaints.GlobalTaintManager;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.TypeUtils;

/**
 * Manager class for passing internal data flow objects to interface
 * implementors
 * 
 * @author Steven Arzt
 *
 */
public class InfoflowManager {

	private final InfoflowConfiguration config;
	private IInfoflowSolver forwardSolver;
	private final IInfoflowCFG icfg;
	private final ISourceSinkManager sourceSinkManager;
	private final ITaintPropagationWrapper taintWrapper;
	private final TypeUtils typeUtils;
	private final FastHierarchy hierarchy;
	private final AccessPathFactory accessPathFactory;
	private final GlobalTaintManager globalTaintManager;
	private Aliasing aliasing;

	protected InfoflowManager(InfoflowConfiguration config, IInfoflowSolver forwardSolver, IInfoflowCFG icfg,
			ISourceSinkManager sourceSinkManager, ITaintPropagationWrapper taintWrapper, FastHierarchy hierarchy,
			AccessPathFactory accessPathFactory, GlobalTaintManager globalTaintManager) {
		this.config = config;
		this.forwardSolver = forwardSolver;
		this.icfg = icfg;
		this.sourceSinkManager = sourceSinkManager;
		this.taintWrapper = taintWrapper;
		this.typeUtils = new TypeUtils(this);
		this.hierarchy = hierarchy;
		this.accessPathFactory = accessPathFactory;
		this.globalTaintManager = globalTaintManager;
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
	 * Sets the IFDS solver that propagates edges forward
	 * 
	 * @param solver The IFDS solver that propagates edges forward
	 */
	public void setForwardSolver(IInfoflowSolver solver) {
		this.forwardSolver = solver;
	}

	/**
	 * Gets the IFDS solver that propagates edges forward
	 * 
	 * @return The IFDS solver that propagates edges forward
	 */
	public IInfoflowSolver getForwardSolver() {
		return this.forwardSolver;
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
		if (forwardSolver instanceof IMemoryBoundedSolver)
			return ((IMemoryBoundedSolver) forwardSolver).isKilled();
		return false;
	}

	/**
	 * Releases all resources that are no longer required after the main step of the
	 * data flow analysis
	 */
	public void cleanup() {
		forwardSolver = null;
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

}
