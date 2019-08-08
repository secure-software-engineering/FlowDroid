package soot.jimple.infoflow.methodSummary.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory;
import soot.jimple.infoflow.entryPointCreators.BaseEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.SequentialEntryPointCreator;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.methodSummary.DefaultSummaryConfig;
import soot.jimple.infoflow.methodSummary.data.factory.SourceSinkFactory;
import soot.jimple.infoflow.methodSummary.data.provider.EagerSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.LazySummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.MemorySummaryProvider;
import soot.jimple.infoflow.methodSummary.data.provider.MergingSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.handler.SummaryTaintPropagationHandler;
import soot.jimple.infoflow.methodSummary.postProcessor.InfoflowResultPostProcessor;
import soot.jimple.infoflow.methodSummary.postProcessor.SummaryFlowCompactor;
import soot.jimple.infoflow.methodSummary.source.SummarySourceSinkManager;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.nativeCallHandler.INativeCallHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.taintWrappers.TaintWrapperList;
import soot.options.Options;

/**
 * Class for generating library summaries
 * 
 * @author Malte Viering
 * @author Steven Arzt
 */
public class SummaryGenerator {

	private static final Logger logger = LoggerFactory.getLogger(SummaryGenerator.class);

	public static final String DUMMY_MAIN_SIG = "<dummyMainClass: void dummyMainMethod(java.lang.String[])>";

	protected boolean debug = false;
	protected INativeCallHandler nativeCallHandler;
	protected IInfoflowConfig sootConfig;
	protected SummaryGeneratorConfiguration config = new SummaryGeneratorConfiguration();

	protected List<String> substitutedWith = new LinkedList<String>();

	protected SummaryTaintWrapper fallbackWrapper;
	protected MemorySummaryProvider onFlySummaryProvider = null;

	public SummaryGenerator() {
		try {
			// Do we want to integrate summaries on the fly?
			List<IMethodSummaryProvider> innerProviders = new ArrayList<>();
			if (config.getApplySummariesOnTheFly()) {
				onFlySummaryProvider = new MemorySummaryProvider();
				innerProviders.add(onFlySummaryProvider);
			}

			// We also want the already existing summaries in the output directory
			Set<String> additionalSummaryDirs = config.getAdditionalSummaryDirectories();
			if (additionalSummaryDirs != null && !additionalSummaryDirs.isEmpty()) {
				LazySummaryProvider lazySummaryProvider = new LazySummaryProvider(
						additionalSummaryDirs.stream().map(d -> new File(d)).collect(Collectors.toList()));
				innerProviders.add(lazySummaryProvider);
			}

			// Load the normal JDK summaries
			innerProviders.add(new EagerSummaryProvider(TaintWrapperFactory.DEFAULT_SUMMARY_DIR));

			// Combine our summary providers
			IMethodSummaryProvider provider = new MergingSummaryProvider(innerProviders);
			fallbackWrapper = new SummaryTaintWrapper(provider);
		} catch (Exception e) {
			LoggerFactory.getLogger(getClass()).error(
					"An error occurred while loading the fallback taint wrapper, proceeding without fallback", e);
		}
	}

	/**
	 * Generates the summaries for the given set of classes
	 * 
	 * @param classpath  The classpath from which to load the given classes
	 * @param classNames The classes for which to create summaries
	 * @return The generated method summaries
	 */
	public ClassSummaries createMethodSummaries(String classpath, Collection<String> classNames) {
		return createMethodSummaries(classpath, classNames, null);
	}

	/**
	 * Class that represents a class and its methods for which summaries shall be
	 * generated
	 * 
	 * @author Steven Arzt
	 *
	 */
	private static class ClassAnalysisTask {

		private final String className;
		private final Set<String> methods = new HashSet<>();

		public ClassAnalysisTask(String className) {
			this.className = className;
		}

		/**
		 * Adds a method to analyze
		 * 
		 * @param signature The signature of the method to analyze
		 */
		public void addMethod(String signature) {
			methods.add(signature);
		}

	}

	/**
	 * Comparator to sort analysis tasks by their number of dependencies
	 * 
	 * @author Steven Arzt
	 *
	 */
	private static class AnalysisTasksComparator implements Comparator<ClassAnalysisTask> {

