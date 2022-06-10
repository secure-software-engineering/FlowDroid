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

					if (isClassSupported) {
						if (classSummaries.isEmpty())
							return SummaryResponse.EMPTY_BUT_SUPPORTED;
						return new SummaryResponse(classSummaries, isClassSupported);
					} else
						return SummaryResponse.NOT_SUPPORTED;
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
							if (checkInterfaces(methodSig, summaries, childClass))
								found++;

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
	 * Resolves the given query for a data flow summary
	 * 
	 * @param query The query that defines the data flow summary to obtain
	 * @return The data flow summaries that match the given query
	 */
	public SummaryResponse resolve(SummaryQuery query) {
		return methodToImplFlows.getUnchecked(query);
	}

}
