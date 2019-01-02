/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.android.source;

import static soot.SootClass.DANGLING;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import heros.solver.IDESolver;
import heros.solver.Pair;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.CallbackSourceMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.LayoutMatchingMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.SourceSinkConfiguration;
import soot.jimple.infoflow.android.callbacks.CallbackDefinition;
import soot.jimple.infoflow.android.callbacks.CallbackDefinition.CallbackType;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointUtils;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.ResPackage;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.FieldSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.StatementSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.IOneSourceAtATimeManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.infoflow.values.IValueProvider;
import soot.jimple.infoflow.values.SimpleConstantValueProvider;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.scalar.ConstantPropagatorAndFolder;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.Tag;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

/**
 * SourceManager implementation for AndroidSources
 *
 * @author Steven Arzt
 */
public class AndroidSourceSinkManager implements ISourceSinkManager, IOneSourceAtATimeManager {

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

	protected final static String Activity_FindViewById = "<android.app.Activity: android.view.View findViewById(int)>";
	protected final static String View_FindViewById = "<android.app.View: android.view.View findViewById(int)>";

	protected SootMethod smActivityFindViewById;
	protected SootMethod smViewFindViewById;

	protected MultiMap<String, SourceSinkDefinition> sourceDefs;
	protected MultiMap<String, SourceSinkDefinition> sinkDefs;

	protected Map<SootMethod, SourceSinkDefinition> sourceMethods;
	protected Map<Stmt, SourceSinkDefinition> sourceStatements;
	protected Map<SootMethod, SourceSinkDefinition> sinkMethods;
	protected Map<SootMethod, SourceSinkDefinition> sinkReturnMethods;
	protected Map<SootMethod, CallbackDefinition> callbackMethods;
	protected Map<SootField, SourceSinkDefinition> sourceFields;
	protected Map<SootField, SourceSinkDefinition> sinkFields;
	protected Map<Stmt, SourceSinkDefinition> sinkStatements;

	protected final SourceSinkConfiguration sourceSinkConfig;
	protected final Map<Integer, AndroidLayoutControl> layoutControls;
	protected List<ARSCFileParser.ResPackage> resourcePackages;

	protected String appPackageName = "";
	protected final Set<SootMethod> excludedMethods = new HashSet<>();

	protected final Set<SootMethod> analyzedLayoutMethods = new HashSet<SootMethod>();
	protected SootClass[] iccBaseClasses = null;
	protected AndroidEntryPointUtils entryPointUtils = new AndroidEntryPointUtils();

	protected boolean oneSourceAtATime = false;
	protected SourceType osaatType = SourceType.MethodCall;
	protected Iterator<SootMethod> osaatIterator = null;
	protected SootMethod currentSource = null;
	protected IValueProvider valueProvider = new SimpleConstantValueProvider();

