package soot.jimple.infoflow.results;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

/**
 * Abstract base class for information on data flow results
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractResultSourceSinkInfo {

	protected final ISourceSinkDefinition definition;
	protected final AccessPath accessPath;
	protected final Stmt stmt;
	protected final Object userData;

	public AbstractResultSourceSinkInfo() {
		this.stmt = null;
		this.definition = null;
		this.accessPath = null;
		this.userData = null;

	}

	public AbstractResultSourceSinkInfo(ISourceSinkDefinition definition, AccessPath accessPath, Stmt stmt) {
		this(definition, accessPath, stmt, null);
	}

	public AbstractResultSourceSinkInfo(ISourceSinkDefinition definition, AccessPath accessPath, Stmt stmt,
			Object userData) {
		assert accessPath != null;

		this.definition = definition;
		this.accessPath = accessPath;
		this.stmt = stmt;
		this.userData = userData;
	}

	public ISourceSinkDefinition getDefinition() {
		return this.definition;
	}

	public AccessPath getAccessPath() {
		return this.accessPath;
	}

	public Stmt getStmt() {
		return this.stmt;
	}

	public Object getUserData() {
		return this.userData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = (InfoflowConfiguration.getOneResultPerAccessPath() ? 31 * this.accessPath.hashCode() : 0);
		result = prime * result + ((definition == null) ? 0 : definition.hashCode());
		result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
		result = prime * result + ((userData == null) ? 0 : userData.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (super.equals(o))
			return true;

		if (o == null || getClass() != o.getClass())
			return false;
		AbstractResultSourceSinkInfo si = (AbstractResultSourceSinkInfo) o;

		if (InfoflowConfiguration.getOneResultPerAccessPath() && !this.accessPath.equals(si.accessPath))
			return false;

		if (definition == null) {
			if (si.definition != null)
				return false;
		} else if (!definition.equals(si.definition))
			return false;
		if (stmt == null) {
			if (si.stmt != null)
				return false;
		} else if (!stmt.equals(si.stmt))
			return false;
		if (userData == null) {
			if (si.userData != null)
				return false;
		} else if (!userData.equals(si.userData))
			return false;

		return true;
	}

}
