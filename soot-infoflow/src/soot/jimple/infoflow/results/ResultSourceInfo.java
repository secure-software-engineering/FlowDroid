package soot.jimple.infoflow.results;

import java.util.Arrays;
import java.util.List;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.tagkit.LineNumberTag;

/**
 * Class for modeling information flowing out of a specific source
 * 
 * @author Steven Arzt
 */
public class ResultSourceInfo extends AbstractResultSourceSinkInfo {
	private final Stmt[] path;
	private final AccessPath[] pathAPs;

	public ResultSourceInfo(SourceSinkDefinition definition, AccessPath source, Stmt context) {
		super(definition, source, context);

		this.path = null;
		this.pathAPs = null;
	}

	public ResultSourceInfo(SourceSinkDefinition definition, AccessPath source, Stmt context, Object userData,
			List<Stmt> path, List<AccessPath> pathAPs) {
		super(definition, source, context, userData);

		this.path = path == null || path.isEmpty() ? null : path.toArray(new Stmt[path.size()]);
		this.pathAPs = pathAPs == null || pathAPs.isEmpty() ? null : pathAPs.toArray(new AccessPath[pathAPs.size()]);
	}

	public Stmt[] getPath() {
		return this.path;
	}

	public AccessPath[] getPathAccessPaths() {
		return this.pathAPs;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(stmt.toString());

		if (stmt.hasTag("LineNumberTag"))
			sb.append(" on line ").append(((LineNumberTag) stmt.getTag("LineNumberTag")).getLineNumber());

		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();

		if (!InfoflowConfiguration.getPathAgnosticResults()) {
			if (path != null)
				result += prime * Arrays.hashCode(this.path);
			if (pathAPs != null)
				result += prime * Arrays.hashCode(this.pathAPs);
		}

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResultSourceInfo other = (ResultSourceInfo) obj;
		if (!InfoflowConfiguration.getPathAgnosticResults()) {
			if (!Arrays.equals(path, other.path))
				return false;
			if (!Arrays.equals(pathAPs, other.pathAPs))
				return false;
		}
		return true;
	}

}
