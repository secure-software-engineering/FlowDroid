package soot.jimple.infoflow.results;

import java.util.Arrays;
import java.util.List;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.tagkit.LineNumberTag;

/**
 * Class for modeling information flowing out of a specific source
 * 
 * @author Steven Arzt
 */
public class ResultSourceInfo extends AbstractResultSourceSinkInfo {

	private final Stmt[] path;
	private final AccessPath[] pathAPs;
	private final Stmt[] pathCallSites;

	private transient boolean pathAgnosticResults = true;

	public ResultSourceInfo() {
		this.path = null;
		this.pathAPs = null;
		this.pathCallSites = null;
	}

	public ResultSourceInfo(ISourceSinkDefinition definition, AccessPath source, Stmt context,
			boolean pathAgnosticResults) {
		super(definition, source, context);

		this.path = null;
		this.pathAPs = null;
		this.pathCallSites = null;
		this.pathAgnosticResults = pathAgnosticResults;
	}

	public ResultSourceInfo(ISourceSinkDefinition definition, AccessPath source, Stmt context, Object userData,
			List<Stmt> path, List<AccessPath> pathAPs, List<Stmt> pathCallSites, boolean pathAgnosticResults) {
		super(definition, source, context, userData);

		this.path = path == null || path.isEmpty() ? null : path.toArray(new Stmt[path.size()]);
		this.pathAPs = pathAPs == null || pathAPs.isEmpty() ? null : pathAPs.toArray(new AccessPath[pathAPs.size()]);
		this.pathCallSites = pathCallSites == null || pathCallSites.isEmpty() ? null
				: pathCallSites.toArray(new Stmt[pathCallSites.size()]);
		this.pathAgnosticResults = pathAgnosticResults;
	}

	public Stmt[] getPath() {
		return this.path;
	}

	public AccessPath[] getPathAccessPaths() {
		return this.pathAPs;
	}

	public Stmt[] getPathCallSites() {
		return this.pathCallSites;
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

		if (!pathAgnosticResults) {
			if (path != null)
				result += prime * Arrays.hashCode(this.path);
			if (pathAPs != null)
				result += prime * Arrays.hashCode(this.pathAPs);
			if (pathCallSites != null)
				result += prime * Arrays.hashCode(this.pathCallSites);
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
		if (!pathAgnosticResults) {
			if (!Arrays.equals(path, other.path))
				return false;
			if (!Arrays.equals(pathAPs, other.pathAPs))
				return false;
			if (!Arrays.equals(pathCallSites, other.pathCallSites))
				return false;
		}
		return true;
	}

}