		@Override
		public int compare(ClassAnalysisTask o1, ClassAnalysisTask o2) {
			SootClass sc1 = Scene.v().getSootClassUnsafe(o1.className);
			SootClass sc2 = Scene.v().getSootClassUnsafe(o2.className);
			if (sc1 != null && sc2 != null) {
				int numDeps1 = getDependencyCount(sc1);
				int numDeps2 = getDependencyCount(sc2);
				return numDeps1 - numDeps2;
			}
			return 0;
		}

		/**
		 * Gets the number of other classes on which the given class depends
		 * 
		 * @param sc The class for which to count the dependencies
		 * @return The number of dependencies on other classes of the given class
		 */
		private int getDependencyCount(SootClass sc) {
			Set<SootClass> dependencies = new HashSet<>();
			for (SootMethod sm : sc.getMethods()) {
				if (sm.isConcrete()) {
					for (Unit u : sm.retrieveActiveBody().getUnits()) {
						Stmt stmt = (Stmt) u;
						if (stmt.containsFieldRef()) {
							SootField fld = stmt.getFieldRef().getField();
							if (fld.getDeclaringClass() != sc)
								dependencies.add(fld.getDeclaringClass());
						}
						if (stmt.containsInvokeExpr()) {
							SootMethod callee = stmt.getInvokeExpr().getMethod();
							if (callee.getDeclaringClass() != sc)
								dependencies.add(callee.getDeclaringClass());
						}
					}
				}
			}
			return dependencies.size();
		}

	}

	/**
	 * Generator for adding hierarchy information to the class summary
	 * 
	 * @author Steven Arzt
	 *
	 */
	private static class SummaryHierarchyGenerator implements ResultsAvailableHandler {

		private final ClassMethodSummaries summaries;

		public SummaryHierarchyGenerator(ClassMethodSummaries summaries) {
			this.summaries = summaries;
		}

