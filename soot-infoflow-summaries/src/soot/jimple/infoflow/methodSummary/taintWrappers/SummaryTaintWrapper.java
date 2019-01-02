package soot.jimple.infoflow.methodSummary.taintWrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import heros.solver.IDESolver;
import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.ArrayType;
import soot.FastHierarchy;
import soot.Hierarchy;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.sourceSink.AbstractFlowSinkSource;
import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodClear;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.data.summary.SummaryMetaData;
import soot.jimple.infoflow.solver.IFollowReturnsPastSeedsHandler;
import soot.jimple.infoflow.taintWrappers.IReversibleTaintWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.ByReferenceBoolean;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.infoflow.util.TypeUtils;
import soot.util.ConcurrentHashMultiMap;
import soot.util.MultiMap;

/**
 * Taint wrapper implementation that applies method summaries created by
 * StubDroid
 * 
 * @author Steven Arzt
 *
 */
public class SummaryTaintWrapper implements IReversibleTaintWrapper {

	private static final int MAX_HIERARCHY_DEPTH = 10;

	private InfoflowManager manager;
	private AtomicInteger wrapperHits = new AtomicInteger();
	private AtomicInteger wrapperMisses = new AtomicInteger();
	private boolean reportMissingSummaries = false;
	private ITaintPropagationWrapper fallbackWrapper = null;

	protected IMethodSummaryProvider flows;

	private Hierarchy hierarchy;
	private FastHierarchy fastHierarchy;

	private MultiMap<Pair<Abstraction, SootMethod>, AccessPathPropagator> userCodeTaints = new ConcurrentHashMultiMap<>();

	protected final LoadingCache<SummaryQuery, SummaryResponse> methodToImplFlows = IDESolver.DEFAULT_CACHE_BUILDER
			.build(new CacheLoader<SummaryQuery, SummaryResponse>() {
				@Override
				public SummaryResponse load(SummaryQuery query) throws Exception {
					final SootClass calleeClass = query.calleeClass;
					final SootClass declaredClass = query.declaredClass;
					final String methodSig = query.subsignature;
					final ClassSummaries classSummaries = new ClassSummaries();
					boolean isClassSupported = false;

					// Get the flows in the target method
					if (calleeClass != null)
						isClassSupported = getSummaries(methodSig, classSummaries, calleeClass);

					// If we haven't found any summaries, we look at the class from the declared
					// type at the call site
					if (declaredClass != null && !isClassSupported)
						isClassSupported = getSummaries(methodSig, classSummaries, declaredClass);

					// If we still don't have anything, we must try the hierarchy. Since this
					// best-effort approach can be fairly imprecise, it is our last resort.
					if (!isClassSupported && calleeClass != null)
						isClassSupported = getSummariesHierarchy(methodSig, classSummaries, calleeClass);
					if (declaredClass != null && !isClassSupported)
						isClassSupported = getSummariesHierarchy(methodSig, classSummaries, declaredClass);

					if (!classSummaries.isEmpty())
						return new SummaryResponse(classSummaries, isClassSupported);
					else
						return isClassSupported ? SummaryResponse.EMPTY_BUT_SUPPORTED : SummaryResponse.NOT_SUPPORTED;
				}

				/**
				 * Checks whether we have summaries for the given method signature in the given
				 * class
				 * 
				 * @param methodSig The subsignature of the method for which to get summaries
				 * @param summaries The summary object to which to add the discovered summaries
				 * @param clazz     The class for which to look for summaries
				 * @return True if summaries were found, false otherwise
				 */
				private boolean getSummaries(final String methodSig, final ClassSummaries summaries, SootClass clazz) {
					// Do we have direct support for the target class?
					if (summaries.merge(flows.getMethodFlows(clazz, methodSig)))
						return true;

					// Do we support any interface this class might have?
					if (checkInterfaces(methodSig, summaries, clazz))
						return true;

					// If the target is abstract and we haven't found any flows,
					// we check for child classes
					SootMethod targetMethod = clazz.getMethodUnsafe(methodSig);
					if (!clazz.isConcrete() || targetMethod == null || !targetMethod.isConcrete()) {
						for (SootClass parentClass : getAllParentClasses(clazz)) {
							// Do we have support for the target class?
							if (summaries.merge(flows.getMethodFlows(parentClass, methodSig)))
								return true;

							// Do we support any interface this class might have?
							if (checkInterfaces(methodSig, summaries, parentClass))
								return true;
						}
					}

					// In case we don't have a real hierarchy, we must reconstruct one from the
					// summaries
					String curClass = clazz.getName();
					while (curClass != null) {
						ClassMethodSummaries classSummaries = flows.getClassFlows(curClass);
						if (classSummaries != null) {
							// Check for flows in the current class
							if (summaries.merge(flows.getMethodFlows(curClass, methodSig)))
								return true;

							// Check for interfaces
							if (checkInterfacesFromSummary(methodSig, summaries, curClass))
								return true;

							curClass = classSummaries.getSuperClass();
						} else
							break;
					}

					return false;
				}

				/**
				 * Checks whether we have summaries for the given method signature in a class in
				 * the hierarchy of the given class
				 * 
				 * @param methodSig The subsignature of the method for which to get summaries
				 * @param summaries The summary object to which to add the discovered summaries
				 * @param clazz     The class for which to look for summaries
				 * @return True if summaries were found, false otherwise
				 */
				private boolean getSummariesHierarchy(final String methodSig, final ClassSummaries summaries,
						SootClass clazz) {
					// Don't try to look up the whole Java hierarchy
					if (clazz == Scene.v().getSootClassUnsafe("java.lang.Object"))
						return false;

					// If the target is abstract and we haven't found any flows,
					// we check for child classes. Since the summaries are class-specific and we
					// don't really know which child class we're looking for, we have to merge the
					// flows for all possible classes.
					SootMethod targetMethod = clazz.getMethodUnsafe(methodSig);
					if (!clazz.isConcrete() || targetMethod == null || !targetMethod.isConcrete()) {
						Set<SootClass> childClasses = getAllChildClasses(clazz);
						if (childClasses.size() > MAX_HIERARCHY_DEPTH)
							return false;

						boolean found = false;
						for (SootClass childClass : childClasses) {
							// Do we have support for the target class?
							if (summaries.merge(flows.getMethodFlows(childClass, methodSig)))
								return true;

							// Do we support any interface this class might have?
							if (checkInterfaces(methodSig, summaries, childClass))
								found = true;
						}
						return found;
					}
					return false;

				}

				/**
				 * Checks whether we have summaries for the given method signature in the given
				 * interface or any of its super-interfaces
				 * 
				 * @param methodSig The subsignature of the method for which to get summaries
				 * @param summaries The summary object to which to add the discovered summaries
				 * @param clazz     The interface for which to look for summaries
				 * @return True if summaries were found, false otherwise
				 */
				private boolean checkInterfaces(String methodSig, ClassSummaries summaries, SootClass clazz) {
					for (SootClass intf : clazz.getInterfaces()) {
						// Directly check the interface
						if (summaries.merge(flows.getMethodFlows(intf, methodSig)))
							return true;

						for (SootClass parent : getAllParentClasses(intf)) {
							// Do we have support for the interface?
							if (summaries.merge(flows.getMethodFlows(parent, methodSig)))
								return true;
						}
					}

					// We might not have hierarchy information in the scene, so let's check the
					// summary itself
					return checkInterfacesFromSummary(methodSig, summaries, clazz.getName());
				}

				/**
				 * Checks for summaries on the interfaces implemented by the given class. This
				 * method relies on the hierarchy data from the summary XML files, rather than
				 * the Soot scene
				 * 
				 * @param methodSig The subsignature of the method for which to get summaries
				 * @param summaries The summary object to which to add the discovered summaries
				 * @param className The interface for which to look for summaries
				 * @return True if summaries were found, false otherwise
				 */
				protected boolean checkInterfacesFromSummary(String methodSig, ClassSummaries summaries,
						String className) {
					List<String> interfaces = new ArrayList<>();
					interfaces.add(className);
					while (!interfaces.isEmpty()) {
						final String intfName = interfaces.remove(0);
						ClassMethodSummaries classSummaries = flows.getClassFlows(intfName);
						if (classSummaries != null && classSummaries.hasInterfaces()) {
							for (String intf : classSummaries.getInterfaces()) {
								// Do we have a summary on the current interface?
								if (summaries.merge(flows.getMethodFlows(intf, methodSig)))
									return true;

								// Recursively check for more interfaces
								interfaces.add(intf);
							}
						}
					}
					return false;
				}

			});

	/**
	 * Query that retrieves summaries for a given class and method.
	 * 
	 * @author Steven Arzt
	 *
	 */
	private static class SummaryQuery {

		public final SootClass calleeClass;
		public final SootClass declaredClass;
		public final String subsignature;

