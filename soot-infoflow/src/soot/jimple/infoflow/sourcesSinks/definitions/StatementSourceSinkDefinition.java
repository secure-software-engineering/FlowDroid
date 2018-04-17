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

}
