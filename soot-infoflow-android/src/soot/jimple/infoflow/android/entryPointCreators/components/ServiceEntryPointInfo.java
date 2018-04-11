package soot.jimple.infoflow.android.entryPointCreators.components;

import java.util.HashSet;
import java.util.Set;

import soot.SootField;
import soot.SootMethod;

public class ServiceEntryPointInfo extends ComponentEntryPointInfo {

	private SootField binderField;

	public ServiceEntryPointInfo(SootMethod entryPoint) {
		super(entryPoint);
	}

	void setBinderField(SootField binderField) {
		this.binderField = binderField;
	}

	public SootField getBinderField() {
		return binderField;
	}

	@Override
	public Set<SootField> getAdditionalFields() {
		Set<SootField> fields = new HashSet<>(super.getAdditionalFields());
		if (binderField != null)
			fields.add(binderField);
		return fields;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((binderField == null) ? 0 : binderField.hashCode());
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
		ServiceEntryPointInfo other = (ServiceEntryPointInfo) obj;
		if (binderField == null) {
			if (other.binderField != null)
				return false;
		} else if (!binderField.equals(other.binderField))
			return false;
		return true;
	}

}