		public SummaryQuery(SootClass calleeClass, SootClass declaredClass, String subsignature) {
			this.calleeClass = calleeClass;
			this.declaredClass = declaredClass;
			this.subsignature = subsignature;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(calleeClass.getName());
			sb.append(": ");
			sb.append(subsignature);
			return sb.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((calleeClass == null) ? 0 : calleeClass.hashCode());
			result = prime * result + ((declaredClass == null) ? 0 : declaredClass.hashCode());
			result = prime * result + ((subsignature == null) ? 0 : subsignature.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SummaryQuery other = (SummaryQuery) obj;
			if (calleeClass == null) {
				if (other.calleeClass != null)
					return false;
			} else if (!calleeClass.equals(other.calleeClass))
				return false;
			if (declaredClass == null) {
				if (other.declaredClass != null)
					return false;
			} else if (!declaredClass.equals(other.declaredClass))
				return false;
			if (subsignature == null) {
				if (other.subsignature != null)
					return false;
			} else if (!subsignature.equals(other.subsignature))
				return false;
			return true;
		}

	}

	/**
	 * Response from a cache that provides flows for a given combination of class
	 * and method
	 * 
	 * @author Steven Arzt
	 *
	 */
	private static class SummaryResponse {

		public final static SummaryResponse NOT_SUPPORTED = new SummaryResponse(null, false);
		public final static SummaryResponse EMPTY_BUT_SUPPORTED = new SummaryResponse(null, true);

		private final ClassSummaries classSummaries;
		private final boolean isClassSupported;

		public SummaryResponse(ClassSummaries classSummaries, boolean isClassSupported) {
			this.classSummaries = classSummaries;
			this.isClassSupported = isClassSupported;
		}

		@Override
		public String toString() {
			if (isClassSupported) {
				if (classSummaries == null)
					return "<Empty summary>";
				else
					return classSummaries.toString();
			} else
				return "<Class not supported>";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((classSummaries == null) ? 0 : classSummaries.hashCode());
			result = prime * result + (isClassSupported ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SummaryResponse other = (SummaryResponse) obj;
			if (classSummaries == null) {
				if (other.classSummaries != null)
					return false;
			} else if (!classSummaries.equals(other.classSummaries))
				return false;
			if (isClassSupported != other.isClassSupported)
				return false;
			return true;
		}

	}

	/**
	 * Handler that is used for injecting taints from callbacks implemented in user
	 * code back into the summary application process
	 * 
	 * @author Steven Arzt
	 * 
	 */
	private class SummaryFRPSHandler implements IFollowReturnsPastSeedsHandler {

		@Override
		public void handleFollowReturnsPastSeeds(Abstraction d1, Unit u, Abstraction d2) {
			SootMethod sm = manager.getICFG().getMethodOf(u);
			Set<AccessPathPropagator> propagators = getUserCodeTaints(d1, sm);
			if (propagators != null) {
				for (AccessPathPropagator propagator : propagators) {
					// Propagate these taints up. We leave the current gap
					AccessPathPropagator parent = safePopParent(propagator);
					GapDefinition parentGap = propagator.getParent() == null ? null : propagator.getParent().getGap();

					// Create taints from the abstractions
					Set<Taint> returnTaints = createTaintFromAccessPathOnReturn(d2.getAccessPath(), (Stmt) u,
							propagator.getGap());
					if (returnTaints == null)
						continue;

					// Get the correct set of flows to apply
					MethodSummaries flowsInTarget = parentGap == null ? getFlowsInOriginalCallee(propagator)
							: getFlowSummariesForGap(parentGap);

					// Create the new propagator, one for every taint
					Set<AccessPathPropagator> workSet = new HashSet<>();
					for (Taint returnTaint : returnTaints) {
						AccessPathPropagator newPropagator = new AccessPathPropagator(returnTaint, parentGap, parent,
								propagator.getParent() == null ? null : propagator.getParent().getStmt(),
								propagator.getParent() == null ? null : propagator.getParent().getD1(),
								propagator.getParent() == null ? null : propagator.getParent().getD2());
						workSet.add(newPropagator);
					}

					// Apply the aggregated propagators
					Set<AccessPath> resultAPs = applyFlowsIterative(flowsInTarget, new ArrayList<>(workSet));

					// Propagate the access paths
					if (resultAPs != null && !resultAPs.isEmpty()) {
						AccessPathPropagator rootPropagator = getOriginalCallSite(propagator);
						for (AccessPath ap : resultAPs) {
							Abstraction newAbs = rootPropagator.getD2().deriveNewAbstraction(ap,
									rootPropagator.getStmt());
							for (Unit succUnit : manager.getICFG().getSuccsOf(rootPropagator.getStmt()))
								manager.getForwardSolver().processEdge(
										new PathEdge<Unit, Abstraction>(rootPropagator.getD1(), succUnit, newAbs));
						}
					}
				}
			}
		}

		/**
		 * Gets the flows in the method that was originally called and from where the
		 * summary application was started
		 * 
		 * @param propagator A propagator somewhere in the call tree
		 * @return The summary flows inside the original callee
		 */
		private MethodSummaries getFlowsInOriginalCallee(AccessPathPropagator propagator) {
			Stmt originalCallSite = getOriginalCallSite(propagator).getStmt();

			// Get the flows in the original callee
			ClassSummaries flowsInCallee = getFlowSummariesForMethod(originalCallSite,
					originalCallSite.getInvokeExpr().getMethod(), null);
			// if (flowsInCallee.getClasses().size() != 1)
			// throw new RuntimeException("Original callee must only be one
			// method");

			String methodSig = originalCallSite.getInvokeExpr().getMethod().getSubSignature();
			return flowsInCallee.getAllSummariesForMethod(methodSig);
		}

		/**
		 * Gets the call site at which the taint application was originally started
		 * 
		 * @param propagator A propagator somewhere in the call tree
		 * @return The call site at which the taint application was originally started
		 *         if successful, otherwise null
		 */
		private AccessPathPropagator getOriginalCallSite(AccessPathPropagator propagator) {
			// Get the original call site
			AccessPathPropagator curProp = propagator;
			while (curProp != null) {
				if (curProp.getParent() == null)
					return curProp;
				curProp = curProp.getParent();
			}
			return null;
		}
	}

	/**
	 * Creates a new instance of the {@link SummaryTaintWrapper} class
	 * 
	 * @param flows The flows loaded from disk
	 */
	public SummaryTaintWrapper(IMethodSummaryProvider flows) {
		this.flows = flows;
	}

	@Override
	public void initialize(InfoflowManager manager) {
		this.manager = manager;

		// Load all classes for which we have summaries to signatures
		Set<String> loadableClasses = flows.getLoadableClasses();
		if (loadableClasses != null) {
			for (String className : loadableClasses)
				loadClass(className);
		}

		for (String className : flows.getSupportedClasses())
			loadClass(className);

		// Get the hierarchy
		this.hierarchy = Scene.v().getActiveHierarchy();
		this.fastHierarchy = Scene.v().getOrMakeFastHierarchy();

		// Register the taint propagation handler
		manager.getForwardSolver().setFollowReturnsPastSeedsHandler(new SummaryFRPSHandler());

		// If we have a fallback wrapper, we need to initialize that one as well
		if (fallbackWrapper != null)
			fallbackWrapper.initialize(manager);
	}

	/**
	 * Loads the class with the given name into the scene. This makes sure that
	 * there is at least a phantom class with the given name
	 * 
	 * @param className The name of the class to load
	 */
	private void loadClass(String className) {
		SootClass sc = Scene.v().getSootClassUnsafe(className);
		if (sc == null) {
			sc = Scene.v().makeSootClass(className);
			sc.setPhantomClass();
			Scene.v().addClass(sc);
		} else if (sc.resolvingLevel() < SootClass.HIERARCHY)
			Scene.v().forceResolve(className, SootClass.HIERARCHY);
	}

	/**
	 * Creates a taint that can be propagated through method summaries based on the
	 * given access path. This method assumes that the given statement is a method
	 * call and that the access path is flowing into this call.
	 * 
	 * @param ap                  The access path from which to create a taint
	 * @param stmt                The statement at which the access path came in
	 * @param matchReturnedValues True if the method shall also match on the left
	 *                            side of assign statements that capture that return
	 *                            value of a method call, otherwise false
	 * @return The set of taints derived from the given access path
	 */
	private Set<Taint> createTaintFromAccessPathOnCall(AccessPath ap, Stmt stmt, boolean matchReturnedValues) {
		Value base = getMethodBase(stmt);
		Set<Taint> newTaints = null;

		// Check whether the base object or some field in it is tainted
		if ((ap.isLocal() || ap.isInstanceFieldRef()) && base != null && base == ap.getPlainValue()) {
			if (newTaints == null)
				newTaints = new HashSet<>();

			newTaints.add(new Taint(SourceSinkType.Field, -1, ap.getBaseType().toString(),
					fieldArrayToStringArray(ap.getFields()), typeArrayToStringArray(ap.getFieldTypes()),
					ap.getTaintSubFields()));
		}

		// Check whether a parameter is tainted
		int paramIdx = getParameterIndex(stmt, ap);
		if (paramIdx >= 0) {
			if (newTaints == null)
				newTaints = new HashSet<>();

			newTaints.add(new Taint(SourceSinkType.Parameter, paramIdx, ap.getBaseType().toString(),
					fieldArrayToStringArray(ap.getFields()), typeArrayToStringArray(ap.getFieldTypes()),
					ap.getTaintSubFields()));
		}

		// If we also match returned values, we must do this here
		if (matchReturnedValues && stmt instanceof DefinitionStmt) {
			DefinitionStmt defStmt = (DefinitionStmt) stmt;
			if (defStmt.getLeftOp() == ap.getPlainValue()) {
				if (newTaints == null)
					newTaints = new HashSet<>();

				newTaints.add(new Taint(SourceSinkType.Return, -1, ap.getBaseType().toString(),
						fieldArrayToStringArray(ap.getFields()), typeArrayToStringArray(ap.getFieldTypes()),
						ap.getTaintSubFields()));
			}
		}

		return newTaints;
	}

