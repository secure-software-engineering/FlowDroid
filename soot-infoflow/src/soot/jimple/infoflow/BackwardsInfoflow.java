package soot.jimple.infoflow;

import java.io.File;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration.DataFlowDirection;
import soot.jimple.infoflow.aliasing.IAliasingStrategy;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.codeOptimization.AddNopStmt;
import soot.jimple.infoflow.codeOptimization.ICodeOptimizer;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.globalTaints.GlobalTaintManager;
import soot.jimple.infoflow.nativeCallHandler.BackwardNativeCallHandler;
import soot.jimple.infoflow.problems.BackwardsInfoflowProblem;
import soot.jimple.infoflow.problems.rules.BackwardPropagationRuleManagerFactory;
import soot.jimple.infoflow.results.BackwardsInfoflowResults;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.memory.IMemoryManager;
import soot.jimple.infoflow.sourcesSinks.manager.IReversibleSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;

public class BackwardsInfoflow extends AbstractInfoflow {

	/**
	 * Creates a new instance of the InfoFlow class for analyzing plain Java code
	 * without any references to APKs or the Android SDK.
	 */
	public BackwardsInfoflow() {
		super();
		config.setDataFlowDirection(DataFlowDirection.Backwards);
		setNativeCallHandler(new BackwardNativeCallHandler());
	}

	/**
	 * Creates a new instance of the Infoflow class for analyzing Android APK files.
	 *
	 * @param androidPath     If forceAndroidJar is false, this is the base
	 *                        directory of the platform files in the Android SDK. If
	 *                        forceAndroidJar is true, this is the full path of a
	 *                        single android.jar file.
	 * @param forceAndroidJar True if a single platform JAR file shall be forced,
	 *                        false if Soot shall pick the appropriate platform
	 *                        version
	 */
	public BackwardsInfoflow(File androidPath, boolean forceAndroidJar) {
		super(null, androidPath, forceAndroidJar);
		config.setDataFlowDirection(DataFlowDirection.Backwards);
		setNativeCallHandler(new BackwardNativeCallHandler());
	}

	/**
	 * Creates a new instance of the Infoflow class for analyzing Android APK files.
	 *
	 * @param androidPath     If forceAndroidJar is false, this is the base
	 *                        directory of the platform files in the Android SDK. If
	 *                        forceAndroidJar is true, this is the full path of a
	 *                        single android.jar file.
	 * @param forceAndroidJar True if a single platform JAR file shall be forced,
	 *                        false if Soot shall pick the appropriate platform
	 *                        version
	 * @param icfgFactory     The interprocedural CFG to be used by the
	 *                        InfoFlowProblem
	 */
	public BackwardsInfoflow(File androidPath, boolean forceAndroidJar, BiDirICFGFactory icfgFactory) {
		super(icfgFactory, androidPath, forceAndroidJar);
		config.setDataFlowDirection(DataFlowDirection.Backwards);
		setNativeCallHandler(new BackwardNativeCallHandler());
	}

	@Override
	protected IAliasingStrategy createAliasAnalysis(final ISourceSinkManager sourcesSinks, IInfoflowCFG iCfg,
			InterruptableExecutor executor, IMemoryManager<Abstraction, Unit> memoryManager) {
		return createBackwardAliasAnalysis(manager, sourcesSinks, iCfg, executor, memoryManager);
	}

	@Override
	protected InfoflowManager initializeInfoflowManager(final ISourceSinkManager sourcesSinks, IInfoflowCFG iCfg,
			GlobalTaintManager globalTaintManager) {
		return new InfoflowManager(config, null, new BackwardsInfoflowCFG(iCfg), sourcesSinks, taintWrapper, hierarchy,
				globalTaintManager);
	}

	@Override
	protected BackwardsInfoflowProblem createInfoflowProblem(Abstraction zeroValue) {
		return new BackwardsInfoflowProblem(manager, zeroValue, ruleManagerFactory);
	}

	@Override
	protected SourceOrSink scanStmtForSourcesSinks(final ISourceSinkManager sourcesSinks, Stmt s) {
		IReversibleSourceSinkManager ssm = (IReversibleSourceSinkManager) sourcesSinks;

		SourceInfo sinkInfo = ssm.getInverseSinkInfo(s, manager);
		SinkInfo sourceInfo = ssm.getInverseSourceInfo(s, manager, null);
		return new SourceOrSink(sinkInfo, sourceInfo);
	}

	@Override
	protected InfoflowResults createResultsObject() {
		return new BackwardsInfoflowResults();
	}

	@Override
	protected BackwardPropagationRuleManagerFactory initializeRuleManagerFactory() {
		return new BackwardPropagationRuleManagerFactory();
	}

	@Override
	protected void performCodeInstrumentationAfterDCE(InfoflowManager dceManager, Set<SootMethod> excludedMethods) {
		super.performCodeInstrumentationAfterDCE(dceManager, excludedMethods);

		// Fixes an edge case, see AddNopStmt
		ICodeOptimizer nopStmt = new AddNopStmt();
		nopStmt.initialize(config);
		nopStmt.run(dceManager, excludedMethods, null, null);
	}

}
