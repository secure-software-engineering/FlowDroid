package soot.jimple.infoflow.data;

import soot.jimple.Stmt;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

import java.util.Collection;
import java.util.Collections;

/**
 * Class representing a source value together with the statement that created it
 * 
 * @author Steven Arzt
 */
public class SourceContext implements Cloneable {
	protected final Collection<ISourceSinkDefinition> definitions;
	protected final AccessPath accessPath;
	protected final Stmt stmt;
	protected final Object userData;

	private int hashCode = 0;

	public SourceContext(ISourceSinkDefinition definition, AccessPath accessPath, Stmt stmt) {
		this(definition, accessPath, stmt, null);
	}

	public SourceContext(ISourceSinkDefinition definition, AccessPath accessPath, Stmt stmt, Object userData) {
		this(Collections.singleton(definition), accessPath, stmt, userData);
	}

	public SourceContext(Collection<ISourceSinkDefinition> definitions, AccessPath accessPath, Stmt stmt) {
		this(definitions, accessPath, stmt, null);
	}

	public SourceContext(Collection<ISourceSinkDefinition> definitions, AccessPath accessPath, Stmt stmt, Object userData) {
		assert accessPath != null;

		this.definitions = definitions;
		this.accessPath = accessPath;
		this.stmt = stmt;
		this.userData = userData;
	}

	public Collection<ISourceSinkDefinition> getDefinitions() {
		return this.definitions;
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
		if (hashCode != 0)
			return hashCode;

		final int prime = 31;
		int result = 1;
		result = prime * result + ((definitions == null) ? 0 : definitions.hashCode());
		result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
		result = prime * result + ((accessPath == null) ? 0 : accessPath.hashCode());
		result = prime * result + ((userData == null) ? 0 : userData.hashCode());
		hashCode = result;

		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		SourceContext other = (SourceContext) obj;

		if (this.hashCode != 0 && other.hashCode != 0 && this.hashCode != other.hashCode)
			return false;

		if (definitions == null) {
			if (other.definitions != null)
				return false;
		} else if (!definitions.equals(other.definitions))
			return false;
		if (stmt == null) {
			if (other.stmt != null)
				return false;
		} else if (!stmt.equals(other.stmt))
			return false;
		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.equals(other.accessPath))
			return false;
		if (userData == null) {
			if (other.userData != null)
				return false;
		} else if (!userData.equals(other.userData))
			return false;
		return true;
	}

	@Override
	public SourceContext clone() {
		SourceContext sc = new SourceContext(definitions, accessPath, stmt, userData);
		assert sc.equals(this);
		return sc;
	}

	@Override
	public String toString() {
		return accessPath.toString() + (stmt == null ? "" : (" in " + stmt.toString()));
	}
}
