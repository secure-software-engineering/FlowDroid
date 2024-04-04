package soot.jimple.infoflow.methodSummary.generator;

import java.util.Collection;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.options.Options;

/**
 * Specialized {@link Infoflow} class for summary generation
 * 
 * @author Steven Arzt
 *
 */
public class SummaryInfoflow extends Infoflow implements ISummaryInfoflow {

	private InfoflowManager cachedManager = null;
	private String libPath;

	public SummaryInfoflow() {
		super();
	}

	/**
	 * Gets the data flow manager. Beware: This is an internal component of the data
	 * flow analysis. Depending on the time of access, it might not be in an
	 * expected state.
	 * 
	 * @return The intenal data flow manager
	 */
	public InfoflowManager getManager() {
		return this.manager == null ? cachedManager : this.manager;
	}

	@Override
	protected void onTaintPropagationCompleted(IInfoflowSolver forwardSolver, IInfoflowSolver aliasSolver,
											   IInfoflowSolver backwardSolver, IInfoflowSolver backwardAliasSolver) {
		cachedManager = this.manager;
	}

	@Override
	protected void initializeSoot(String appPath, String libPath, Collection<String> classes) {
		this.libPath = libPath;
		super.initializeSoot(appPath, libPath, classes);
	}

	@Override
	protected void initializeSoot(String appPath, String libPath, Collection<String> classes, String extraSeed) {
		this.libPath = libPath;
		super.initializeSoot(appPath, libPath, classes, extraSeed);
	}

	@Override
	protected void setSourcePrec() {
		if (libPath != null && libPath.toLowerCase().endsWith(".apk"))
			Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
		else
			super.setSourcePrec();
	}

}
