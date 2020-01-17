package soot.jimple.infoflow.android.resources.controls;

import java.util.Collections;
import java.util.Map;

import soot.SootClass;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;

/**
 * Generic layout control that can be anything
 * 
 * @author Steven Arzt
 *
 */
public class GenericLayoutControl extends AndroidLayoutControl {

	protected final static ISourceSinkDefinition UI_SOURCE_DEF = new MethodSourceSinkDefinition(null, null,
			Collections.singleton(AccessPathTuple.fromPathElements(Collections.singletonList("content"),
					Collections.singletonList("java.lang.Object"), SourceSinkType.Source)),
			CallType.MethodCall);

	public GenericLayoutControl(int id, SootClass viewClass, Map<String, Object> additionalAttributes) {
		super(id, viewClass, additionalAttributes);
	}

	public GenericLayoutControl(int id, SootClass viewClass) {
		super(id, viewClass);
	}

	public GenericLayoutControl(SootClass viewClass) {
		super(viewClass);
	}

	@Override
	public ISourceSinkDefinition getSourceDefinition() {
		return UI_SOURCE_DEF;
	}

}