	protected final LoadingCache<SootClass, Collection<SootClass>> interfacesOf = IDESolver.DEFAULT_CACHE_BUILDER
			.build(new CacheLoader<SootClass, Collection<SootClass>>() {

				@Override
				public Collection<SootClass> load(SootClass sc) throws Exception {
					Set<SootClass> set = new HashSet<SootClass>(sc.getInterfaceCount());
					for (SootClass i : sc.getInterfaces()) {
						set.add(i);
						set.addAll(interfacesOf.getUnchecked(i));
					}
					if (sc.hasSuperclass())
						set.addAll(interfacesOf.getUnchecked(sc.getSuperclass()));
					return set;
				}

			});

	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with
	 * either strong or weak matching.
	 *
	 * @param sources
	 *            The list of source methods
	 * @param sinks
	 *            The list of sink methods
	 * @param config
	 *            The configuration of the data flow analyzer
	 */
	public AndroidSourceSinkManager(Set<SourceSinkDefinition> sources, Set<SourceSinkDefinition> sinks,
			InfoflowAndroidConfiguration config) {
		this(sources, sinks, Collections.<CallbackDefinition>emptySet(), config, null);
	}

	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with
	 * strong matching, i.e. the methods in the code must exactly match those in the
	 * list.
	 *
	 * @param sources
	 *            The list of source methods
	 * @param sinks
	 *            The list of sink methods
	 * @param callbackMethods
	 *            The list of callback methods whose parameters are sources through
	 *            which the application receives data from the operating system
	 * @param weakMatching
	 *            True for weak matching: If an entry in the list has no return
	 *            type, it matches arbitrary return types if the rest of the method
	 *            signature is compatible. False for strong matching: The method
	 *            signature in the code exactly match the one in the list.
	 * @param config
	 *            The configuration of the data flow analyzer
	 * @param layoutControls
	 *            A map from reference identifiers to the respective Android layout
	 *            controls
	 */
	public AndroidSourceSinkManager(Set<SourceSinkDefinition> sources, Set<SourceSinkDefinition> sinks,
			Set<CallbackDefinition> callbackMethods, InfoflowAndroidConfiguration config,
			Map<Integer, AndroidLayoutControl> layoutControls) {
		this.sourceSinkConfig = config.getSourceSinkConfig();

		this.sourceDefs = new HashMultiMap<>();
		for (SourceSinkDefinition am : sources)
			this.sourceDefs.put(getSignature(am), am);

		this.sinkDefs = new HashMultiMap<>();
		for (SourceSinkDefinition am : sinks)
			this.sinkDefs.put(getSignature(am), am);

		this.callbackMethods = new HashMap<>();
		for (CallbackDefinition cb : callbackMethods)
			this.callbackMethods.put(cb.getTargetMethod(), cb);

		this.layoutControls = layoutControls;

		logger.info(String.format("Created a SourceSinkManager with %d sources, %d sinks, and %d callback methods.",
				this.sourceDefs.size(), this.sinkDefs.size(), this.callbackMethods.size()));
	}

