package soot.jimple.infoflow.android.entryPointCreators.components;

import java.util.Collections;
import java.util.Set;

import soot.SootField;
import soot.SootMethod;

/**
 * Data class with additional information about the entry point generated for an
 * Android component
 * 
 * @author Steven Arzt
 *
 */
public class ComponentEntryPointInfo {

	private final SootMethod entryPoint;
	private SootField intentField;

	public ComponentEntryPointInfo(SootMethod entryPoint) {
		this.entryPoint = entryPoint;
	}

	public SootMethod getEntryPoint() {
		return entryPoint;
	}

	void setIntentField(SootField intentField) {
		this.intentField = intentField;
	}

	public SootField getIntentField() {
		return intentField;
	}

	public Set<SootField> getAdditionalFields() {
		if (intentField == null)
			return Collections.emptySet();
		return Collections.singleton(intentField);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entryPoint == null) ? 0 : entryPoint.hashCode());
		result = prime * result + ((intentField == null) ? 0 : intentField.hashCode());
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
		ComponentEntryPointInfo other = (ComponentEntryPointInfo) obj;
		if (entryPoint == null) {
			if (other.entryPoint != null)
				return false;
		} else if (!entryPoint.equals(other.entryPoint))
			return false;
		if (intentField == null) {
			if (other.intentField != null)
				return false;
		} else if (!intentField.equals(other.intentField))
			return false;
		return true;
	}

}
