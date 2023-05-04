package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Collection;
import java.util.Collections;
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
public class StatementSourceSinkDefinition extends AbstractSourceSinkDefinition
		implements IAccessPathBasedSourceSinkDefinition {

	protected final Stmt stmt;
	protected final Local local;
	protected Set<AccessPathTuple> accessPaths;

	public StatementSourceSinkDefinition(Stmt stmt, Local local, Set<AccessPathTuple> accessPaths) {
		if (accessPaths == null || accessPaths.isEmpty())
			throw new IllegalArgumentException("Access Paths must not be empty");
		this.stmt = stmt;
		this.local = local;
		this.accessPaths = new HashSet<>(accessPaths);
	}

	public static StatementSourceSinkDefinition createBlankStatementSourceDefinition(Stmt stmt, Local local) {
		return new StatementSourceSinkDefinition(stmt, local,
				Collections.singleton(AccessPathTuple.getBlankSourceTuple()));

	}

	public static StatementSourceSinkDefinition createBlankStatementSinkDefinition(Stmt stmt, Local local) {
		return new StatementSourceSinkDefinition(stmt, local,
				Collections.singleton(AccessPathTuple.getBlankSinkTuple()));
	}

	@Override
	public StatementSourceSinkDefinition getSourceOnlyDefinition() {
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
		return buildNewDefinition(stmt, local, newSet);
	}

	@Override
	public StatementSourceSinkDefinition getSinkOnlyDefinition() {
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
		return buildNewDefinition(stmt, local, newSet);
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
	public boolean isEmpty() {
		return false;
	}

	@Override
	public String toString() {
		return String.format("Local %s at %s", local, stmt);
	}

	@Override
	public Set<AccessPathTuple> getAllAccessPaths() {
		return accessPaths;
	}

	@Override
	public StatementSourceSinkDefinition filter(Collection<AccessPathTuple> toFilter) {
		// Filter the access paths
		Set<AccessPathTuple> filteredAPs = null;
		if (accessPaths != null && !accessPaths.isEmpty()) {
			filteredAPs = new HashSet<>(accessPaths.size());
			for (AccessPathTuple ap : accessPaths)
				if (toFilter.contains(ap))
					filteredAPs.add(ap);
		}
		StatementSourceSinkDefinition def = buildNewDefinition(stmt, local, filteredAPs);
		def.setCategory(category);
		return def;
	}

	protected StatementSourceSinkDefinition buildNewDefinition(Stmt stmt, Local local,
			Set<AccessPathTuple> accessPaths) {
		StatementSourceSinkDefinition sssd = new StatementSourceSinkDefinition(stmt, local, accessPaths);
		sssd.category = category;
		return sssd;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((accessPaths == null) ? 0 : accessPaths.hashCode());
		result = prime * result + ((local == null) ? 0 : local.hashCode());
		result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
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
