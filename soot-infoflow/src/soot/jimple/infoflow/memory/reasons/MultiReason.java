package soot.jimple.infoflow.memory.reasons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import soot.jimple.infoflow.memory.ISolverTerminationReason;

/**
 * Class that models multiple reasons that all contributed to the termination of
 * a data flow solver
 * 
 * @author Steven Arzt
 *
 */
public class MultiReason implements ISolverTerminationReason, Cloneable {

	private List<ISolverTerminationReason> reasons = new ArrayList<>();

	public MultiReason(ISolverTerminationReason... reasons) {
		if (reasons != null && reasons.length > 0)
			for (ISolverTerminationReason reason : reasons)
				this.reasons.add(reason);
	}

	public MultiReason(Collection<ISolverTerminationReason> reasons) {
		this.reasons.addAll(reasons);
	}

	@Override
	public MultiReason clone() {
		return new MultiReason(reasons);
	}

	/**
	 * Gets the reasons why the solver was terminated
	 * 
	 * @return The reasons why the solver was terminated
	 */
	public List<ISolverTerminationReason> getReasons() {
		return reasons;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((reasons == null) ? 0 : reasons.hashCode());
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
		MultiReason other = (MultiReason) obj;
		if (reasons == null) {
			if (other.reasons != null)
				return false;
		} else if (!reasons.equals(other.reasons))
			return false;
		return true;
	}

	@Override
	public ISolverTerminationReason combine(ISolverTerminationReason terminationReason) {
		MultiReason multiReason = clone();
		multiReason.reasons.add(terminationReason);
		return multiReason;
	}

}
