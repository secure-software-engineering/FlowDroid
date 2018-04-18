package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.HashSet;
import java.util.Set;

import soot.Local;
import soot.jimple.Stmt;

/**
 * A source/sink definition that corresponds to a concrete statement in the
 * Jimple code
 * 
 * @author Steven Arzt
 *
 */
public class StatementSourceSinkDefinition extends SourceSinkDefinition {

	private final Stmt stmt;
	private final Local local;
	private Set<AccessPathTuple> accessPaths;

	public StatementSourceSinkDefinition(Stmt stmt, Local local, Set<AccessPathTuple> accessPaths) {
		this.stmt = stmt;
		this.local = local;
		this.accessPaths = accessPaths;
	}

	@Override
	public SourceSinkDefinition getSourceOnlyDefinition() {
		Set<AccessPathTuple> newSet = null;
		if (accessPaths != null) {
			newSet = new HashSet<>(accessPaths.size());
			for (AccessPathTuple apt : accessPaths) {
				SourceSinkType ssType = apt.getSourceSinkType();
				if (ssType == SourceSinkType.Source)
					newSet.add(apt);
				else if (ssType == SourceSinkType.Both) {
					newSet.add(new AccessPathTuple(apt.getBaseType(), apt.getFields(), apt.getFieldTypes(),
							SourceSinkType.Source));
				}
			}
		}
		return new StatementSourceSinkDefinition(stmt, local, newSet);
	}

	@Override
	public SourceSinkDefinition getSinkOnlyDefinition() {
		Set<AccessPathTuple> newSet = null;
		if (accessPaths != null) {
			newSet = new HashSet<>(accessPaths.size());
			for (AccessPathTuple apt : accessPaths) {
				SourceSinkType ssType = apt.getSourceSinkType();
				if (ssType == SourceSinkType.Sink)
					newSet.add(apt);
				else if (ssType == SourceSinkType.Both) {
					newSet.add(new AccessPathTuple(apt.getBaseType(), apt.getFields(), apt.getFieldTypes(),
							SourceSinkType.Sink));
				}
			}
		}
		return new StatementSourceSinkDefinition(stmt, local, newSet);
	}

	public Stmt getStmt() {
		return stmt;
	}

	public Local getLocal() {
		return local;
	}

	public Set<AccessPathTuple> getAccessPaths() {
		return accessPaths;
	}

	@Override
	public void merge(SourceSinkDefinition other) {
		if (other instanceof StatementSourceSinkDefinition) {
			StatementSourceSinkDefinition otherStmt = (StatementSourceSinkDefinition) other;

			// Merge the base object definitions
			if (otherStmt.accessPaths != null && !otherStmt.accessPaths.isEmpty()) {
				if (this.accessPaths == null)
					this.accessPaths = new HashSet<>();
				for (AccessPathTuple apt : otherStmt.accessPaths)
					this.accessPaths.add(apt);
			}
		}
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public String toString() {
		return String.format("Local %s at %s", local, stmt);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessPaths == null) ? 0 : accessPaths.hashCode());
		result = prime * result + ((local == null) ? 0 : local.hashCode());
		result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatementSourceSinkDefinition other = (StatementSourceSinkDefinition) obj;
		if (accessPaths == null) {
			if (other.accessPaths != null)
				return false;
		} else if (!accessPaths.equals(other.accessPaths))
			return false;
		if (local == null) {
			if (other.local != null)
				return false;
		} else if (!local.equals(other.local))
			return false;
		if (stmt == null) {
			if (other.stmt != null)
				return false;
		} else if (!stmt.equals(other.stmt))
			return false;
		return true;
	}

}
