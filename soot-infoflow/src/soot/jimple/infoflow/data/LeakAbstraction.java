package soot.jimple.infoflow.data;

import soot.jimple.Stmt;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;

/**
 * Abstraction that models a leak that has happened inside a method
 * 
 * @author Steven Arzt
 *
 */
public class LeakAbstraction extends AbstractDataFlowAbstraction {

	private Stmt stmt;
	private TaintAbstraction abstraction;

	public LeakAbstraction(SourceSinkDefinition definition, TaintAbstraction source, Stmt stmt) {
		this.sourceContext = source.sourceContext;
		this.stmt = stmt;
		this.abstraction = source;
	}

	public Stmt getStmt() {
		return stmt;
	}

	public TaintAbstraction getAbstraction() {
		return abstraction;
	}

	@Override
	public int getPathLength() {
		return 0;
	}

	@Override
	public LeakAbstraction clone() {
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((abstraction == null) ? 0 : abstraction.hashCode());
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
		LeakAbstraction other = (LeakAbstraction) obj;
		if (abstraction == null) {
			if (other.abstraction != null)
				return false;
		} else if (!abstraction.equals(other.abstraction))
			return false;
		if (stmt == null) {
			if (other.stmt != null)
				return false;
		} else if (!stmt.equals(other.stmt))
			return false;
		return true;
	}

}