	/**
	 * Creates a set of taints that can be propagated through method summaries based
	 * on the given access path. This method assumes that the given statement is a
	 * return site from a method.
	 * 
	 * @param ap   The access path from which to create a taint
	 * @param stmt The statement at which the access path came in
	 * @param gap  The gap in which the taint is valid
	 * @return The taint derived from the given access path
	 */
	private Set<Taint> createTaintFromAccessPathOnReturn(AccessPath ap, Stmt stmt, GapDefinition gap) {
		SootMethod sm = manager.getICFG().getMethodOf(stmt);
		Set<Taint> res = null;

		// Check whether the base object or some field in it is tainted
		if (!sm.isStatic() && (ap.isLocal() || ap.isInstanceFieldRef())
				&& ap.getPlainValue() == sm.getActiveBody().getThisLocal()) {
			if (res == null)
				res = new HashSet<>();
			res.add(new Taint(SourceSinkType.Field, -1, ap.getBaseType().toString(),
					fieldArrayToStringArray(ap.getFields()), typeArrayToStringArray(ap.getFieldTypes()),
					ap.getTaintSubFields(), gap));
		}

		// Check whether a parameter is tainted
		int paramIdx = getParameterIndex(sm, ap);
		if (paramIdx >= 0) {
			if (res == null)
				res = new HashSet<>();
			res.add(new Taint(SourceSinkType.Parameter, paramIdx, ap.getBaseType().toString(),
					fieldArrayToStringArray(ap.getFields()), typeArrayToStringArray(ap.getFieldTypes()),
					ap.getTaintSubFields(), gap));
		}

		// Check whether the return value is tainted
		if (stmt instanceof ReturnStmt) {
			ReturnStmt retStmt = (ReturnStmt) stmt;
			if (retStmt.getOp() == ap.getPlainValue()) {
				if (res == null)
					res = new HashSet<>();
				res.add(new Taint(SourceSinkType.Return, -1, ap.getBaseType().toString(),
						fieldArrayToStringArray(ap.getFields()), typeArrayToStringArray(ap.getFieldTypes()),
						ap.getTaintSubFields(), gap));
			}
		}

		return res;
	}

	/**
	 * Converts a taint back into an access path that is valid at the given
	 * statement
	 * 
	 * @param t    The taint to convert into an access path
	 * @param stmt The statement at which the access path shall be valid
	 * @return The access path derived from the given taint
	 */
	protected AccessPath createAccessPathFromTaint(Taint t, Stmt stmt) {
		// Convert the taints to Soot objects
		SootField[] fields = safeGetFields(t.getAccessPath());
		Type[] types = safeGetTypes(t.getAccessPathTypes(), fields);
		Type baseType = TypeUtils.getTypeFromString(t.getBaseType());

		// If the taint is a return value, we taint the left side of the
		// assignment
		if (t.isReturn()) {
			// If the return value is not used, we can abort
			if (!(stmt instanceof DefinitionStmt))
				return null;

			DefinitionStmt defStmt = (DefinitionStmt) stmt;
			return manager.getAccessPathFactory().createAccessPath(defStmt.getLeftOp(), fields, baseType, types,
					t.taintSubFields(), false, true, ArrayTaintType.ContentsAndLength);
		}

		// If the taint is a parameter value, we need to identify the
		// corresponding local
		if (t.isParameter() && stmt.containsInvokeExpr()) {
			InvokeExpr iexpr = stmt.getInvokeExpr();
			Value paramVal = iexpr.getArg(t.getParameterIndex());
			if (!AccessPath.canContainValue(paramVal))
				return null;

			return manager.getAccessPathFactory().createAccessPath(paramVal, fields, baseType, types,
					t.taintSubFields(), false, true, ArrayTaintType.ContentsAndLength);
		}

		// If the taint is on the base value, we need to taint the base local
		if (t.isField() && stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr iiexpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
			return manager.getAccessPathFactory().createAccessPath(iiexpr.getBase(), fields, baseType, types,
					t.taintSubFields(), false, true, ArrayTaintType.ContentsAndLength);
		}

		throw new RuntimeException("Could not convert taint to access path: " + t + " at " + stmt);
	}

	/**
	 * Converts a taint into an access path that is valid inside a given method.
	 * This models that a taint is propagated into the method and from there on in
	 * normal IFDS.
	 * 
	 * @param t  The taint to convert
	 * @param sm The method in which the access path shall be created
	 * @return The access path derived from the given taint and method
	 */
	private AccessPath createAccessPathInMethod(Taint t, SootMethod sm) {
		// Convert the taints to Soot objects
		SootField[] fields = safeGetFields(t.getAccessPath());
		Type[] types = safeGetTypes(t.getAccessPathTypes(), fields);
		Type baseType = TypeUtils.getTypeFromString(t.getBaseType());

		// A return value cannot be propagated into a method
		if (t.isReturn())
			throw new RuntimeException("Unsupported taint type");

		if (t.isParameter()) {
			Local l = sm.getActiveBody().getParameterLocal(t.getParameterIndex());
			return manager.getAccessPathFactory().createAccessPath(l, fields, baseType, types, true, false, true,
					ArrayTaintType.ContentsAndLength);
		}

		if (t.isField() || t.isGapBaseObject()) {
			Local l = sm.getActiveBody().getThisLocal();
			return manager.getAccessPathFactory().createAccessPath(l, fields, baseType, types, true, false, true,
					ArrayTaintType.ContentsAndLength);
		}

		throw new RuntimeException("Failed to convert taint " + t);
	}

	/**
	 * Converts an array of SootFields to an array of strings
	 * 
	 * @param fields The array of SootFields to convert
	 * @return The array of strings corresponding to the given array of SootFields
	 */
	private String[] fieldArrayToStringArray(SootField[] fields) {
		if (fields == null)
			return null;
		String[] stringFields = new String[fields.length];
		for (int i = 0; i < fields.length; i++)
			stringFields[i] = fields[i].toString();
		return stringFields;
	}

	/**
	 * Converts an array of Soot Types to an array of strings
	 * 
	 * @param fields The array of Soot Types to convert
	 * @return The array of strings corresponding to the given array of Soot Types
	 */
	private String[] typeArrayToStringArray(Type[] types) {
		if (types == null)
			return null;
		String[] stringTypes = new String[types.length];
		for (int i = 0; i < types.length; i++)
			stringTypes[i] = types[i].toString();
		return stringTypes;
	}

	@Override
	public Set<Abstraction> getTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedAbs) {
		// We only care about method invocations
		if (!stmt.containsInvokeExpr())
			return Collections.singleton(taintedAbs);

		Set<Abstraction> resAbs = null;
		ByReferenceBoolean killIncomingTaint = new ByReferenceBoolean(false);
		ByReferenceBoolean classSupported = new ByReferenceBoolean(false);

		// Compute the wrapper taints for the current method
		Set<AccessPath> res = computeTaintsForMethod(stmt, d1, taintedAbs, stmt.getInvokeExpr().getMethod(),
				killIncomingTaint, classSupported);

		// Create abstractions from the access paths
		if (res != null && !res.isEmpty()) {
			if (resAbs == null)
				resAbs = new HashSet<>();
			for (AccessPath ap : res)
				resAbs.add(taintedAbs.deriveNewAbstraction(ap, stmt));
		}

		// If we have no data flows, we can abort early
		if (!killIncomingTaint.value && (resAbs == null || resAbs.isEmpty())) {
			wrapperMisses.incrementAndGet();
			SootMethod method = stmt.getInvokeExpr().getMethod();

			if (!classSupported.value)
				reportMissingMethod(method);

			if (classSupported.value || fallbackWrapper == null)
				return Collections.singleton(taintedAbs);
			else {
				Set<Abstraction> fallbackTaints = fallbackWrapper.getTaintsForMethod(stmt, d1, taintedAbs);
				return fallbackTaints;
			}
		}

		// We always retain the incoming abstraction unless it is explicitly
		// cleared
		if (!killIncomingTaint.value)
			resAbs.add(taintedAbs);
		return resAbs;
	}

