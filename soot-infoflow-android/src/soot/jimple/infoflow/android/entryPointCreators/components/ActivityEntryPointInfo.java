package soot.jimple.infoflow.android.entryPointCreators.components;

import java.util.HashSet;
import java.util.Set;

import soot.SootField;
import soot.SootMethod;

public class ActivityEntryPointInfo extends ComponentEntryPointInfo {

	private SootField resultIntentField;

	public ActivityEntryPointInfo(SootMethod entryPoint) {
		super(entryPoint);
	}

	void setResultIntentField(SootField resultIntentField) {
		this.resultIntentField = resultIntentField;
	}

	public SootField getResultIntentField() {
		return resultIntentField;
	}

	@Override
	public Set<SootField> getAdditionalFields() {
		Set<SootField> fields = new HashSet<>(super.getAdditionalFields());
		if (resultIntentField != null)
			fields.add(resultIntentField);
		return fields;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((resultIntentField == null) ? 0 : resultIntentField.hashCode());
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
		ActivityEntryPointInfo other = (ActivityEntryPointInfo) obj;
		if (resultIntentField == null) {
			if (other.resultIntentField != null)
				return false;
		} else if (!resultIntentField.equals(other.resultIntentField))
			return false;
		return true;
	}

}
