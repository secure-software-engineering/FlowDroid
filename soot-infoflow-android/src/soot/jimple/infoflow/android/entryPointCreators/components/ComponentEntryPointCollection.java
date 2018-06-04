package soot.jimple.infoflow.android.entryPointCreators.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

/**
 * Mapping class for associating components with the data object for their
 * respective entry point
 * 
 * @author Steven Arzt
 *
 */
public class ComponentEntryPointCollection {

	protected Map<SootClass, ComponentEntryPointInfo> componentToEntryPointInfo = new HashMap<>();

	public void put(SootClass component, ComponentEntryPointInfo info) {
		componentToEntryPointInfo.put(component, info);
	}

	public void put(SootClass component, SootMethod lifecycleMethod) {
		componentToEntryPointInfo.put(component, new ComponentEntryPointInfo(lifecycleMethod));
	}

	public ComponentEntryPointInfo get(SootClass component) {
		return componentToEntryPointInfo.get(component);
	}

	public Collection<SootMethod> getLifecycleMethods() {
		List<SootMethod> list = new ArrayList<>();
		for (ComponentEntryPointInfo info : componentToEntryPointInfo.values())
			list.add(info.getEntryPoint());
		return list;
	}

	public Collection<SootField> getAdditionalFields() {
		List<SootField> list = new ArrayList<>();
		for (ComponentEntryPointInfo info : componentToEntryPointInfo.values())
			list.addAll(info.getAdditionalFields());
		return list;
	}

	public SootMethod getEntryPoint(SootClass component) {
		ComponentEntryPointInfo info = componentToEntryPointInfo.get(component);
		return info == null ? null : info.getEntryPoint();
	}

	public void clear() {
		componentToEntryPointInfo.clear();
	}

	public boolean hasEntryPointForComponent(SootClass component) {
		return componentToEntryPointInfo.containsKey(component);
	}

}