	protected void reportMissingMethod(SootMethod method) {
		if (reportMissingSummaries && SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
			System.out.println("Missing summary for class " + method.getDeclaringClass());
	}

	/**
	 * Computes library taints for the given method and incoming abstraction
	 * 
	 * @param stmt              The statement to which to apply the library summary
	 * @param d1                The context of the incoming taint
	 * @param taintedAbs        The incoming taint
	 * @param method            The method for which to get library model taints
	 * @param killIncomingTaint Outgoing value that defines whether the original
	 *                          taint shall be killed instead of being propagated
	 *                          onwards
	 * @param classSupported    Outgoing parameter that informs the caller whether
	 *                          the callee class is supported, i.e., there is a
	 *                          summary configuration for that class
	 * @return The artificial taints coming from the libary model if any, otherwise
	 *         null
	 */
	private Set<AccessPath> computeTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedAbs,
			final SootMethod method, ByReferenceBoolean killIncomingTaint, ByReferenceBoolean classSupported) {
		wrapperHits.incrementAndGet();

		// Get the cached data flows
		ClassSummaries flowsInCallees = getFlowSummariesForMethod(stmt, method, taintedAbs, classSupported);
		if (flowsInCallees == null || flowsInCallees.isEmpty())
			return null;

		// Create a level-0 propagator for the initially tainted access path
		Set<Taint> taintsFromAP = createTaintFromAccessPathOnCall(taintedAbs.getAccessPath(), stmt, false);
		if (taintsFromAP == null || taintsFromAP.isEmpty())
			return null;

		Set<AccessPath> res = null;
		for (String className : flowsInCallees.getClasses()) {
			// Get the flows in this class
			ClassMethodSummaries classFlows = flowsInCallees.getClassSummaries(className);
			if (classFlows == null || classFlows.isEmpty())
				continue;

			// Get the method-level flows
			MethodSummaries flowsInCallee = classFlows.getMethodSummaries();
			if (flowsInCallee == null || flowsInCallee.isEmpty())
				continue;

			// Check whether the incoming taint matches a clear
			List<AccessPathPropagator> workList = new ArrayList<AccessPathPropagator>();
			for (Taint taint : taintsFromAP) {
				boolean killTaint = false;
				if (killIncomingTaint != null && flowsInCallee.hasClears()) {
					for (MethodClear clear : flowsInCallee.getAllClears()) {
						if (flowMatchesTaint(clear.getClearDefinition(), taint)) {
							killTaint = true;
							break;
						}
					}
				}

				if (killTaint)
					killIncomingTaint.value = true;
				else
					workList.add(new AccessPathPropagator(taint, null, null, stmt, d1, taintedAbs));
			}

			// Apply the data flows until we reach a fixed point
			Set<AccessPath> resCallee = applyFlowsIterative(flowsInCallee, workList);
			if (resCallee != null && !resCallee.isEmpty()) {
				if (res == null)
					res = new HashSet<>();
				res.addAll(resCallee);
			}
		}
		return res;
	}

	/**
	 * Iteratively applies all of the given flow summaries until a fixed point is
	 * reached. if the flow enters user code, an analysis of the corresponding
	 * method will be spawned.
	 * 
	 * @param flowsInCallee The flow summaries for the given callee
	 * @param workList      The incoming propagators on which to apply the flow
	 *                      summaries
	 * @param aliasingQuery True if this query is for aliases, false if it is for
	 *                      normal taint propagation.
	 * @return The set of outgoing access paths
	 */
	private Set<AccessPath> applyFlowsIterative(MethodSummaries flowsInCallee, List<AccessPathPropagator> workList) {
		Set<AccessPath> res = null;
		Set<AccessPathPropagator> doneSet = new HashSet<AccessPathPropagator>(workList);
		while (!workList.isEmpty()) {
			final AccessPathPropagator curPropagator = workList.remove(0);
			final GapDefinition curGap = curPropagator.getGap();

			// Make sure we don't have invalid data
			if (curGap != null && curPropagator.getParent() == null)
				throw new RuntimeException("Gap flow without parent detected");

			// Get the correct set of flows to apply
			MethodSummaries flowsInTarget = curGap == null ? flowsInCallee : getFlowSummariesForGap(curGap);

			// If we don't have summaries for the current gap, we look for
			// implementations in the application code
			if ((flowsInTarget == null || flowsInTarget.isEmpty()) && curGap != null) {
				SootMethod callee = Scene.v().grabMethod(curGap.getSignature());
				if (callee != null)
					for (SootMethod implementor : getAllImplementors(callee))
						if (implementor.getDeclaringClass().isConcrete() && !implementor.getDeclaringClass().isPhantom()
								&& implementor.isConcrete()) {
							Set<AccessPathPropagator> implementorPropagators = spawnAnalysisIntoClientCode(implementor,
									curPropagator);
							if (implementorPropagators != null)
								workList.addAll(implementorPropagators);
						}
			}

			// Apply the flow summaries for other libraries
			if (flowsInTarget != null && !flowsInTarget.isEmpty())
				for (MethodFlow flow : flowsInTarget) {
					// Apply the flow summary
					AccessPathPropagator newPropagator = applyFlow(flow, curPropagator);
					if (newPropagator == null) {
						// Can we reverse the flow and apply it in the other direction?
						flow = getReverseFlowForAlias(flow);
						if (flow == null)
							continue;

						// Apply the reversed flow
						newPropagator = applyFlow(flow, curPropagator);
						if (newPropagator == null)
							continue;
					}

					// Propagate it
					if (newPropagator.getParent() == null && newPropagator.getTaint().getGap() == null) {
						AccessPath ap = createAccessPathFromTaint(newPropagator.getTaint(), newPropagator.getStmt());
						if (ap == null)
							continue;
						else {
							if (res == null)
								res = new HashSet<>();
							res.add(ap);
						}
					}
					if (doneSet.add(newPropagator))
						workList.add(newPropagator);

					// If we have have tainted a heap field, we need to look for
					// aliases as well
					if (newPropagator.getTaint().hasAccessPath()) {
						AccessPathPropagator backwardsPropagator = newPropagator.deriveInversePropagator();
						if (doneSet.add(backwardsPropagator))
							workList.add(backwardsPropagator);
					}
				}
		}
		return res;
	}

	/**
	 * Checks whether the given flow denotes an aliasing relationship and can thus
	 * be applied in reverse
	 * 
	 * @param flow The flow to check
	 * @return The reverse flow if the given flow works in both directions, null
	 *         otherwise
	 */
	private MethodFlow getReverseFlowForAlias(MethodFlow flow) {
		// Reverse flows can only be applied if the flow is an
		// aliasing relationship
		if (!flow.isAlias())
			return null;

		// Reverse flows can only be applied to heap objects
		if (!canTypeAlias(flow.source().getLastFieldType()))
			return null;
		if (!canTypeAlias(flow.sink().getLastFieldType()))
			return null;

		// There cannot be any flows to the return values of
		// gaps
		if (flow.source().getGap() != null && flow.source().getType() == SourceSinkType.Return)
			return null;

		// Reverse the flow if necessary
		return flow.reverse();
	}

	/**
	 * Checks whether objects of the given type can have aliases
	 * 
	 * @param type The type to check
	 * @return True if objects of the given type can have aliases, otherwise false
	 */
	private boolean canTypeAlias(String type) {
		Type tp = TypeUtils.getTypeFromString(type);
		if (tp instanceof PrimType)
			return false;
		if (tp instanceof RefType)
			if (((RefType) tp).getClassName().equals("java.lang.String"))
				return false;
		return true;
	}

	/**
	 * Spawns the analysis into a gap implementation inside user code
	 * 
	 * @param implementor The target method inside the user code into which the
	 *                    propagator shall be propagated
	 * @param propagator  The implementor that gets propagated into user code
	 * @return The taints at the end of the implementor method if a summary already
	 *         exists, otherwise false
	 */
	private Set<AccessPathPropagator> spawnAnalysisIntoClientCode(SootMethod implementor,
			AccessPathPropagator propagator) {
		// If the implementor has not yet been loaded, we must do this now
		if (!implementor.hasActiveBody()) {
			synchronized (implementor) {
				if (!implementor.hasActiveBody()) {
					implementor.retrieveActiveBody();
					manager.getICFG().notifyMethodChanged(implementor);
				}
			}
		}

		AccessPath ap = createAccessPathInMethod(propagator.getTaint(), implementor);
		Abstraction abs = new Abstraction(null, ap, null, null, false, false);

		// We need to pop the last gap element off the stack
		AccessPathPropagator parent = safePopParent(propagator);
		GapDefinition gap = propagator.getParent() == null ? null : propagator.getParent().getGap();

		// We might already have a summary for the callee
		Set<AccessPathPropagator> outgoingTaints = null;
		Set<Pair<Unit, Abstraction>> endSummary = manager.getForwardSolver().endSummary(implementor, abs);
		if (endSummary != null && !endSummary.isEmpty()) {
			for (Pair<Unit, Abstraction> pair : endSummary) {
				if (outgoingTaints == null)
					outgoingTaints = new HashSet<>();

				// Create the taint that corresponds to the access path leaving
				// the user-code method
				Set<Taint> newTaints = createTaintFromAccessPathOnReturn(pair.getO2().getAccessPath(),
						(Stmt) pair.getO1(), propagator.getGap());
				if (newTaints != null)
					for (Taint newTaint : newTaints) {
						AccessPathPropagator newPropagator = new AccessPathPropagator(newTaint, gap, parent,
								propagator.getParent() == null ? null : propagator.getParent().getStmt(),
								propagator.getParent() == null ? null : propagator.getParent().getD1(),
								propagator.getParent() == null ? null : propagator.getParent().getD2());
						outgoingTaints.add(newPropagator);
					}
			}
			return outgoingTaints;
		}

		// Create a new edge at the start point of the callee
		for (Unit sP : manager.getICFG().getStartPointsOf(implementor)) {
			PathEdge<Unit, Abstraction> edge = new PathEdge<>(abs, sP, abs);
			manager.getForwardSolver().processEdge(edge);
		}

		// Register the new context so that we can get the taints back
		this.userCodeTaints.put(new Pair<>(abs, implementor), propagator);
		return null;
	}

