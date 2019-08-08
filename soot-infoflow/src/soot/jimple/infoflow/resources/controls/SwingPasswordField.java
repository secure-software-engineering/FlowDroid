package soot.jimple.infoflow.resources.controls;

import java.util.Collections;

import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkCategory;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;

/**
 * A password field in a Java Swing application
 * 
 * @author Steven Arzt
 *
 */
public class SwingPasswordField extends JavaSwingLayoutControl {

	protected final static ISourceSinkDefinition UI_PASSWORD_SOURCE_DEF;

	static {
		UI_PASSWORD_SOURCE_DEF = new MethodSourceSinkDefinition(null, null,
				Collections.singleton(AccessPathTuple.fromPathElements("javax.swing.JPasswordField",
						Collections.singletonList("content"), Collections.singletonList("java.lang.String"),
						SourceSinkType.Source)),
				CallType.MethodCall);

		UI_PASSWORD_SOURCE_DEF.setCategory(new ISourceSinkCategory() {

			@Override
			public String getHumanReadableDescription() {
				return "Password Input";
			}

			@Override
			public String toString() {
				return "Password Input";
			}

			@Override
			public String getID() {
				return "PASSWORD";
			}

		});
	}

	@Override
	public boolean isSensitive() {
		// Passwords are always sensitive
		return true;
	}

	@Override
	public ISourceSinkDefinition getSourceDefinition() {
		return UI_PASSWORD_SOURCE_DEF;
	}

}
