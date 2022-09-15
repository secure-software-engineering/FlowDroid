package soot.jimple.infoflow.methodSummary.data.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.jimple.infoflow.methodSummary.data.summary.ClassMethodSummaries;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

/**
 * Abstract base class for method summary providers
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractMethodSummaryProvider implements IMethodSummaryProvider {

	protected Map<String, String> classToSuperclass = null;
	protected MultiMap<String, String> classToSubclasses = null;
	protected MultiMap<String, String> classToInterfaces = null;
	protected MultiMap<String, String> interfaceToImplementers = null;
	protected MultiMap<String, String> interfaceToSubInterfaces = null;

	@Override
	public List<String> getSubclassesOf(String className) {
		ensureHierarchy();

		List<String> worklist = new ArrayList<>();
		List<String> subclasses = new ArrayList<>();
		worklist.add(className);
		while (!worklist.isEmpty()) {
			Set<String> scs = classToSubclasses.get(worklist.remove(0));
			if (subclasses.addAll(scs))
				worklist.addAll(scs);
		}
		return subclasses;
	}

	@Override
	public List<String> getSuperclassesOf(String className) {
		ensureHierarchy();

		List<String> worklist = new ArrayList<>();
		List<String> superclasses = new ArrayList<>();
		worklist.add(className);
		while (!worklist.isEmpty()) {
			String scs = classToSuperclass.get(worklist.remove(0));
			if (scs != null && !scs.isEmpty()) {
				superclasses.add(scs);
				worklist.add(scs);
			}
		}
		return superclasses;
	}

	@Override
	public Collection<String> getSuperinterfacesOf(String className) {
		ensureHierarchy();

		List<String> worklist = new ArrayList<>();
		List<String> superinterfaces = new ArrayList<>();
		worklist.add(className);
		while (!worklist.isEmpty()) {
			Set<String> intfs = classToInterfaces.get(worklist.remove(0));
			if (intfs != null && !intfs.isEmpty()) {
				superinterfaces.addAll(intfs);
				worklist.addAll(intfs);
			}
		}
		return superinterfaces;
	}

	@Override
	public List<String> getImplementersOfInterface(String className) {
		ensureHierarchy();

		List<String> worklist = new ArrayList<>();
		List<String> implementers = new ArrayList<>();
		worklist.add(className);
		worklist.addAll(getSubInterfacesOf(className));
		while (!worklist.isEmpty()) {
			Set<String> scs = interfaceToImplementers.get(worklist.remove(0));
			implementers.addAll(scs);
		}
		return implementers;
	}

	@Override
	public Set<String> getSubInterfacesOf(String className) {
		ensureHierarchy();

		List<String> worklist = new ArrayList<>();
		Set<String> subInterfaces = new HashSet<>();
		worklist.add(className);
		while (!worklist.isEmpty()) {
			Set<String> scs = interfaceToSubInterfaces.get(worklist.remove(0));
			if (subInterfaces.addAll(scs))
				worklist.addAll(scs);
		}
		return subInterfaces;
	}

	/**
	 * Ensures that the hierarchy information is present
	 */
	private void ensureHierarchy() {
		if (this.classToSuperclass == null || this.classToSubclasses == null || interfaceToImplementers == null) {
			Map<String, String> classToSuperclass = new HashMap<>();
			MultiMap<String, String> classToSubclasses = new HashMultiMap<>();
			MultiMap<String, String> classToInterfaces = new HashMultiMap<>();
			MultiMap<String, String> interfaceToImplementers = new HashMultiMap<>();
			MultiMap<String, String> interfaceToSubInterfaces = new HashMultiMap<>();

			Set<String> classNames = getAllClassesWithSummaries();
			for (String curClass : classNames) {
				ClassMethodSummaries curSummaries = getClassFlows(curClass);
				if (curSummaries != null) {
					String superclass = curSummaries.getSuperClass();
					if (superclass != null && !superclass.isEmpty()) {
						classToSuperclass.put(curClass, superclass);
						classToSubclasses.put(superclass, curClass);
					}

					Set<String> intfs = curSummaries.getInterfaces();
					if (intfs != null && !intfs.isEmpty()) {
						for (String intf : intfs) {
							if (curSummaries.isInterface())
								interfaceToSubInterfaces.put(intf, curClass);
							else {
								classToInterfaces.put(curClass, intf);
								interfaceToImplementers.put(intf, curClass);
							}
						}
					}
				}
			}
			this.classToSuperclass = classToSuperclass;
			this.classToSubclasses = classToSubclasses;
			this.classToInterfaces = classToInterfaces;
			this.interfaceToImplementers = interfaceToImplementers;
			this.interfaceToSubInterfaces = interfaceToSubInterfaces;
		}
	}

}