	private AccessPathPropagator safePopParent(AccessPathPropagator curPropagator) {
		if (curPropagator.getParent() == null)
			return null;
		return curPropagator.getParent().getParent();
	}

	/**
	 * Gets the flow summaries for the given gap definition, i.e., for the method in
	 * the gap
	 * 
	 * @param gap The gap definition
	 * @return The flow summaries for the method in the given gap if they exist,
	 *         otherwise null
	 */
	private MethodSummaries getFlowSummariesForGap(GapDefinition gap) {
		// If we have the method in Soot, we can be more clever
		if (Scene.v().containsMethod(gap.getSignature())) {
			SootMethod gapMethod = Scene.v().getMethod(gap.getSignature());
			ClassSummaries flows = getFlowSummariesForMethod(null, gapMethod, null);
			if (flows != null && !flows.isEmpty()) {
				MethodSummaries summaries = new MethodSummaries();
				summaries.mergeSummaries(flows.getAllMethodSummaries());
				return summaries;
			}
		}

		// If we don't have the method, we can only directly look for the
		// signature
		SootMethodAndClass smac = SootMethodRepresentationParser.v().parseSootMethodString(gap.getSignature());
		ClassMethodSummaries cms = flows.getMethodFlows(smac.getClassName(), smac.getSubSignature());
		return cms == null ? null : cms.getMethodSummaries();
	}

	/**
	 * Gets the flow summaries for the given method
	 * 
	 * @param stmt           (Optional) The invocation statement at which the given
	 *                       method is called. If this parameter is not null, it is
	 *                       used to find further potential callees if there are no
	 *                       flow summaries for the given method.
	 * @param method         The method for which to get the flow summaries
	 * @param classSupported Outgoing parameter that informs the caller whether the
	 *                       callee class is supported, i.e., there is a summary
	 *                       configuration for that class
	 * @return The set of flow summaries for the given method if they exist,
	 *         otherwise null. Note that this is a set of sets, one set per possible
	 *         callee.
	 */
	private ClassSummaries getFlowSummariesForMethod(Stmt stmt, final SootMethod method,
			ByReferenceBoolean classSupported) {
		return getFlowSummariesForMethod(stmt, method, null, classSupported);
	}

	/**
	 * Gets the flow summaries for the given method
	 * 
	 * @param stmt           (Optional) The invocation statement at which the given
	 *                       method is called. If this parameter is not null, it is
	 *                       used to find further potential callees if there are no
	 *                       flow summaries for the given method.
	 * @param method         The method for which to get the flow summaries
	 * @param taintedAbs     The tainted incoming abstraction
	 * @param classSupported Outgoing parameter that informs the caller whether the
	 *                       callee class is supported, i.e., there is a summary
	 *                       configuration for that class
	 * @return The set of flow summaries for the given method if they exist,
	 *         otherwise null. Note that this is a set of sets, one set per possible
	 *         callee.
	 */
	private ClassSummaries getFlowSummariesForMethod(Stmt stmt, final SootMethod method, Abstraction taintedAbs,
			ByReferenceBoolean classSupported) {
		final String subsig = method.getSubSignature();
		if (!flows.mayHaveSummaryForMethod(subsig))
			return ClassSummaries.EMPTY_SUMMARIES;

		ClassSummaries classSummaries = null;
		if (!method.isConstructor() && !method.isStaticInitializer() && !method.isStatic()) {
			// Check the callgraph
			if (stmt != null) {
				// Check the callees reported by the ICFG
				for (SootMethod callee : manager.getICFG().getCalleesOfCallAt(stmt)) {
					ClassMethodSummaries flows = this.flows.getMethodFlows(callee.getDeclaringClass(), subsig);
					if (flows != null && !flows.isEmpty()) {
						if (classSupported != null)
							classSupported.value = true;
						if (classSummaries == null)
							classSummaries = new ClassSummaries();
						classSummaries.merge("<dummy>", flows.getMethodSummaries());
					}
				}
			}
		}

		// We may have a call such as
		// x = editable.toString();
		// In that case, the callee is Object.toString(), since in the stub Android
		// JAR, the class android.text.Editable does not override toString(). On a
		// real device, it does. Consequently, we have a summary in the "Editable"
		// class. To handle such weird cases, we walk the class hierarchy based on
		// the declared type of the base object.
		SootClass declaredClass = null;
		if (stmt != null && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();
			Type baseType = iinv.getBase().getType();
			if (baseType instanceof RefType)
				declaredClass = ((RefType) baseType).getSootClass();
		}

		// Check the direct callee
		if (classSummaries == null || classSummaries.isEmpty()) {
			SummaryResponse response = methodToImplFlows
					.getUnchecked(new SummaryQuery(method.getDeclaringClass(), declaredClass, subsig));
			if (response != null) {
				if (classSupported != null)
					classSupported.value = response.isClassSupported;
				classSummaries = new ClassSummaries();
				classSummaries.merge(response.classSummaries);
			}
		}

		return classSummaries;
	}

	/**
	 * Gets all methods that implement the given abstract method. These are all
	 * concrete methods with the same signature in all derived classes.
	 * 
	 * @param method The method for which to find implementations
	 * @return A set containing all implementations of the given method
	 */
	private Collection<SootMethod> getAllImplementors(SootMethod method) {
		final String subSig = method.getSubSignature();
		Set<SootMethod> implementors = new HashSet<SootMethod>();

		List<SootClass> workList = new ArrayList<SootClass>();
		workList.add(method.getDeclaringClass());
		Set<SootClass> doneSet = new HashSet<SootClass>();

		while (!workList.isEmpty()) {
			SootClass curClass = workList.remove(0);
			if (!doneSet.add(curClass))
				continue;

			if (curClass.isInterface()) {
				workList.addAll(hierarchy.getImplementersOf(curClass));
				workList.addAll(hierarchy.getSubinterfacesOf(curClass));
			} else
				workList.addAll(hierarchy.getSubclassesOf(curClass));

			SootMethod ifm = curClass.getMethodUnsafe(subSig);
			if (ifm != null)
				implementors.add(ifm);
		}

		return implementors;
	}

	/**
	 * Gets all child classes of the given class. If the given class is an
	 * interface, all implementors of this interface and its all of child-
	 * interfaces are returned.
	 * 
	 * @param sc The class or interface for which to get the children
	 * @return The children of the given class or interface
	 */
	private Set<SootClass> getAllChildClasses(SootClass sc) {
		List<SootClass> workList = new ArrayList<SootClass>();
		workList.add(sc);

		Set<SootClass> doneSet = new HashSet<SootClass>();
		Set<SootClass> classes = new HashSet<>();

		while (!workList.isEmpty()) {
			SootClass curClass = workList.remove(0);
			if (!doneSet.add(curClass))
				continue;

			if (curClass.isInterface()) {
				workList.addAll(hierarchy.getImplementersOf(curClass));
				workList.addAll(hierarchy.getSubinterfacesOf(curClass));
			} else {
				workList.addAll(hierarchy.getSubclassesOf(curClass));
				classes.add(curClass);
			}
		}

		return classes;
	}

	/**
	 * Gets all parent classes of the given class. If the given class is an
	 * interface, all parent implementors of this interface are returned.
	 * 
	 * @param sc The class or interface for which to get the parents
	 * @return The parents of the given class or interface
	 */
	private Set<SootClass> getAllParentClasses(SootClass sc) {
		List<SootClass> workList = new ArrayList<SootClass>();
		workList.add(sc);

		Set<SootClass> doneSet = new HashSet<SootClass>();
		Set<SootClass> classes = new HashSet<>();

		while (!workList.isEmpty()) {
			SootClass curClass = workList.remove(0);
			if (!doneSet.add(curClass))
				continue;

			if (curClass.isInterface()) {
				workList.addAll(hierarchy.getSuperinterfacesOf(curClass));
			} else {
				workList.addAll(hierarchy.getSuperclassesOf(curClass));
				classes.add(curClass);
			}
		}

		return classes;
	}

