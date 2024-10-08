package soot.jimple.infoflow;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import heros.solver.Pair;
import soot.ArrayType;
import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FastHierarchy;
import soot.FloatType;
import soot.G;
import soot.IntType;
import soot.Local;
import soot.LocalGenerator;
import soot.LongType;
import soot.MethodOrMethodContext;
import soot.MethodSource;
import soot.PackManager;
import soot.PatchingChain;
import soot.PointsToAnalysis;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.javaToJimple.DefaultLocalGenerator;
import soot.jimple.AssignStmt;
import soot.jimple.DynamicInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration.AccessPathConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;
import soot.jimple.infoflow.InfoflowConfiguration.CodeEliminationMode;
import soot.jimple.infoflow.InfoflowConfiguration.DataFlowDirection;
import soot.jimple.infoflow.InfoflowConfiguration.DataFlowSolver;
import soot.jimple.infoflow.InfoflowConfiguration.PathConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.SolverConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.SootIntegrationMode;
import soot.jimple.infoflow.InfoflowConfiguration.StaticFieldTrackingMode;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.aliasing.BackwardsFlowSensitiveAliasStrategy;
import soot.jimple.infoflow.aliasing.IAliasingStrategy;
import soot.jimple.infoflow.aliasing.NullAliasStrategy;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.cfg.FlowDroidSinkStatement;
import soot.jimple.infoflow.cfg.FlowDroidSourceStatement;
import soot.jimple.infoflow.cfg.FlowDroidUserClass;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.infoflow.codeOptimization.DeadCodeEliminator;
import soot.jimple.infoflow.codeOptimization.ICodeOptimizer;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.FlowDroidMemoryManager;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory;
import soot.jimple.infoflow.data.pathBuilders.IAbstractionPathBuilder;
import soot.jimple.infoflow.data.pathBuilders.IAbstractionPathBuilder.OnPathBuilderResultAvailable;
import soot.jimple.infoflow.data.pathBuilders.IPathBuilderFactory;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.jimple.infoflow.globalTaints.GlobalTaintManager;
import soot.jimple.infoflow.handlers.PostAnalysisHandler;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler2;
import soot.jimple.infoflow.handlers.SequentialTaintPropagationHandler;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.ipc.DefaultIPCManager;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.jimple.infoflow.memory.FlowDroidMemoryWatcher;
import soot.jimple.infoflow.memory.FlowDroidTimeoutWatcher;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.memory.ISolverTerminationReason;
import soot.jimple.infoflow.memory.reasons.AbortRequestedReason;
import soot.jimple.infoflow.memory.reasons.OutOfMemoryReason;
import soot.jimple.infoflow.memory.reasons.TimeoutReason;
import soot.jimple.infoflow.nativeCallHandler.BackwardNativeCallHandler;
import soot.jimple.infoflow.nativeCallHandler.INativeCallHandler;
import soot.jimple.infoflow.problems.AbstractInfoflowProblem;
import soot.jimple.infoflow.problems.BackwardsAliasProblem;
import soot.jimple.infoflow.problems.BackwardsInfoflowProblem;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.TaintPropagationResults.OnTaintPropagationResultAdded;
import soot.jimple.infoflow.problems.rules.IPropagationRuleManagerFactory;
import soot.jimple.infoflow.results.InfoflowPerformanceData;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.river.ConditionalFlowPostProcessor;
import soot.jimple.infoflow.river.ConditionalFlowSourceSinkManagerWrapper;
import soot.jimple.infoflow.river.EmptyUsageContextProvider;
import soot.jimple.infoflow.river.IConditionalFlowManager;
import soot.jimple.infoflow.river.IUsageContextProvider;
import soot.jimple.infoflow.river.SecondaryFlowGenerator;
import soot.jimple.infoflow.river.SecondaryFlowListener;
import soot.jimple.infoflow.solver.DefaultSolverPeerGroup;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.ISolverPeerGroup;
import soot.jimple.infoflow.solver.PredecessorShorteningMode;
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.fastSolver.InfoflowSolver;
import soot.jimple.infoflow.solver.gcSolver.GCSolverPeerGroup;
import soot.jimple.infoflow.solver.memory.DefaultMemoryManagerFactory;
import soot.jimple.infoflow.solver.memory.IMemoryManager;
import soot.jimple.infoflow.solver.memory.IMemoryManagerFactory;
import soot.jimple.infoflow.solver.sparseSolver.SparseInfoflowSolver;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.DefaultSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.IOneSourceAtATimeManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.threading.DefaultExecutorFactory;
import soot.jimple.infoflow.threading.IExecutorFactory;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.options.Options;
import soot.util.NumberedString;

