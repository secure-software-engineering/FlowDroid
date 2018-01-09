package soot.jimple.infoflow.methodSummary.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory;
import soot.jimple.infoflow.entryPointCreators.BaseEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.SequentialEntryPointCreator;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.methodSummary.DefaultSummaryConfig;
import soot.jimple.infoflow.methodSummary.data.factory.SourceSinkFactory;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.handler.SummaryTaintPropagationHandler;
import soot.jimple.infoflow.methodSummary.postProcessor.InfoflowResultPostProcessor;
import soot.jimple.infoflow.methodSummary.postProcessor.SummaryFlowCompactor;
import soot.jimple.infoflow.methodSummary.source.SummarySourceSinkManager;
import soot.jimple.infoflow.nativ.INativeCallHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.options.Options;

/**
 * Class for generating library summaries
 * 
 * @author Malte Viering
 * @author Steven Arzt
 */
public class SummaryGenerator {

	public static final String DUMMY_MAIN_SIG = "<dummyMainClass: void dummyMainMethod(java.lang.String[])>";

	protected boolean debug = false;
	protected InfoflowManager manager;
	protected ITaintPropagationWrapper taintWrapper;
	protected INativeCallHandler nativeCallHandler;
	protected IInfoflowConfig sootConfig;
	protected SummaryGeneratorConfiguration config = new SummaryGeneratorConfiguration();

	protected List<String> substitutedWith = new LinkedList<String>();

	public SummaryGenerator() {
		//
	}

	/**
	 * Generates the summaries for the given set of classes
	 * 
	 * @param classpath
	 *            The classpath from which to load the given classes
	 * @param classNames
	 *            The classes for which to create summaries
	 * @return The generated method summaries
	 */
	public ClassSummaries createMethodSummaries(String classpath, Collection<String> classNames) {
		return createMethodSummaries(classpath, classNames, null);
	}

	/**
	 * Generates the summaries for the given set of classes
	 * 
	 * @param classpath
	 *            The classpath from which to load the given classes
	 * @param classNames
	 *            The classes for which to create summaries
	 * @param handler
	 *            The handler that shall be invoked when all methods inside one
	 *            class have been summarized
	 * @return The generated method summaries
	 */
	public ClassSummaries createMethodSummaries(String classpath, Collection<String> classNames,
			IClassSummaryHandler handler) {
		G.reset();

		// Check whether we have a wildcard in the target classes
		boolean hasWildcard = false;
		for (String className : classNames)
			if (className.endsWith(".*")) {
				hasWildcard = true;
				break;
			}

		Options.v().set_src_prec(Options.src_prec_class);
		Options.v().set_output_format(Options.output_format_none);
		if (hasWildcard || config.getLoadFullJAR())
			Options.v().set_process_dir(Arrays.asList(classpath.split(File.pathSeparator)));
		else
			Options.v().set_soot_classpath(classpath);
		Options.v().set_whole_program(false);
		Options.v().set_allow_phantom_refs(true);

		for (String className : classNames)
			if (!className.endsWith(".*"))
				Scene.v().addBasicClass(className, SootClass.SIGNATURES);
		Scene.v().loadNecessaryClasses();

		// Resolve placeholder classes
		Set<String> realClasses = new HashSet<>(classNames.size());
		for (String className : classNames)
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

		// Collect all the public methods in the given classes. We cannot
		// directly start the summary generation as this resets Soot.
		Map<String, Collection<String>> methodsToAnalyze = new HashMap<>();
		for (String className : realClasses) {
			Collection<String> methods = new ArrayList<>();
			methodsToAnalyze.put(className, methods);

			Set<String> doneMethods = new HashSet<>();
			SootClass sc = Scene.v().getSootClass(className);
			for (SootMethod sm : sc.getMethods())
				if (sm.isPublic() || sm.isProtected()) {
					methods.add(sm.getSignature());
					doneMethods.add(sm.getSubSignature());
				}

			// We also need to analyze methods of parent classes except for
			// those methods that have been overwritten in the child class
			SootClass curClass = sc;
			while (curClass.hasSuperclass()) {
				curClass = curClass.getSuperclass();
				for (SootMethod sm : curClass.getMethods())
					if (sm.isConcrete() && (sm.isPublic() || sm.isProtected()))
						if (doneMethods.add(sm.getSubSignature()))
							methods.add(sm.getSignature());
			}
		}

		// Make sure that we don't have any strange leftovers
		G.reset();

		// We share one gap manager across all method analyses
		final GapManager gapManager = new GapManager();

		// Do the actual analysis
		ClassSummaries summaries = new ClassSummaries();
		for (Entry<String, Collection<String>> entry : methodsToAnalyze.entrySet()) {
			// Check if we really need to analyze this class
			if (handler != null)
				if (!handler.onBeforeAnalyzeClass(entry.getKey())) {
					System.out.println("Skipping over class " + entry.getKey());
					continue;
				}

			MethodSummaries curSummaries = null;
			for (int i = 0; i < config.getRepeatCount(); i++) {
				// Clean up the memory so that we don't get any remnants from
				// the last run
				System.gc();
				long nanosBeforeClass = System.nanoTime();
				System.out.println("Analyzing class " + entry.getKey());

				curSummaries = new MethodSummaries();
				for (String methodSig : entry.getValue()) {
					MethodSummaries newSums = createMethodSummary(classpath, methodSig, entry.getKey(), gapManager);
					if (handler != null)
						handler.onMethodFinished(methodSig, curSummaries);
					curSummaries.merge(newSums);
				}

				System.out.println("Class summaries for " + entry.getKey() + " done in "
						+ (System.nanoTime() - nanosBeforeClass) / 1E9 + " seconds for " + curSummaries.getFlowCount()
						+ " summaries");
			}

			// Notify the handler that we're done
			if (handler != null)
				handler.onClassFinished(entry.getKey(), curSummaries);
			summaries.merge(entry.getKey(), curSummaries);

			// Remove duplicate summaries on alias flows. We need to re-do this
			// as we might have created new duplicates during the merge.
			new SummaryFlowCompactor(curSummaries).compact();
		}

		// Calculate the dependencies
		calculateDependencies(summaries);

		return summaries;
	}

