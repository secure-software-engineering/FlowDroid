package soot.jimple.infoflow.results;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.tagkit.LineNumberTag;

/**
 * Class for modeling information flowing into a specific sink
 * 
 * @author Steven Arzt
 * 
 */
public class ResultSinkInfo extends AbstractResultSourceSinkInfo {

	public ResultSinkInfo() {
	}

	public ResultSinkInfo(ISourceSinkDefinition definition, AccessPath sink, Stmt context) {
		super(definition, sink, context);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(stmt == null ? accessPath.toString() : stmt.toString());

		if (stmt != null && stmt.hasTag("LineNumberTag"))
			sb.append(" on line ").append(((LineNumberTag) stmt.getTag("LineNumberTag")).getLineNumber());

		return sb.toString();
	}

}