/**
 * Abstract base class for all data/information flow analyses in FlowDroid
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractInfoflow implements IInfoflow {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected InfoflowResults results = null;
	protected InfoflowManager manager;
	protected ISolverPeerGroup solverPeerGroup;

	protected IPathBuilderFactory pathBuilderFactory;
	protected InfoflowConfiguration config = new InfoflowConfiguration();
	protected ITaintPropagationWrapper taintWrapper;
	protected INativeCallHandler nativeCallHandler;
	protected IIPCManager ipcManager = new DefaultIPCManager(new ArrayList<String>());

	protected final BiDirICFGFactory icfgFactory;
	protected Collection<PreAnalysisHandler> preProcessors = new ArrayList<>();
	protected Collection<PostAnalysisHandler> postProcessors = new ArrayList<>();

	protected final File androidPath;
	protected final boolean forceAndroidJar;
	protected IInfoflowConfig sootConfig;
	protected FastHierarchy hierarchy;

	protected IMemoryManagerFactory memoryManagerFactory = new DefaultMemoryManagerFactory();
	protected IExecutorFactory executorFactory = new DefaultExecutorFactory();
	protected IPropagationRuleManagerFactory ruleManagerFactory = initializeRuleManagerFactory();
	protected IPropagationRuleManagerFactory reverseRuleManagerFactory = initializeReverseRuleManagerFactory();

	protected Set<Stmt> collectedSources;
	protected Set<Stmt> collectedSinks;

	protected SootMethod dummyMainMethod;
	protected Collection<SootMethod> additionalEntryPointMethods;

	protected boolean throwExceptions;

	protected Set<ResultsAvailableHandler> onResultsAvailable = new HashSet<>();
	protected TaintPropagationHandler taintPropagationHandler = null;
	protected TaintPropagationHandler aliasPropagationHandler = null;
	protected TaintPropagationHandler reverseTaintPropagationHandler = null;
	protected TaintPropagationHandler reverseAliasPropagationHandler = null;

	protected IUsageContextProvider usageContextProvider = null;

	protected FlowDroidMemoryWatcher memoryWatcher = null;

	/**
	 * Creates a new instance of the abstract info flow problem
	 */
	public AbstractInfoflow() {
		this(null, null, false);
	}

	/**
	 * Creates a new instance of the abstract info flow problem
	 * 
	 * @param icfgFactory     The interprocedural CFG to be used by the
	 *                        InfoFlowProblem
	 * @param androidPath     If forceAndroidJar is false, this is the base
	 *                        directory of the platform files in the Android SDK. If
	 *                        forceAndroidJar is true, this is the full path of a
	 *                        single android.jar file.
	 * @param forceAndroidJar True if a single platform JAR file shall be forced,
	 *                        false if Soot shall pick the appropriate platform
	 *                        version
	 */
	public AbstractInfoflow(BiDirICFGFactory icfgFactory, File androidPath, boolean forceAndroidJar) {
		if (icfgFactory == null) {
			DefaultBiDiICFGFactory factory = new DefaultBiDiICFGFactory();
			factory.setIsAndroid(androidPath != null && androidPath.exists());
			this.icfgFactory = factory;
		} else
			this.icfgFactory = icfgFactory;
		this.androidPath = androidPath;
		this.forceAndroidJar = forceAndroidJar;
	}

	@Override
	public InfoflowConfiguration getConfig() {
		return this.config;
	}

	@Override
	public void setConfig(InfoflowConfiguration config) {
		this.config = config;
	}

	@Override
	public void setTaintWrapper(ITaintPropagationWrapper wrapper) {
		taintWrapper = wrapper;
	}

	@Override
	public void setNativeCallHandler(INativeCallHandler handler) {
		this.nativeCallHandler = handler;
	}

	@Override
	public ITaintPropagationWrapper getTaintWrapper() {
		return taintWrapper;
	}

	@Override
	public void addPreprocessor(PreAnalysisHandler preprocessor) {
		this.preProcessors.add(preprocessor);
	}

	@Override
	public void addPostProcessor(PostAnalysisHandler postprocessor) {
		this.postProcessors.add(postprocessor);
	}

	@Override
	public void computeInfoflow(String appPath, String libPath, IEntryPointCreator entryPointCreator,
			List<String> sources, List<String> sinks) {
		this.computeInfoflow(appPath, libPath, entryPointCreator, new DefaultSourceSinkManager(sources, sinks));
	}

	@Override
	public void computeInfoflow(String appPath, String libPath, Collection<String> entryPoints,
			Collection<String> sources, Collection<String> sinks) {
		this.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(entryPoints),
				new DefaultSourceSinkManager(sources, sinks));
	}

	@Override
	public void computeInfoflow(String libPath, String appPath, String entryPoint, Collection<String> sources,
			Collection<String> sinks) {
		this.computeInfoflow(appPath, libPath, entryPoint, new DefaultSourceSinkManager(sources, sinks));
	}

	/**
	 * Appends two elements to build a classpath
	 * 
	 * @param appPath The first entry of the classpath
	 * @param libPath The second entry of the classpath
	 * @return The concatenated classpath
	 */
	private String appendClasspath(String appPath, String libPath) {
		String s = (appPath != null && !appPath.isEmpty()) ? appPath : "";

		if (libPath != null && !libPath.isEmpty()) {
			if (!s.isEmpty())
				s += File.pathSeparator;
			s += libPath;
		}
		return s;
	}

	/**
	 * Initializes Soot.
	 * 
	 * @param appPath The application path containing the analysis client
	 * @param libPath The Soot classpath containing the libraries
	 * @param classes The set of classes that shall be checked for data flow
	 *                analysis seeds. All sources in these classes are used as
	 *                seeds.
	 */
	protected void initializeSoot(String appPath, String libPath, Collection<String> classes) {
		initializeSoot(appPath, libPath, classes, "");
	}

	/**
	 * Initializes Soot.
	 * 
	 * @param appPath The application path containing the analysis client
	 * @param libPath The Soot classpath containing the libraries
	 * @param classes The set of classes that shall be checked for data flow
	 *                analysis seeds. All sources in these classes are used as
	 *                seeds. If a non-empty extra seed is given, this one is used
	 *                too.
	 */
	protected void initializeSoot(String appPath, String libPath, Collection<String> classes, String extraSeed) {
		if (config.getSootIntegrationMode().needsToInitializeSoot()) {
			// reset Soot:
			logger.info("Resetting Soot...");
			soot.G.reset();

			Options.v().set_no_bodies_for_excluded(true);
			Options.v().set_allow_phantom_refs(true);
			if (config.getWriteOutputFiles())
				Options.v().set_output_format(Options.output_format_jimple);
			else
				Options.v().set_output_format(Options.output_format_none);

			// We only need to distinguish between application and library classes
			// if we use the OnTheFly ICFG
			if (config.getCallgraphAlgorithm() == CallgraphAlgorithm.OnDemand) {
				Options.v().set_soot_classpath(libPath);
				if (appPath != null) {
					List<String> processDirs = new LinkedList<String>();
					for (String ap : appPath.split(File.pathSeparator))
						processDirs.add(ap);
					Options.v().set_process_dir(processDirs);
				}
			} else
				Options.v().set_soot_classpath(appendClasspath(appPath, libPath));

			// do not merge variables (causes problems with PointsToSets)
			Options.v().setPhaseOption("jb.ulp", "off");

			setSourcePrec();
		}

		if (config.getSootIntegrationMode().needsToBuildCallgraph()) {
			// Configure the callgraph algorithm
			switch (config.getCallgraphAlgorithm()) {
			case AutomaticSelection:
				// If we analyze a distinct entry point which is not static,
				// SPARK fails due to the missing allocation site and we fall
				// back to CHA.
				if (extraSeed == null || extraSeed.isEmpty()) {
					setSparkOptions();
				} else {
					setChaOptions();
				}
				break;
			case CHA:
				setChaOptions();
				break;
			case RTA:
				Options.v().setPhaseOption("cg.spark", "on");
				Options.v().setPhaseOption("cg.spark", "rta:true");
				Options.v().setPhaseOption("cg.spark", "on-fly-cg:false");
				Options.v().setPhaseOption("cg.spark", "string-constants:true");
				break;
			case VTA:
				Options.v().setPhaseOption("cg.spark", "on");
				Options.v().setPhaseOption("cg.spark", "vta:true");
				Options.v().setPhaseOption("cg.spark", "string-constants:true");
				break;
			case SPARK:
				setSparkOptions();
				break;
			case GEOM:
				setSparkOptions();
				setGeomPtaSpecificOptions();
				break;
			case OnDemand:
				// nothing to set here
				break;
			default:
				throw new RuntimeException("Invalid callgraph algorithm");
			}

			// Specify additional options required for the callgraph
			if (config.getCallgraphAlgorithm() != CallgraphAlgorithm.OnDemand) {
				Options.v().set_whole_program(true);
				Options.v().setPhaseOption("cg", "trim-clinit:false");
				if (config.getEnableReflection())
					Options.v().setPhaseOption("cg", "types-for-invoke:true");
			}
		}

		if (config.getSootIntegrationMode().needsToInitializeSoot()) {
			// at the end of setting: load user settings:
			if (sootConfig != null)
				sootConfig.setSootOptions(Options.v(), config);

			// load all entryPoint classes with their bodies
			for (String className : classes)
				Scene.v().addBasicClass(className, SootClass.BODIES);
			Scene.v().loadNecessaryClasses();
			logger.info("Basic class loading done.");

			boolean hasClasses = false;
			for (String className : classes) {
				SootClass c = Scene.v().forceResolve(className, SootClass.BODIES);
				if (c != null) {
					c.setApplicationClass();
					if (!c.isPhantomClass() && !c.isPhantom())
						hasClasses = true;
				}
			}
			if (!hasClasses) {
				logger.error("Only phantom classes loaded, skipping analysis...");
				return;
			}
		}
	}

	protected void setSourcePrec() {
		if (this.androidPath != null) {
			Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
			if (this.forceAndroidJar)
				soot.options.Options.v().set_force_android_jar(this.androidPath.getAbsolutePath());
			else
				soot.options.Options.v().set_android_jars(this.androidPath.getAbsolutePath());
		} else
			Options.v().set_src_prec(Options.src_prec_java);
	}

	private void setChaOptions() {
		Options.v().setPhaseOption("cg.cha", "on");
	}

	private void setSparkOptions() {
		Options.v().setPhaseOption("cg.spark", "on");
		Options.v().setPhaseOption("cg.spark", "string-constants:true");
	}

	public static void setGeomPtaSpecificOptions() {
		Options.v().setPhaseOption("cg.spark", "geom-pta:true");

		// Those are default options, not sure whether removing them works.
		Options.v().setPhaseOption("cg.spark", "geom-encoding:Geom");
		Options.v().setPhaseOption("cg.spark", "geom-worklist:PQ");
	}

	@Override
	public void setSootConfig(IInfoflowConfig config) {
		sootConfig = config;
	}

	@Override
	public void setIPCManager(IIPCManager ipcManager) {
		this.ipcManager = ipcManager;
	}

	@Override
	public void setPathBuilderFactory(IPathBuilderFactory factory) {
		this.pathBuilderFactory = factory;
	}

	/**
	 * Constructs the callgraph
	 */
	protected void constructCallgraph() {
		if (config.getSootIntegrationMode().needsToBuildCallgraph()) {
			// Allow the ICC manager to change the Soot Scene before we continue
			if (ipcManager != null)
				ipcManager.updateJimpleForICC();

			// We might need to patch invokedynamic instructions
			if (config.isPatchInvokeDynamicInstructions())
				patchDynamicInvokeInstructions();

			// Run the preprocessors
			for (PreAnalysisHandler tr : preProcessors)
				tr.onBeforeCallgraphConstruction();

			// Patch the system libraries we need for callgraph construction
			LibraryClassPatcher patcher = getLibraryClassPatcher();
			patcher.patchLibraries();

			hierarchy = Scene.v().getOrMakeFastHierarchy();

			// To cope with broken APK files, we convert all classes that are still
			// dangling after resolution into phantoms
			for (SootClass sc : Scene.v().getClasses())
				if (sc.resolvingLevel() == SootClass.DANGLING) {
					sc.setResolvingLevel(SootClass.BODIES);
					sc.setPhantomClass();
				}

			// We explicitly select the packs we want to run for performance
			// reasons. Do not re-run the callgraph algorithm if the host
			// application already provides us with a CG.
			if (config.getCallgraphAlgorithm() != CallgraphAlgorithm.OnDemand && !Scene.v().hasCallGraph()) {
				PackManager.v().getPack("wjpp").apply();
				PackManager.v().getPack("cg").apply();
			}
		}

		// If we don't have a FastHierarchy, we need to create it - even if we use an
		// existing callgraph
		hierarchy = Scene.v().getOrMakeFastHierarchy();

		if (config.getSootIntegrationMode().needsToBuildCallgraph()) {
			// Run the preprocessors
			for (PreAnalysisHandler tr : preProcessors)
				tr.onAfterCallgraphConstruction();
		}
	}

	/**
	 * Re-writes dynamic invocation instructions into traditional invcations
	 */
	private void patchDynamicInvokeInstructions() {
		for (SootClass sc : Scene.v().getClasses()) {
			for (SootMethod sm : sc.getMethods()) {
				if (sm.hasActiveBody()) {
					Body body = sm.getActiveBody();
					patchDynamicInvokeInstructions(body);
				} else if (!(sm.getSource() instanceof MethodSourceInjector) && sm.getSource() != null) {
					sm.setSource(new MethodSourceInjector(sm.getSource()) {

						@Override
						protected void onMethodSourceLoaded(SootMethod m, Body b) {
							patchDynamicInvokeInstructions(b);
						}

					});
				}
			}
		}
	}

	/**
	 * Patches the dynamic invocation instructions in the given method body
	 * 
	 * @param body The method body in which to patch the dynamic invocation
	 *             instructions
	 */
	protected static void patchDynamicInvokeInstructions(Body body) {
		for (Iterator<Unit> unitIt = body.getUnits().snapshotIterator(); unitIt.hasNext();) {
			Stmt stmt = (Stmt) unitIt.next();
			if (stmt.containsInvokeExpr()) {
				InvokeExpr iexpr = stmt.getInvokeExpr();
				if (iexpr instanceof DynamicInvokeExpr) {
					DynamicInvokeExpr diexpr = (DynamicInvokeExpr) iexpr;
					SootMethodRef bsmRef = diexpr.getBootstrapMethodRef();
					List<Stmt> newStmts = null;
					switch (bsmRef.getDeclaringClass().getName()) {
					case "java.lang.invoke.StringConcatFactory":
						newStmts = patchStringConcatInstruction(stmt, diexpr, body);
					}
					if (newStmts != null && !newStmts.isEmpty())
						body.getUnits().insertAfter(newStmts, stmt);
				}
			}
		}
	}

	/**
	 * Wrapper around a method source to be notified when a new body is loaded
	 * 
	 * @author Steven Arzt
	 *
	 */
	private abstract static class MethodSourceInjector implements MethodSource {

		private MethodSource innerSource;

		public MethodSourceInjector(MethodSource innerSource) {
			this.innerSource = innerSource;
		}

		@Override
		public Body getBody(SootMethod m, String phaseName) {
			Body b = innerSource.getBody(m, phaseName);
			onMethodSourceLoaded(m, b);
			return b;
		}

		protected abstract void onMethodSourceLoaded(SootMethod m, Body b);

	}

	/**
	 * Patches a specific string concatenation instruction
	 * 
	 * @param callSite The call site of the concatenation instruction
	 * @param diexpr   The dynamic invocation instruction
	 * @param b        The body that contains the dynamic invocation instruction
	 * @return The new statements that must be added after the dynamic invocation
	 *         instruction
	 */
	private static List<Stmt> patchStringConcatInstruction(Stmt callSite, DynamicInvokeExpr diexpr, Body b) {
		final String SIG_CONCAT_CONSTANTS = "java.lang.invoke.CallSite makeConcatWithConstants(java.lang.invoke.MethodHandles$Lookup,java.lang.String,java.lang.invoke.MethodType,java.lang.String,java.lang.Object[])";
		final String SIG_CONCAT = "java.lang.invoke.CallSite makeConcatWithConstants(java.lang.invoke.MethodHandles$Lookup,java.lang.String,java.lang.invoke.MethodType)";

		Scene scene = Scene.v();
		Jimple jimple = Jimple.v();
		LocalGenerator lg = new DefaultLocalGenerator(b);

		RefType rtStringBuilder = RefType.v("java.lang.StringBuilder");
		SootClass scStringBuilder = rtStringBuilder.getSootClass();
		SootMethodRef appendObjectRef = scene.makeMethodRef(scStringBuilder,
				"java.lang.StringBuilder append(java.lang.Object)", false);
		SootMethodRef toStringRef = scene.makeMethodRef(scene.getObjectType().getSootClass(),
				"java.lang.String toString()", false);

		SootClass scArrays = Scene.v().getSootClass("java.util.Arrays");

		List<Stmt> newStmts = new ArrayList<>();
		NumberedString calleeSubSig = diexpr.getBootstrapMethodRef().getSubSignature();
		if (calleeSubSig.equals(scene.getSubSigNumberer().findOrAdd(SIG_CONCAT_CONSTANTS))
				|| calleeSubSig.equals(scene.getSubSigNumberer().findOrAdd(SIG_CONCAT))) {
			// We initialize a StringBuilder
			Local sb = lg.generateLocal(rtStringBuilder);

			Stmt stmt = jimple.newAssignStmt(sb, jimple.newNewExpr(rtStringBuilder));
			stmt.addTag(SimulatedCodeElementTag.TAG);
			newStmts.add(stmt);

			stmt = jimple.newInvokeStmt(jimple.newSpecialInvokeExpr(sb,
					scene.makeMethodRef(scStringBuilder, "void <init>(java.lang.String)", false)));
			stmt.addTag(SimulatedCodeElementTag.TAG);
			newStmts.add(stmt);

			// Add all partial strings
			for (int i = 0; i < diexpr.getArgCount(); i++) {
				// Call toString() on the argument
				Value arg = diexpr.getArg(i);
				Type argType = arg.getType();
				SootMethodRef appendRef;
				if (argType instanceof RefType)
					appendRef = appendObjectRef;
				else if (argType instanceof ByteType)
					appendRef = scene.makeMethodRef(scStringBuilder, "java.lang.StringBuilder append(byte)", false);
				else if (argType instanceof BooleanType)
					appendRef = scene.makeMethodRef(scStringBuilder, "java.lang.StringBuilder append(boolean)", false);
				else if (argType instanceof CharType)
					appendRef = scene.makeMethodRef(scStringBuilder, "java.lang.StringBuilder append(char)", false);
				else if (argType instanceof ShortType)
					appendRef = scene.makeMethodRef(scStringBuilder, "java.lang.StringBuilder append(short)", false);
				else if (argType instanceof IntType)
					appendRef = scene.makeMethodRef(scStringBuilder, "java.lang.StringBuilder append(int)", false);
				else if (argType instanceof LongType)
					appendRef = scene.makeMethodRef(scStringBuilder, "java.lang.StringBuilder append(long)", false);
				else if (argType instanceof FloatType)
					appendRef = scene.makeMethodRef(scStringBuilder, "java.lang.StringBuilder append(float)", false);
				else if (argType instanceof DoubleType)
					appendRef = scene.makeMethodRef(scStringBuilder, "java.lang.StringBuilder append(double)", false);
				else if (argType instanceof ArrayType) {
					// For an array argument, we need to Arrays.toString() first
					ArrayType at = (ArrayType) argType;
					Type elementType = at.getElementType();

					Local sarg = lg.generateLocal(RefType.v("java.lang.String"));
					SootMethodRef elementToStringRef = null;
					if (elementType instanceof RefType)
						elementToStringRef = scene.makeMethodRef(scArrays,
								"java.lang.String toString(java.lang.Object[])", true);
					else if (elementType instanceof ByteType)
						elementToStringRef = scene.makeMethodRef(scArrays, "java.lang.String toString(byte[])", true);
					else if (elementType instanceof BooleanType)
						elementToStringRef = scene.makeMethodRef(scArrays, "java.lang.String toString(boolean[])",
								true);
					else if (elementType instanceof CharType)
						elementToStringRef = scene.makeMethodRef(scArrays, "java.lang.String toString(char[])", true);
					else if (elementType instanceof ShortType)
						elementToStringRef = scene.makeMethodRef(scArrays, "java.lang.String toString(short[])", true);
					else if (elementType instanceof IntType)
						elementToStringRef = scene.makeMethodRef(scArrays, "java.lang.String toString(int[])", true);
					else if (elementType instanceof LongType)
						elementToStringRef = scene.makeMethodRef(scArrays, "java.lang.String toString(long[])", true);
					else if (elementType instanceof FloatType)
						elementToStringRef = scene.makeMethodRef(scArrays, "java.lang.String toString(float[])", true);
					else if (elementType instanceof DoubleType)
						elementToStringRef = scene.makeMethodRef(scArrays, "java.lang.String toString(double[])", true);
					else {
						throw new RuntimeException(String.format(
								"Invalid array element type %s for string concatenation in dynamic invocation",
								elementType.toString()));
					}

					Stmt toStringStmt = jimple.newAssignStmt(sarg,
							jimple.newStaticInvokeExpr(elementToStringRef, Collections.singletonList(arg)));
					toStringStmt.addTag(SimulatedCodeElementTag.TAG);
					newStmts.add(toStringStmt);

					arg = sarg;
					appendRef = scStringBuilder.getMethod("java.lang.StringBuilder append(java.lang.String)").makeRef();
				} else {
					throw new RuntimeException(String.format(
							"Invalid type %s for string concatenation in dynamic invocation", argType.toString()));
				}

				stmt = jimple.newInvokeStmt(jimple.newVirtualInvokeExpr(sb, appendRef, Collections.singletonList(arg)));
				stmt.addTag(SimulatedCodeElementTag.TAG);
				newStmts.add(stmt);
			}

			// Obtain the result
			if (callSite instanceof AssignStmt) {
				AssignStmt assignStmt = (AssignStmt) callSite;
				stmt = jimple.newAssignStmt((Local) assignStmt.getLeftOp(),
						jimple.newVirtualInvokeExpr(sb, toStringRef));
				stmt.addTag(SimulatedCodeElementTag.TAG);
				newStmts.add(stmt);
			}
		}
		return newStmts;
	}

	protected LibraryClassPatcher getLibraryClassPatcher() {
		return new LibraryClassPatcher();
	}

	@Override
	public void setMemoryManagerFactory(IMemoryManagerFactory factory) {
		this.memoryManagerFactory = factory;
	}

	@Override
	public void computeInfoflow(String appPath, String libPath, IEntryPointCreator entryPointCreator,
			ISourceSinkManager sourcesSinks) {
		if (sourcesSinks == null) {
			logger.error("SourceSinkManager not specified");
			return;
		}

		if (config.getSootIntegrationMode() != SootIntegrationMode.UseExistingInstance)
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

	@Override
	public void computeInfoflow(String appPath, String libPath, String entryPoint, ISourceSinkManager sourcesSinks) {
		if (sourcesSinks == null) {
			logger.error("No source/sink manager specified");
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
	 * Conducts a taint analysis on an already initialized callgraph
	 * 
	 * @param sourcesSinks The sources and sinks to be used
	 */
	protected void runAnalysis(final ISourceSinkManager sourcesSinks) {
		runAnalysis(sourcesSinks, null);
	}

	/**
	 * Conducts a taint analysis on an already initialized callgraph
	 * 
	 * @param sourcesSinks    The sources and sinks to be used
	 * @param additionalSeeds Additional seeds at which to create A ZERO fact even
	 *                        if they are not sources
	 */
	protected void runAnalysis(final ISourceSinkManager sourcesSinks, final Set<String> additionalSeeds) {
		final InfoflowPerformanceData performanceData = createPerformanceDataClass();
		try {
			// Clear the data from previous runs
			results = createResultsObject();
			results.setPerformanceData(performanceData);

			// Print and check our configuration
			checkAndFixConfiguration();
			config.printSummary();

			// Register a memory watcher
			if (memoryWatcher != null) {
				memoryWatcher.clearSolvers();
				memoryWatcher = null;
			}
			memoryWatcher = new FlowDroidMemoryWatcher(results, config.getMemoryThreshold());

			// Initialize the abstraction configuration
			Abstraction.initialize(config);

			if (taintWrapper != null)
				preProcessors.addAll(taintWrapper.getPreAnalysisHandlers());

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

			IInfoflowCFG iCfg = icfgFactory.buildBiDirICFG(config.getCallgraphAlgorithm(),
					config.getEnableExceptionTracking());

			if (config.isTaintAnalysisEnabled())
				runTaintAnalysis(sourcesSinks, additionalSeeds, iCfg, performanceData);

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
			if (results != null)
				results.addException(ex.getClass().getName() + ": " + ex.getMessage() + "\n" + stacktrace.toString());
			logger.error("Exception during data flow analysis", ex);
			if (throwExceptions)
				throw ex;
		}
	}

	private void runTaintAnalysis(final ISourceSinkManager sourcesSinks, final Set<String> additionalSeeds,
			IInfoflowCFG iCfg, InfoflowPerformanceData performanceData) {
		logger.info("Starting Taint Analysis");

		// Make sure that we have a path builder factory
		if (pathBuilderFactory == null)
			pathBuilderFactory = new DefaultPathBuilderFactory(config.getPathConfiguration());

		// Check whether we need to run with one source at a time
		IOneSourceAtATimeManager oneSourceAtATime = config.getOneSourceAtATime() && sourcesSinks != null
				&& sourcesSinks instanceof IOneSourceAtATimeManager ? (IOneSourceAtATimeManager) sourcesSinks : null;

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
			switch (manager.getConfig().getSolverConfiguration().getDataFlowSolver()) {
			case FineGrainedGC:
				solverPeerGroup = new GCSolverPeerGroup<Pair<SootMethod, Abstraction>>();
				break;
			case GarbageCollecting:
				solverPeerGroup = new GCSolverPeerGroup<SootMethod>();
				break;
			default:
				solverPeerGroup = new DefaultSolverPeerGroup();
				break;
			}

			// Initialize the alias analysis
			Abstraction zeroValue = Abstraction.getZeroAbstraction(manager.getConfig().getFlowSensitiveAliasing());
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
			AbstractInfoflowProblem forwardProblem = createInfoflowProblem(zeroValue);

			// We need to create the right data flow solver
			IInfoflowSolver forwardSolver = createDataFlowSolver(executor, forwardProblem);

			// Set the options
			manager.setMainSolver(forwardSolver);
			if (aliasingStrategy.getSolver() != null)
				aliasingStrategy.getSolver().getTabulationProblem().getManager().setMainSolver(forwardSolver);
			solvers.add(forwardSolver);

			memoryWatcher.addSolver((IMemoryBoundedSolver) forwardSolver);

			forwardSolver.setMemoryManager(memoryManager);
			// forwardSolver.setEnableMergePointChecking(true);

			forwardProblem.setTaintPropagationHandler(taintPropagationHandler);
			forwardProblem.setTaintWrapper(taintWrapper);
			if (nativeCallHandler != null)
				forwardProblem.setNativeCallHandler(nativeCallHandler);

			if (aliasingStrategy.getSolver() != null) {
				aliasingStrategy.getSolver().getTabulationProblem().setActivationUnitsToCallSites(forwardProblem);
				manager.setAliasSolver(aliasingStrategy.getSolver());
			}

			IInfoflowSolver additionalSolver = null;
			IInfoflowSolver additionalAliasSolver = null;
			INativeCallHandler additionalNativeCallHandler = null;
			if (config.getAdditionalFlowsEnabled()) {
				// Add the SecondaryFlowGenerator to the main forward taint analysis
				TaintPropagationHandler forwardHandler = forwardProblem.getTaintPropagationHandler();
				if (forwardHandler != null) {
					if (forwardHandler instanceof SequentialTaintPropagationHandler) {
						((SequentialTaintPropagationHandler) forwardHandler).addHandler(new SecondaryFlowGenerator());
					} else {
						SequentialTaintPropagationHandler seqTpg = new SequentialTaintPropagationHandler();
						seqTpg.addHandler(forwardHandler);
						seqTpg.addHandler(new SecondaryFlowGenerator());
						forwardProblem.setTaintPropagationHandler(seqTpg);
					}
				} else {
					forwardProblem.setTaintPropagationHandler(new SecondaryFlowGenerator());
				}

				if (!(manager.getSourceSinkManager() instanceof IConditionalFlowManager))
					throw new IllegalStateException("Additional Flows enabled but no ConditionalFlowManager in place!");

				// Additional flows get their taints injected dependent on the flow of the taint
				// anlaysis.
				// Thus, we don't have any taints before.
				InfoflowManager additionalManager = new InfoflowManager(config, null, new BackwardsInfoflowCFG(iCfg),
						new ConditionalFlowSourceSinkManagerWrapper(
								(IConditionalFlowManager) manager.getSourceSinkManager()),
						taintWrapper, hierarchy, globalTaintManager);

				AbstractInfoflowProblem additionalProblem = new BackwardsInfoflowProblem(additionalManager, zeroValue,
						reverseRuleManagerFactory);

				additionalSolver = createDataFlowSolver(executor, additionalProblem);
				additionalManager.setMainSolver(additionalSolver);
				additionalSolver.setMemoryManager(memoryManager);
				memoryWatcher.addSolver((IMemoryBoundedSolver) additionalSolver);

				// Set all handlers to the additional problem
				additionalProblem.setTaintPropagationHandler(new SecondaryFlowListener());
				additionalProblem.setTaintWrapper(taintWrapper);
				additionalNativeCallHandler = new BackwardNativeCallHandler();
				additionalProblem.setNativeCallHandler(additionalNativeCallHandler);

				// Initialize the alias analysis
				IAliasingStrategy revereAliasingStrategy = createBackwardAliasAnalysis(additionalManager, sourcesSinks,
						iCfg, executor, memoryManager);
				if (revereAliasingStrategy.getSolver() != null)
					revereAliasingStrategy.getSolver().getTabulationProblem().getManager()
							.setMainSolver(additionalSolver);

				additionalAliasSolver = revereAliasingStrategy.getSolver();

				// Initialize the aliasing infrastructure
				Aliasing reverseAliasing = createAliasController(revereAliasingStrategy);
				if (dummyMainMethod != null)
					reverseAliasing.excludeMethodFromMustAlias(dummyMainMethod);
				additionalManager.setAliasing(reverseAliasing);
				additionalManager.setAliasSolver(additionalAliasSolver);

				manager.additionalManager = additionalManager;

				// Add the post processor if necessary
				if (config.getFilterConditionalSinks())
					addPostProcessor(new ConditionalFlowPostProcessor(manager));

				// If the user did not provide an UsageContextProvider, provide the default
				// implementation
				if (usageContextProvider == null)
					usageContextProvider = new EmptyUsageContextProvider();
				manager.setUsageContextProvider(usageContextProvider);
				additionalManager.setUsageContextProvider(usageContextProvider);
			}

			// Start a thread for enforcing the timeout
			FlowDroidTimeoutWatcher timeoutWatcher = null;
			FlowDroidTimeoutWatcher pathTimeoutWatcher = null;
			if (config.getDataFlowTimeout() > 0) {
				timeoutWatcher = new FlowDroidTimeoutWatcher(config.getDataFlowTimeout(), results);
				timeoutWatcher.addSolver((IMemoryBoundedSolver) forwardSolver);
				if (aliasingStrategy.getSolver() != null)
					timeoutWatcher.addSolver((IMemoryBoundedSolver) aliasingStrategy.getSolver());
				if (additionalSolver != null)
					timeoutWatcher.addSolver((IMemoryBoundedSolver) additionalSolver);
				if (additionalAliasSolver != null)
					timeoutWatcher.addSolver((IMemoryBoundedSolver) additionalAliasSolver);
				timeoutWatcher.start();
			}

			InterruptableExecutor resultExecutor = null;
			long beforePathReconstruction = 0;
			try {
				// Print our configuration
				if (config.getFlowSensitiveAliasing() && !aliasingStrategy.isFlowSensitive())
					logger.warn("Trying to use a flow-sensitive aliasing with an "
							+ "aliasing strategy that does not support this feature");
				if (config.getFlowSensitiveAliasing()
						&& config.getSolverConfiguration().getMaxJoinPointAbstractions() > 0)
					logger.warn("Running with limited join point abstractions can break context-"
							+ "sensitive path builders");

				// We have to look through the complete program to find
				// sources which are then taken as seeds.
				int sinkCount = 0;
				logger.info("Looking for sources and sinks...");

				for (SootMethod sm : getMethodsForSeeds(iCfg))
					sinkCount += scanMethodForSourcesSinks(sourcesSinks, forwardProblem, sm);

				// We optionally also allow additional seeds to be specified
				if (additionalSeeds != null)
					for (String meth : additionalSeeds) {
						SootMethod m = Scene.v().getMethod(meth);
						if (!m.hasActiveBody()) {
							logger.warn("Seed method {} has no active body", m);
							continue;
						}
						forwardProblem.addInitialSeeds(m.getActiveBody().getUnits().getFirst(),
								Collections.singleton(forwardProblem.zeroValue()));
					}

				// Report on the sources and sinks we have found
				if (!forwardProblem.hasInitialSeeds()) {
					logger.error("No sources found, aborting analysis");
					continue;
				}
				if (sinkCount == 0) {
					logger.error("No sinks found, aborting analysis");
					continue;
				}
				logger.info("Source lookup done, found {} sources and {} sinks.",
						forwardProblem.getInitialSeeds().size(), sinkCount);

				// Update the performance statistics
				performanceData.setSourceCount(forwardProblem.getInitialSeeds().size());
				performanceData.setSinkCount(sinkCount);

				// Initialize the taint wrapper if we have one
				if (taintWrapper != null)
					taintWrapper.initialize(manager);
				if (nativeCallHandler != null)
					nativeCallHandler.initialize(manager);
				if (additionalNativeCallHandler != null)
					additionalNativeCallHandler.initialize(manager);

				// Register the handler for interim results
				TaintPropagationResults propagationResults = forwardProblem.getResults();
				resultExecutor = executorFactory.createExecutor(numThreads, false, config);
				resultExecutor.setThreadFactory(new ThreadFactory() {

					@Override
					public Thread newThread(Runnable r) {
						return createNewThread(r);
					}
				});

				// Create the path builder
				final IAbstractionPathBuilder builder = createPathBuilder(resultExecutor);
				// final IAbstractionPathBuilder builder = new
				// DebuggingPathBuilder(pathBuilderFactory, manager);

				// If we want incremental result reporting, we have to
				// initialize it before we start the taint tracking
				if (config.getIncrementalResultReporting())
					initializeIncrementalResultReporting(propagationResults, builder);

				// Initialize the performance data
				if (performanceData.getTaintPropagationSeconds() < 0)
					performanceData.setTaintPropagationSeconds(0);
				long beforeTaintPropagation = System.nanoTime();

				onBeforeTaintPropagation(forwardSolver, backwardSolver);
				forwardSolver.solve();

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
				performanceData.addEdgePropagationCount(forwardSolver.getPropagationCount());
				performanceData.setInfoflowPropagationCount(forwardSolver.getPropagationCount());
				if (backwardSolver != null) {
					performanceData.setAliasPropagationCount(backwardSolver.getPropagationCount());
					performanceData.addEdgePropagationCount(backwardSolver.getPropagationCount());
				}

				// Print taint wrapper statistics
				if (taintWrapper != null) {
					logger.info("Taint wrapper hits: " + taintWrapper.getWrapperHits());
					logger.info("Taint wrapper misses: " + taintWrapper.getWrapperMisses());
				}

				// Give derived classes a chance to do whatever they need before we remove stuff
				// from memory
				onTaintPropagationCompleted(forwardSolver, backwardSolver, additionalSolver, additionalAliasSolver);

				// Get the result abstractions
				Set<AbstractionAtSink> res = propagationResults.getResults();
				propagationResults = null;

				// We need to prune access paths that are entailed by
				// another one
				if (config.getDataFlowDirection() != DataFlowDirection.Backwards)
					removeEntailedAbstractions(res);

				if (config.getAdditionalFlowsEnabled()) {
					res = new HashSet<>(res);
					Set<AbstractionAtSink> additionalRes = manager.additionalManager.getMainSolver()
							.getTabulationProblem().getResults().getResults();
					res.addAll(additionalRes);
				}

				// Shut down the native call handler
				if (nativeCallHandler != null)
					nativeCallHandler.shutdown();
				if (additionalNativeCallHandler != null)
					additionalNativeCallHandler.shutdown();

				if (config.getAdditionalFlowsEnabled()) {
					logger.info(
							"IFDS problem with {} forward, {} backward, {} additional backward and {} additional"
									+ " forward edges, solved in {} seconds, processing {} results...",
							forwardSolver.getPropagationCount(),
							aliasingStrategy.getSolver() == null ? 0
									: aliasingStrategy.getSolver().getPropagationCount(),
							additionalSolver == null ? 0 : additionalSolver.getPropagationCount(),
							additionalAliasSolver == null ? 0 : additionalAliasSolver.getPropagationCount(),
							taintPropagationSeconds, res == null ? 0 : res.size());
				} else {
					logger.info(
							"IFDS problem with {} forward and {} backward edges solved in {} seconds, "
									+ "processing {} results...",
							forwardSolver.getPropagationCount(),
							aliasingStrategy.getSolver() == null ? 0
									: aliasingStrategy.getSolver().getPropagationCount(),
							taintPropagationSeconds, res == null ? 0 : res.size());
				}

				// Update the statistics
				{
					ISolverTerminationReason reason = ((IMemoryBoundedSolver) forwardSolver).getTerminationReason();
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
				logger.info(String.format("Current memory consumption: %d MB", getUsedMemory()));

				if (timeoutWatcher != null)
					timeoutWatcher.stop();
				memoryWatcher.removeSolver((IMemoryBoundedSolver) forwardSolver);
				forwardSolver.cleanup();
				forwardSolver = null;
				forwardProblem = null;

				solverPeerGroup = null;

				// Remove the alias analysis from memory
				aliasing = null;
				if (aliasingStrategy.getSolver() != null) {
					aliasingStrategy.getSolver().terminate();
					memoryWatcher.removeSolver((IMemoryBoundedSolver) aliasingStrategy.getSolver());
				}
				aliasingStrategy.cleanup();
				aliasingStrategy = null;

				if (config.getIncrementalResultReporting())
					res = null;
				iCfg.purge();

				// Clean up possible additional flow things
				if (manager.additionalManager != null) {
					additionalSolver.cleanup();
					memoryWatcher.removeSolver((IMemoryBoundedSolver) additionalSolver);
					additionalSolver = null;

					manager.additionalManager.setAliasing(null);
					additionalAliasSolver.cleanup();
					memoryWatcher.removeSolver((IMemoryBoundedSolver) additionalAliasSolver);
					additionalAliasSolver = null;

					manager.additionalManager.cleanup();
					manager.additionalManager = null;
				}

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
					builder.runIncrementalPathComputation();

					try {
						resultExecutor.awaitCompletion();
					} catch (InterruptedException e) {
						logger.error("Could not wait for executor termination", e);
					}
				} else {
					memoryWatcher.addSolver(builder);
					builder.computeTaintPaths(res);
					res = null;

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

				if (aliasingStrategy != null) {
					IInfoflowSolver solver = aliasingStrategy.getSolver();
					if (solver != null)
						solver.terminate();
				}

				// Do we have any more sources?
				hasMoreSources = oneSourceAtATime != null && oneSourceAtATime.hasNextSource();

				// Shut down the memory watcher
				memoryWatcher.close();

				// Get rid of all the stuff that's still floating around in
				// memory
				forwardProblem = null;
				forwardSolver = null;
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
							if (p != null) {
								logger.info("\t -> " + iCfg.getMethodOf(p));
								int ln = p.getJavaSourceStartLineNumber();
								logger.info("\t\t -> " + p + (ln != -1 ? " in line " + ln : ""));
							}
						}
					}
				}
			}
		}
	}

	protected Thread createNewThread(Runnable r) {
		Thread thrPath = new Thread(r);
		thrPath.setDaemon(true);
		thrPath.setName("FlowDroid Path Reconstruction");
		return thrPath;
	}

	/**
	 * Creates the problem for the IFDS taint propagation problem
	 * 
	 * @param zeroValue The taint abstraction for the tautology
	 * @return The IDFS problem
	 */
	protected abstract AbstractInfoflowProblem createInfoflowProblem(Abstraction zeroValue);

	/**
	 * Creates the reverse problem for the IFDS taint propagation problem
	 * 
	 * @param zeroValue The taint abstraction for the tautology
	 * @return The IDFS problem
	 */
	protected AbstractInfoflowProblem createReverseInfoflowProblem(InfoflowManager manager, Abstraction zeroValue) {
		return null;
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
			InfoflowSolver infoflowSolver = new InfoflowSolver(problem, executor);
			solverPeerGroup.addSolver(infoflowSolver);
			return infoflowSolver;
		case SparseContextFlowSensitive:
			InfoflowConfiguration.SparsePropagationStrategy opt = config.getSolverConfiguration()
					.getSparsePropagationStrategy();
			logger.info(
					"Using sparse context-sensitive and flow-sensitive solver with sparsification " + opt.toString());
			IInfoflowSolver sparseSolver = new SparseInfoflowSolver(problem, executor, opt);
			solverPeerGroup.addSolver(sparseSolver);
			return sparseSolver;
		case FlowInsensitive:
			logger.info("Using context-sensitive, but flow-insensitive solver");
			return new soot.jimple.infoflow.solver.fastSolver.flowInsensitive.InfoflowSolver(problem, executor);
		case GarbageCollecting:
			logger.info("Using garbage-collecting solver");
			IInfoflowSolver solver = new soot.jimple.infoflow.solver.gcSolver.InfoflowSolver(problem, executor,
					solverConfig.getSleepTime());
			solverPeerGroup.addSolver(solver);
			return solver;
		case FineGrainedGC:
			logger.info("Using fine-grained garbage-collecting solver");
			IInfoflowSolver fgSolver = new soot.jimple.infoflow.solver.gcSolver.fpc.InfoflowSolver(problem, executor,
					solverConfig.getSleepTime());
			solverPeerGroup.addSolver(fgSolver);
			return fgSolver;
		default:
			throw new RuntimeException("Unsupported data flow solver");
		}
	}

	protected enum SourceSinkState {
		SOURCE, SINK, NEITHER, BOTH
	}

	protected static class SourceOrSink {
		private final SourceInfo sourceInfo;
		private final SinkInfo sinkInfo;
		private final SourceSinkState state;

		protected SourceOrSink(SourceInfo sourceInfo, SinkInfo sinkInfo) {
			this.sourceInfo = sourceInfo;
			this.sinkInfo = sinkInfo;
			if (sourceInfo != null && sinkInfo == null)
				this.state = SourceSinkState.SOURCE;
			else if (sinkInfo != null && sourceInfo == null)
				this.state = SourceSinkState.SINK;
			else if (sourceInfo != null && sinkInfo != null)
				this.state = SourceSinkState.BOTH;
			else
				this.state = SourceSinkState.NEITHER;
		}

		protected SourceSinkState getState() {
			return state;
		}

		protected SourceInfo getSourceInfo() {
			return sourceInfo;
		}

		protected SinkInfo getSinkInfo() {
			return sinkInfo;
		}

		@Override
		public String toString() {
			switch (state) {
			case SOURCE:
				return "Source";
			case SINK:
				return "Sink";
			case BOTH:
				return "Source and Sink";
			case NEITHER:
				return "Neither";
			default:
				return "Unknown. That's a bad state to be in.";
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(sinkInfo, sourceInfo, state);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SourceOrSink other = (SourceOrSink) obj;
			return Objects.equals(sinkInfo, other.sinkInfo) && Objects.equals(sourceInfo, other.sourceInfo)
					&& state == other.state;
		}
	}

	/**
	 * Checks whether the given source/sink definition is a callback or references
	 * the return value of a method
	 *
	 * @param definitions The source/sink definition to check
	 * @return True if the given source/sink definition references a callback or a
	 *         method return value
	 */
	private boolean isCallbackOrReturn(Collection<ISourceSinkDefinition> definitions) {
		for (ISourceSinkDefinition definition : definitions) {
			if (definition instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) definition;
				MethodSourceSinkDefinition.CallType callType = methodDef.getCallType();
				if (callType == MethodSourceSinkDefinition.CallType.Callback
						|| callType == MethodSourceSinkDefinition.CallType.Return)
					return true;
			}
		}
		return false;
	}

	/**
	 * Scans the given method for sources and sinks contained in it. Sinks are just
	 * counted, sources are added to the InfoflowProblem as seeds.
	 * 
	 * @param sourcesSinks   The SourceSinkManager to be used for identifying
	 *                       sources and sinks
	 * @param forwardProblem The InfoflowProblem in which to register the sources as
	 *                       seeds
	 * @param m              The method to scan for sources and sinks
	 * @return The number of sinks found in this method
	 */
	private int scanMethodForSourcesSinks(final ISourceSinkManager sourcesSinks, AbstractInfoflowProblem forwardProblem,
			SootMethod m) {
		if (getConfig().getLogSourcesAndSinks() && collectedSources == null) {
			collectedSources = new HashSet<>();
			collectedSinks = new HashSet<>();
		}

		int sinkCount = 0;
		if (m.hasActiveBody()) {
			// Check whether this is a system class we need to ignore
			if (!isValidSeedMethod(m))
				return sinkCount;

			// Look for a source in the method. Also look for sinks. If we
			// have no sink in the program, we don't need to perform any
			// analysis
			PatchingChain<Unit> units = m.getActiveBody().getUnits();
			for (Unit u : units) {
				Stmt s = (Stmt) u;
				SourceOrSink sos = scanStmtForSourcesSinks(sourcesSinks, s);
				switch (sos.getState()) {
				case SOURCE:
					if (s.containsInvokeExpr() && !isCallbackOrReturn(sos.getSourceInfo().getAllDefinitions()))
						s.addTag(FlowDroidSourceStatement.INSTANCE);
					forwardProblem.addInitialSeeds(s, Collections.singleton(forwardProblem.zeroValue()));
					if (getConfig().getLogSourcesAndSinks())
						collectedSources.add(s);
					break;
				case SINK:
					if (s.containsInvokeExpr())
						s.addTag(FlowDroidSinkStatement.INSTANCE);
					if (getConfig().getLogSourcesAndSinks())
						collectedSinks.add(s);
					sinkCount++;
					break;
				case BOTH:
					if (s.containsInvokeExpr()) {
						if (!isCallbackOrReturn(sos.getSourceInfo().getAllDefinitions()))
							s.addTag(FlowDroidSourceStatement.INSTANCE);
						s.addTag(FlowDroidSinkStatement.INSTANCE);
					}
					forwardProblem.addInitialSeeds(s, Collections.singleton(forwardProblem.zeroValue()));
					if (getConfig().getLogSourcesAndSinks()) {
						collectedSources.add(s);
						collectedSinks.add(s);
					}
					sinkCount++;
					break;
				case NEITHER:
					break;
				}
			}

		}
		return sinkCount;
	}

	/**
	 * Checks whether the given statement is a source or a sink
	 *
	 * @param sourcesSinks The source/sink manager
	 * @param s            The statement to check
	 * @return An enumeration value that defines whether the given statement is a
	 *         source, a sink, or neither
	 */
	protected abstract SourceOrSink scanStmtForSourcesSinks(final ISourceSinkManager sourcesSinks, Stmt s);

	/**
	 * Given a callgraph, obtains all methods that may contain sources, i.e.,
	 * statements that can serve as seeds for the taint propagation.
	 * 
	 * @param icfg The interprocedural control flow graph
	 * @return The methods that may contain seeds
	 */
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
		if (config.getIgnoreFlowsInSystemPackages() && SystemClassHandler.v().isClassInSystemPackage(className)) {
			if (isUserCodeClass(className)) {
				// Sometimes the namespace used for apps coincides with a system package prefix.
				// isUserCodeClass allows to still mark such methods as user code. To remember
				// this decision
				// without always calling isUserCodeClass (with a possible inefficient string
				// lookup), we do use
				// a tag instead.
				if (!sm.getDeclaringClass().hasTag(FlowDroidUserClass.TAG_NAME))
					sm.getDeclaringClass().addTag(FlowDroidUserClass.v());
				return true;
			} else {
				return false;
			}
		}

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

	/**
	 * Runs all code optimizers
	 * 
	 * @param sourcesSinks The SourceSinkManager
	 */
	protected void eliminateDeadCode(ISourceSinkManager sourcesSinks) {
		InfoflowManager dceManager = new InfoflowManager(config, null,
				icfgFactory.buildBiDirICFG(config.getCallgraphAlgorithm(), config.getEnableExceptionTracking()));

		// Dead code elimination may drop the points-to analysis. We need to restore it.
		final Scene scene = Scene.v();
		PointsToAnalysis pta = scene.getPointsToAnalysis();

		// We need to exclude the dummy main method and all other artificial methods
		// that the entry point creator may have generated as well
		Set<SootMethod> excludedMethods = new HashSet<>();
		if (additionalEntryPointMethods != null)
			excludedMethods.addAll(additionalEntryPointMethods);
		excludedMethods.addAll(Scene.v().getEntryPoints());

		// Allow for additional code instrumentation steps
		performCodeInstrumentationBeforeDCE(dceManager, excludedMethods);

		ICodeOptimizer dce = new DeadCodeEliminator();
		dce.initialize(config);
		dce.run(dceManager, excludedMethods, sourcesSinks, taintWrapper);

		// Restore the points-to analysis. This may restore a PAG that contains outdated
		// methods, but it's still better than running the entire callgraph algorithm
		// again. Continuing with the DumbPointerAnalysis is not a viable solution
		// either, since it heavily over-approximates.
		if (pta != null && !(pta instanceof DumbPointerAnalysis)) {
			PointsToAnalysis newPta = scene.getPointsToAnalysis();
			if (newPta == null || newPta instanceof DumbPointerAnalysis)
				scene.setPointsToAnalysis(pta);
		}

		// Allow for additional code instrumentation steps
		performCodeInstrumentationAfterDCE(dceManager, excludedMethods);
	}

	/**
	 * Allows subclasses to perform additional code instrumentation tasks
	 *
	 * @param dceManager      The manager class for dead code elimination and
	 *                        instrumentation
	 * @param excludedMethods The methods that shall not be modified
	 */
	protected void performCodeInstrumentationBeforeDCE(InfoflowManager dceManager, Set<SootMethod> excludedMethods) {
	}

	/**
	 * Allows subclasses to perform additional code instrumentation tasks
	 * 
	 * @param dceManager      The manager class for dead code elimination and
	 *                        instrumentation
	 * @param excludedMethods The methods that shall not be modified
	 */
	protected void performCodeInstrumentationAfterDCE(InfoflowManager dceManager, Set<SootMethod> excludedMethods) {
	}

	/**
	 * Creates the IFDS solver for the forward data flow problem
	 *
	 * @param executor The executor in which to run the tasks or propagating IFDS
	 *                 edges
	 * @param problem  The implementation of the forward problem
	 * @return The solver that solves the forward taint analysis problem
	 */
	protected IInfoflowSolver createDataFlowSolver(InterruptableExecutor executor, AbstractInfoflowProblem problem) {
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
	protected PredecessorShorteningMode pathConfigToShorteningMode(PathConfiguration pathConfiguration) {
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
	 * Creates the memory manager that helps reduce the memory consumption of the
	 * data flow analysis
	 *
	 * @return The memory manager object
	 */
	protected IMemoryManager<Abstraction, Unit> createMemoryManager() {
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
	 * Releases the callgraph and all intermediate objects associated with it
	 */
	protected void releaseCallgraph() {
		Scene.v().releaseCallGraph();
		Scene.v().releasePointsToAnalysis();
		Scene.v().releaseReachableMethods();
		G.v().resetSpark();
	}

	/**
	 * Checks the configuration of the data flow solver for errors and automatically
	 * fixes some common issues
	 */
	private void checkAndFixConfiguration() {
		final AccessPathConfiguration accessPathConfig = config.getAccessPathConfiguration();
		if (config.getStaticFieldTrackingMode() != StaticFieldTrackingMode.None
				&& accessPathConfig.getAccessPathLength() == 0)
			throw new RuntimeException("Static field tracking must be disabled if the access path length is zero");
		if (config.getSolverConfiguration().getDataFlowSolver() == DataFlowSolver.FlowInsensitive) {
			config.setFlowSensitiveAliasing(false);
			config.setEnableTypeChecking(false);
			logger.warn("Disabled flow-sensitive aliasing because we are running with "
					+ "a flow-insensitive data flow solver");
		}
		if (config.getAdditionalFlowsEnabled()
				&& config.getDataFlowDirection() == InfoflowConfiguration.DataFlowDirection.Backwards)
			throw new RuntimeException(
					"Invalid configuration: the backward direction does not support additional flows");
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

	@Override
	public void setExecutorFactory(IExecutorFactory executorFactory) {
		this.executorFactory = executorFactory;
	}

	@Override
	public void setPropagationRuleManagerFactory(IPropagationRuleManagerFactory ruleManagerFactory) {
		this.ruleManagerFactory = ruleManagerFactory;
	}

	@Override
	public Set<Stmt> getCollectedSources() {
		return this.collectedSources;
	}

	@Override
	public Set<Stmt> getCollectedSinks() {
		return this.collectedSinks;
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
		this.results = createResultsObject();
		propagationResults.addResultAvailableHandler(new OnTaintPropagationResultAdded() {

			@Override
			public boolean onResultAvailable(AbstractionAtSink abs) {
				builder.addResultAvailableHandler(new OnPathBuilderResultAvailable() {

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
	 * Creates a new instance of the result class appropriate for the current data
	 * flow analysis
	 * 
	 * @return The new result object
	 */
	protected abstract InfoflowResults createResultsObject();

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
						&& checkAbs.getAbstraction().localEquals(curAbs.getAbstraction())
						&& checkAbs.getSinkDefinitions().equals(curAbs.getSinkDefinitions())) {
					if (checkAbs.getAbstraction().getAccessPath().entails(curAbs.getAbstraction().getAccessPath())) {
						absAtSinkIt.remove();
						break;
					}
				}
			}
		}
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
	protected abstract IAliasingStrategy createAliasAnalysis(final ISourceSinkManager sourcesSinks, IInfoflowCFG iCfg,
			InterruptableExecutor executor, IMemoryManager<Abstraction, Unit> memoryManager);

	/**
	 * Initializes the alias analysis for the backward direction
	 *
	 * @param manager       The infoflow manager
	 * @param sourcesSinks  The set of sources and sinks
	 * @param iCfg          The interprocedural control flow graph
	 * @param executor      The executor in which to run concurrent tasks
	 * @param memoryManager The memory manager for reducing the memory load during
	 *                      IFDS propagation
	 * @return The backward alias analysis implementation
	 */
	protected IAliasingStrategy createBackwardAliasAnalysis(InfoflowManager manager,
			final ISourceSinkManager sourcesSinks, IInfoflowCFG iCfg, InterruptableExecutor executor,
			IMemoryManager<Abstraction, Unit> memoryManager) {
		IAliasingStrategy aliasingStrategy;
		IInfoflowSolver aliasSolver = null;
		BackwardsAliasProblem aliasProblem = null;
		InfoflowManager aliasManager = null;
		switch (getConfig().getAliasingAlgorithm()) {
		case FlowSensitive:
			// The original icfg is already backwards for the backwards data flow analysis
			aliasManager = new InfoflowManager(config, null, iCfg, sourcesSinks, taintWrapper, hierarchy, manager);
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
			aliasProblem.setTaintPropagationHandler(aliasPropagationHandler);
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
		default:
			throw new RuntimeException("Unsupported aliasing algorithm: " + getConfig().getAliasingAlgorithm());
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

	@Override
	public void addResultsAvailableHandler(ResultsAvailableHandler handler) {
		this.onResultsAvailable.add(handler);
	}

	@Override
	public void setTaintPropagationHandler(TaintPropagationHandler handler) {
		this.taintPropagationHandler = handler;
	}

	@Override
	public void setAliasPropagationHandler(TaintPropagationHandler handler) {
		this.aliasPropagationHandler = handler;
	}

	@Override
	public void removeResultsAvailableHandler(ResultsAvailableHandler handler) {
		onResultsAvailable.remove(handler);
	}

	@Override
	public InfoflowResults getResults() {
		return results;
	}

	@Override
	public boolean isResultAvailable() {
		if (results == null) {
			return false;
		}
		return true;
	}

	@Override
	public void abortAnalysis() {
		ISolverTerminationReason reason = new AbortRequestedReason();

		if (manager != null) {
			// Stop the forward taint analysis
			IInfoflowSolver forwardSolver = manager.getMainSolver();
			if (forwardSolver instanceof IMemoryBoundedSolver) {
				IMemoryBoundedSolver boundedSolver = (IMemoryBoundedSolver) forwardSolver;
				boundedSolver.forceTerminate(reason);
			}

			// Stop the alias analysis
			IInfoflowSolver backwardSolver = manager.getAliasSolver();
			if (backwardSolver instanceof IMemoryBoundedSolver) {
				IMemoryBoundedSolver boundedSolver = (IMemoryBoundedSolver) backwardSolver;
				boundedSolver.forceTerminate(reason);
			}

			if (manager.additionalManager != null) {
				IInfoflowSolver additionalSolver = manager.additionalManager.getMainSolver();
				if (additionalSolver instanceof IMemoryBoundedSolver) {
					IMemoryBoundedSolver boundedSolver = (IMemoryBoundedSolver) additionalSolver;
					boundedSolver.forceTerminate(reason);
				}

				IInfoflowSolver additionalAliasSolver = manager.additionalManager.getAliasSolver();
				if (additionalAliasSolver instanceof IMemoryBoundedSolver) {
					IMemoryBoundedSolver boundedSolver = (IMemoryBoundedSolver) additionalAliasSolver;
					boundedSolver.forceTerminate(reason);
				}
			}
		}

		if (memoryWatcher != null) {
			// Stop all registered solvers
			memoryWatcher.forceTerminate(reason);
		}
	}

	public void setThrowExceptions(boolean b) {
		this.throwExceptions = b;
	}

	/**
	 * Initializes the data flow manager with which propagation rules can interact
	 * with the data flow engine
	 * 
	 * @param sourcesSinks       The source/sink definitions
	 * @param iCfg               The interprocedural control flow graph
	 * @param globalTaintManager The manager object for storing and processing
	 *                           global taints
	 * @return The data flow manager
	 */
	protected abstract InfoflowManager initializeInfoflowManager(final ISourceSinkManager sourcesSinks,
			IInfoflowCFG iCfg, GlobalTaintManager globalTaintManager);

	protected InfoflowManager initializeReverseInfoflowManager(IInfoflowCFG iCfg,
			GlobalTaintManager globalTaintManager) {
		return null;
	}

	/**
	 * Callback that is invoked when the main taint propagation is about to start
	 * 
	 * @param forwardSolver  The forward data flow solver
	 * @param backwardSolver The backward data flow solver
	 */
	protected void onBeforeTaintPropagation(IInfoflowSolver forwardSolver, IInfoflowSolver backwardSolver) {
		//
	}

	/**
	 * Callback that is invoked when the main taint propagation has completed. This
	 * method is called before memory cleanup happens.
	 * 
	 * @param forwardSolver  The forward data flow solver
	 * @param backwardSolver The backward data flow solver
	 */
	protected void onTaintPropagationCompleted(IInfoflowSolver forwardSolver, IInfoflowSolver aliasSolver,
			IInfoflowSolver backwardSolver, IInfoflowSolver backwardAliasSolver) {
		//
	}

	/**
	 * Creates the path builder that shall be used for path reconstruction
	 * 
	 * @param executor The execute in which to run the parallel path reconstruction
	 *                 tasks
	 * @return The path builder implementation
	 */
	protected IAbstractionPathBuilder createPathBuilder(InterruptableExecutor executor) {
		return pathBuilderFactory.createPathBuilder(manager, executor);
	}

	/**
	 * Initializes an appropriate instance of the rule manager factory
	 * 
	 * @return The rule manager factory
	 */
	protected abstract IPropagationRuleManagerFactory initializeRuleManagerFactory();

	/**
	 * Initializes an appropriate instance of the rule manager factory
	 * 
	 * @return The rule manager factory
	 */
	protected IPropagationRuleManagerFactory initializeReverseRuleManagerFactory() {
		return null;
	};

	@Override
	public void setUsageContextProvider(IUsageContextProvider usageContextProvider) {
		this.usageContextProvider = usageContextProvider;
	}
}