		@Override
		public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
			SootClass sc = Scene.v().getSootClassUnsafe(summaries.getClassName());
			if (sc != null) {
				if (sc.hasSuperclass())
					summaries.setSuperClass(sc.getSuperclass().getName());
				for (SootClass intf : sc.getInterfaces())
					summaries.addInterface(intf.getName());
			}
		}

	}

	/**
	 * Generates the summaries for the given set of classes
	 * 
	 * @param classpath  The classpath from which to load the given classes
	 * @param classNames The classes for which to create summaries
	 * @param handler    The handler that shall be invoked when all methods inside
	 *                   one class have been summarized
	 * @return The generated method summaries
	 */
	public ClassSummaries createMethodSummaries(String classpath, Collection<String> classNames,
			IClassSummaryHandler handler) {
		G.reset();

		// Check whether we have a wildcard in the target classes
		boolean hasWildcard = false;
		for (String className : classNames) {
			if (className.endsWith(".*")) {
				hasWildcard = true;
				break;
			}
		}

		Options.v().set_output_format(Options.output_format_none);
		if (hasWildcard || config.getLoadFullJAR())
			Options.v().set_process_dir(Arrays.asList(classpath.split(File.pathSeparator)));
		else
			Options.v().set_soot_classpath(classpath);
		Options.v().set_whole_program(false);
		Options.v().set_allow_phantom_refs(true);

		for (String className : classNames) {
			if (!className.endsWith(".*"))
				Scene.v().addBasicClass(className, SootClass.SIGNATURES);
		}
		if (classpath.toLowerCase().endsWith(".apk")) {
			Options.v().set_src_prec(Options.src_prec_apk);
			Options.v().set_process_multiple_dex(true);
			if (config.getAndroidPlatformDir() != null)
				Options.v().set_android_jars(config.getAndroidPlatformDir());
		} else
			Options.v().set_src_prec(Options.src_prec_class);
		Scene.v().loadNecessaryClasses();

		Set<ClassAnalysisTask> realClasses = new HashSet<>(classNames.size());
		if (config.getLoadFullJAR()) {
			for (Iterator<SootClass> scIt = Scene.v().getApplicationClasses().snapshotIterator(); scIt.hasNext();) {
				SootClass sc = scIt.next();
				Scene.v().forceResolve(sc.getName(), SootClass.SIGNATURES);
				if (sc.isConcrete())
					checkAndAdd(realClasses, sc.getName());
			}
		}
		// Resolve placeholder classes
		for (String className : classNames) {
			if (className.endsWith(".*")) {
				String prefix = className.substring(0, className.length() - 1);
				for (Iterator<SootClass> scIt = Scene.v().getClasses().snapshotIterator(); scIt.hasNext();) {
					SootClass sc = scIt.next();
					if (sc.getName().startsWith(prefix)) {
						Scene.v().forceResolve(sc.getName(), SootClass.SIGNATURES);
						if (sc.isConcrete())
							checkAndAdd(realClasses, sc.getName());
					}
				}
			} else {
				SootClass sc = Scene.v().getSootClass(className);
				if (!sc.isConcrete()) {
					// If this is an interface or an abstract class, we
					// take all concrete child classes
					for (String impl : getImplementorsOf(sc))
						checkAndAdd(realClasses, impl);
				} else
					checkAndAdd(realClasses, className);
			}
		}

		// We first process those classes that do not have any further dependencies. The
		// summaries generated for these classes can later be employed when processing
		// the more complex classes.
		List<ClassAnalysisTask> sortedTasks = new ArrayList<>(realClasses);
		if (config.getApplySummariesOnTheFly())
			sortedTasks.sort(new AnalysisTasksComparator());

		// Collect all the public methods in the given classes. We cannot
		// directly start the summary generation as this resets Soot.
		for (ClassAnalysisTask analysisTask : sortedTasks) {
			Set<String> doneMethods = new HashSet<>();
			SootClass sc = Scene.v().getSootClass(analysisTask.className);
			for (SootMethod sm : sc.getMethods()) {
				if (checkAndAdd(analysisTask, sm)) {
					doneMethods.add(sm.getSubSignature());
				}
			}

			// We also need to analyze methods of parent classes except for
			// those methods that have been overwritten in the child class
			SootClass curClass = sc.getSuperclassUnsafe();
			while (curClass != null) {
				if (!curClass.isConcrete() || curClass.isLibraryClass())
					break;

				for (SootMethod sm : curClass.getMethods()) {
					if (checkAndAdd(analysisTask, sm))
						doneMethods.add(sm.getSubSignature());
				}
				curClass = curClass.getSuperclassUnsafe();
			}
		}

		// Make sure that we don't have any strange leftovers
		G.reset();

		// We share one gap manager across all method analyses
		final GapManager gapManager = new GapManager();

		// Do the actual analysis
		ClassSummaries summaries = new ClassSummaries();
		for (ClassAnalysisTask analysisTask : realClasses) {
			final String className = analysisTask.className;

			// Check if we really need to analyze this class
			if (handler != null) {
				if (!handler.onBeforeAnalyzeClass(className)) {
					logger.info(String.format("Skipping over class %s", className));
					continue;
				}
			}

			ClassMethodSummaries curSummaries = null;
			for (int i = 0; i < config.getRepeatCount(); i++) {
				// Clean up the memory so that we don't get any remnants from
				// the last run
				System.gc();
				long nanosBeforeClass = System.nanoTime();
				System.out.println(String.format("Analyzing class %s", className));

				curSummaries = new ClassMethodSummaries(className);
				for (String methodSig : analysisTask.methods) {
					MethodSummaries newSums = createMethodSummary(classpath, methodSig, className, gapManager,
							new SummaryHierarchyGenerator(curSummaries));
					if (handler != null) {
						handler.onMethodFinished(methodSig, newSums);
						if (onFlySummaryProvider != null)
							onFlySummaryProvider.addSummary(new ClassMethodSummaries(className, newSums));
					}
					curSummaries.merge(newSums);

					// Check for timeouts
					if (config.getClassSummaryTimeout() > 0) {
						if ((System.nanoTime() - nanosBeforeClass) / 1E9 > config.getClassSummaryTimeout()) {
							logger.info(String.format(
									"Class summaries for %s aborted after %.2f seconds. Still got %d summaries.",
									className, (System.nanoTime() - nanosBeforeClass) / 1E9,
									curSummaries.getFlowCount()));
							break;
						}
					}
				}

				logger.info(String.format("Class summaries for %s done in %.2f seconds for %d summaries", className,
						(System.nanoTime() - nanosBeforeClass) / 1E9, curSummaries.getFlowCount()));
			}

			// Notify the handler that we're done
			if (handler != null)
				handler.onClassFinished(curSummaries);
			summaries.merge(curSummaries);

			// Remove duplicate summaries on alias flows. We need to re-do this
			// as we might have created new duplicates during the merge.
			new SummaryFlowCompactor(curSummaries.getMethodSummaries()).compact();
		}

		// Calculate the dependencies
		calculateDependencies(summaries);

		return summaries;
	}

	/**
	 * Checks whether the given method shall be included in summary generation. If
	 * so, it is added to the analysis task
	 * 
	 * @param analysisTask The analysis task to which to add the method
	 * @param sm           The method to analyze
	 * @return True if the method is to be analyzed, false otherwise
	 */
	private boolean checkAndAdd(ClassAnalysisTask analysisTask, SootMethod sm) {
		if (!sm.isConcrete())
			return false;
		if (!sm.isPublic() && !sm.isProtected())
			return false;

		// We normally don't analyze hashCode() and equals()
		final String sig = sm.getSignature();
		if (!config.getSummarizeHashCodeEquals()) {
			if (sig.equals("int hashCode()") || sig.equals("boolean equals(java.lang.Object)"))
				return false;
		}

		analysisTask.addMethod(sig);
		return true;
	}

	/**
	 * Checks whether the given class is to be included in the summary generation.
	 * If so, it is added to the set of classes to be analyzed
	 * 
	 * @param classes   The set of classes to be analyzed
	 * @param className The class to check
	 */
	private void checkAndAdd(Set<ClassAnalysisTask> classes, String className) {
		if (config.getExcludes() != null)
			for (String excl : config.getExcludes()) {
				if (excl.equals(className))
					return;
				if (excl.endsWith(".*")) {
					String baseName = excl.substring(0, excl.length() - 1);
					if (className.startsWith(baseName))
						return;
				}
			}
		classes.add(new ClassAnalysisTask(className));
	}

	/**
	 * Gets all classes that are sub-classes of the given class / implementors of
	 * the given interface
	 * 
	 * @param sc The class or interface of which to get the implementors
	 * @return The concrete implementors of the given interface / subclasses of the
	 *         given parent class
	 */
	private Collection<? extends String> getImplementorsOf(SootClass sc) {
		Set<String> classes = new HashSet<>();
		Set<SootClass> doneSet = new HashSet<>();
		List<SootClass> workList = new ArrayList<>();
		workList.add(sc);

		while (!workList.isEmpty()) {
			SootClass curClass = workList.remove(0);
			if (!doneSet.add(curClass))
				continue;
			if (curClass.isConcrete())
				classes.add(curClass.getName());

			if (sc.isInterface()) {
				workList.addAll(Scene.v().getActiveHierarchy().getImplementersOf(sc));
				workList.addAll(Scene.v().getActiveHierarchy().getSubinterfacesOf(sc));
			} else
				for (SootClass c : Scene.v().getActiveHierarchy().getSubclassesOf(sc))
					classes.add(c.getName());
		}
		return classes;
	}

	/**
	 * Calculates the external dependencies of the given summary set
	 * 
	 * @param summaries The summary set for which to calculate the dependencies
	 */
	private void calculateDependencies(ClassSummaries summaries) {
		for (MethodFlow flow : summaries.getAllFlows()) {
			if (flow.source().hasAccessPath())
				for (String apElement : flow.source().getAccessPath()) {
					String className = getTypeFromFieldDef(apElement);
					if (!summaries.hasSummariesForClass(className))
						summaries.addDependency(className);
				}
			if (flow.sink().hasAccessPath())
				for (String apElement : flow.sink().getAccessPath()) {
					String className = getTypeFromFieldDef(apElement);
					if (!summaries.hasSummariesForClass(className))
						summaries.addDependency(className);
				}
		}
	}

	/**
	 * Gets the name of the field type from a field definition
	 * 
	 * @param apElement The field definition to parse
	 * @return The type name in the field definition
	 */
	private String getTypeFromFieldDef(String apElement) {
		apElement = apElement.substring(apElement.indexOf(":") + 1).trim();
		apElement = apElement.substring(0, apElement.indexOf(" "));

		// If this is an array, we drop the parantheses
		while (apElement.endsWith("[]"))
			apElement = apElement.substring(0, apElement.length() - 2);
		return apElement;
	}

	/**
	 * Creates a method summary for the method m
	 * 
	 * It is assumed that only the default constructor of c is executed before m is
	 * called.
	 * 
	 * The result of that assumption is that some fields of c may be null. A null
	 * field is not identified as a source and there for will not create a Field ->
	 * X flow.
	 * 
	 * @param classpath The classpath containing the classes to summarize
	 * @param methodSig method for which a summary will be created
	 * @return summary of method m
	 */
	public MethodSummaries createMethodSummary(String classpath, String methodSig) {
		return createMethodSummary(classpath, methodSig, "", new GapManager());
	}

	/**
	 * Creates a method summary for the method m.
	 * 
	 * It is assumed that all method in mDependencies and the default constructor of
	 * c is executed before m is executed.
	 * 
	 * That allows e.g. to call a setter before a getter method is analyzed and
	 * there for the getter field is not null.
	 * 
	 * @param classpath   The classpath containing the classes to summarize
	 * @param methodSig   method for which a summary will be created
	 * @param parentClass The parent class on which the method to be analyzed shall
	 *                    be invoked
	 * @param gapManager  The gap manager to be used for creating new gaps
	 * @return summary of method m
	 */
	private MethodSummaries createMethodSummary(String classpath, final String methodSig, final String parentClass,
			final GapManager gapManager) {
		return createMethodSummary(classpath, methodSig, parentClass, gapManager, null);
	}

	/**
	 * Creates a method summary for the method m.
	 * 
	 * It is assumed that all method in mDependencies and the default constructor of
	 * c is executed before m is executed.
	 * 
	 * That allows e.g. to call a setter before a getter method is analyzed and
	 * there for the getter field is not null.
	 * 
	 * @param classpath     The classpath containing the classes to summarize
	 * @param methodSig     method for which a summary will be created
	 * @param parentClass   The parent class on which the method to be analyzed
	 *                      shall be invoked
	 * @param gapManager    The gap manager to be used for creating new gaps
	 * @param resultHandler Optional handler for conducting additional tasks at the
	 *                      end (but in the context) of the data flow analysis
	 * @return summary of method m
	 */
	private MethodSummaries createMethodSummary(String classpath, final String methodSig, final String parentClass,
			final GapManager gapManager, final ResultsAvailableHandler resultHandler) {
		logger.info(String.format("Computing method summary for %s...", methodSig));
		long nanosBeforeMethod = System.nanoTime();

		final SourceSinkFactory sourceSinkFactory = new SourceSinkFactory(
				config.getAccessPathConfiguration().getAccessPathLength());
		final SummarySourceSinkManager sourceSinkManager = new SummarySourceSinkManager(methodSig, parentClass,
				sourceSinkFactory);
		final MethodSummaries summaries = new MethodSummaries();

		final SummaryInfoflow infoflow = initInfoflow(summaries, gapManager);

		final SummaryTaintPropagationHandler listener = new SummaryTaintPropagationHandler(methodSig, parentClass,
				gapManager);
		infoflow.setTaintPropagationHandler(listener);
		infoflow.setPreProcessors(Collections.singleton(new PreAnalysisHandler() {

			@Override
			public void onBeforeCallgraphConstruction() {
			}

			@Override
			public void onAfterCallgraphConstruction() {
				listener.addExcludedMethod(Scene.v().getMethod(DUMMY_MAIN_SIG));
			}

		}));

		infoflow.addResultsAvailableHandler(new ResultsAvailableHandler() {

			@Override
			public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
				InfoflowResultPostProcessor processor;
				if (infoflow.getManager() != null)
					processor = new InfoflowResultPostProcessor(listener.getResult(), infoflow.getManager(), methodSig,
							sourceSinkFactory, gapManager);
				else
					processor = new InfoflowResultPostProcessor(listener.getResult(), infoflow.getConfig(), methodSig,
							sourceSinkFactory, gapManager);
				processor.postProcess(summaries);

				if (resultHandler != null)
					resultHandler.onResultsAvailable(cfg, results);
			}

		});

		try {
			infoflow.computeInfoflow(null, classpath,
					createEntryPoint(Collections.singletonList(methodSig), parentClass), sourceSinkManager);
		} catch (Exception e) {
			logger.error(String.format("Could not generate summary for method %s", methodSig, e));
			throw e;
		}

		logger.info("Method summary for " + methodSig + " done in " + (System.nanoTime() - nanosBeforeMethod) / 1E9
				+ " seconds");
		return summaries;
	}

	private BaseEntryPointCreator createEntryPoint(Collection<String> entryPoints, String parentClass) {
		SequentialEntryPointCreator dEntryPointCreater = new SequentialEntryPointCreator(entryPoints);

		List<String> substClasses = new ArrayList<String>(substitutedWith);
		if (parentClass != null && !parentClass.isEmpty())
			substClasses.add(parentClass);
		dEntryPointCreater.setSubstituteClasses(substClasses);
		dEntryPointCreater.setSubstituteCallParams(true);
		dEntryPointCreater.setIgnoreSystemClassParams(false);

		return dEntryPointCreater;
	}

	/**
	 * Creates a new instance of the Infoflow class which will then be used for
	 * computing summaries.
	 * 
	 * @return The newly constructed Infoflow instance
	 */
	protected SummaryInfoflow getInfoflowInstance() {
		SummaryInfoflow infoflow = new SummaryInfoflow();
		infoflow.setPathBuilderFactory(new DefaultPathBuilderFactory(config.getPathConfiguration()) {

			@Override
			public boolean supportsPathReconstruction() {
				return true;
			}

		});
		return infoflow;
	}

	/**
	 * Initializes the taint wrapper to be used for constructing gaps during summary
	 * generation
	 * 
	 * @param summaries  The summary data object to receive the flows
	 * @param gapManager The gap manager to be used when handling callbacks
	 * @return The taint wrapper to be used during summary generation
	 */
	protected SummaryGenerationTaintWrapper createTaintWrapper(MethodSummaries summaries, GapManager gapManager) {
		return new SummaryGenerationTaintWrapper(summaries, gapManager);
	}

	/**
	 * Initializes the data flow tracker
	 * 
	 * @param summaries  The summary data object to receive the flows
	 * @param gapManager The gap manager to be used when handling callbacks
	 * @return The initialized data flow engine
	 */
	protected SummaryInfoflow initInfoflow(MethodSummaries summaries, GapManager gapManager) {
		// Disable the default path reconstruction. However, still make sure to
		// retain the contents of the callees.
		SummaryInfoflow iFlow = getInfoflowInstance();
		InfoflowConfiguration.setMergeNeighbors(true);
		iFlow.setConfig(config);

		if (nativeCallHandler == null)
			iFlow.setNativeCallHandler(new SummaryNativeCallHandler());
		else
			iFlow.setNativeCallHandler(new SummaryNativeCallHandler(nativeCallHandler));

		final SummaryGenerationTaintWrapper summaryWrapper = createTaintWrapper(summaries, gapManager);
		iFlow.setTaintWrapper(new TaintWrapperList(fallbackWrapper, summaryWrapper));

		// Set the Soot configuration
		if (sootConfig == null)
			iFlow.setSootConfig(new DefaultSummaryConfig());
		else
			iFlow.setSootConfig(sootConfig);

		return iFlow;
	}

	public void setNativeCallHandler(INativeCallHandler nativeCallHandler) {
		this.nativeCallHandler = nativeCallHandler;
	}

	public void setSootConfig(IInfoflowConfig config) {
		this.sootConfig = config;
	}

	public List<String> getSubstitutedWith() {
		return substitutedWith;
	}

	public void setSubstitutedWith(List<String> substitutedWith) {
		this.substitutedWith = substitutedWith;
	}

	/**
	 * Gets the configuration for this summary generator
	 * 
	 * @return The current configuration for this summary generator
	 */
	public SummaryGeneratorConfiguration getConfig() {
		return config;
	}

	/**
	 * Sets the configuration object to be used when generating summaries
	 * 
	 * @param config The configuration object to be used when generating summaries
	 */
	public void setConfig(SummaryGeneratorConfiguration config) {
		this.config = config;
	}

}
