package soot.jimple.infoflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration.*;
import soot.jimple.infoflow.aliasing.*;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.codeOptimization.AddNopStmt;
import soot.jimple.infoflow.codeOptimization.DeadCodeEliminator;
import soot.jimple.infoflow.codeOptimization.ICodeOptimizer;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.AccessPathFactory;
import soot.jimple.infoflow.data.FlowDroidMemoryManager;
import soot.jimple.infoflow.data.pathBuilders.BatchPathBuilder;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory;
import soot.jimple.infoflow.data.pathBuilders.IAbstractionPathBuilder;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.globalTaints.GlobalTaintManager;
import soot.jimple.infoflow.handlers.PostAnalysisHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler2;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.memory.FlowDroidMemoryWatcher;
import soot.jimple.infoflow.memory.FlowDroidTimeoutWatcher;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.memory.ISolverTerminationReason;
import soot.jimple.infoflow.memory.reasons.OutOfMemoryReason;
import soot.jimple.infoflow.memory.reasons.TimeoutReason;
import soot.jimple.infoflow.nativeCallHandler.BackwardNativeCallHandler;
import soot.jimple.infoflow.problems.*;
import soot.jimple.infoflow.problems.rules.BackwardPropagationRuleManagerFactory;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.results.*;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.PredecessorShorteningMode;
import soot.jimple.infoflow.solver.SolverPeerGroup;
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.gcSolver.GCSolverPeerGroup;
import soot.jimple.infoflow.solver.memory.DefaultMemoryManagerFactory;
import soot.jimple.infoflow.solver.memory.IMemoryManager;
import soot.jimple.infoflow.solver.memory.IMemoryManagerFactory;
import soot.jimple.infoflow.sourcesSinks.manager.IOneSourceAtATimeManager;
import soot.jimple.infoflow.sourcesSinks.manager.IReversibleSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.threading.DefaultExecutorFactory;
import soot.jimple.infoflow.threading.IExecutorFactory;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.options.Options;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class BackwardsInfoflow extends AbstractInfoflow {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected InfoflowResults results;
    protected InfoflowManager manager;

    protected Set<ResultsAvailableHandler> onResultsAvailable = new HashSet<>();
    protected TaintPropagationHandler taintPropagationHandler = null;
    protected TaintPropagationHandler backwardsPropagationHandler = null;

    protected IMemoryManagerFactory memoryManagerFactory = new DefaultMemoryManagerFactory();
    protected IExecutorFactory executorFactory = new DefaultExecutorFactory();
    protected IPropagationRuleManagerFactory ruleManagerFactory = new BackwardPropagationRuleManagerFactory();


    protected FlowDroidMemoryWatcher memoryWatcher;

    protected Set<Stmt> collectedSources;
    protected Set<Stmt> collectedSinks;

    protected SootMethod dummyMainMethod;
    protected Collection<SootMethod> additionalEntryPointMethods;

    private boolean throwExceptions;

    protected SolverPeerGroup solverPeerGroup;

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
    public BackwardsInfoflow(String androidPath, boolean forceAndroidJar) {
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
    public BackwardsInfoflow(String androidPath, boolean forceAndroidJar, BiDirICFGFactory icfgFactory) {
        super(icfgFactory, androidPath, forceAndroidJar);
        config.setDataFlowDirection(DataFlowDirection.Backwards);
        setNativeCallHandler(new BackwardNativeCallHandler());
    }

    /**
     * Computes the information flow on a list of entry point methods. This list is
     * used to construct an artificial main method following the Android life cycle
     * for all methods that are detected to be part of Android's application
     * infrastructure (e.g. android.app.Activity.onCreate)
     *
     * @param appPath           The path containing the client program's files
     * @param libPath           the path to the main folder of the (unpacked)
     *                          library class files
     * @param entryPointCreator the entry point creator to use for generating the
     *                          dummy main method
     * @param sourcesSinks      manager class for identifying sources and sinks in
     */
    @Override
    public void computeInfoflow(String appPath, String libPath, IEntryPointCreator entryPointCreator,
                                ISourceSinkManager sourcesSinks) {
        if (sourcesSinks == null) {
            logger.error("Sources are empty!");
            return;
        }

        if (config.getSootIntegrationMode() != InfoflowConfiguration.SootIntegrationMode.UseExistingInstance)
            initializeSoot(appPath, libPath, entryPointCreator.getRequiredClasses());

        // entryPoints are the entryPoints required by Soot to calculate Graph -
        // if there is no main method, we have to create a new main method and
        // use it as entryPoint and store our real entryPoints
        this.dummyMainMethod = entryPointCreator.createDummyMain();
        this.additionalEntryPointMethods = entryPointCreator.getAdditionalMethods();
        Scene.v().setEntryPoints(Collections.singletonList(dummyMainMethod));

        // Run the analysis
        runAnalysis(sourcesSinks, null);
    }

    /**
     * Computes the information flow on a single method. This method is directly
     * taken as the entry point into the program, even if it is an instance method.
     *
     * @param appPath      The path containing the client program's files
     * @param libPath      the path to the main folder of the (unpacked) library
     *                     class files
     * @param entryPoint   the main method to analyze
     * @param sourcesSinks manager class for identifying sources and sinks in the
     */
    @Override
    public void computeInfoflow(String appPath, String libPath, String entryPoint, ISourceSinkManager sourcesSinks) {
        if (sourcesSinks == null) {
            logger.error("Sources are empty!");
            return;
        }

        initializeSoot(appPath, libPath, SootMethodRepresentationParser.v()
                .parseClassNames(Collections.singletonList(entryPoint), false).keySet(), entryPoint);

        if (!Scene.v().containsMethod(entryPoint)) {
            logger.error("Entry point not found: " + entryPoint);
            return;
        }
        SootMethod ep = Scene.v().getMethod(entryPoint);
        if (ep.isConcrete())
            ep.retrieveActiveBody();
        else {
            logger.debug("Skipping non-concrete method " + ep);
            return;
        }
        this.dummyMainMethod = null;
        Scene.v().setEntryPoints(Collections.singletonList(ep));
        Options.v().set_main_class(ep.getDeclaringClass().getName());

        // Compute the additional seeds if they are specified
        Set<String> seeds = Collections.emptySet();
        if (entryPoint != null && !entryPoint.isEmpty())
            seeds = Collections.singleton(entryPoint);
        ipcManager.updateJimpleForICC();

        // Run the analysis
        runAnalysis(sourcesSinks, seeds);
    }

    /**
     * Releases the callgraph and all intermediate objects associated with it
     */
    private void releaseCallgraph() {
        Scene.v().releaseCallGraph();
        Scene.v().releasePointsToAnalysis();
        Scene.v().releaseReachableMethods();
        G.v().resetSpark();
    }

    /**
     * Creates the memory manager that helps reduce the memory consumption of the
     * data flow analysis
     *
     * @return The memory manager object
     */
    private IMemoryManager<Abstraction, Unit> createMemoryManager() {
        if (memoryManagerFactory == null)
            return null;

        FlowDroidMemoryManager.PathDataErasureMode erasureMode;
        if (config.getPathConfiguration().mustKeepStatements())
            erasureMode = FlowDroidMemoryManager.PathDataErasureMode.EraseNothing;
        else if (pathBuilderFactory.supportsPathReconstruction())
            erasureMode = FlowDroidMemoryManager.PathDataErasureMode.EraseNothing;
        else if (pathBuilderFactory.isContextSensitive())
            erasureMode = FlowDroidMemoryManager.PathDataErasureMode.KeepOnlyContextData;
        else
            erasureMode = FlowDroidMemoryManager.PathDataErasureMode.EraseAll;
        IMemoryManager<Abstraction, Unit> memoryManager = memoryManagerFactory.getMemoryManager(false, erasureMode);
        return memoryManager;
    }

    /**
     * Creates the IFDS solver for the forward data flow problem
     *
     * @param executor       The executor in which to run the tasks or propagating
     *                       IFDS edges
     * @param problem The implementation of the forward problem
     * @return The solver that solves the forward taint analysis problem
     */
    private IInfoflowSolver createSolver(InterruptableExecutor executor, AbstractInfoflowProblem problem) {
        // Depending on the configured solver algorithm, we have to create a
        // different solver object
        IInfoflowSolver solver;
        SolverConfiguration solverConfig = config.getSolverConfiguration();
        solver = createDataFlowSolver(executor, problem, solverConfig);

        // Configure the solver
        solver.setSolverId(true);
        solver.setPredecessorShorteningMode(pathConfigToShorteningMode(manager.getConfig().getPathConfiguration()));
        solver.setMaxJoinPointAbstractions(solverConfig.getMaxJoinPointAbstractions());
        solver.setMaxCalleesPerCallSite(solverConfig.getMaxCalleesPerCallSite());
        solver.setMaxAbstractionPathLength(solverConfig.getMaxAbstractionPathLength());

        return solver;
    }

    /**
     * Gets the path shortening mode that shall be applied given a certain path
     * reconstruction configuration. This method computes the most aggressive path
     * shortening that is possible without eliminating data that is necessary for
     * the requested path reconstruction.
     *
     * @param pathConfiguration The path reconstruction configuration
     * @return The computed path shortening mode
     */
    private PredecessorShorteningMode pathConfigToShorteningMode(PathConfiguration pathConfiguration) {
        if (pathBuilderFactory.supportsPathReconstruction()) {
            switch (pathConfiguration.getPathReconstructionMode()) {
                case Fast:
                    return PredecessorShorteningMode.ShortenIfEqual;
                case NoPaths:
                    return PredecessorShorteningMode.AlwaysShorten;
                case Precise:
                    return PredecessorShorteningMode.NeverShorten;
                default:
                    throw new RuntimeException("Unknown path reconstruction mode");
            }
        } else
            return PredecessorShorteningMode.AlwaysShorten;
    }

    /**
     * Creates the instance of the data flow solver
     *
     * @param executor     The executor on which the solver shall run its tasks
     * @param problem      The problem to be solved by the new solver
     * @param solverConfig The solver configuration
     * @return The new data flow solver
     */
    protected IInfoflowSolver createDataFlowSolver(InterruptableExecutor executor, AbstractInfoflowProblem problem,
                                                   SolverConfiguration solverConfig) {
        switch (solverConfig.getDataFlowSolver()) {
            case ContextFlowSensitive:
                logger.info("Using context- and flow-sensitive solver");
                return new soot.jimple.infoflow.solver.fastSolver.InfoflowSolver(problem, executor);
            case FlowInsensitive:
                logger.info("Using context-sensitive, but flow-insensitive solver");
                return new soot.jimple.infoflow.solver.fastSolver.flowInsensitive.InfoflowSolver(problem, executor);
            case GarbageCollecting:
                logger.info("Using garbage-collecting solver");
                IInfoflowSolver solver = new soot.jimple.infoflow.solver.gcSolver.InfoflowSolver(problem, executor);
                solverPeerGroup.addSolver(solver);
                solver.setPeerGroup(solverPeerGroup);
                return solver;
            default:
                throw new RuntimeException("Unsupported data flow solver");
        }
    }

    protected Collection<SootMethod> getMethodsForSeeds(IInfoflowCFG icfg) {
        List<SootMethod> seeds = new LinkedList<>();
        // If we have a callgraph, we retrieve the reachable methods. Otherwise,
        // we have no choice but take all application methods as an
        // approximation
        if (Scene.v().hasCallGraph()) {
            ReachableMethods reachableMethods = Scene.v().getReachableMethods();
            reachableMethods.update();
            for (Iterator<MethodOrMethodContext> iter = reachableMethods.listener(); iter.hasNext();) {
                SootMethod sm = iter.next().method();
                if (isValidSeedMethod(sm))
                    seeds.add(sm);
            }
        } else {
            long beforeSeedMethods = System.nanoTime();
            Set<SootMethod> doneSet = new HashSet<>();
            for (SootMethod sm : Scene.v().getEntryPoints())
                getMethodsForSeedsIncremental(sm, doneSet, seeds, icfg);
            logger.info("Collecting seed methods took {} seconds", (System.nanoTime() - beforeSeedMethods) / 1E9);
        }
        return seeds;
    }

    private void getMethodsForSeedsIncremental(SootMethod sm, Set<SootMethod> doneSet, List<SootMethod> seeds,
                                               IInfoflowCFG icfg) {
        assert Scene.v().hasFastHierarchy();
        if (!sm.isConcrete() || !sm.getDeclaringClass().isApplicationClass() || !doneSet.add(sm))
            return;
        seeds.add(sm);
        for (Unit u : sm.retrieveActiveBody().getUnits()) {
            Stmt stmt = (Stmt) u;
            if (stmt.containsInvokeExpr()) {
                for (SootMethod callee : icfg.getCalleesOfCallAt(stmt)) {
                    if (isValidSeedMethod(callee))
                        getMethodsForSeedsIncremental(callee, doneSet, seeds, icfg);
                }
            }
        }
    }

    /**
     * Gets whether the given method is a valid seen when scanning for sources and
     * sinks. A method is a valid seed it it (or one of its transitive callees) can
     * contain calls to source or sink methods.
     *
     * @param sm The method to check
     * @return True if this method or one of its transitive callees can contain
     *         sources or sinks, otherwise false
     */
    protected boolean isValidSeedMethod(SootMethod sm) {
        if (sm == dummyMainMethod)
            return false;
        if (dummyMainMethod != null && sm.getDeclaringClass() == dummyMainMethod.getDeclaringClass())
            return false;

        // Exclude system classes
        final String className = sm.getDeclaringClass().getName();
        if (config.getIgnoreFlowsInSystemPackages() && SystemClassHandler.v().isClassInSystemPackage(className)
                && !isUserCodeClass(className))
            return false;

        // Exclude library classes
        if (config.getExcludeSootLibraryClasses() && sm.getDeclaringClass().isLibraryClass())
            return false;

        return true;
    }

    /**
     * Checks whether the given class is user code and should not be filtered out.
     * By default, this method assumes that all code is potentially user code.
     *
     * @param className The name of the class to check
     * @return True if the given class is user code, false otherwise
     */
    protected boolean isUserCodeClass(String className) {
        return false;
    }

    private int scanMethodForSourcesSinks(final ISourceSinkManager sourcesSinks, BackwardsInfoflowProblem forwardProblem,
                                            SootMethod m) {
        if (!(manager.getSourceSinkManager() instanceof IReversibleSourceSinkManager)) {
            logger.error("SourceSinkManager is incompatle with backwards analysis." +
                    "Please use an IReversibleSourceSinkManager.");
            return 0;
        }
        IReversibleSourceSinkManager ssm = (IReversibleSourceSinkManager) manager.getSourceSinkManager();

        if (getConfig().getLogSourcesAndSinks() && collectedSources == null) {
            collectedSources = new HashSet<>();
            collectedSinks = new HashSet<>();
        }

        int sourceCount = 0;
        if (m.hasActiveBody()) {
            // Check whether this is a system class we need to ignore
            if (!isValidSeedMethod(m))
                return sourceCount;
            // Look for a source in the method. Also look for sinks. If we
            // have no sink in the program, we don't need to perform any
            // analysis
            PatchingChain<Unit> units = m.getActiveBody().getUnits();
            for (Unit u : units) {
                Stmt s = (Stmt) u;
                if (ssm.getInverseSinkInfo(s, manager) != null) {
                    forwardProblem.addInitialSeeds(u, Collections.singleton(forwardProblem.zeroValue()));
                    if (getConfig().getLogSourcesAndSinks())
                        collectedSinks.add(s);
                    logger.info("Sink found: {} in {}", u, m.getSignature());
                    foundStartingPointHandler(m, s);
                }
                if (ssm.getInverseSourceInfo(s, manager, null) != null) {
                    sourceCount++;
                    if (getConfig().getLogSourcesAndSinks())
                        collectedSources.add(s);
                    logger.info("Source found: {} in {}", u, m.getSignature());
                }
            }
        }
        return sourceCount;
    }

    protected void foundStartingPointHandler(SootMethod sm, Stmt stmt) {
        // Do nothing
    }

    /**
     * Initializes the mechanism for incremental result reporting
     *
     * @param propagationResults A reference to the result object of the forward
     *                           data flow solver
     * @param builder            The path builder to use for reconstructing the
     *                           taint propagation paths
     */
    private void initializeIncrementalResultReporting(TaintPropagationResults propagationResults,
                                                      final IAbstractionPathBuilder builder) {
        // Create the path builder
        memoryWatcher.addSolver(builder);
        this.results = new BackwardsInfoflowResults();
        propagationResults.addResultAvailableHandler(new TaintPropagationResults.OnTaintPropagationResultAdded() {

            @Override
            public boolean onResultAvailable(AbstractionAtSink abs) {
                builder.addResultAvailableHandler(new IAbstractionPathBuilder.OnPathBuilderResultAvailable() {

                    @Override
                    public void onResultAvailable(ResultSourceInfo source, ResultSinkInfo sink) {
                        // Notify our external handlers
                        for (ResultsAvailableHandler handler : onResultsAvailable) {
                            if (handler instanceof ResultsAvailableHandler2) {
                                ResultsAvailableHandler2 handler2 = (ResultsAvailableHandler2) handler;
                                handler2.onSingleResultAvailable(source, sink);
                            }
                        }
                        results.addResult(sink, source);
                    }

                });

                // Compute the result paths
                builder.computeTaintPaths(Collections.singleton(abs));
                return true;
            }

        });
    }

    /**
     * Removes all abstractions from the given set that arrive at the same sink
     * statement as another abstraction, but cover less tainted variables. If, e.g.,
     * a.b.* and a.* arrive at the same sink, a.b.* is already covered by a.* and
     * can thus safely be removed.
     *
     * @param res The result set from which to remove all entailed abstractions
     */
    private void removeEntailedAbstractions(Set<AbstractionAtSink> res) {
        for (Iterator<AbstractionAtSink> absAtSinkIt = res.iterator(); absAtSinkIt.hasNext();) {
            AbstractionAtSink curAbs = absAtSinkIt.next();
            for (AbstractionAtSink checkAbs : res) {
                if (checkAbs != curAbs && checkAbs.getSinkStmt() == curAbs.getSinkStmt()
                        && checkAbs.getAbstraction().isImplicit() == curAbs.getAbstraction().isImplicit()
                        && checkAbs.getAbstraction().getSourceContext() == curAbs.getAbstraction().getSourceContext()
                        && checkAbs.getAbstraction().getTurnUnit() == curAbs.getAbstraction().getTurnUnit()) {
                    if (checkAbs.getAbstraction().getAccessPath().entails(curAbs.getAbstraction().getAccessPath())) {
                        absAtSinkIt.remove();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Gets the memory used by FlowDroid at the moment
     *
     * @return FlowDroid's current memory consumption in megabytes
     */
    private int getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return (int) Math.round((runtime.totalMemory() - runtime.freeMemory()) / 1E6);
    }

    /**
     * Runs all code optimizers
     *
     * @param sourcesSinks The SourceSinkManager
     */
    protected void eliminateDeadCode(ISourceSinkManager sourcesSinks) {
        InfoflowManager dceManager = new InfoflowManager(config, null,
                icfgFactory.buildBiDirICFG(config.getCallgraphAlgorithm(), config.getEnableExceptionTracking()), null,
                null, null, new AccessPathFactory(config), null);

        // We need to exclude the dummy main method and all other artificial methods
        // that the entry point creator may have generated as well
        Set<SootMethod> entryPoints = new HashSet<>();
        if (additionalEntryPointMethods != null)
            entryPoints.addAll(additionalEntryPointMethods);
        entryPoints.addAll(Scene.v().getEntryPoints());

        ICodeOptimizer dce = new DeadCodeEliminator();
        dce.initialize(config);
        dce.run(dceManager, entryPoints, sourcesSinks, taintWrapper);

        // Fixes an edge case, see AddNopStmt
        ICodeOptimizer nopStmt = new AddNopStmt();
        nopStmt.initialize(config);
        nopStmt.run(dceManager, entryPoints, null, null);
    }

    /**
     * Initializes the alias analysis
     *
     * @param sourcesSinks  The set of sources and sinks
     * @param iCfg          The interprocedural control flow graph
     * @param executor      The executor in which to run concurrent tasks
     * @param memoryManager The memory manager for rducing the memory load during
     *                      IFDS propagation
     * @return The alias analysis implementation to use for the data flow analysis
     */
    private IAliasingStrategy createAliasAnalysis(final ISourceSinkManager sourcesSinks, IInfoflowCFG iCfg,
                                                  InterruptableExecutor executor, IMemoryManager<Abstraction, Unit> memoryManager) {
        IAliasingStrategy aliasingStrategy;
        IInfoflowSolver aliasSolver = null;
        BackwardsAliasProblem aliasProblem = null;
        InfoflowManager aliasManager = null;
        switch (getConfig().getAliasingAlgorithm()) {
            case FlowSensitive:
                aliasManager = new InfoflowManager(config, null, iCfg, sourcesSinks,
                        taintWrapper, hierarchy, manager.getAccessPathFactory(), manager.getGlobalTaintManager());
                aliasProblem = new BackwardsAliasProblem(aliasManager);

                // We need to create the right data flow solver
                SolverConfiguration solverConfig = config.getSolverConfiguration();
                aliasSolver = createDataFlowSolver(executor, aliasProblem, solverConfig);

                aliasSolver.setMemoryManager(memoryManager);
                aliasSolver.setPredecessorShorteningMode(
                        pathConfigToShorteningMode(manager.getConfig().getPathConfiguration()));
                // aliasSolver.setEnableMergePointChecking(true);
                aliasSolver.setMaxJoinPointAbstractions(solverConfig.getMaxJoinPointAbstractions());
                aliasSolver.setMaxCalleesPerCallSite(solverConfig.getMaxCalleesPerCallSite());
                aliasSolver.setMaxAbstractionPathLength(solverConfig.getMaxAbstractionPathLength());
                aliasSolver.setSolverId(false);
                aliasProblem.setTaintPropagationHandler(backwardsPropagationHandler);
                aliasProblem.setTaintWrapper(taintWrapper);
                if (nativeCallHandler != null)
                    aliasProblem.setNativeCallHandler(nativeCallHandler);

                memoryWatcher.addSolver((IMemoryBoundedSolver) aliasSolver);

                aliasingStrategy = new BackwardsFlowSensitiveAliasStrategy(manager, aliasSolver);
                break;
            case None:
                aliasProblem = null;
                aliasSolver = null;
                aliasingStrategy = new NullAliasStrategy();
                break;
            // TODO: currently not supported
//            case PtsBased:
//                aliasProblem = null;
//                aliasSolver = null;
//                aliasingStrategy = new PtsBasedAliasStrategy(manager);
//                break;
//            case Lazy:
//                aliasProblem = null;
//                aliasSolver = null;
//                aliasingStrategy = new LazyAliasingStrategy(manager);
//                break;
            default:
                throw new RuntimeException("Unsupported aliasing algorithm");
        }
        return aliasingStrategy;
    }
    /**
     * Creates the controller object that handles aliasing operations. Derived
     * classes can override this method to supply custom aliasing implementations.
     *
     * @param aliasingStrategy The aliasing strategy to use
     * @return The new alias controller object
     */
    protected Aliasing createAliasController(IAliasingStrategy aliasingStrategy) {
        return new Aliasing(aliasingStrategy, manager);
    }

    protected InfoflowManager initializeInfoflowManager(final ISourceSinkManager sourcesSinks, IInfoflowCFG iCfg,
                                                        GlobalTaintManager globalTaintManager) {
        return new InfoflowManager(config, null, new BackwardsInfoflowCFG(iCfg), sourcesSinks,
                taintWrapper, hierarchy, new AccessPathFactory(config), globalTaintManager);
    }

    protected void finishedSourceSinkHandler() {

    }

    protected void runAnalysis(ISourceSinkManager sourcesSinks) {
        runAnalysis(sourcesSinks, null);
    }
    /**
     * Conducts a taint analysis on an already initialized callgraph
     *
     * @param sourcesSinks    The sources and sinks to be used
     * @param additionalSeeds Additional seeds at which to create A ZERO fact even
     *                        if they are not sources
     */
     private void runAnalysis(ISourceSinkManager sourcesSinks, Set<String> additionalSeeds) {
        final InfoflowPerformanceData performanceData = createPerformanceDataClass();
        try {
            // Clear the data from previous runs
            results = new BackwardsInfoflowResults();
            results.setPerformanceData(performanceData);

            // Print our configuration
            config.printSummary();

            // Register a memory watcher
            if (memoryWatcher != null) {
                memoryWatcher.clearSolvers();
                memoryWatcher = null;
            }
            memoryWatcher = new FlowDroidMemoryWatcher(results, config.getMemoryThreshold());

            // Initialize the abstraction configuration
            Abstraction.initialize(config);

            // Build the callgraph
            long beforeCallgraph = System.nanoTime();
            constructCallgraph();
            performanceData
                    .setCallgraphConstructionSeconds((int) Math.round((System.nanoTime() - beforeCallgraph) / 1E9));
            logger.info(String.format(Locale.getDefault(), "Callgraph construction took %d seconds",
                    performanceData.getCallgraphConstructionSeconds()));

            // Initialize the source sink manager
            if (sourcesSinks != null)
                sourcesSinks.initialize();

            // Perform constant propagation and remove dead code
			if (config.getCodeEliminationMode() != CodeEliminationMode.NoCodeElimination) {
				long currentMillis = System.nanoTime();
				eliminateDeadCode(sourcesSinks);
				logger.info("Dead code elimination took " + (System.nanoTime() - currentMillis) / 1E9 + " seconds");
			}

            // After constant value propagation, we might find more call edges
            // for reflective method calls
            if (config.getEnableReflection()) {
                releaseCallgraph();
                constructCallgraph();
            }

            if (config.getCallgraphAlgorithm() != CallgraphAlgorithm.OnDemand)
                logger.info("Callgraph has {} edges", Scene.v().getCallGraph().size());

            if (!config.isTaintAnalysisEnabled())
                return;

            // Make sure that we have a path builder factory
            if (pathBuilderFactory == null)
                pathBuilderFactory = new DefaultPathBuilderFactory(config.getPathConfiguration());

            logger.info("Starting Taint Analysis");
            IInfoflowCFG iCfg = icfgFactory.buildBiDirICFG(config.getCallgraphAlgorithm(),
                    config.getEnableExceptionTracking());

            // Check whether we need to run with one source at a time
            IOneSourceAtATimeManager oneSourceAtATime = config.getOneSourceAtATime()
                    && sourcesSinks instanceof IOneSourceAtATimeManager ? (IOneSourceAtATimeManager) sourcesSinks
                    : null;

            // Reset the current source
            if (oneSourceAtATime != null)
                oneSourceAtATime.resetCurrentSource();
            boolean hasMoreSources = oneSourceAtATime == null || oneSourceAtATime.hasNextSource();

            while (hasMoreSources) {
                // Fetch the next source
                if (oneSourceAtATime != null)
                    oneSourceAtATime.nextSource();

                // Create the executor that takes care of the workers
                int numThreads = Runtime.getRuntime().availableProcessors();
                InterruptableExecutor executor = executorFactory.createExecutor(numThreads, true, config);
                executor.setThreadFactory(new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thrIFDS = new Thread(r);
                        thrIFDS.setDaemon(true);
                        thrIFDS.setName("FlowDroid");
                        return thrIFDS;
                    }

                });

                // Initialize the memory manager
                IMemoryManager<Abstraction, Unit> memoryManager = createMemoryManager();

                // Initialize our infrastructure for global taints
                final Set<IInfoflowSolver> solvers = new HashSet<>();
                GlobalTaintManager globalTaintManager = new GlobalTaintManager(solvers);

                // Initialize the data flow manager
                manager = initializeInfoflowManager(sourcesSinks, iCfg, globalTaintManager);

                // Create the solver peer group
                solverPeerGroup = new GCSolverPeerGroup();

                // Initialize the alias analysis
//                Abstraction zeroValue = Abstraction.getZeroAbstraction(manager.getConfig().getFlowSensitiveAliasing());;
                Abstraction zeroValue = null;
                IAliasingStrategy aliasingStrategy = createAliasAnalysis(sourcesSinks, iCfg, executor, memoryManager);
                IInfoflowSolver backwardSolver = aliasingStrategy.getSolver();
                if (backwardSolver != null) {
                    zeroValue = backwardSolver.getTabulationProblem().createZeroValue();
                    solvers.add(backwardSolver);
                }

                // Initialize the aliasing infrastructure
                Aliasing aliasing = createAliasController(aliasingStrategy);
                if (dummyMainMethod != null)
                    aliasing.excludeMethodFromMustAlias(dummyMainMethod);
                manager.setAliasing(aliasing);

                // Initialize the data flow problem
                BackwardsInfoflowProblem infoflowProblem = new BackwardsInfoflowProblem(manager, zeroValue, ruleManagerFactory);
                infoflowProblem.setNativeCallHandler(this.nativeCallHandler);
                // We need to create the right data flow solver
                IInfoflowSolver solver = createSolver(executor, infoflowProblem);

                memoryWatcher.addSolver((IMemoryBoundedSolver) solver);
                manager.setForwardSolver(solver);
                if (aliasingStrategy.getSolver() != null)
                    aliasingStrategy.getSolver().getTabulationProblem().getManager().setForwardSolver(solver);
                solvers.add(solver);
                

                // Start a thread for enforcing the timeout
                FlowDroidTimeoutWatcher timeoutWatcher = null;
                FlowDroidTimeoutWatcher pathTimeoutWatcher = null;
                if (config.getDataFlowTimeout() > 0) {
                    timeoutWatcher = new FlowDroidTimeoutWatcher(config.getDataFlowTimeout(), results);
                    timeoutWatcher.addSolver((IMemoryBoundedSolver) solver);
                    timeoutWatcher.start();
                }

                InterruptableExecutor resultExecutor = null;
                long beforePathReconstruction = 0;
                try {
                    // Print our configuration
                    if (config.getFlowSensitiveAliasing()
                            && config.getSolverConfiguration().getMaxJoinPointAbstractions() > 0)
                        logger.warn("Running with limited join point abstractions can break context-"
                                + "sensitive path builders");

                    // We have to look through the complete program to find
                    // sources which are then taken as seeds.
                    int sourceCount = 0;
                    logger.info("Looking for sources and sinks...");

                    for (SootMethod sm : getMethodsForSeeds(iCfg))
                        sourceCount += scanMethodForSourcesSinks(sourcesSinks, infoflowProblem, sm);

                    // We optionally also allow additional seeds to be specified
                    if (additionalSeeds != null)
                        for (String meth : additionalSeeds) {
                            SootMethod m = Scene.v().getMethod(meth);
                            if (!m.hasActiveBody()) {
                                logger.warn("Seed method {} has no active body", m);
                                continue;
                            }
                            infoflowProblem.addInitialSeeds(m.getActiveBody().getUnits().getFirst(),
                                    Collections.singleton(infoflowProblem.zeroValue()));
                        }

                    // Report on the sources and sinks we have found
                    if (!infoflowProblem.hasInitialSeeds()) {
                        logger.error("No sinks found, aborting analysis");
                        continue;
                    }
                    if (sourceCount == 0) {
                        logger.error("No sources found, aborting analysis");
                        continue;
                    }
                    logger.info("Source lookup done, found {} sources and {} sinks.",
                            sourceCount, infoflowProblem.getInitialSeeds().size());

                    // Update the performance statistics
                    performanceData.setSourceCount(sourceCount);
                    performanceData.setSinkCount(infoflowProblem.getInitialSeeds().size());

                    // Initialize the taint wrapper if we have one
                    if (taintWrapper != null)
                        taintWrapper.initialize(manager);
                    if (nativeCallHandler != null)
                        nativeCallHandler.initialize(manager);

                    // Register the handler for interim results
                    TaintPropagationResults propagationResults = infoflowProblem.getResults();
                    resultExecutor = executorFactory.createExecutor(numThreads, false, config);
                    resultExecutor.setThreadFactory(new ThreadFactory() {

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread thrPath = new Thread(r);
                            thrPath.setDaemon(true);
                            thrPath.setName("FlowDroid Path Reconstruction");
                            return thrPath;
                        }
                    });

                    // Create the path builder
                    final IAbstractionPathBuilder builder = new BatchPathBuilder(manager,
                            pathBuilderFactory.createPathBuilder(manager, resultExecutor));

                    // If we want incremental result reporting, we have to
                    // initialize it before we start the taint tracking
                    if (config.getIncrementalResultReporting())
                        initializeIncrementalResultReporting(propagationResults, builder);

                    // Initialize the performance data
                    if (performanceData.getTaintPropagationSeconds() < 0)
                        performanceData.setTaintPropagationSeconds(0);
                    long beforeTaintPropagation = System.nanoTime();

                    onBeforeTaintPropagation(solver, backwardSolver);
                    solver.solve();

                    // Not really nice, but sometimes Heros returns before all
                    // executor tasks are actually done. This way, we give it a
                    // chance to terminate gracefully before moving on.
                    int terminateTries = 0;
                    while (terminateTries < 10) {
                        if (executor.getActiveCount() != 0 || !executor.isTerminated()) {
                            terminateTries++;
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                logger.error("Could not wait for executor termination", e);
                            }
                        } else
                            break;
                    }
                    if (executor.getActiveCount() != 0 || !executor.isTerminated())
                        logger.error("Executor did not terminate gracefully");
                    if (executor.getException() != null) {
                        throw new RuntimeException("An exception has occurred in an executor", executor.getException());
                    }

                    // Update performance statistics
                    performanceData.updateMaxMemoryConsumption(getUsedMemory());
                    int taintPropagationSeconds = (int) Math.round((System.nanoTime() - beforeTaintPropagation) / 1E9);
                    performanceData.addTaintPropagationSeconds(taintPropagationSeconds);

                    // Print taint wrapper statistics
                    if (taintWrapper != null) {
                        logger.info("Taint wrapper hits: " + taintWrapper.getWrapperHits());
                        logger.info("Taint wrapper misses: " + taintWrapper.getWrapperMisses());
                    }

                    // Give derived classes a chance to do whatever they need before we remove stuff
                    // from memory
					onTaintPropagationCompleted(solver, backwardSolver);

                    // Get the result abstractions
                    Set<AbstractionAtSink> res = propagationResults.getResults();
                    propagationResults = null;

                    // We need to prune access paths that are entailed by
                    // another one
                    removeEntailedAbstractions(res);

                    // Shut down the native call handler
                    if (nativeCallHandler != null)
                        nativeCallHandler.shutdown();

                    logger.info(
                            "IFDS problem with {} infoflow edges and {} alias edges solved in {} seconds, processing {} results...",
                            solver.getPropagationCount(),
                            backwardSolver == null ? 0 : backwardSolver.getPropagationCount(),
                            taintPropagationSeconds, res == null ? 0 : res.size());

                    // Update the statistics
                    {
                        ISolverTerminationReason reason = ((IMemoryBoundedSolver) solver).getTerminationReason();
                        if (reason != null) {
                            if (reason instanceof OutOfMemoryReason)
                                results.setTerminationState(
                                        results.getTerminationState() | InfoflowResults.TERMINATION_DATA_FLOW_OOM);
                            else if (reason instanceof TimeoutReason)
                                results.setTerminationState(
                                        results.getTerminationState() | InfoflowResults.TERMINATION_DATA_FLOW_TIMEOUT);
                        }
                    }

                    // Force a cleanup. Everything we need is reachable through
                    // the results set, the other abstractions can be killed
                    // now.
                    performanceData.updateMaxMemoryConsumption(getUsedMemory());
                    performanceData.setInfoflowPropagationCount(solver.getPropagationCount());
                    performanceData.setAliasPropagationCount(backwardSolver == null ? -1 : backwardSolver.getPropagationCount());

                    logger.info(String.format("Current memory consumption: %d MB", getUsedMemory()));

                    if (timeoutWatcher != null)
                        timeoutWatcher.stop();
                    memoryWatcher.removeSolver((IMemoryBoundedSolver) solver);
                    solver.cleanup();
                    solver = null;
                    infoflowProblem = null;

                    solverPeerGroup = null;

                    if (config.getIncrementalResultReporting())
                        res = null;
                    iCfg.purge();

                    // Clean up the manager. Make sure to free objects, even if
                    // the manager is still held by other objects
                    if (manager != null)
                        manager.cleanup();
                    manager = null;

                    // Report the remaining memory consumption
                    Runtime.getRuntime().gc();
                    performanceData.updateMaxMemoryConsumption(getUsedMemory());
                    logger.info(String.format("Memory consumption after cleanup: %d MB", getUsedMemory()));

                    // Apply the timeout to path reconstruction
                    if (config.getPathConfiguration().getPathReconstructionTimeout() > 0) {
                        pathTimeoutWatcher = new FlowDroidTimeoutWatcher(
                                config.getPathConfiguration().getPathReconstructionTimeout(), results);
                        pathTimeoutWatcher.addSolver(builder);
                        pathTimeoutWatcher.start();
                    }
                    beforePathReconstruction = System.nanoTime();

                    // Do the normal result computation in the end unless we
                    // have used incremental path building
                    if (config.getIncrementalResultReporting()) {
                        // After the last intermediate result has been computed,
                        // we need to re-process those abstractions that
                        // received new neighbors in the meantime
                        builder.runIncrementalPathCompuation();

                        try {
                            resultExecutor.awaitCompletion();
                        } catch (InterruptedException e) {
                            logger.error("Could not wait for executor termination", e);
                        }
                    } else {
                        memoryWatcher.addSolver(builder);
                        builder.computeTaintPaths(res);
                        res = null;

                        // Update the statistics
                        {
                            ISolverTerminationReason reason = builder.getTerminationReason();
                            if (reason != null) {
                                if (reason instanceof OutOfMemoryReason)
                                    results.setTerminationState(results.getTerminationState()
                                            | InfoflowResults.TERMINATION_PATH_RECONSTRUCTION_OOM);
                                else if (reason instanceof TimeoutReason)
                                    results.setTerminationState(results.getTerminationState()
                                            | InfoflowResults.TERMINATION_PATH_RECONSTRUCTION_TIMEOUT);
                            }
                        }

                        // Wait for the path builders to terminate
                        try {
                            // The path reconstruction should stop on time anyway. In case it doesn't, we
                            // make sure that we don't get stuck.
                            long pathTimeout = config.getPathConfiguration().getPathReconstructionTimeout();
                            if (pathTimeout > 0)
                                resultExecutor.awaitCompletion(pathTimeout + 20, TimeUnit.SECONDS);
                            else
                                resultExecutor.awaitCompletion();
                        } catch (InterruptedException e) {
                            logger.error("Could not wait for executor termination", e);
                        }

                        // Get the results once the path builder is done
                        this.results.addAll(builder.getResults());
                    }
                    resultExecutor.shutdown();

                    // If the path builder was aborted, we warn the user
                    if (builder.isKilled())
                        logger.warn("Path reconstruction aborted. The reported results may be incomplete. "
                                + "You might want to try again with sequential path processing enabled.");
                } finally {
                    // Terminate the executor
                    if (resultExecutor != null)
                        resultExecutor.shutdown();

                    // Make sure to stop the watcher thread
                    if (timeoutWatcher != null)
                        timeoutWatcher.stop();
                    if (pathTimeoutWatcher != null)
                        pathTimeoutWatcher.stop();

                    // Do we have any more sources?
                    hasMoreSources = oneSourceAtATime != null && oneSourceAtATime.hasNextSource();

                    // Shut down the memory watcher
                    memoryWatcher.close();

                    // Get rid of all the stuff that's still floating around in
                    // memory
                    infoflowProblem = null;
                    solver = null;
                    if (manager != null)
                        manager.cleanup();
                    manager = null;
                }

                // Make sure that we are in a sensible state even if we ran out
                // of memory before
                Runtime.getRuntime().gc();
                performanceData.updateMaxMemoryConsumption((int) getUsedMemory());
                performanceData.setPathReconstructionSeconds(
                        (int) Math.round((System.nanoTime() - beforePathReconstruction) / 1E9));

                logger.info(String.format("Memory consumption after path building: %d MB", getUsedMemory()));
                logger.info(String.format("Path reconstruction took %d seconds",
                        performanceData.getPathReconstructionSeconds()));
            }

            // Execute the post-processors
            for (PostAnalysisHandler handler : this.postProcessors)
                results = handler.onResultsAvailable(results, iCfg);

            if (results == null || results.isEmpty())
                logger.warn("No results found.");
            else if (logger.isInfoEnabled()) {
                for (ResultSinkInfo sink : results.getResults().keySet()) {
                    logger.info("The sink {} in method {} was called with values from the following sources:", sink,
                            iCfg.getMethodOf(sink.getStmt()).getSignature());
                    for (ResultSourceInfo source : results.getResults().get(sink)) {
                        logger.info("- {} in method {}", source, iCfg.getMethodOf(source.getStmt()).getSignature());
                        if (source.getPath() != null) {
                            logger.info("\ton Path: ");
                            for (Unit p : source.getPath()) {
                                logger.info("\t -> " + iCfg.getMethodOf(p));
                                logger.info("\t\t -> " + p);
                            }
                        }
                    }
                }
            }

            // Gather performance data
            performanceData.setTotalRuntimeSeconds((int) Math.round((System.nanoTime() - beforeCallgraph) / 1E9));
            performanceData.updateMaxMemoryConsumption(getUsedMemory());
            logger.info(String.format("Data flow solver took %d seconds. Maximum memory consumption: %d MB",
                    performanceData.getTotalRuntimeSeconds(), performanceData.getMaxMemoryConsumption()));

            // Provide the handler with the final results
            for (ResultsAvailableHandler handler : onResultsAvailable)
                handler.onResultsAvailable(iCfg, results);

            // Write the Jimple files to disk if requested
            if (config.getWriteOutputFiles())
                PackManager.v().writeOutput();
        } catch (Exception ex) {
            StringWriter stacktrace = new StringWriter();
            PrintWriter pw = new PrintWriter(stacktrace);
            ex.printStackTrace(pw);
            results.addException(ex.getClass().getName() + ": " + ex.getMessage() + "\n" + stacktrace.toString());
            logger.error("Exception during data flow analysis", ex);
            if (throwExceptions)
                throw ex;
        }
    }

    /**
     * getResults returns the results found by the analysis
     *
     * @return the results
     */
    @Override
    public InfoflowResults getResults() {
        return results;
    }

    /**
     * A result is available if the analysis has finished - so if this method
     * returns false the analysis has not finished yet or was not started (e.g. no
     * sources or sinks found)
     *
     * @return boolean that states if a result is available
     */
    @Override
    public boolean isResultAvailable() {
        return results != null;
    }

    /**
     * Gets the concrete set of sources that have been collected in preparation for
     * the taint analysis. This method will return null if source and sink logging
     * has not been enabled (see InfoflowConfiguration. setLogSourcesAndSinks()),
     *
     * @return The set of sources collected for taint analysis
     */
    @Override
    public Set<Stmt> getCollectedSources() {
        return this.collectedSources;
    }

    /**
     * Gets the concrete set of sinks that have been collected in preparation for
     * the taint analysis. This method will return null if source and sink logging
     * has not been enabled (see InfoflowConfiguration. setLogSourcesAndSinks()),
     *
     * @return The set of sinks collected for taint analysis
     */
    @Override
    public Set<Stmt> getCollectedSinks() {
        return this.collectedSinks;
    }

    /**
     * Adds a handler that is called when information flow results are available
     *
     * @param handler The handler to add
     */
    @Override
    public void addResultsAvailableHandler(ResultsAvailableHandler handler) {
        this.onResultsAvailable.add(handler);
    }

    /**
     * Removes a handler that is called when information flow results are available
     *
     * @param handler The handler to remove
     */
    @Override
    public void removeResultsAvailableHandler(ResultsAvailableHandler handler) {
        this.onResultsAvailable.remove(handler);
    }

    /**
     * Aborts the data flow analysis. This is useful when the analysis controller is
     * running in a different thread and the main thread (e.g., a GUI) wants to
     * abort the analysis
     */
    @Override
    public void abortAnalysis() {

    }

    /**
     * Sets a handler which is invoked whenever a taint is propagated
     *
     * @param handler The handler to be invoked when propagating taints
     */
    @Override
    public void setTaintPropagationHandler(TaintPropagationHandler handler) {
        this.taintPropagationHandler = handler;
    }

    /**
     * Sets a handler which is invoked whenever an alias is propagated backwards
     *
     * @param handler The handler to be invoked when propagating aliases
     */
    @Override
    public void setBackwardsPropagationHandler(TaintPropagationHandler handler) {
        this.backwardsPropagationHandler = handler;
    }

    protected void onBeforeTaintPropagation(IInfoflowSolver forwardSolver, IInfoflowSolver backwardSolver) {
        //
    }

    protected void onTaintPropagationCompleted(IInfoflowSolver forwardSolver, IInfoflowSolver backwardSolver) {
        //
    }
    /**
     * Sets the factory to be used for creating memory managers
     *
     * @param factory The memory manager factory to use
     */
    @Override
    public void setMemoryManagerFactory(IMemoryManagerFactory factory) {
        this.memoryManagerFactory = memoryManagerFactory;
    }

    /**
     * Factory method for creating the data object that will receive the data flow
     * solver's performance data
     *
     * @return The data object for the performance data
     */
    protected InfoflowPerformanceData createPerformanceDataClass() {
        return new InfoflowPerformanceData();
    }

    /**
     * Sets the factory to be used for creating thread pool executors
     *
     * @param executorFactory The executor factory to use
     */
    @Override
    public void setExecutorFactory(IExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
    }

    /**
     * Sets the factory to be used for creating the propagation rule manager, which
     * can then add features to the core data flow engine
     *
     * @param ruleManagerFactory The factory class for the propagation rule manager
     */
    @Override
    public void setPropagationRuleManagerFactory(IPropagationRuleManagerFactory ruleManagerFactory) {
        this.ruleManagerFactory = ruleManagerFactory;
    }

    public void setThrowExceptions(boolean b) {
        this.throwExceptions = b;
    }
}