	/**
	 * Applies a data flow summary to a given tainted access path
	 * 
	 * @param flow       The data flow summary to apply
	 * @param propagator The access path propagator on which to apply the given flow
	 * @return The access path propagator obtained by applying the given data flow
	 *         summary to the given access path propagator. if the summary is not
	 *         applicable, null is returned.
	 */
	private AccessPathPropagator applyFlow(MethodFlow flow, AccessPathPropagator propagator) {
		final AbstractFlowSinkSource flowSource = flow.source();
		AbstractFlowSinkSource flowSink = flow.sink();
		final Taint taint = propagator.getTaint();

		// Make sure that the base type of the incoming taint and the one of
		// the summary are compatible
		boolean typesCompatible = flowSource.getBaseType() == null
				|| isCastCompatible(TypeUtils.getTypeFromString(taint.getBaseType()),
						TypeUtils.getTypeFromString(flowSource.getBaseType()));
		if (!typesCompatible)
			return null;

		// If this flow starts at a gap, our current taint must be at that gap
		if (taint.getGap() != flow.source().getGap())
			return null;

		// Maintain the stack of access path propagations
		final AccessPathPropagator parent;
		final GapDefinition gap, taintGap;
		final Stmt stmt;
		final Abstraction d1, d2;
		if (flowSink.getGap() != null) { // ends in gap, push on stack
			parent = propagator;
			gap = flowSink.getGap();
			stmt = null;
			d1 = null;
			d2 = null;
			taintGap = null;
		} else {
			parent = safePopParent(propagator);
			gap = propagator.getParent() == null ? null : propagator.getParent().getGap();
			stmt = propagator.getParent() == null ? propagator.getStmt() : propagator.getParent().getStmt();
			d1 = propagator.getParent() == null ? propagator.getD1() : propagator.getParent().getD1();
			d2 = propagator.getParent() == null ? propagator.getD2() : propagator.getParent().getD2();
			taintGap = propagator.getGap();
		}

		boolean addTaint = flowMatchesTaint(flowSource, taint);

		// If we didn't find a match, there's little we can do
		if (!addTaint)
			return null;

		// Construct a new propagator
		Taint newTaint = null;
		if (flow.isCustom()) {
			newTaint = addCustomSinkTaint(flow, taint, taintGap);
		} else
			newTaint = addSinkTaint(flow, taint, taintGap);
		if (newTaint == null)
			return null;

		AccessPathPropagator newPropagator = new AccessPathPropagator(newTaint, gap, parent, stmt, d1, d2);
		return newPropagator;
	}

	/**
	 * Checks whether the given source matches the given taint
	 * 
	 * @param flowSource The source to match
	 * @param taint      The taint to match
	 * @return True if the given source matches the given taint, otherwise false
	 */
	private boolean flowMatchesTaint(final AbstractFlowSinkSource flowSource, final Taint taint) {
		if (flowSource.isParameter() && taint.isParameter()) {
			// Get the parameter index from the call and compare it to the
			// parameter index in the flow summary
			if (taint.getParameterIndex() == flowSource.getParameterIndex()) {
				if (compareFields(taint, flowSource))
					return true;
			}
		} else if (flowSource.isField()) {
			// Flows from a field can either be applied to the same field or
			// the base object in total
			boolean doTaint = (taint.isGapBaseObject() || taint.isField());
			if (doTaint && compareFields(taint, flowSource))
				return true;
		}
		// We can have a flow from a local or a field
		else if (flowSource.isThis() && taint.isField())
			return true;
		// A value can also flow from the return value of a gap to somewhere
		else if (flowSource.isReturn() && flowSource.getGap() != null && taint.getGap() != null
				&& compareFields(taint, flowSource))
			return true;
		// For aliases, we over-approximate flows from the return edge to all
		// possible exit nodes
		else if (flowSource.isReturn() && flowSource.getGap() == null && taint.getGap() == null && taint.isReturn()
				&& compareFields(taint, flowSource))
			return true;
		return false;
	}

	/**
	 * Checks whether the type tracked in the access path is compatible with the
	 * type of the base object expected by the flow summary
	 * 
	 * @param baseType  The base type tracked in the access path
	 * @param checkType The type in the summary
	 * @return True if the tracked base type is compatible with the type expected by
	 *         the flow summary, otherwise false
	 */
	private boolean isCastCompatible(Type baseType, Type checkType) {
		if (baseType == null || checkType == null)
			return false;

		if (baseType == Scene.v().getObjectType())
			return checkType instanceof RefType;
		if (checkType == Scene.v().getObjectType())
			return baseType instanceof RefType;

		return baseType == checkType || fastHierarchy.canStoreType(baseType, checkType)
				|| fastHierarchy.canStoreType(checkType, baseType);
	}

	/**
	 * Gets the parameter index to which the given access path refers
	 * 
	 * @param stmt  The invocation statement
	 * @param curAP The access path
	 * @return The parameter index to which the given access path refers if it
	 *         exists. Otherwise, if the given access path does not refer to a
	 *         parameter, -1 is returned.
	 */
	private int getParameterIndex(Stmt stmt, AccessPath curAP) {
		if (!stmt.containsInvokeExpr())
			return -1;
		if (curAP.isStaticFieldRef())
			return -1;

		final InvokeExpr iexpr = stmt.getInvokeExpr();
		for (int i = 0; i < iexpr.getArgCount(); i++)
			if (iexpr.getArg(i) == curAP.getPlainValue())
				return i;
		return -1;
	}

	/**
	 * Gets the parameter index to which the given access path refers
	 * 
	 * @param sm    The method in which to check the parameter locals
	 * @param curAP The access path
	 * @return The parameter index to which the given access path refers if it
	 *         exists. Otherwise, if the given access path does not refer to a
	 *         parameter, -1 is returned.
	 */
	private int getParameterIndex(SootMethod sm, AccessPath curAP) {
		if (curAP.isStaticFieldRef())
			return -1;

		for (int i = 0; i < sm.getParameterCount(); i++)
			if (curAP.getPlainValue() == sm.getActiveBody().getParameterLocal(i))
				return i;
		return -1;
	}

	/**
	 * Checks whether the fields mentioned in the given taint correspond to those of
	 * the given flow source
	 * 
	 * @param taintedPath The tainted access path
	 * @param flowSource  The flow source with which to compare the taint
	 * @return True if the given taint references the same fields as the given flow
	 *         source, otherwise false
	 */
	private boolean compareFields(Taint taintedPath, AbstractFlowSinkSource flowSource) {
		// if we have x.f....fn and the source is x.f'.f1'...f'n+1 and we don't
		// taint sub, we can't have a match
		if (taintedPath.getAccessPathLength() < flowSource.getAccessPathLength()) {
			if (!taintedPath.taintSubFields() || flowSource.isMatchStrict())
				return false;
		}

		// Compare the shared sub-path
		for (int i = 0; i < taintedPath.getAccessPathLength() && i < flowSource.getAccessPathLength(); i++) {
			String taintField = taintedPath.getAccessPath()[i];
			String sourceField = flowSource.getAccessPath()[i];
			if (!sourceField.equals(taintField))
				return false;
		}

		return true;
	}

	/**
	 * Gets the field with the specified signature if it exists, otherwise returns
	 * null
	 * 
	 * @param fieldSig The signature of the field to retrieve
	 * @return The field with the given signature if it exists, otherwise null
	 */
	private SootField safeGetField(String fieldSig) {
		if (fieldSig == null || fieldSig.equals(""))
			return null;

		SootField sf = Scene.v().grabField(fieldSig);
		if (sf != null)
			return sf;

		// This field does not exist, so we need to create it
		String className = fieldSig.substring(1);
		className = className.substring(0, className.indexOf(":"));
		SootClass sc = Scene.v().getSootClassUnsafe(className, true);
		if (sc.resolvingLevel() < SootClass.SIGNATURES && !sc.isPhantom()) {
			System.err.println("WARNING: Class not loaded: " + sc);
			return null;
		}

		String type = fieldSig.substring(fieldSig.indexOf(": ") + 2);
		type = type.substring(0, type.indexOf(" "));

		String fieldName = fieldSig.substring(fieldSig.lastIndexOf(" ") + 1);
		fieldName = fieldName.substring(0, fieldName.length() - 1);

		return Scene.v().makeFieldRef(sc, fieldName, TypeUtils.getTypeFromString(type), false).resolve();
	}

	/**
	 * Gets an array of fields with the specified signatures
	 * 
	 * @param fieldSigs , list of the field signatures to retrieve
	 * @return The Array of fields with the given signature if all exists, otherwise
	 *         null
	 */
	private SootField[] safeGetFields(String[] fieldSigs) {
		if (fieldSigs == null || fieldSigs.length == 0)
			return null;
		SootField[] fields = new SootField[fieldSigs.length];
		for (int i = 0; i < fieldSigs.length; i++) {
			fields[i] = safeGetField(fieldSigs[i]);
			if (fields[i] == null)
				return null;
		}
		return fields;
	}