	/**
	 * Gets the field or method signature of the given source/sink definition
	 *
	 * @param am
	 *            The source/sink definition for which to get a Soot signature
	 * @return The Soot signature associated with the given source/sink definition
	 */
	private String getSignature(SourceSinkDefinition am) {
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
	 * @param sCallSite
	 *            The call site
	 * @param manager
	 *            The manager object providing access to the configuration and the
	 *            interprocedural control flow graph
	 * @param ap
	 *            The incoming tainted access path
	 * @return The sink definition of the method that is called at the given call
	 *         site if such a definition exists, otherwise null
	 */
	protected SourceSinkDefinition getSinkDefinition(Stmt sCallSite, InfoflowManager manager, AccessPath ap) {
		// Do we have a statement-specific definition?
		{
			SourceSinkDefinition def = sinkStatements.get(sCallSite);
			if (def != null)
				return def;
		}

		if (sCallSite.containsInvokeExpr()) {
			// Check whether the taint is even visible inside the callee
			final SootMethod callee = sCallSite.getInvokeExpr().getMethod();
			if (!SystemClassHandler.isTaintVisible(ap, callee))
				return null;

			// Do we have a direct hit?
			{
				SourceSinkDefinition def = this.sinkMethods.get(sCallSite.getInvokeExpr().getMethod());
				if (def != null)
					return def;
			}

			final SootClass sc = callee.getDeclaringClass();
			final String subSig = callee.getSubSignature();

			// Check whether we have any of the interfaces on the list
			for (SootClass i : interfacesOf.getUnchecked(sCallSite.getInvokeExpr().getMethod().getDeclaringClass())) {
				if (i.declaresMethod(subSig)) {
					SourceSinkDefinition def = this.sinkMethods.get(i.getMethod(subSig));
					if (def != null)
						return def;
				}
			}

			// Ask the CFG in case we don't know any better
			for (SootMethod sm : manager.getICFG().getCalleesOfCallAt(sCallSite)) {
				SourceSinkDefinition def = this.sinkMethods.get(sm);
				if (def != null)
					return def;
			}

			// If the target method is in a phantom class, we scan the hierarchy
			// upwards to see whether we have a sink definition for a parent
			// class
			if (callee.getDeclaringClass().isPhantom()) {
				SourceSinkDefinition def = findDefinitionInHierarchy(callee, this.sinkMethods);
				if (def != null)
					return def;
			}

			// Do not consider ICC methods as sinks if only the base object is
			// tainted
			boolean isParamTainted = false;
			if (ap != null) {
				if (!sc.isInterface() && !ap.isStaticFieldRef()) {
					for (int i = 0; i < sCallSite.getInvokeExpr().getArgCount(); i++) {
						if (sCallSite.getInvokeExpr().getArg(i) == ap.getPlainValue()) {
							isParamTainted = true;
							break;
						}
					}
				}
			}

			if (isParamTainted || ap == null) {
				for (SootClass clazz : iccBaseClasses) {
					if (Scene.v().getOrMakeFastHierarchy().isSubclass(sc, clazz)) {
						SootMethod sm = clazz.getMethodUnsafe(subSig);
						if (sm != null) {
							SourceSinkDefinition def = this.sinkMethods.get(sm);
							if (def != null)
								return def;
							break;
						}
					}
				}
			}
		} else if (sCallSite instanceof AssignStmt) {
			// Check if the target is a sink field
			AssignStmt assignStmt = (AssignStmt) sCallSite;
			if (assignStmt.getLeftOp() instanceof FieldRef) {
				FieldRef fieldRef = (FieldRef) assignStmt.getLeftOp();
				SourceSinkDefinition def = sinkFields.get(fieldRef.getField());
				if (def != null)
					return def;
			}
		} else if (sCallSite instanceof ReturnStmt) {
			return sinkReturnMethods.get(manager.getICFG().getMethodOf(sCallSite));
		}

		return null;
	}

	/**
	 * Scans the hierarchy of the class containing the given method to find any
	 * implementations of the same method further up in the hierarchy for which
	 * there is a SourceSinkDefinition in the given map
	 *
	 * @param callee
	 *            The method for which to look for a SourceSinkDefinition
	 * @param map
	 *            A map from methods to their corresponding SourceSinkDefinitions
	 * @return A SourceSinKDefinition for an implementation of the given method
	 *         somewhere up in the class hiearchy if it exists, otherwise null.
	 */
	private static SourceSinkDefinition findDefinitionInHierarchy(SootMethod callee,
			Map<SootMethod, SourceSinkDefinition> map) {
		final String subSig = callee.getSubSignature();
		SootClass curClass = callee.getDeclaringClass();
		while (curClass != null) {
			// Does the current class declare the requested method?
			SootMethod curMethod = curClass.getMethodUnsafe(subSig);
			if (curMethod != null) {
				SourceSinkDefinition def = map.get(curMethod);
				if (def != null) {
					// Patch the map to contain a direct link
					map.put(callee, def);
					return def;
				}
			}

			// Try the next class up the hierarchy
			if (curClass.hasSuperclass() && curClass.isPhantom())
				curClass = curClass.getSuperclass();
			else
				curClass = null;
		}

		return null;
	}

	@Override
	public SinkInfo getSinkInfo(Stmt sCallSite, InfoflowManager manager, AccessPath ap) {
		SourceSinkDefinition def = getSinkDefinition(sCallSite, manager, ap);
		return def == null ? null : new SinkInfo(def);
	}

	@Override
	public SourceInfo getSourceInfo(Stmt sCallSite, InfoflowManager manager) {
		// Do not look for sources in excluded methods
		if (excludedMethods.contains(manager.getICFG().getMethodOf(sCallSite)))
			return null;

		SourceSinkDefinition def = getSource(sCallSite, manager.getICFG());
		return createSourceInfo(sCallSite, manager, def);
	}

	protected SourceInfo createSourceInfo(Stmt sCallSite, InfoflowManager manager, SourceSinkDefinition def) {
		// Do we have data at all?
		if (def == null)
			return null;

		// If we don't have an invocation, we just taint the left side of the
		// assignment
		if (!sCallSite.containsInvokeExpr()) {
			if (sCallSite instanceof DefinitionStmt) {
				DefinitionStmt defStmt = (DefinitionStmt) sCallSite;
				return new SourceInfo(def, manager.getAccessPathFactory().createAccessPath(defStmt.getLeftOp(), null,
						null, null, true, false, true, ArrayTaintType.ContentsAndLength, false));
			}
			return null;
		}

		// If this is a method call and we have a return value, we taint it.
		// Otherwise, if we have an instance invocation, we taint the base
		// object
		final InvokeExpr iexpr = sCallSite.getInvokeExpr();
		if (sCallSite instanceof DefinitionStmt && iexpr.getMethod().getReturnType() != null) {
			DefinitionStmt defStmt = (DefinitionStmt) sCallSite;
			// no immutable aliases, we overwrite the return values as a whole
			return new SourceInfo(def, manager.getAccessPathFactory().createAccessPath(defStmt.getLeftOp(), null, null,
					null, true, false, true, ArrayTaintType.ContentsAndLength, false));
		} else if (iexpr instanceof InstanceInvokeExpr && iexpr.getMethod().getReturnType() == VoidType.v()) {
			InstanceInvokeExpr iinv = (InstanceInvokeExpr) sCallSite.getInvokeExpr();
			return new SourceInfo(def, manager.getAccessPathFactory().createAccessPath(iinv.getBase(), true));
		} else
			return null;
	}

	/**
	 * Checks whether the given method is registered as a source method. If so,
	 * returns the corresponding definition, otherwise null.
	 *
	 * @param method
	 *            The method to check
	 * @return The respective source definition if the given method is a source
	 *         method, otherwise null
	 */
	protected SourceSinkDefinition getSourceMethod(SootMethod method) {
		if (oneSourceAtATime && (osaatType != SourceType.MethodCall || currentSource != method))
			return null;
		return this.sourceMethods.get(method);
	}

	/**
	 * Checks whether the given method is registered as a source method
	 *
	 * @param method
	 *            The method to check
	 * @return True if the given method is a source method, otherwise false
	 */
	protected SourceSinkDefinition getSourceDefinition(SootMethod method) {
		if (oneSourceAtATime) {
			if (osaatType == SourceType.MethodCall && currentSource == method)
				return this.sourceMethods.get(method);
			else
				return null;
		} else
			return this.sourceMethods.get(method);
	}

	/**
	 * Checks whether the given method is registered as a callback method. If so,
	 * the corresponding source definition is returned, otherwise null is returned.
	 *
	 * @param method
	 *            The method to check
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
	 * @param sCallSite
	 *            The statement to check for a source
	 * @param cfg
	 *            An interprocedural CFG containing the statement
	 * @return The definition of the discovered source if the given statement is a
	 *         source, null otherwise
	 */
	protected SourceSinkDefinition getSource(Stmt sCallSite, IInfoflowCFG cfg) {
		assert cfg != null;
		assert cfg instanceof BiDiInterproceduralCFG;

		// Do we have a statement-specific definition?
		{
			SourceSinkDefinition def = sourceStatements.get(sCallSite);
			if (def != null)
				return def;
		}

		SourceSinkDefinition def = null;
		if ((!oneSourceAtATime || osaatType == SourceType.MethodCall) && sCallSite.containsInvokeExpr()) {
			// This might be a normal source method
			final SootMethod callee = sCallSite.getInvokeExpr().getMethod();
			def = getSourceDefinition(callee);
			if (def != null)
				return def;

			// Check whether we have any of the interfaces on the list
			final String subSig = callee.getSubSignature();
			for (SootClass i : interfacesOf.getUnchecked(callee.getDeclaringClass())) {
				SootMethod m = i.getMethodUnsafe(subSig);
				if (m != null) {
					def = getSourceDefinition(m);
					if (def != null)
						return def;
				}
			}

			// Ask the CFG in case we don't know any better
			for (SootMethod sm : cfg.getCalleesOfCallAt(sCallSite)) {
				def = getSourceDefinition(sm);
				if (def != null)
					return def;
			}

			// If the target method is in a phantom class, we scan the hierarchy
			// upwards
			// to see whether we have a sink definition for a parent class
			if (callee.getDeclaringClass().isPhantom()) {
				def = findDefinitionInHierarchy(callee, this.sourceMethods);
				if (def != null)
					return def;
			}
		}

		// This call might read out sensitive data from the UI
		if ((!oneSourceAtATime || osaatType == SourceType.UISource)) {
			def = getUISourceDefinition(sCallSite, cfg);
			if (def != null)
				return def;
		}

		// This statement might access a sensitive parameter in a callback
		// method
		def = checkCallbackParamSource(sCallSite, cfg);
		if (def != null)
			return def;

		// This statement may read sensitive data from a field
		def = checkFieldSource(sCallSite, cfg);
		if (def != null)
			return def;

		return null;
	}

	/**
	 * Checks whether the given statement accesses a field that has been marked as a
	 * source
	 *
	 * @param stmt
	 *            The statement to check
	 * @param cfg
	 *            The interprocedural control flow graph
	 * @return The source and sink definition that corresponds to the detected field
	 *         source if the given statement is a source, otherwise null
	 */
	private SourceSinkDefinition checkFieldSource(Stmt stmt, IInfoflowCFG cfg) {
		if (stmt instanceof AssignStmt) {
			AssignStmt assignStmt = (AssignStmt) stmt;
			if (assignStmt.getRightOp() instanceof FieldRef) {
				FieldRef fieldRef = (FieldRef) assignStmt.getRightOp();
				return sourceFields.get(fieldRef.getField());
			}
		}
		return null;
	}

	/**
	 * Checks whether the given statement obtains data from a callback source
	 *
	 * @param sCallSite
	 *            The statement to check
	 * @param cfg
	 *            The interprocedural control flow graph
	 * @return The source and sink definition that corresponds to the detected
	 *         callback source if the given statement is a source, otherwise null
	 */
	protected SourceSinkDefinition checkCallbackParamSource(Stmt sCallSite, IInfoflowCFG cfg) {
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
		if (!sourceSinkConfig.getEnableLifecycleSources() && entryPointUtils.isEntryPointMethod(parentMethod))
			return null;

		// Obtain the callback definition for the method in which this parameter
		// access occurs
		CallbackDefinition def = getCallbackDefinition(parentMethod);
		if (def == null)
			return null;

		// If this is a UI element, we only consider it as a
		// source if we actually want to taint all UI elements
		if (def.getCallbackType() == CallbackType.Widget
				&& sourceSinkConfig.getLayoutMatchingMode() != LayoutMatchingMode.MatchAll)
			return null;

		// Do we match all callbacks?
		if (sourceSinkConfig.getCallbackSourceMode() == CallbackSourceMode.AllParametersAsSources)
			return MethodSourceSinkDefinition.createParameterSource(paramRef.getIndex(), CallType.Callback);

		// Do we only match registered callback methods?
		SourceSinkDefinition sourceSinkDef = this.sourceMethods.get(def.getParentMethod());
		if (sourceSinkDef instanceof MethodSourceSinkDefinition) {
			MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) sourceSinkDef;
			if (sourceSinkConfig.getCallbackSourceMode() == CallbackSourceMode.SourceListOnly
					&& sourceSinkDef != null) {
				// Check the parameter index
				if (methodDef.getParameters().length > paramRef.getIndex()) {
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
	 * Checks whether the given call site indicates a UI source, e.g. a password
	 * input. If so, creates a {@link SourceSinkDefinition} for it
	 *
	 * @param sCallSite
	 *            The call site that may potentially read data from a sensitive UI
	 *            control
	 * @param cfg
	 *            The bidirectional control flow graph
	 * @return The generated {@link SourceSinkDefinition} if the given call site
	 *         reads data from a UI source, null otherwise
	 */
	private SourceSinkDefinition getUISourceDefinition(Stmt sCallSite, IInfoflowCFG cfg) {
		// If we match input controls, we need to check whether this is a call
		// to one of the well-known resource handling functions in Android
		if (sourceSinkConfig.getLayoutMatchingMode() == LayoutMatchingMode.NoMatch || !sCallSite.containsInvokeExpr())
			return null;

		// If nobody cares about the value obtained from the UI, we can ignore
		// the call
		if (!(sCallSite instanceof AssignStmt))
			return null;

		InvokeExpr ie = sCallSite.getInvokeExpr();
		SootMethod callee = ie.getMethod();

		// Is this a call to resource-handling method?
		boolean isResourceCall = callee == smActivityFindViewById || callee == smViewFindViewById;
		if (!isResourceCall) {
			for (SootMethod cfgCallee : cfg.getCalleesOfCallAt(sCallSite)) {
				if (cfgCallee == smActivityFindViewById || cfgCallee == smViewFindViewById) {
					isResourceCall = true;
					break;
				}
			}
		}

		// We need special treatment for the Android support classes
		if (!isResourceCall) {
			if (callee.getDeclaringClass().getName().startsWith("android.support.v")
					&& callee.getSubSignature().equals(smActivityFindViewById.getSubSignature()))
				isResourceCall = true;
		}

		if (isResourceCall) {
			// If we match all controls, we don't care about the specific
			// control we're dealing with
			if (sourceSinkConfig.getLayoutMatchingMode() == LayoutMatchingMode.MatchAll) {
				return MethodSourceSinkDefinition.createReturnSource(CallType.MethodCall);
			}

			// If we don't have a layout control list, we cannot perform any
			// more specific checks
			if (this.layoutControls == null)
				return null;

			// Perform a constant propagation inside this method exactly
			// once
			SootMethod uiMethod = cfg.getMethodOf(sCallSite);
			if (analyzedLayoutMethods.add(uiMethod))
				ConstantPropagatorAndFolder.v().transform(uiMethod.getActiveBody());

			// If we match specific controls, we need to get the ID of
			// control and look up the respective data object
			if (ie.getArgCount() != 1) {
				logger.error("Framework method call with unexpected number of arguments");
				return null;
			}

			Integer id = valueProvider.getValue(uiMethod, sCallSite, ie.getArg(0), Integer.class);
			if (id == null && ie.getArg(0) instanceof Local) {
				id = findLastResIDAssignment(sCallSite, (Local) ie.getArg(0), cfg,
						new HashSet<Stmt>(cfg.getMethodOf(sCallSite).getActiveBody().getUnits().size()));
			}
			if (id == null) {
				logger.debug("Could not find assignment to local "
						+ ((Local) ie.getArg(0)).getName()
						+ " in method "
						+ cfg.getMethodOf(sCallSite).getSignature());
				return null;
			}

			AndroidLayoutControl control = this.layoutControls.get(id);
			if (control == null)
				return null;
			if (sourceSinkConfig.getLayoutMatchingMode() == LayoutMatchingMode.MatchSensitiveOnly
					&& control.isSensitive()) {
				return control.getSourceDefinition();
			}
		}
		return null;
	}

	/**
	 * Finds the last assignment to the given local representing a resource ID by
	 * searching upwards from the given statement
	 *
	 * @param stmt
	 *            The statement from which to look backwards
	 * @param local
	 *            The variable for which to look for assignments
	 * @return The last value assigned to the given variable
	 */
	private Integer findLastResIDAssignment(Stmt stmt, Local local, BiDiInterproceduralCFG<Unit, SootMethod> cfg,
			Set<Stmt> doneSet) {
		if (!doneSet.add(stmt))
			return null;

		// If this is an assign statement, we need to check whether it changes
		// the variable we're looking for
		if (stmt instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) stmt;
			if (assign.getLeftOp() == local) {
				// ok, now find the new value from the right side
				if (assign.getRightOp() instanceof IntConstant)
					return ((IntConstant) assign.getRightOp()).value;
				else if (assign.getRightOp() instanceof FieldRef) {
					SootField field = ((FieldRef) assign.getRightOp()).getField();
					for (Tag tag : field.getTags())
						if (tag instanceof IntegerConstantValueTag)
							return ((IntegerConstantValueTag) tag).getIntValue();
						else
							logger.error("Constant %s was of unexpected type", field.toString());
				} else if (assign.getRightOp() instanceof InvokeExpr) {
					InvokeExpr inv = (InvokeExpr) assign.getRightOp();
					if (inv.getMethod().getName().equals("getIdentifier")
							&& inv.getMethod().getDeclaringClass().getName().equals("android.content.res.Resources")
							&& this.resourcePackages != null) {
						// The right side of the assignment is a call into the
						// well-known
						// Android API method for resource handling
						if (inv.getArgCount() != 3) {
							logger.error("Invalid parameter count (%d) for call to getIdentifier", inv.getArgCount());
							return null;
						}

						// Find the parameter values
						String resName = "";
						String resID = "";
						String packageName = "";

						// In the trivial case, these values are constants
						if (inv.getArg(0) instanceof StringConstant)
							resName = ((StringConstant) inv.getArg(0)).value;
						if (inv.getArg(1) instanceof StringConstant)
							resID = ((StringConstant) inv.getArg(1)).value;
						if (inv.getArg(2) instanceof StringConstant)
							packageName = ((StringConstant) inv.getArg(2)).value;
						else if (inv.getArg(2) instanceof Local)
							packageName = findLastStringAssignment(stmt, (Local) inv.getArg(2), cfg);
						else {
							logger.error("Unknown parameter type %s in call to getIdentifier",
									inv.getArg(2).getClass().getName());
							return null;
						}

						// Find the resource
						ARSCFileParser.AbstractResource res = findResource(resName, resID, packageName);
						if (res != null)
							return res.getResourceID();
					}
				}
			}
		}

		// Continue the search upwards
		for (Unit pred : cfg.getPredsOf(stmt)) {
			if (!(pred instanceof Stmt))
				continue;
			Integer lastAssignment = findLastResIDAssignment((Stmt) pred, local, cfg, doneSet);
			if (lastAssignment != null)
				return lastAssignment;
		}
		return null;
	}

	/**
	 * Finds the given resource in the given package
	 *
	 * @param resName
	 *            The name of the resource to retrieve
	 * @param resID
	 * @param packageName
	 *            The name of the package in which to look for the resource
	 * @return The specified resource if available, otherwise null
	 */
	private AbstractResource findResource(String resName, String resID, String packageName) {
		// Find the correct package
		for (ARSCFileParser.ResPackage pkg : this.resourcePackages) {
			// If we don't have any package specification, we pick the app's
			// default package
			boolean matches = (packageName == null || packageName.isEmpty())
					&& pkg.getPackageName().equals(this.appPackageName);
			matches |= pkg.getPackageName().equals(packageName);
			if (!matches)
				continue;

			// We have found a suitable package, now look for the resource
			for (ARSCFileParser.ResType type : pkg.getDeclaredTypes())
				if (type.getTypeName().equals(resID)) {
					AbstractResource res = type.getFirstResource(resName);
					return res;
				}
		}
		return null;
	}

	/**
	 * Finds the last assignment to the given String local by searching upwards from
	 * the given statement
	 *
	 * @param stmt
	 *            The statement from which to look backwards
	 * @param local
	 *            The variable for which to look for assignments
	 * @return The last value assigned to the given variable
	 */
	private String findLastStringAssignment(Stmt stmt, Local local, BiDiInterproceduralCFG<Unit, SootMethod> cfg) {
		LinkedList<Stmt> workList = new LinkedList<Stmt>();
		Set<Stmt> seen = new HashSet<Stmt>();
		workList.add(stmt);
		while (!workList.isEmpty()) {
			stmt = workList.removeFirst();

			if (stmt instanceof AssignStmt) {
				AssignStmt assign = (AssignStmt) stmt;
				if (assign.getLeftOp() == local) {
					// ok, now find the new value from the right side
					if (assign.getRightOp() instanceof StringConstant)
						return ((StringConstant) assign.getRightOp()).value;
				}
			}

			// Continue the search upwards
			for (Unit pred : cfg.getPredsOf(stmt)) {
				if (!(pred instanceof Stmt))
					continue;

				Stmt s = (Stmt) pred;
				if (seen.add(s))
					workList.add(s);
			}
		}
		return null;
	}

	/**
	 * Sets the resource packages to be used for finding sensitive layout controls
	 * as sources
	 *
	 * @param resourcePackages
	 *            The resource packages to be used for looking up layout controls
	 */
	public void setResourcePackages(List<ResPackage> resourcePackages) {
		this.resourcePackages = resourcePackages;
	}

	/**
	 * Sets the name of the app's base package
	 *
	 * @param appPackageName
	 *            The name of the app's base package
	 */
	public void setAppPackageName(String appPackageName) {
		this.appPackageName = appPackageName;
	}

	/**
	 * Gets a soot method defined by class name and its sub signature from the
	 * loaded methods in the Scene object
	 *
	 * @param sootClassName
	 *            The class name of the method
	 * @param subSignature
	 *            The sub signature of the method which is the method name and its
	 *            parameters
	 * @return The soot method of the given class and sub signature or null
	 */
	private SootMethod grabMethodWithoutReturn(String sootClassName, String subSignature) {
		SootClass sootClass = Scene.v().getSootClassUnsafe(sootClassName);
		if (sootClass == null)
			return null;

		List<SootMethod> sootMethods = null;
		if (sootClass.resolvingLevel() != DANGLING) {
			sootMethods = sootClass.getMethods();

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

	@Override
	public void initialize() {
		// Get the Soot method or field for the source signatures we have
		if (sourceDefs != null) {
			sourceMethods = new HashMap<>();
			sourceFields = new HashMap<>();
			sourceStatements = new HashMap<>();
			for (Pair<String, SourceSinkDefinition> entry : sourceDefs) {
				SourceSinkDefinition sourceSinkDef = entry.getO2();
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
						SootMethod sm = Scene.v().grabMethod(entry.getO1());
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
			sinkMethods = new HashMap<>();
			sinkFields = new HashMap<>();
			sinkReturnMethods = new HashMap<>();
			sinkStatements = new HashMap<>();
			for (Pair<String, SourceSinkDefinition> entry : sinkDefs) {
				SourceSinkDefinition sourceSinkDef = entry.getO2();
				if (sourceSinkDef instanceof MethodSourceSinkDefinition) {
					MethodSourceSinkDefinition methodSourceSinkDef = ((MethodSourceSinkDefinition) sourceSinkDef);
					if (methodSourceSinkDef.getCallType() == CallType.Return) {
						SootMethodAndClass method = methodSourceSinkDef.getMethod();
						SootMethod m = Scene.v().grabMethod(method.getSignature());
						if (m != null)
							sinkReturnMethods.put(m, methodSourceSinkDef);
					} else {
						SootMethodAndClass method = methodSourceSinkDef.getMethod();
						String returnType = method.getReturnType();
						boolean isMethodWithoutReturnType = returnType == null || returnType.isEmpty();
						if (isMethodWithoutReturnType) {
							String className = method.getClassName();
							String subSignatureWithoutReturnType = (((MethodSourceSinkDefinition) sourceSinkDef)
									.getMethod().getSubSignature());
							SootMethod sootMethod = grabMethodWithoutReturn(className, subSignatureWithoutReturnType);
							if (sootMethod != null)
								sinkMethods.put(sootMethod, sourceSinkDef);
						} else {
							SootMethod sm = Scene.v().grabMethod(entry.getO1());
							if (sm != null)
								sinkMethods.put(sm, entry.getO2());
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

		// For ICC methods (e.g., startService), the classes name of these
		// methods may change through user's definition. We match all the
		// ICC methods through their base class name.
		if (iccBaseClasses == null)
			iccBaseClasses = new SootClass[] { Scene.v().getSootClass("android.content.Context"), // activity,
					// service
					// and
					// broadcast
					Scene.v().getSootClass("android.content.ContentResolver"), // provider
					Scene.v().getSootClass("android.app.Activity") // some
					// methods
					// (e.g.,
					// onActivityResult)
					// only
					// defined
					// in
					// Activity
					// class
			};

		// Get some frequently-used methods
		this.smActivityFindViewById = Scene.v().grabMethod(Activity_FindViewById);
		this.smViewFindViewById = Scene.v().grabMethod(View_FindViewById);
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
	 * @param toExclude
	 *            The method to exclude
	 */
	public void excludeMethod(SootMethod toExclude) {
		this.excludedMethods.add(toExclude);
	}

}