	/**
	 * Checks whether the given method is to be included in the summary
	 * generation. If so, it is added to the set of classes to be analyzed
	 * 
	 * @param classes
	 *            The set of classes to be analyzed
	 * @param className
	 *            The class to check
	 */
	private void checkAndAdd(Set<String> classes, String className) {
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
		classes.add(className);
	}

	/**
	 * Gets all classes that are sub-classes of the given class / implementors
	 * of the given interface
	 * 
	 * @param sc
	 *            The class or interface of which to get the implementors
	 * @return The concrete implementors of the given interface / subclasses of
	 *         the given parent class
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
	 * @param summaries
	 *            The summary set for which to calculate the dependencies
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
	 * @param apElement
	 *            The field definition to parse
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
	 * It is assumed that only the default constructor of c is executed before m
	 * is called.
	 * 
	 * The result of that assumption is that some fields of c may be null. A
	 * null field is not identified as a source and there for will not create a
	 * Field -> X flow.
	 * 
	 * @param classpath
	 *            The classpath containing the classes to summarize
	 * @param methodSig
	 *            method for which a summary will be created
	 * @return summary of method m
	 */
	public MethodSummaries createMethodSummary(String classpath, String methodSig) {
		return createMethodSummary(classpath, methodSig, "", new GapManager());
	}