	/**
	 * Gets an array of types with the specified class names
	 * 
	 * @param fieldTypes , list of the type names to retrieve
	 * @param fields     The fields from which to get the types if we don't have any
	 *                   explicit ones
	 * @return The Array of fields with the given signature if all exists, otherwise
	 *         null
	 */
	private Type[] safeGetTypes(String[] fieldTypes, SootField[] fields) {
		if (fieldTypes == null || fieldTypes.length == 0) {
			// If we don't have type information, but fields, we can use the declared field
			// types
			if (fields != null && fields.length > 0) {
				Type[] types = new Type[fields.length];
				for (int i = 0; i < fields.length; i++)
					types[i] = fields[i].getType();
				return types;
			}
			return null;
		}

		// Parse the explicit type information
		Type[] types = new Type[fieldTypes.length];
		for (int i = 0; i < fieldTypes.length; i++)
			types[i] = TypeUtils.getTypeFromString(fieldTypes[i]);
		return types;
	}

	/**
	 * Given the taint at the source and the flow, computes the taint at the sink.
	 * This method allows custom extensions to the taint wrapper. The default
	 * implementation always returns null.
	 * 
	 * @param flow  The flow between source and sink
	 * @param taint The taint at the source statement
	 * @param gap   The gap at which the new flow will hold
	 * @return The taint at the sink that is obtained when applying the given flow
	 *         to the given source taint
	 */
	protected Taint addCustomSinkTaint(MethodFlow flow, Taint taint, GapDefinition gap) {
		return null;
	}

	/**
	 * Given the taint at the source and the flow, computes the taint at the sink
	 * 
	 * @param flow  The flow between source and sink
	 * @param taint The taint at the source statement
	 * @param gap   The gap at which the new flow will hold
	 * @return The taint at the sink that is obtained when applying the given flow
	 *         to the given source taint
	 */
	private Taint addSinkTaint(MethodFlow flow, Taint taint, GapDefinition gap) {
		final AbstractFlowSinkSource flowSource = flow.source();
		final AbstractFlowSinkSource flowSink = flow.sink();
		final boolean taintSubFields = flow.sink().taintSubFields();
		final boolean checkTypes = flow.getTypeChecking();

		final String[] remainingFields = flow.getCutSubFields() ? null : getRemainingFields(flowSource, taint);
		final String[] remainingFieldTypes = flow.getCutSubFields() ? null : getRemainingFieldTypes(flowSource, taint);

		final String[] appendedFields = append(flowSink.getAccessPath(), remainingFields);
		final String[] appendedFieldTypes = append(flowSink.getAccessPathTypes(), remainingFieldTypes, true);

		int lastCommonAPIdx = Math.min(flowSource.getAccessPathLength(), taint.getAccessPathLength());

		Type sinkType = TypeUtils.getTypeFromString(getAssignmentType(flowSink));
		Type taintType = TypeUtils.getTypeFromString(getAssignmentType(taint, lastCommonAPIdx - 1));

		if (checkTypes) {
			// For type checking, we need types
			if (sinkType == null || taintType == null)
				return null;

			// If we taint something in the base object, its type must match. We
			// might have a taint for "a" in o.add(a) and need to check whether
			// "o" matches the expected type in our summary.
			if (!(sinkType instanceof PrimType) && !isCastCompatible(taintType, sinkType)
					&& flowSink.getType() == SourceSinkType.Field && !checkTypes) {
				// If the target is an array, the value might also flow into an
				// element
				boolean found = false;
				while (sinkType instanceof ArrayType) {
					sinkType = ((ArrayType) sinkType).getElementType();
					if (isCastCompatible(taintType, sinkType)) {
						found = true;
						break;
					}
				}
				while (taintType instanceof ArrayType) {
					taintType = ((ArrayType) taintType).getElementType();
					if (isCastCompatible(taintType, sinkType)) {
						found = true;
						break;
					}
				}
				if (!found)
					return null;
			}
		}

		// If we enter a gap with a type "GapBaseObject", we need to convert
		// it to a regular field
		SourceSinkType sourceSinkType = flowSink.getType();
		if (flowSink.getType() == SourceSinkType.GapBaseObject && remainingFields != null && remainingFields.length > 0)
			sourceSinkType = SourceSinkType.Field;

		// Compute the new base type
		Type newBaseType = TypeUtils.getMorePreciseType(taintType, sinkType);
		if (newBaseType == null)
			newBaseType = sinkType;
		String sBaseType = sinkType == null ? null : "" + sinkType;

		// Set the correct type. In case x -> b.x, the new type is not the type
		// of b, but of the field x.
		if (flowSink.hasAccessPath()) {
			if (appendedFieldTypes != null)
				appendedFieldTypes[flowSink.getAccessPathLength() - 1] = "" + newBaseType;
			sBaseType = flowSink.getBaseType();
		}

		// Taint the correct fields
		return new Taint(sourceSinkType, flowSink.getParameterIndex(), sBaseType, appendedFields, appendedFieldTypes,
				taintSubFields || taint.taintSubFields(), gap);
	}

	/**
	 * Gets the type at the given position from a taint.
	 * 
	 * @param taint The taint from which to get the propagation type
	 * @param idx   The index inside the access path from which to get the type. -1
	 *              refers to the base type
	 * @return The type at the given index inside the access path
	 */
	private String getAssignmentType(Taint taint, int idx) {
		if (idx < 0)
			return taint.getBaseType();
		return taint.getAccessPathTypes() == null ? null : taint.getAccessPathTypes()[idx];
	}

	/**
	 * Gets the type that is finally assigned when propagating this source or sink.
	 * For an access path a.b.c, this would be the type of "c".
	 * 
	 * @param srcSink The source or sink from which to get the propagation type
	 * @return The type of the value which the access path of the given source or
	 *         sink finally references
	 */
	private String getAssignmentType(AbstractFlowSinkSource srcSink) {
		if (!srcSink.hasAccessPath())
			return srcSink.getBaseType();

		// If we don't have explicit access path types, we use the declared
		// types instead
		if (srcSink.getAccessPathTypes() == null && srcSink.getAccessPath() != null) {
			String[] ap = srcSink.getAccessPath();
			String apElement = ap[srcSink.getAccessPathLength() - 1];

			Pattern pattern = Pattern.compile("^\\s*<(.*?):\\s*(.*?)>\\s*$");
			Matcher matcher = pattern.matcher(apElement);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}

		return srcSink.getAccessPathTypes() == null ? null
				: srcSink.getAccessPathTypes()[srcSink.getAccessPathLength() - 1];
	}

	/**
	 * Concatenates the two given arrays to one bigger array
	 * 
	 * @param fields          The first array
	 * @param remainingFields The second array
	 * @return The concatenated array containing all elements from both given arrays
	 */
	private String[] append(String[] fields, String[] remainingFields) {
		return append(fields, remainingFields, false);
	}

	/**
	 * Concatenates the two given arrays to one bigger array
	 * 
	 * @param fields          The first array
	 * @param remainingFields The second array
	 * @return The concatenated array containing all elements from both given arrays
	 */
	private String[] append(String[] fields, String[] remainingFields, boolean alwaysCopy) {
		if (fields == null) {
			if (alwaysCopy && remainingFields != null)
				return Arrays.copyOf(remainingFields, remainingFields.length);
			else
				return remainingFields;
		}
		if (remainingFields == null) {
			if (alwaysCopy && fields != null)
				return Arrays.copyOf(fields, fields.length);
			else
				return fields;
		}

		int cnt = fields.length + remainingFields.length;
		String[] appended = new String[cnt];
		System.arraycopy(fields, 0, appended, 0, fields.length);
		System.arraycopy(remainingFields, 0, appended, fields.length, remainingFields.length);
		return appended;
	}

	/**
	 * Gets the remaining fields which are tainted, but not covered by the given
	 * flow summary source
	 * 
	 * @param flowSource  The flow summary source
	 * @param taintedPath The tainted access path
	 * @return The remaining fields which are tainted in the given access path, but
	 *         which are not covered by the given flow summary source
	 */
	private String[] getRemainingFields(AbstractFlowSinkSource flowSource, Taint taintedPath) {
		if (!flowSource.hasAccessPath())
			return taintedPath.getAccessPath();

		int fieldCnt = taintedPath.getAccessPathLength() - flowSource.getAccessPathLength();
		if (fieldCnt <= 0)
			return null;

		String[] fields = new String[fieldCnt];
		System.arraycopy(taintedPath.getAccessPath(), flowSource.getAccessPathLength(), fields, 0, fieldCnt);
		return fields;
	}

	/**
	 * Gets the types of the remaining fields which are tainted, but not covered by
	 * the given flow summary source
	 * 
	 * @param flowSource  The flow summary source
	 * @param taintedPath The tainted access path
	 * @return The types of the remaining fields which are tainted in the given
	 *         access path, but which are not covered by the given flow summary
	 *         source
	 */
	private String[] getRemainingFieldTypes(AbstractFlowSinkSource flowSource, Taint taintedPath) {
		if (!flowSource.hasAccessPath())
			return taintedPath.getAccessPathTypes();

		int fieldCnt = taintedPath.getAccessPathLength() - flowSource.getAccessPathLength();
		if (fieldCnt <= 0)
			return null;

		String[] fields = new String[fieldCnt];
		System.arraycopy(taintedPath.getAccessPathTypes(), flowSource.getAccessPathLength(), fields, 0, fieldCnt);
		return fields;
	}

