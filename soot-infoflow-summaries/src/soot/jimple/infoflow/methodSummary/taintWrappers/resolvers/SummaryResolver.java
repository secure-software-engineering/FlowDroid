package soot.jimple.infoflow.methodSummary.taintWrappers.resolvers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import heros.solver.IDESolver;
import soot.Hierarchy;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider;
import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.ClassSummaries;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * A resolver for finding all applicable summaries for a given method call
 * 
 * @author Steven Arzt
 *
 */
public class SummaryResolver {

	private static final int MAX_HIERARCHY_DEPTH = 10;

	protected final LoadingCache<SummaryQuery, SummaryResponse> methodToImplFlows = IDESolver.DEFAULT_CACHE_BUILDER
			.build(new CacheLoader<SummaryQuery, SummaryResponse>() {
				@Override
				public SummaryResponse load(SummaryQuery query) throws Exception {
					final SootClass calleeClass = query.calleeClass;
					final SootClass declaredClass = query.declaredClass;
					final String methodSig = query.subsignature;
					final ClassSummaries classSummaries = new ClassSummaries();
					boolean directHit = false;
					ByReferenceBoolean isClassSupported = new ByReferenceBoolean(false);

					// Get the flows in the target method
					if (calleeClass != null)
						directHit = getSummaries(methodSig, classSummaries, calleeClass, isClassSupported);

					// If we haven't found any summaries, we look at the class from the declared
					// type at the call site
					if (declaredClass != null && !directHit)
						directHit = getSummaries(methodSig, classSummaries, declaredClass, isClassSupported);

					// If we still don't have anything, we must try the hierarchy. Since this
					// best-effort approach can be fairly imprecise, it is our last resort.
					if (!directHit && calleeClass != null)
						directHit = getSummariesHierarchy(methodSig, classSummaries, calleeClass, isClassSupported);
					if (declaredClass != null && !directHit)
						directHit = getSummariesHierarchy(methodSig, classSummaries, declaredClass, isClassSupported);

					if (directHit && !classSummaries.isEmpty())
						return new SummaryResponse(classSummaries, true);
					else if (directHit || isClassSupported.value)
						return SummaryResponse.EMPTY_BUT_SUPPORTED;
					else
						return SummaryResponse.NOT_SUPPORTED;
				}

				private void updateClassExclusive(ByReferenceBoolean classSupported, SootClass sc, String subsig) {
					if (classSupported.value)
						return;

					if (sc.getMethodUnsafe(subsig) == null)
						return;

					ClassMethodSummaries cms = flows.getClassFlows(sc.getName());
					classSupported.value |= cms != null && cms.isExclusiveForClass();
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
				private boolean getSummaries(final String methodSig, final ClassSummaries summaries, SootClass clazz,
						ByReferenceBoolean classSupported) {
					// Do we have direct support for the target class?
					if (summaries.merge(flows.getMethodFlows(clazz, methodSig)))
						return true;

					// Do we support any interface this class might have?
					if (checkInterfaces(methodSig, summaries, clazz, classSupported))
						return true;

					updateClassExclusive(classSupported, clazz, methodSig);

					SootMethod targetMethod = clazz.getMethodUnsafe(methodSig);
					// If the target is abstract and we haven't found any flows,
					// we check for child classes
					if (!clazz.isConcrete() || targetMethod == null || !targetMethod.isConcrete()) {
						for (SootClass parentClass : getAllParentClasses(clazz)) {
							// Do we have support for the target class?
							if (summaries.merge(flows.getMethodFlows(parentClass, methodSig)))
								return true;

							// Do we support any interface this class might have?
							if (checkInterfaces(methodSig, summaries, parentClass, classSupported))
								return true;

							updateClassExclusive(classSupported, parentClass, methodSig);
						}
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
						SootClass clazz, ByReferenceBoolean classSupported) {
					// Don't try to look up the whole Java hierarchy
					if (clazz.getName().equals("java.lang.Object"))
						return false;

					// If the target is abstract and we haven't found any flows,
					// we check for child classes. Since the summaries are class-specific and we
					// don't really know which child class we're looking for, we have to merge the
					// flows for all possible classes.
					SootMethod targetMethod = clazz.getMethodUnsafe(methodSig);
					if (!clazz.isConcrete() || targetMethod == null || !targetMethod.isConcrete()) {
						int found = 0;
						Set<SootClass> childClasses = getAllChildClasses(clazz);
						for (SootClass childClass : childClasses) {
							// Do we have support for the target class?
							if (summaries.merge(flows.getMethodFlows(childClass, methodSig)))
								found++;

							// Do we support any interface this class might have?
							if (checkInterfaces(methodSig, summaries, childClass, classSupported))
								found++;

							updateClassExclusive(classSupported, childClass, methodSig);

							// If we have too many summaries that could be applicable, we abort here to
							// avoid false positives
							if (found > MAX_HIERARCHY_DEPTH)
								return false;
						}
						return found > 0;
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
				private boolean checkInterfaces(String methodSig, ClassSummaries summaries, SootClass clazz,
						ByReferenceBoolean classSupported) {
					for (SootClass intf : clazz.getInterfaces()) {
						// Directly check the interface
						if (summaries.merge(flows.getMethodFlows(intf, methodSig)))
							return true;

						for (SootClass parent : getAllParentClasses(intf)) {
							// Do we have support for the interface?
							if (summaries.merge(flows.getMethodFlows(parent, methodSig)))
								return true;

							updateClassExclusive(classSupported, parent, methodSig);
						}
					}

					// We inject the hierarchy from summaries before the data flow analysis, thus
					// the soot hierarchy already contains the manual information provided in the
					// xmls.
					return false;
				}

			});

	protected final IMethodSummaryProvider flows;
	private final Hierarchy hierarchy;

	/**
	 * Creates a new instance of the {@link SummaryResolver} class
	 * 
	 * @param flows The provider that supplies the data flow summaries
	 */
	public SummaryResolver(IMethodSummaryProvider flows) {
		this.flows = flows;
		this.hierarchy = Scene.v().getActiveHierarchy();
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
				List<SootClass> hierarchyImplementers = hierarchy.getImplementersOf(curClass);
				workList.addAll(hierarchyImplementers);

				List<SootClass> subinterfaces = hierarchy.getSubinterfacesOf(curClass);
				workList.addAll(subinterfaces);
			} else {
				List<SootClass> hierarchyClasses = hierarchy.getSubclassesOf(curClass);
				workList.addAll(hierarchyClasses);
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
				List<SootClass> hierarchyClasses = hierarchy.getSuperinterfacesOf(curClass);
				workList.addAll(hierarchyClasses);
				classes.add(curClass);
			} else {
				List<SootClass> hierarchyClasses = hierarchy.getSuperclassesOf(curClass);
				workList.addAll(hierarchyClasses);
				classes.add(curClass);
			}
		}

		return classes;
	}

	/**
	 * Resolves the given query for a data flow summary
	 * 
	 * @param query The query that defines the data flow summary to obtain
	 * @return The data flow summaries that match the given query
	 */
	public SummaryResponse resolve(SummaryQuery query) {
		return methodToImplFlows.getUnchecked(query);
	}

}
