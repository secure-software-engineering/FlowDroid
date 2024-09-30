/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow;

import java.io.File;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration.SolverConfiguration;
import soot.jimple.infoflow.aliasing.FlowSensitiveAliasStrategy;
import soot.jimple.infoflow.aliasing.IAliasingStrategy;
import soot.jimple.infoflow.aliasing.LazyAliasingStrategy;
import soot.jimple.infoflow.aliasing.NullAliasStrategy;
import soot.jimple.infoflow.aliasing.PtsBasedAliasStrategy;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.globalTaints.GlobalTaintManager;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.nativeCallHandler.DefaultNativeCallHandler;
import soot.jimple.infoflow.problems.AliasProblem;
import soot.jimple.infoflow.problems.InfoflowProblem;
import soot.jimple.infoflow.problems.rules.DefaultPropagationRuleManagerFactory;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.river.BackwardNoSinkRuleManagerFactory;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.memory.IMemoryManager;
import soot.jimple.infoflow.sourcesSinks.manager.EmptySourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;

/**
 * main infoflow class which triggers the analysis and offers method to
 * customize it.
 *
 */
public class Infoflow extends AbstractInfoflow {

	/**
	 * Creates a new instance of the InfoFlow class for analyzing plain Java code
	 * without any references to APKs or the Android SDK.
	 */
	public Infoflow() {
		super();
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
	public Infoflow(File androidPath, boolean forceAndroidJar) {
		super(null, androidPath, forceAndroidJar);
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
	public Infoflow(File androidPath, boolean forceAndroidJar, BiDirICFGFactory icfgFactory) {
		super(icfgFactory, androidPath, forceAndroidJar);
		setNativeCallHandler(new DefaultNativeCallHandler());
	}

	@Override
	protected InfoflowManager initializeInfoflowManager(final ISourceSinkManager sourcesSinks, IInfoflowCFG iCfg,
			GlobalTaintManager globalTaintManager) {
		return new InfoflowManager(config, null, iCfg, sourcesSinks, taintWrapper, hierarchy, globalTaintManager);
	}

	@Override
	protected IAliasingStrategy createAliasAnalysis(final ISourceSinkManager sourcesSinks, IInfoflowCFG iCfg,
			InterruptableExecutor executor, IMemoryManager<Abstraction, Unit> memoryManager) {
		IAliasingStrategy aliasingStrategy;
		IInfoflowSolver backSolver = null;
		AliasProblem backProblem = null;
		InfoflowManager aliasManager = null;
		switch (getConfig().getAliasingAlgorithm()) {
		case FlowSensitive:
			aliasManager = new InfoflowManager(config, backSolver, new BackwardsInfoflowCFG(iCfg), sourcesSinks,
					taintWrapper, hierarchy, manager);
			backProblem = new AliasProblem(aliasManager);
			// We need to create the right data flow solver
			SolverConfiguration solverConfig = config.getSolverConfiguration();
			backSolver = createDataFlowSolver(executor, backProblem, solverConfig);
			aliasManager.setMainSolver(backSolver);
			backSolver.setMemoryManager(memoryManager);
			backSolver.setPredecessorShorteningMode(
					pathConfigToShorteningMode(manager.getConfig().getPathConfiguration()));
			// backSolver.setEnableMergePointChecking(true);
			backSolver.setMaxJoinPointAbstractions(solverConfig.getMaxJoinPointAbstractions());
			backSolver.setMaxCalleesPerCallSite(solverConfig.getMaxCalleesPerCallSite());
			backSolver.setMaxAbstractionPathLength(solverConfig.getMaxAbstractionPathLength());
			backSolver.setSolverId(false);
			backProblem.setTaintPropagationHandler(aliasPropagationHandler);
			backProblem.setTaintWrapper(taintWrapper);
			if (nativeCallHandler != null)
				backProblem.setNativeCallHandler(nativeCallHandler);

			memoryWatcher.addSolver((IMemoryBoundedSolver) backSolver);

			aliasingStrategy = new FlowSensitiveAliasStrategy(manager, backSolver);
			break;
		case PtsBased:
			backProblem = null;
			backSolver = null;
			aliasingStrategy = new PtsBasedAliasStrategy(manager);
			break;
		case None:
			backProblem = null;
			backSolver = null;
			aliasingStrategy = new NullAliasStrategy();
			break;
		case Lazy:
			backProblem = null;
			backSolver = null;
			aliasingStrategy = new LazyAliasingStrategy(manager);
			break;
		default:
			throw new RuntimeException("Unsupported aliasing algorithm");
		}
		return aliasingStrategy;
	}

	@Override
	protected InfoflowProblem createInfoflowProblem(Abstraction zeroValue) {
		return new InfoflowProblem(manager, zeroValue, ruleManagerFactory);
	}

	@Override
	protected SourceOrSink scanStmtForSourcesSinks(final ISourceSinkManager sourcesSinks, Stmt s) {
		SourceInfo sourceInfo = sourcesSinks.getSourceInfo(s, manager);
		SinkInfo sinkInfo = sourcesSinks.getSinkInfo(s, manager, null);
		return new SourceOrSink(sourceInfo, sinkInfo);
	}

	@Override
	protected InfoflowResults createResultsObject() {
		return new InfoflowResults(config.getPathAgnosticResults());
	}

	@Override
	protected IPropagationRuleManagerFactory initializeRuleManagerFactory() {
		return new DefaultPropagationRuleManagerFactory();
	}

	@Override
	protected BackwardNoSinkRuleManagerFactory initializeReverseRuleManagerFactory() {
		return new BackwardNoSinkRuleManagerFactory();
	}

	@Override
	protected InfoflowManager initializeReverseInfoflowManager(IInfoflowCFG iCfg,
			GlobalTaintManager globalTaintManager) {
		return new InfoflowManager(config, null, new BackwardsInfoflowCFG(iCfg), new EmptySourceSinkManager(),
				taintWrapper, hierarchy, globalTaintManager);
	}
}