	/**
	 * Gets the base object on which the method is invoked
	 * 
	 * @param stmt The statement for which to get the base of the method call
	 * @return The base object of the method call if it exists, otherwise null
	 */
	private Value getMethodBase(Stmt stmt) {
		if (!stmt.containsInvokeExpr())
			throw new RuntimeException("Statement is not a method call: " + stmt);
		InvokeExpr invExpr = stmt.getInvokeExpr();
		if (invExpr instanceof InstanceInvokeExpr)
			return ((InstanceInvokeExpr) invExpr).getBase();
		return null;
	}

	@Override
	public boolean isExclusive(Stmt stmt, Abstraction taintedPath) {
		// If we directly support the callee, we are exclusive in any case
		if (supportsCallee(stmt))
			return true;

		// If the fallback wrapper supports the method and is exclusive for it,
		// we are as well
		if (fallbackWrapper != null && fallbackWrapper.isExclusive(stmt, taintedPath))
			return true;

		// We may also be exclusive for a complete class
		if (stmt.containsInvokeExpr()) {
			SootClass targetClass = stmt.getInvokeExpr().getMethod().getDeclaringClass();
			// The target class should never be null, but it happened
			if (targetClass != null) {
				// Are the class flows configured to be exclusive?
				final String targetClassName = targetClass.getName();
				ClassMethodSummaries cms = flows.getClassFlows(targetClassName);
				if (cms != null && cms.isExclusiveForClass())
					return true;

				// Check for classes excluded by meta data
				ClassSummaries summaries = flows.getSummaries();
				SummaryMetaData metaData = summaries.getMetaData();
				if (metaData != null) {
					if (metaData.isClassExclusive(targetClassName))
						return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean supportsCallee(SootMethod method) {
		// Check whether we directly support that class. We assume that if we
		// have some summary for that class, we have all summaries for that
		// class.
		if (flows.supportsClass(method.getDeclaringClass().getName()))
			return true;

		return false;
	}

	@Override
	public boolean supportsCallee(Stmt callSite) {
		if (!callSite.containsInvokeExpr())
			return false;

		if (manager == null) {
			// If we support the method, we are exclusive for it
			SootMethod method = callSite.getInvokeExpr().getMethod();
			if (supportsCallee(method))
				return true;
		} else {
			// Check if we are exclusive for any potential callee
			for (SootMethod callee : manager.getICFG().getCalleesOfCallAt(callSite))
				if (!callee.isStaticInitializer())
					if (supportsCallee(callee))
						return true;
		}

		return false;
	}

	/**
	 * Gets the propagators that have been registered as being passed into user code
	 * with the given context and for the given callee
	 * 
	 * @param abs    The context abstraction with which the taint was passed into
	 *               the callee
	 * @param callee The callee into which the taint was passed
	 * @return The of taint propagators passed into the given callee with the given
	 *         context. If no such propagators exist, null is returned.
	 */
	Set<AccessPathPropagator> getUserCodeTaints(Abstraction abs, SootMethod callee) {
		return this.userCodeTaints.get(new Pair<>(abs, callee));
	}

	@Override
	public int getWrapperHits() {
		return wrapperHits.get();
	}

	@Override
	public int getWrapperMisses() {
		return wrapperMisses.get();
	}

	@Override
	public Set<Abstraction> getAliasesForMethod(Stmt stmt, Abstraction d1, Abstraction taintedAbs) {
		// We only care about method invocations
		if (!stmt.containsInvokeExpr())
			return Collections.singleton(taintedAbs);

		// Get the cached data flows
		final SootMethod method = stmt.getInvokeExpr().getMethod();
		ClassSummaries flowsInCallees = getFlowSummariesForMethod(stmt, method, null);

		// If we have no data flows, we can abort early
		if (flowsInCallees.isEmpty()) {
			if (fallbackWrapper == null)
				return null;
			else
				return fallbackWrapper.getAliasesForMethod(stmt, d1, taintedAbs);
		}

		// Create a level-0 propagator for the initially tainted access path
		Set<Taint> taintsFromAP = createTaintFromAccessPathOnCall(taintedAbs.getAccessPath(), stmt, true);
		if (taintsFromAP == null || taintsFromAP.isEmpty())
			return Collections.emptySet();

		Set<AccessPath> res = null;
		for (String className : flowsInCallees.getClasses()) {
			List<AccessPathPropagator> workList = new ArrayList<AccessPathPropagator>();
			for (Taint taint : taintsFromAP)
				workList.add(new AccessPathPropagator(taint, null, null, stmt, d1, taintedAbs, true));

			// Get the flows in this class
			ClassMethodSummaries classFlows = flowsInCallees.getClassSummaries(className);
			if (classFlows == null)
				continue;

			// Get the method-level flows
			MethodSummaries flowsInCallee = classFlows.getMethodSummaries();
			if (flowsInCallee == null || flowsInCallee.isEmpty())
				continue;

			// Apply the data flows until we reach a fixed point
			Set<AccessPath> resCallee = applyFlowsIterative(flowsInCallee, workList);
			if (resCallee != null && !resCallee.isEmpty()) {
				if (res == null)
					res = new HashSet<>();
				res.addAll(resCallee);
			}
		}

		// We always retain the incoming taint
		if (res == null || res.isEmpty())
			return Collections.singleton(taintedAbs);

		// Create abstractions from the access paths
		Set<Abstraction> resAbs = new HashSet<>(res.size() + 1);
		resAbs.add(taintedAbs);
		for (AccessPath ap : res) {
			Abstraction newAbs = taintedAbs.deriveNewAbstraction(ap, stmt);
			newAbs.setCorrespondingCallSite(stmt);
			resAbs.add(newAbs);
		}
		return resAbs;
	}

	/**
	 * Sets whether missing summaries for classes shall be reported on the
	 * command-line
	 * 
	 * @param report True if missing summaries shall be reported on the command
	 *               line, otherwise false
	 */
	public void setReportMissingDummaries(boolean report) {
		this.reportMissingSummaries = report;
	}

	/**
	 * Sets the fallback taint wrapper to be used if there is no StubDroid summary
	 * for a certain class
	 * 
	 * @param fallbackWrapper The fallback taint wrapper to be used if there is no
	 *                        StubDroid summary for a certain class
	 */
	public void setFallbackTaintWrapper(ITaintPropagationWrapper fallbackWrapper) {
		this.fallbackWrapper = fallbackWrapper;
	}

	/**
	 * Gets the provider from which this taint wrapper loads it flows
	 * 
	 * @return The provider from which this taint wrapper loads it flows
	 */
	public IMethodSummaryProvider getProvider() {
		return this.flows;
	}

	@Override
	public Set<Abstraction> getInverseTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedAbs) {
		// We only care about method invocations
		if (!stmt.containsInvokeExpr())
			return Collections.singleton(taintedAbs);

		// Get the cached data flows
		final SootMethod method = stmt.getInvokeExpr().getMethod();
		ClassSummaries flowsInCallees = getFlowSummariesForMethod(stmt, method, null);

		// If we have no data flows, we can abort early
		if (flowsInCallees.isEmpty()) {
			if (fallbackWrapper != null && fallbackWrapper instanceof IReversibleTaintWrapper)
				return ((IReversibleTaintWrapper) fallbackWrapper).getInverseTaintsForMethod(stmt, d1, taintedAbs);
			else
				return null;
		}

		// Create a level-0 propagator for the initially tainted access path
		Set<Taint> taintsFromAP = createTaintFromAccessPathOnCall(taintedAbs.getAccessPath(), stmt, true);
		if (taintsFromAP == null || taintsFromAP.isEmpty())
			return Collections.emptySet();

		Set<AccessPath> res = null;
		for (String className : flowsInCallees.getClasses()) {
			List<AccessPathPropagator> workList = new ArrayList<AccessPathPropagator>();
			for (Taint taint : taintsFromAP)
				workList.add(new AccessPathPropagator(taint, null, null, stmt, d1, taintedAbs, true));

			// Get the flows in this class
			ClassMethodSummaries classFlows = flowsInCallees.getClassSummaries(className);
			if (classFlows == null)
				continue;

			// Get the method-level flows
			MethodSummaries flowsInCallee = classFlows.getMethodSummaries();
			if (flowsInCallee == null || flowsInCallee.isEmpty())
				continue;

			// Since we are scanning backwards, we need to reverse the flows
			flowsInCallee = flowsInCallee.reverse();

			// Apply the data flows until we reach a fixed point
			Set<AccessPath> resCallee = applyFlowsIterative(flowsInCallee, workList);
			if (resCallee != null && !resCallee.isEmpty()) {
				if (res == null)
					res = new HashSet<>();
				res.addAll(resCallee);
			}
		}

		// We always retain the incoming taint
		if (res == null || res.isEmpty())
			return Collections.singleton(taintedAbs);

		// Create abstractions from the access paths
		Set<Abstraction> resAbs = new HashSet<>(res.size() + 1);
		resAbs.add(taintedAbs);
		for (AccessPath ap : res) {
			Abstraction newAbs = taintedAbs.deriveNewAbstraction(ap, stmt);
			newAbs.setCorrespondingCallSite(stmt);
			resAbs.add(newAbs);
		}
		return resAbs;
	}

}
