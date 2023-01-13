package soot.jimple.infoflow.river;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.SourceContext;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

/**
 * Source context implementation for additional data flow information on a
 * vulnerability, i.e. for finding which data is encrypted with a hard-coded key
 * 
 * @author Steven Arzt
 *
 */
public class AdditionalFlowInfoSourceContext extends SourceContext {

	public AdditionalFlowInfoSourceContext(ISourceSinkDefinition definition, AccessPath accessPath, Stmt stmt) {
		super(definition, accessPath, stmt);
	}

}
