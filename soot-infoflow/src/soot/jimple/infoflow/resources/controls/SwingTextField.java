package soot.jimple.infoflow.resources.controls;

import java.util.Collections;

import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkCategory;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;

/**
 * A text field in a Java Swing application
 * 
 * @author Steven Arzt
 *
 */
public class SwingTextField extends JavaSwingLayoutControl {

	protected final static SourceSinkDefinition UI_SOURCE_DEF;

	static {
		UI_SOURCE_DEF = new MethodSourceSinkDefinition(null, null,
				Collections.singleton(
						AccessPathTuple.fromPathElements("javax.swing.JTextField", Collections.singletonList("content"),
								Collections.singletonList("java.lang.String"), SourceSinkType.Source)),
				CallType.MethodCall);

		UI_SOURCE_DEF.setCategory(new ISourceSinkCategory() {

			@Override
			public String getHumanReadableDescription() {
				return "Text Input";
			}

			@Override
			public String toString() {
				return "Text Input";
			}

			@Override
			public String getID() {
				return "TEXTINPUT";
			}

		});
	}

	@Override
	public SourceSinkDefinition getSourceDefinition() {
		return UI_SOURCE_DEF;
	}

}
