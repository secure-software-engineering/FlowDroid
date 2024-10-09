package soot.jimple.infoflow.sourcesSinks.manager;

import static soot.SootClass.DANGLING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import heros.solver.IDESolver;
import heros.solver.Pair;
import soot.FastHierarchy;
import soot.Hierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.CallbackSourceMode;
import soot.jimple.infoflow.InfoflowConfiguration.SourceSinkConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.callbacks.CallbackDefinition;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.jimple.infoflow.river.IConditionalFlowManager;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.FieldSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkCondition;
import soot.jimple.infoflow.sourcesSinks.definitions.StatementSourceSinkDefinition;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.infoflow.values.IValueProvider;
import soot.jimple.infoflow.values.SimpleConstantValueProvider;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

public abstract class BaseSourceSinkManager
		implements IReversibleSourceSinkManager, IOneSourceAtATimeManager, IConditionalFlowManager {
	private final static String GLOBAL_SIG = "--GLOBAL--";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Types of sources supported by this SourceSinkManager
	 *
	 * @author Steven Arzt
	 */
	public static enum SourceType {
		/**
		 * Not a source
		 */
		NoSource,
		/**
		 * The data is obtained via a method call
		 */
		MethodCall,
		/**
		 * The data is retrieved through a callback parameter
		 */
		Callback,
		/**
		 * The data is read from a UI element
		 */
		UISource
	}

	protected MultiMap<String, ISourceSinkDefinition> sourceDefs;
	protected MultiMap<String, ISourceSinkDefinition> sinkDefs;

	protected MultiMap<SootMethod, ISourceSinkDefinition> sourceMethods;
	protected Map<SootMethod, ISourceSinkDefinition> sourceCallbackMethods;
	protected MultiMap<Stmt, ISourceSinkDefinition> sourceStatements;
	protected MultiMap<SootMethod, ISourceSinkDefinition> sinkMethods;
	protected MultiMap<SootMethod, ISourceSinkDefinition> sinkReturnMethods;
	protected Map<SootMethod, CallbackDefinition> callbackMethods;
	protected MultiMap<SootField, ISourceSinkDefinition> sourceFields;
	protected MultiMap<SootField, ISourceSinkDefinition> sinkFields;
	protected MultiMap<Stmt, ISourceSinkDefinition> sinkStatements;

	protected Set<SootMethod> conditionalSinks = new HashSet<>();
	protected MultiMap<SootMethod, SootClass> conditionalSinkToExcludedClasses = new HashMultiMap<>();
	protected Set<SootMethod> secondarySinkMethods = new HashSet<>();
	protected Set<SootClass> secondarySinkClasses = new HashSet<>();

	protected final SourceSinkConfiguration sourceSinkConfig;

	protected final Set<SootMethod> excludedMethods = new HashSet<>();

	protected boolean oneSourceAtATime = false;
	protected SourceType osaatType = SourceType.MethodCall;
	protected Iterator<SootMethod> osaatIterator = null;
	protected SootMethod currentSource = null;
	protected IValueProvider valueProvider = new SimpleConstantValueProvider();

	protected final LoadingCache<SootClass, Collection<SootClass>> parentClassesAndInterfaces = IDESolver.DEFAULT_CACHE_BUILDER
			.build(new CacheLoader<SootClass, Collection<SootClass>>() {

				@Override
				public Collection<SootClass> load(SootClass sc) throws Exception {
					Hierarchy h = Scene.v().getActiveHierarchy();

					// Don't try to find sources or sinks in irrelevant classes
					if (sc.hasTag(SimulatedCodeElementTag.TAG_NAME))
						return Collections.emptySet();

					// For interfaces, we compute the transitive list of parent interfaces
					if (sc.isInterface())
						return h.getSuperinterfacesOfIncluding(sc);

					// We need to collect all interfaces of all superclasses. First, we take the
					// superclasses, and then we call this method recursively on all interfaces
					// declared on these classes.
					// This has to be a list such that an iterator walks the hierarchy upwards.
					// Otherwise, iterating on it might return an interface with more callees
					// than the nearest superclass leading to more definitions.
					ArrayList<SootClass> res = new ArrayList<>();
					res.addAll(h.getSuperclassesOfIncluding(sc));

					res.addAll(res.stream().flatMap(c -> c.getInterfaces().stream()).flatMap(i -> {
						try {
							return parentClassesAndInterfaces.get(i).stream();
						} catch (ExecutionException e) {
							throw new RuntimeException(e);
						}
					}).collect(Collectors.toList()));
					return res;
				}

			});

	/**
	 * Creates a new instance of the {@link BaseSourceSinkManager} class with either
	 * strong or weak matching.
	 *
	 * @param sources The list of source methods
	 * @param sinks   The list of sink methods
	 * @param config  The configuration of the data flow analyzer
	 */
	public BaseSourceSinkManager(Collection<? extends ISourceSinkDefinition> sources,
			Collection<? extends ISourceSinkDefinition> sinks, InfoflowConfiguration config) {
		this(sources, sinks, Collections.<CallbackDefinition>emptySet(), config);
	}

	/**
	 * Creates a new instance of the {@link BaseSourceSinkManager} class with strong
	 * matching, i.e. the methods in the code must exactly match those in the list.
	 *
	 * @param sources         The list of source methods
	 * @param sinks           The list of sink methods
	 * @param callbackMethods The list of callback methods whose parameters are
	 *                        sources through which the application receives data
	 *                        from the operating system
	 * @param config          The configuration of the data flow analyzer
	 */
	public BaseSourceSinkManager(Collection<? extends ISourceSinkDefinition> sources,
			Collection<? extends ISourceSinkDefinition> sinks, Set<? extends CallbackDefinition> callbackMethods,
			InfoflowConfiguration config) {
		this.sourceSinkConfig = config.getSourceSinkConfig();

		this.sourceDefs = new HashMultiMap<>();
		for (ISourceSinkDefinition am : sources)
			this.sourceDefs.put(getSignature(am), am);

		this.sourceCallbackMethods = new HashMap<>();

		this.sinkDefs = new HashMultiMap<>();
		for (ISourceSinkDefinition am : sinks)
			this.sinkDefs.put(getSignature(am), am);

		this.callbackMethods = new HashMap<>();
		for (CallbackDefinition cb : callbackMethods)
			this.callbackMethods.put(cb.getTargetMethod(), cb);

		logger.info(String.format("Created a SourceSinkManager with %d sources, %d sinks, and %d callback methods.",
				this.sourceDefs.size(), this.sinkDefs.size(), this.callbackMethods.size()));
	}

	/**
	 * Collects the sources that are parameters of callback methods
	 */
	private void collectSourceCallbacks() {
		MultiMap<String, MethodSourceSinkDefinition> subsigs = new HashMultiMap<>();
		for (ISourceSinkDefinition ssd : sourceDefs.values()) {
			if (ssd instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition mssd = (MethodSourceSinkDefinition) ssd;
				if (mssd.getCallType() == CallType.Callback)
					subsigs.put(mssd.getMethod().getSubSignature(), mssd);
			}
		}

		FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();
		for (SootClass sc : Scene.v().getApplicationClasses()) {
			for (SootMethod sm : sc.getMethods()) {
				if (sm.isConcrete()) {
					final String subsig = sm.getSubSignature();
					Set<MethodSourceSinkDefinition> defs = subsigs.get(subsig);
					if (defs != null && !defs.isEmpty()) {
						for (MethodSourceSinkDefinition mssd : defs) {
							SootClass sourceClass = Scene.v().getSootClassUnsafe(mssd.getMethod().getClassName());
							if (sourceClass != null && fh.canStoreClass(sc, sourceClass)) {
								this.sourceCallbackMethods.put(sm, mssd);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Gets the field or method signature of the given source/sink definition
	 *
	 * @param am The source/sink definition for which to get a Soot signature
	 * @return The Soot signature associated with the given source/sink definition
	 */
	private String getSignature(ISourceSinkDefinition am) {
		if (am instanceof MethodSourceSinkDefinition) {
			MethodSourceSinkDefinition methodSource = (MethodSourceSinkDefinition) am;
			return methodSource.getMethod().getSignature();
		} else if (am instanceof FieldSourceSinkDefinition) {
			FieldSourceSinkDefinition fieldSource = (FieldSourceSinkDefinition) am;
			return fieldSource.getFieldSignature();
		} else if (am instanceof StatementSourceSinkDefinition)
			return GLOBAL_SIG;
		else
			throw new RuntimeException(
					String.format("Invalid type of source/sink definition: %s", am.getClass().getName()));
	}

	/**
	 * Gets the sink definition for the given call site and tainted access path
	 *
	 * @param sCallSite The call site
	 * @param manager   The manager object providing access to the configuration and
	 *                  the interprocedural control flow graph
	 * @param ap        The incoming tainted access path
	 * @return The sink definition of the method that is called at the given call
	 *         site if such a definition exists, otherwise null
	 */
	protected Collection<ISourceSinkDefinition> getSinkDefinitions(Stmt sCallSite, InfoflowManager manager,
			AccessPath ap) {
		// Do we have a statement-specific definition?
		{
			Collection<ISourceSinkDefinition> def = sinkStatements.get(sCallSite);
			if (def.size() > 0)
				return def;
		}

		if (sCallSite.containsInvokeExpr()) {
			// Check whether the taint is even visible inside the callee
			final SootMethod callee = sCallSite.getInvokeExpr().getMethod();
			if (!SystemClassHandler.v().isTaintVisible(ap, callee))
				return Collections.emptySet();

			// Do we have a direct hit?
			{
				Collection<ISourceSinkDefinition> def = this.sinkMethods.get(sCallSite.getInvokeExpr().getMethod());
				if (def.size() > 0)
					return def;
			}

			final String subSig = callee.getSubSignature();

			// Check whether we have any of the interfaces on the list
			for (SootClass i : parentClassesAndInterfaces
					.getUnchecked(sCallSite.getInvokeExpr().getMethod().getDeclaringClass())) {
				if (i.declaresMethod(subSig)) {
					Collection<ISourceSinkDefinition> def = this.sinkMethods.get(i.getMethod(subSig));
					if (def.size() > 0)
						return def;
				}
			}

			// Ask the CFG in case we don't know any better
			for (SootMethod sm : manager.getICFG().getCalleesOfCallAt(sCallSite)) {
				Collection<ISourceSinkDefinition> def = this.sinkMethods.get(sm);
				if (def.size() > 0)
					return def;
			}
		} else if (sCallSite instanceof AssignStmt) {
			// Check if the target is a sink field
			AssignStmt assignStmt = (AssignStmt) sCallSite;
			if (assignStmt.getLeftOp() instanceof FieldRef) {
				FieldRef fieldRef = (FieldRef) assignStmt.getLeftOp();
				Collection<ISourceSinkDefinition> def = sinkFields.get(fieldRef.getField());
				if (def.size() > 0)
					return def;
			}
		} else if (sCallSite instanceof ReturnStmt) {
			return sinkReturnMethods.get(manager.getICFG().getMethodOf(sCallSite));
		}

		// nothing found
		return Collections.emptySet();
	}

	protected Collection<ISourceSinkDefinition> getInverseSourceDefinition(Stmt sCallSite, InfoflowManager manager,
			AccessPath ap) {
		// Do we have a statement-specific definition?
		{
			Collection<ISourceSinkDefinition> defs = sourceStatements.get(sCallSite);
			if (defs.size() > 0)
				return defs;
		}

		if (sCallSite.containsInvokeExpr()) {
			// This might be a normal source method
			final SootMethod callee = sCallSite.getInvokeExpr().getMethod();

			// Only difference to getSource
			if (!SystemClassHandler.v().isTaintVisible(ap, callee))
				return Collections.emptySet();

			Collection<ISourceSinkDefinition> defs = getSourceDefinition(callee);
			if (defs.size() > 0)
				return defs;

			// Check whether we have any of the interfaces on the list
			final String subSig = callee.getSubSignature();
			for (SootClass i : parentClassesAndInterfaces.getUnchecked(callee.getDeclaringClass())) {
				SootMethod m = i.getMethodUnsafe(subSig);
				if (m != null) {
					defs = getSourceDefinition(m);
					if (defs.size() > 0)
						return defs;
				}
			}

			// Ask the CFG in case we don't know any better
			for (SootMethod sm : manager.getICFG().getCalleesOfCallAt(sCallSite)) {
				defs = getSourceDefinition(sm);
				if (defs.size() > 0)
					return defs;
			}
		}

		// This call might read out sensitive data from the UI
		ISourceSinkDefinition uiDef = getUISourceDefinition(sCallSite, manager.getICFG());
		if (uiDef != null)
			return Collections.singleton(uiDef);

		// This statement might access a sensitive parameter in a callback
		// method
		ISourceSinkDefinition callbackDef = checkCallbackParamSource(sCallSite, manager.getICFG());
		if (callbackDef != null)
			return Collections.singleton(callbackDef);

		// This statement may read sensitive data from a field
		Collection<ISourceSinkDefinition> fieldDefs = checkFieldSource(sCallSite, manager.getICFG());
		if (fieldDefs != null)
			return fieldDefs;

		return Collections.emptySet();
	}

	@Override
	public SinkInfo getSinkInfo(Stmt sCallSite, InfoflowManager manager, AccessPath ap) {
		// Do not look for sinks in excluded methods
		if (excludedMethods.contains(manager.getICFG().getMethodOf(sCallSite)))
			return null;
		if (sCallSite.hasTag(SimulatedCodeElementTag.TAG_NAME))
			return null;

		Collection<ISourceSinkDefinition> def = getSinkDefinitions(sCallSite, manager, ap);
		return def.size() > 0 ? new SinkInfo(def) : null;
	}

	@Override
	public SinkInfo getInverseSourceInfo(Stmt sCallSite, InfoflowManager manager, AccessPath ap) {
		if (oneSourceAtATime) {
			logger.error("This does not support one source at a time for inverse methods.");
			return null;
		}

		Collection<ISourceSinkDefinition> defs = getInverseSourceDefinition(sCallSite, manager, ap);
		return defs.size() > 0 ? new SinkInfo(defs) : null;
	}

	@Override
	public SourceInfo getSourceInfo(Stmt sCallSite, InfoflowManager manager) {
		// Do not look for sources in excluded methods
		if (excludedMethods.contains(manager.getICFG().getMethodOf(sCallSite)))
			return null;
		if (sCallSite.hasTag(SimulatedCodeElementTag.TAG_NAME))
			return null;

		Collection<ISourceSinkDefinition> defs = getSource(sCallSite, manager.getICFG());
		Collection<Pair<AccessPath, ISourceSinkDefinition>> pairs = createSourceInfoPairs(sCallSite, manager, defs);
		return pairs.size() > 0 ? new SourceInfo(pairs) : null;
	}

	@Override
	public SourceInfo getInverseSinkInfo(Stmt sCallSite, InfoflowManager manager) {
		if (oneSourceAtATime) {
			logger.error("This does not support one source at a time for inverse methods.");
			return null;
		}

		// This results in different behavior than in forwards search
		if (excludedMethods.contains(manager.getICFG().getMethodOf(sCallSite)))
			return null;
		if (sCallSite.hasTag(SimulatedCodeElementTag.TAG_NAME))
			return null;

		Collection<ISourceSinkDefinition> defs = getInverseSinkDefinition(sCallSite, manager.getICFG());
		Collection<Pair<AccessPath, ISourceSinkDefinition>> pairs = createInverseSinkInfoPairs(sCallSite, manager,
				defs);
		return pairs.size() > 0 ? new SourceInfo(pairs) : null;
	}

	protected Collection<Pair<AccessPath, ISourceSinkDefinition>> createSourceInfoPairs(Stmt sCallSite,
			InfoflowManager manager, Collection<ISourceSinkDefinition> defs) {
		HashSet<Pair<AccessPath, ISourceSinkDefinition>> sourcePairs = new HashSet<>();
		for (ISourceSinkDefinition def : defs) {
			// If we don't have an invocation, we just taint the left side of the
			// assignment
			if (!sCallSite.containsInvokeExpr()) {
				if (sCallSite instanceof DefinitionStmt) {
					DefinitionStmt defStmt = (DefinitionStmt) sCallSite;
					AccessPath ap = manager.getAccessPathFactory().createAccessPath(defStmt.getLeftOp(), null, null,
							true, false, true, ArrayTaintType.ContentsAndLength, false);
					sourcePairs.add(new Pair<>(ap, def));
				}
				continue;
			}

			// If this is a method call and we have a return value, we taint it.
			// Otherwise, if we have an instance invocation, we taint the base
			// object
			final InvokeExpr iexpr = sCallSite.getInvokeExpr();
			final Type returnType = iexpr.getMethod().getReturnType();
			if (sCallSite instanceof DefinitionStmt && returnType != null && returnType != VoidType.v()) {
				DefinitionStmt defStmt = (DefinitionStmt) sCallSite;
				// no immutable aliases, we overwrite the return values as a whole
				AccessPath ap = manager.getAccessPathFactory().createAccessPath(defStmt.getLeftOp(), null, null, true,
						false, true, ArrayTaintType.ContentsAndLength, false);
				sourcePairs.add(new Pair<>(ap, def));
			} else if (iexpr instanceof InstanceInvokeExpr && returnType == VoidType.v()) {
				InstanceInvokeExpr iinv = (InstanceInvokeExpr) sCallSite.getInvokeExpr();
				AccessPath ap = manager.getAccessPathFactory().createAccessPath(iinv.getBase(), true);
				sourcePairs.add(new Pair<>(ap, def));
			}
		}
		return sourcePairs;
	}

	protected Collection<Pair<AccessPath, ISourceSinkDefinition>> createInverseSinkInfoPairs(Stmt sCallSite,
			InfoflowManager manager, Collection<ISourceSinkDefinition> defs) {
		Collection<Pair<AccessPath, ISourceSinkDefinition>> pairs = new HashSet<>();
		for (ISourceSinkDefinition def : defs) {
			HashSet<AccessPath> aps = new HashSet<>();

			if (sCallSite.containsInvokeExpr()) {
				InvokeExpr iExpr = sCallSite.getInvokeExpr();

				// taint parameters
				for (Value arg : iExpr.getArgs()) {
					if (!(arg instanceof Constant))
						aps.add(manager.getAccessPathFactory().createAccessPath(arg, true));
				}

				// taint base object
				if (iExpr instanceof InstanceInvokeExpr) {
					InstanceInvokeExpr iiExpr = (InstanceInvokeExpr) iExpr;
					aps.add(manager.getAccessPathFactory().createAccessPath(iiExpr.getBase(), true));
				}
			} else if (sCallSite instanceof AssignStmt) {
				AssignStmt assignStmt = (AssignStmt) sCallSite;

				// Taint rhs in case of lhs being a sink field
				for (Value rightVal : BaseSelector.selectBaseList(assignStmt.getRightOp(), true))
					aps.add(manager.getAccessPathFactory().createAccessPath(rightVal, true));
			} else if (sCallSite instanceof ReturnStmt) {
				ReturnStmt retStmt = (ReturnStmt) sCallSite;

				// taint return value
				aps.add(manager.getAccessPathFactory().createAccessPath(retStmt.getOp(), true));
			}

			for (AccessPath ap : aps)
				pairs.add(new Pair<>(ap, def));

			if (pairs.isEmpty() && manager.getConfig()
					.getImplicitFlowMode() != InfoflowConfiguration.ImplicitFlowMode.NoImplicitFlows) {
				// We have to create at least one pair regardless whether we could find any
				// access path
				// if implicit flows are enabled. Think of a method with only constant arguments
				// but
				// is influenced by a tainted condition.
				pairs.add(new Pair<>(AccessPath.getEmptyAccessPath(), def));
			}
		}

		return pairs;
	}

	/**
	 * Checks whether the given method is registered as a source method. If so,
	 * returns the corresponding definition, otherwise null.
	 *
	 * @param method The method to check
	 * @return The respective source definition if the given method is a source
	 *         method, otherwise null
	 */
	protected Collection<ISourceSinkDefinition> getSourceMethod(SootMethod method) {
		if (oneSourceAtATime && (osaatType != SourceType.MethodCall || currentSource != method))
			return null;
		return this.sourceMethods.get(method);
	}

	protected Collection<ISourceSinkDefinition> getInverseSourceMethod(SootMethod method) {
		if (oneSourceAtATime && (osaatType != SourceType.MethodCall || currentSource != method))
			return null;
		return this.sinkMethods.get(method);
	}

	/**
	 * Checks whether the given method is registered as a source method
	 *
	 * @param method The method to check
	 * @return True if the given method is a source method, otherwise false
	 */
	protected Collection<ISourceSinkDefinition> getSourceDefinition(SootMethod method) {
		return getDefsFromMap(this.sourceMethods, method);
	}

	private Collection<ISourceSinkDefinition> getDefsFromMap(MultiMap<SootMethod, ISourceSinkDefinition> map,
			SootMethod method) {
		if (oneSourceAtATime) {
			if (osaatType == SourceType.MethodCall && currentSource == method)
				return map.get(method);
			else
				return null;
		} else
			return map.get(method);
	}

	/**
	 * Checks whether the given method is registered as a callback method. If so,
	 * the corresponding source definition is returned, otherwise null is returned.
	 *
	 * @param method The method to check
	 * @return The source definition object if the given method is a callback
	 *         method, otherwise null
	 */
	protected CallbackDefinition getCallbackDefinition(SootMethod method) {
		if (oneSourceAtATime) {
			if (osaatType == SourceType.Callback && currentSource == method)
				return this.callbackMethods.get(method);
			else
				return null;
		} else
			return this.callbackMethods.get(method);
	}

	/**
	 * Checks whether the given statement is a source, i.e. introduces new
	 * information into the application. If so, the source definition is returned,
	 * otherwise null
	 *
	 * @param sCallSite The statement to check for a source
	 * @param cfg       An interprocedural CFG containing the statement
	 * @return The definition of the discovered source if the given statement is a
	 *         source, null otherwise
	 */
	protected Collection<ISourceSinkDefinition> getSource(Stmt sCallSite, IInfoflowCFG cfg) {
		assert cfg != null;
		assert cfg instanceof BiDiInterproceduralCFG;

		// Do we have a statement-specific definition?
		{
			Collection<ISourceSinkDefinition> defs = sourceStatements.get(sCallSite);
			if (defs.size() > 0)
				return defs;
		}

		if ((!oneSourceAtATime || osaatType == SourceType.MethodCall) && sCallSite.containsInvokeExpr()) {
			// This might be a normal source method
			final SootMethod callee = sCallSite.getInvokeExpr().getMethod();
			Collection<ISourceSinkDefinition> defs = getSourceDefinition(callee);
			if (defs.size() > 0)
				return defs;

			// Check whether we have any of the interfaces on the list
			final String subSig = callee.getSubSignature();
			for (SootClass i : parentClassesAndInterfaces.getUnchecked(callee.getDeclaringClass())) {
				SootMethod m = i.getMethodUnsafe(subSig);
				if (m != null) {
					defs = getSourceDefinition(m);
					if (defs.size() > 0)
						return defs;
				}
			}

			// Ask the CFG in case we don't know any better
			for (SootMethod sm : cfg.getCalleesOfCallAt(sCallSite)) {
				defs = getSourceDefinition(sm);
				if (defs.size() > 0)
					return defs;
			}
		}

		// This call might read out sensitive data from the UI
		if ((!oneSourceAtATime || osaatType == SourceType.UISource)) {
			ISourceSinkDefinition uiDef = getUISourceDefinition(sCallSite, cfg);
			if (uiDef != null)
				return Collections.singleton(uiDef);
		}

		// This statement might access a sensitive parameter in a callback
		// method
		ISourceSinkDefinition callbackDef = checkCallbackParamSource(sCallSite, cfg);
		if (callbackDef != null)
			return Collections.singleton(callbackDef);

		// This statement may read sensitive data from a field
		Collection<ISourceSinkDefinition> fieldDefs = checkFieldSource(sCallSite, cfg);
		if (fieldDefs.size() > 0)
			return fieldDefs;

		return Collections.emptySet();
	}

	protected Collection<ISourceSinkDefinition> getInverseSinkDefinition(Stmt sCallSite, IInfoflowCFG cfg) {
		// Do we have a statement-specific definition?
		{
			Collection<ISourceSinkDefinition> defs = this.sinkStatements.get(sCallSite);
			if (defs.size() > 0)
				return defs;
		}

		if ((!oneSourceAtATime || osaatType == SourceType.MethodCall) && sCallSite.containsInvokeExpr()) {
			// Check whether the taint is even visible inside the callee
			final SootMethod callee = sCallSite.getInvokeExpr().getMethod();

			// Do we have a direct hit?
			{
				Collection<ISourceSinkDefinition> defs = this.sinkMethods.get(callee);
				if (defs.size() > 0)
					return defs;
			}

			final String subSig = callee.getSubSignature();

			// Check whether we have any of the interfaces on the list
			for (SootClass i : parentClassesAndInterfaces.getUnchecked(callee.getDeclaringClass())) {
				if (i.declaresMethod(subSig)) {
					Collection<ISourceSinkDefinition> defs = this.sinkMethods.get(i.getMethod(subSig));
					if (defs.size() > 0)
						return defs;
				}
			}

			// Ask the CFG in case we don't know any better
			for (SootMethod sm : cfg.getCalleesOfCallAt(sCallSite)) {
				Collection<ISourceSinkDefinition> defs = this.sinkMethods.get(sm);
				if (defs.size() > 0)
					return defs;
			}
		} else if (sCallSite instanceof AssignStmt) {
			// Check if the target is a sink field
			AssignStmt assignStmt = (AssignStmt) sCallSite;
			if (assignStmt.getLeftOp() instanceof FieldRef) {
				FieldRef fieldRef = (FieldRef) assignStmt.getLeftOp();
				Collection<ISourceSinkDefinition> defs = this.sinkFields.get(fieldRef.getField());
				if (defs.size() > 0)
					return defs;
			}
		} else if (sCallSite instanceof ReturnStmt) {
			return sinkReturnMethods.get(cfg.getMethodOf(sCallSite));
		}

		return Collections.emptySet();
	}

	/**
	 * Checks whether the given statement accesses a field that has been marked as a
	 * source
	 *
	 * @param stmt The statement to check
	 * @param cfg  The interprocedural control flow graph
	 * @return The source and sink definition that corresponds to the detected field
	 *         source if the given statement is a source, otherwise null
	 */
	private Collection<ISourceSinkDefinition> checkFieldSource(Stmt stmt, IInfoflowCFG cfg) {
		if (stmt instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) stmt;
			if (assignStmt.getRightOp() instanceof FieldRef) {
				FieldRef fieldRef = (FieldRef) assignStmt.getRightOp();
				return sourceFields.get(fieldRef.getField());
			}
		}
		return Collections.emptySet();
	}

	/**
	 * Checks whether the given statement obtains data from a callback source
	 *
	 * @param sCallSite The statement to check
	 * @param cfg       The interprocedural control flow graph
	 * @return The source and sink definition that corresponds to the detected
	 *         callback source if the given statement is a source, otherwise null
	 */
	protected ISourceSinkDefinition checkCallbackParamSource(Stmt sCallSite, IInfoflowCFG cfg) {
		// Do we handle callback sources at all?
		if (sourceSinkConfig.getCallbackSourceMode() == CallbackSourceMode.NoParametersAsSources)
			return null;
		if (oneSourceAtATime && osaatType != SourceType.Callback)
			return null;

		// Callback sources can only be parameter references
		if (!(sCallSite instanceof IdentityStmt))
			return null;
		IdentityStmt is = (IdentityStmt) sCallSite;
		if (!(is.getRightOp() instanceof ParameterRef))
			return null;
		ParameterRef paramRef = (ParameterRef) is.getRightOp();

		// We do not consider the parameters of lifecycle methods as
		// sources by default
		SootMethod parentMethod = cfg.getMethodOf(sCallSite);
		if (parentMethod == null)
			return null;
		if (!sourceSinkConfig.getEnableLifecycleSources() && isEntryPointMethod(parentMethod))
			return null;

		// Do we match all callbacks?
		if (sourceSinkConfig.getCallbackSourceMode() == CallbackSourceMode.AllParametersAsSources) {
			// Obtain the callback definition for the method in which this parameter
			// access occurs
			CallbackDefinition def = getCallbackDefinition(parentMethod);
			if (def != null)
				return MethodSourceSinkDefinition.createParameterSource(paramRef.getIndex(), CallType.Callback);
		}

		// Do we only match registered callback methods?
		ISourceSinkDefinition sourceSinkDef = this.sourceCallbackMethods.get(parentMethod);
		if (sourceSinkDef instanceof MethodSourceSinkDefinition) {
			MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) sourceSinkDef;
			if (sourceSinkConfig.getCallbackSourceMode() == CallbackSourceMode.SourceListOnly
					&& sourceSinkDef != null) {
				// Check the parameter index
				Set<AccessPathTuple>[] methodParamDefs = methodDef.getParameters();
				if (methodParamDefs != null && methodParamDefs.length > paramRef.getIndex()) {
					Set<AccessPathTuple> apTuples = methodDef.getParameters()[paramRef.getIndex()];
					if (apTuples != null && !apTuples.isEmpty()) {
						for (AccessPathTuple curTuple : apTuples)
							if (curTuple.getSourceSinkType().isSource())
								return sourceSinkDef;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Checks whether the given method is an entry point, i.e., a lifecycle method
	 * 
	 * @param method the method
	 * @return true if the method is an entry point
	 */
	protected abstract boolean isEntryPointMethod(SootMethod method);

	/**
	 * Checks whether the given call site indicates a UI source, e.g. a password
	 * input. If so, creates an {@link ISourceSinkDefinition} for it
	 *
	 * @param sCallSite The call site that may potentially read data from a
	 *                  sensitive UI control
	 * @param cfg       The bidirectional control flow graph
	 * @return The generated {@link ISourceSinkDefinition} if the given call site
	 *         reads data from a UI source, null otherwise
	 */
	protected ISourceSinkDefinition getUISourceDefinition(Stmt sCallSite, IInfoflowCFG cfg) {
		return null;
	}

	private void initializeConditions(SootMethod m, ISourceSinkDefinition def) {
		if (m == null || def.getConditions() == null || def.getConditions().isEmpty())
			return;

		conditionalSinks.add(m);
		for (SourceSinkCondition cond : def.getConditions()) {
			conditionalSinkToExcludedClasses.putAll(m, cond.getExcludedClasses());
			secondarySinkMethods.addAll(cond.getReferencedMethods());
			secondarySinkClasses.addAll(cond.getReferencedClasses());
		}
	}

	@Override
	public void initialize() {
		// Get the Soot method or field for the source signatures we have
		if (sourceDefs != null) {
			collectSourceCallbacks();

			sourceMethods = new HashMultiMap<>();
			sourceFields = new HashMultiMap<>();
			sourceStatements = new HashMultiMap<>();
			for (Pair<String, ISourceSinkDefinition> entry : sourceDefs) {
				ISourceSinkDefinition sourceSinkDef = entry.getO2();
				if (sourceSinkDef instanceof MethodSourceSinkDefinition) {
					SootMethodAndClass method = ((MethodSourceSinkDefinition) sourceSinkDef).getMethod();
					String returnType = method.getReturnType();

					// We need special handling for methods for which no return type has been
					// specified, i.e., the signature is incomplete
					if (returnType == null || returnType.isEmpty()) {
						String className = method.getClassName();

						String subSignatureWithoutReturnType = (((MethodSourceSinkDefinition) sourceSinkDef).getMethod()
								.getSubSignature());

						SootMethod sootMethod = grabMethodWithoutReturn(className, subSignatureWithoutReturnType);

						if (sootMethod != null)
							sourceMethods.put(sootMethod, sourceSinkDef);
					} else {
						SootMethod sm = grabMethod(entry.getO1());
						if (sm != null)
							sourceMethods.put(sm, sourceSinkDef);
					}

				} else if (sourceSinkDef instanceof FieldSourceSinkDefinition) {
					SootField sf = Scene.v().grabField(entry.getO1());
					if (sf != null)
						sourceFields.put(sf, sourceSinkDef);
				} else if (sourceSinkDef instanceof StatementSourceSinkDefinition) {
					StatementSourceSinkDefinition sssd = (StatementSourceSinkDefinition) sourceSinkDef;
					sourceStatements.put(sssd.getStmt(), sssd);
				}
			}
			sourceDefs = null;
		}

		// Get the Soot method or field for the sink signatures we have
		if (sinkDefs != null) {
			sinkMethods = new HashMultiMap<>();
			sinkFields = new HashMultiMap<>();
			sinkReturnMethods = new HashMultiMap<>();
			sinkStatements = new HashMultiMap<>();
			for (Pair<String, ISourceSinkDefinition> entry : sinkDefs) {
				ISourceSinkDefinition sourceSinkDef = entry.getO2();
				if (sourceSinkDef instanceof MethodSourceSinkDefinition) {
					MethodSourceSinkDefinition methodSourceSinkDef = ((MethodSourceSinkDefinition) sourceSinkDef);
					if (methodSourceSinkDef.getCallType() == CallType.Return) {
						SootMethodAndClass method = methodSourceSinkDef.getMethod();
						SootMethod m = Scene.v().grabMethod(method.getSignature());
						if (m != null) {
							sinkReturnMethods.put(m, methodSourceSinkDef);
							initializeConditions(m, methodSourceSinkDef);
						}
					} else {
						SootMethodAndClass method = methodSourceSinkDef.getMethod();
						String returnType = method.getReturnType();
						boolean isMethodWithoutReturnType = returnType == null || returnType.isEmpty();

						if (isMethodWithoutReturnType) {
							String className = method.getClassName();
							String subSignatureWithoutReturnType = (((MethodSourceSinkDefinition) sourceSinkDef)
									.getMethod().getSubSignature());
							SootMethod sootMethod = grabMethodWithoutReturn(className, subSignatureWithoutReturnType);
							if (sootMethod != null) {
								sinkMethods.put(sootMethod, sourceSinkDef);
								initializeConditions(sootMethod, methodSourceSinkDef);
							}
						} else {
							SootMethod sm = grabMethod(entry.getO1());
							if (sm != null) {
								sinkMethods.put(sm, entry.getO2());
								initializeConditions(sm, methodSourceSinkDef);
							}
						}
					}

				} else if (sourceSinkDef instanceof FieldSourceSinkDefinition) {
					SootField sf = Scene.v().grabField(entry.getO1());
					if (sf != null)
						sinkFields.put(sf, sourceSinkDef);
				} else if (sourceSinkDef instanceof StatementSourceSinkDefinition) {
					StatementSourceSinkDefinition sssd = (StatementSourceSinkDefinition) sourceSinkDef;
					sinkStatements.put(sssd.getStmt(), sssd);
				}
			}
			sinkDefs = null;
		}
	}

	/**
	 * Get the method of a class without matching the return type.
	 *
	 * @param sootClass    The class of the method
	 * @param subSignature The sub signature of the method which is the method name
	 *                     and its parameters
	 * @return The soot method of the given class and sub signature or null
	 */
	private SootMethod matchMethodWithoutReturn(SootClass sootClass, String subSignature) {
		if (sootClass.resolvingLevel() == DANGLING) {
			List<SootMethod> sootMethods = sootClass.getMethods();

			for (SootMethod s : sootMethods) {
				String[] tempSignature = s.getSubSignature().split(" ");

				if (tempSignature.length == 2) {
					if (tempSignature[1].equals(subSignature))
						return s;
				}
			}
		}

		return null;
	}

	/**
	 * Gets a soot method defined by class name and its sub signature from the
	 * loaded methods in the Scene object
	 *
	 * @param sootClassName The class name of the method
	 * @param subSignature  The sub signature of the method which is the method name
	 *                      and its parameters
	 * @return The soot method of the given class and sub signature or null
	 */
	private SootMethod grabMethodWithoutReturn(String sootClassName, String subSignature) {
		SootClass sootClass = Scene.v().getSootClassUnsafe(sootClassName);
		if (sootClass == null)
			return null;

		SootMethod sootMethod = matchMethodWithoutReturn(sootClass, subSignature);
		if (sootMethod != null)
			return sootMethod;

		for (SootClass i : parentClassesAndInterfaces.getUnchecked(sootClass)) {
			sootMethod = matchMethodWithoutReturn(i, subSignature);
			if (sootMethod != null)
				return sootMethod;
		}

		return null;
	}

	/**
	 * Gets a soot method defined by the class name or one of its superclasses.
	 *
	 * @param signature method signature
	 * @return The soot method of the given class or above in the hierarchy. Null if
	 *         the method doesn't exist.
	 */
	private SootMethod grabMethod(String signature) {
		String sootClassName = Scene.signatureToClass(signature);
		SootClass sootClass = Scene.v().getSootClassUnsafe(sootClassName);
		if (sootClass == null)
			return null;

		String subSignature = Scene.signatureToSubsignature(signature);
		SootMethod sootMethod = sootClass.getMethodUnsafe(subSignature);
		if (sootMethod != null)
			return sootMethod;

		for (SootClass i : parentClassesAndInterfaces.getUnchecked(sootClass)) {
			sootMethod = i.getMethodUnsafe(subSignature);
			if (sootMethod != null)
				return sootMethod;
		}

		return null;
	}

	@Override
	public void setOneSourceAtATimeEnabled(boolean enabled) {
		this.oneSourceAtATime = enabled;
	}

	@Override
	public boolean isOneSourceAtATimeEnabled() {
		return this.oneSourceAtATime;
	}

	@Override
	public void resetCurrentSource() {
		this.osaatIterator = this.sourceMethods.keySet().iterator();
		this.osaatType = SourceType.MethodCall;
	}

	@Override
	public void nextSource() {
		if (osaatType == SourceType.MethodCall || osaatType == SourceType.Callback)
			currentSource = this.osaatIterator.next();
	}

	@Override
	public boolean hasNextSource() {
		if (osaatType == SourceType.MethodCall) {
			if (this.osaatIterator.hasNext())
				return true;
			else {
				this.osaatType = SourceType.Callback;
				this.osaatIterator = this.callbackMethods.keySet().iterator();
				return hasNextSource();
			}
		} else if (osaatType == SourceType.Callback) {
			if (this.osaatIterator.hasNext())
				return true;
			else {
				this.osaatType = SourceType.UISource;
				return true;
			}
		} else if (osaatType == SourceType.UISource) {
			osaatType = SourceType.NoSource;
			return false;
		}
		return false;
	}

	/**
	 * Excludes the given method from the source/sink analysis. No sources or sinks
	 * will be detected in excluded methods.
	 *
	 * @param toExclude The method to exclude
	 */
	public void excludeMethod(SootMethod toExclude) {
		this.excludedMethods.add(toExclude);
	}

	@Override
	public boolean isSecondarySink(Stmt stmt) {
		if (!stmt.containsInvokeExpr() || !(stmt.getInvokeExpr() instanceof InstanceInvokeExpr))
			return false;

		SootMethod callee = stmt.getInvokeExpr().getMethod();
		SootClass dc = callee.getDeclaringClass();
		if (secondarySinkMethods.contains(callee) || secondarySinkClasses.contains(dc))
			return true;

		// Check if the current method inherits from a conditional sink
		for (SootClass superClass : parentClassesAndInterfaces.getUnchecked(dc)) {
			if (secondarySinkClasses.contains(superClass))
				return true;

			SootMethod superMethod = superClass.getMethodUnsafe(callee.getSubSignature());
			if (secondarySinkMethods.contains(superMethod))
				return true;
		}

		return false;
	}

	@Override
	public void registerSecondarySink(Stmt stmt) {
		secondarySinkMethods.add(stmt.getInvokeExpr().getMethod());
	}

	@Override
	public void registerSecondarySink(SootMethod sm) {
		secondarySinkMethods.add(sm);
	}

	/**
	 * Returns whether the callee is excluded from the additional flow condition for
	 * the matched sink.
	 *
	 * @param matchedSink Sink method
	 * @return true if class is excluded in the condition
	 */
	private boolean isExcludedInCondition(SootMethod matchedSink, SootClass baseClass) {
		Set<SootClass> excludedClasses = conditionalSinkToExcludedClasses.get(matchedSink);
		if (excludedClasses.contains(baseClass))
			return true;

		for (SootClass sc : parentClassesAndInterfaces.getUnchecked(matchedSink.getDeclaringClass())) {
			if (excludedClasses.contains(sc))
				return true;
		}

		return false;
	}

	@Override
	public boolean isConditionalSink(Stmt stmt, SootClass baseClass) {
		// River only supports InstanceInvokeExprs
		if (!stmt.containsInvokeExpr() || !(stmt.getInvokeExpr() instanceof InstanceInvokeExpr))
			return false;

		// Check if we have a direct hit
		SootMethod callee = stmt.getInvokeExpr().getMethod();
		if (conditionalSinks.contains(callee))
			return !isExcludedInCondition(callee, baseClass);

		// Check if the current method inherits from a conditional sink
		for (SootClass sc : parentClassesAndInterfaces.getUnchecked(callee.getDeclaringClass())) {
			SootMethod superMethod = sc.getMethodUnsafe(callee.getSubSignature());
			if (conditionalSinks.contains(superMethod))
				return !isExcludedInCondition(superMethod, baseClass);
		}

		return false;
	}
}