	/**
	 * Creates a method summary for the method m.
	 * 
	 * It is assumed that all method in mDependencies and the default
	 * constructor of c is executed before m is executed.
	 * 
	 * That allows e.g. to call a setter before a getter method is analyzed and
	 * there for the getter field is not null.
	 * 
	 * @param classpath
	 *            The classpath containing the classes to summarize
	 * @param methodSig
	 *            method for which a summary will be created
	 * @param parentClass
	 *            The parent class on which the method to be analyzed shall be
	 *            invoked
	 * @param gapManager
	 *            The gap manager to be used for creating new gaps
	 * @return summary of method m
	 */
	private MethodSummaries createMethodSummary(String classpath, final String methodSig, final String parentClass,
			final GapManager gapManager) {
		System.out.println("Computing method summary for " + methodSig);
		long nanosBeforeMethod = System.nanoTime();

		final SourceSinkFactory sourceSinkFactory = new SourceSinkFactory(config.getAccessPathLength());
		final SummarySourceSinkManager sourceSinkManager = new SummarySourceSinkManager(methodSig, parentClass,
				sourceSinkFactory);
		final MethodSummaries summaries = new MethodSummaries();

		final Infoflow infoflow = initInfoflow(summaries, gapManager);

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
				InfoflowResultPostProcessor processor = new InfoflowResultPostProcessor(listener.getResult(), manager,
						methodSig, sourceSinkFactory, gapManager);
				processor.postProcess(summaries);
			}
		});

		try {
			infoflow.computeInfoflow(null, classpath,
					createEntryPoint(Collections.singletonList(methodSig), parentClass), sourceSinkManager);
		} catch (Exception e) {
			System.err.println("Could not generate summary for method " + methodSig);
			e.printStackTrace();
			throw e;
		}

		System.out.println("Method summary for " + methodSig + " done in "
				+ (System.nanoTime() - nanosBeforeMethod) / 1E9 + " seconds");
		return summaries;
	}

	private BaseEntryPointCreator createEntryPoint(Collection<String> entryPoints, String parentClass) {
		SequentialEntryPointCreator dEntryPointCreater = new SequentialEntryPointCreator(entryPoints);

		List<String> substClasses = new ArrayList<String>(substitutedWith);
		if (parentClass != null && !parentClass.isEmpty())
			substClasses.add(parentClass);
		dEntryPointCreater.setSubstituteClasses(substClasses);
		dEntryPointCreater.setSubstituteCallParams(true);

		return dEntryPointCreater;
	}

	/**
	 * Creates a new instance of the Infoflow class which will then be used for
	 * computing summaries.
	 * 
	 * @return The newly constructed Infoflow instance
	 */
	protected Infoflow getInfoflowInstance() {
		Infoflow infoflow = new Infoflow("", false);
		infoflow.setPathBuilderFactory(new DefaultPathBuilderFactory(config.getPathConfiguration()) {

			@Override
			public boolean supportsPathReconstruction() {
				return true;
			}

		});
		return infoflow;
	}

	/**
	 * Initializes the taint wrapper to be used for constructing gaps during
	 * summary generation
	 * 
	 * @param summaries
	 *            The summary data object to receive the flows
	 * @param gapManager
	 *            The gap manager to be used when handling callbacks
	 * @return The taint wrapper to be used during summary generation
	 */
	protected SummaryGenerationTaintWrapper createTaintWrapper(MethodSummaries summaries, GapManager gapManager) {
		return new SummaryGenerationTaintWrapper(summaries, gapManager);
	}

	/**
	 * Initializes the data flow tracker
	 * 
	 * @param summaries
	 *            The summary data object to receive the flows
	 * @param gapManager
	 *            The gap manager to be used when handling callbacks
	 * @return The initialized data flow engine
	 */
	protected Infoflow initInfoflow(MethodSummaries summaries, GapManager gapManager) {
		// Disable the default path reconstruction. However, still make sure to
		// retain the contents of the callees.
		Infoflow iFlow = getInfoflowInstance();
		InfoflowConfiguration.setMergeNeighbors(true);
		iFlow.setConfig(config);

		if (nativeCallHandler == null)
			iFlow.setNativeCallHandler(new SummaryNativeCallHandler());
		else
			iFlow.setNativeCallHandler(new SummaryNativeCallHandler(nativeCallHandler));

		final SummaryGenerationTaintWrapper summaryWrapper = createTaintWrapper(summaries, gapManager);
		if (taintWrapper == null)
			iFlow.setTaintWrapper(summaryWrapper);
		else {
			ITaintPropagationWrapper wrapper = new ITaintPropagationWrapper() {

				@Override
				public void initialize(InfoflowManager manager) {

				}

				@Override
				public Set<Abstraction> getTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
					Set<Abstraction> taints = taintWrapper.getTaintsForMethod(stmt, d1, taintedPath);
					if (taints != null && !taints.isEmpty())
						return taints;

					return summaryWrapper.getTaintsForMethod(stmt, d1, taintedPath);
				}

				@Override
				public boolean isExclusive(Stmt stmt, Abstraction taintedPath) {
					return taintWrapper.isExclusive(stmt, taintedPath) || summaryWrapper.isExclusive(stmt, taintedPath);
				}

				@Override
				public boolean supportsCallee(SootMethod method) {
					return taintWrapper.supportsCallee(method) || summaryWrapper.supportsCallee(method);
				}

				@Override
				public boolean supportsCallee(Stmt callSite) {
					return taintWrapper.supportsCallee(callSite) || summaryWrapper.supportsCallee(callSite);
				}

				@Override
				public int getWrapperHits() {
					// Statistics are not supported by this taint wrapper
					return -1;
				}

				@Override
				public int getWrapperMisses() {
					// Statistics are not supported by this taint wrapper
					return -1;
				}

				@Override
				public Set<Abstraction> getAliasesForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
					Set<Abstraction> absSet = taintWrapper.getAliasesForMethod(stmt, d1, taintedPath);
					if (absSet != null && !absSet.isEmpty())
						return absSet;

					return taintWrapper.getAliasesForMethod(stmt, d1, taintedPath);
				}

			};
			iFlow.setTaintWrapper(wrapper);
		}

		// Set the Soot configuration
		if (sootConfig == null)
			iFlow.setSootConfig(new DefaultSummaryConfig());
		else
			iFlow.setSootConfig(sootConfig);

		return iFlow;
	}

	public void setTaintWrapper(ITaintPropagationWrapper taintWrapper) {
		this.taintWrapper = taintWrapper;
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
	 * @param config
	 *            The configuration object to be used when generating summaries
	 */
	public void setConfig(SummaryGeneratorConfiguration config) {
		this.config = config;
	}

}
