package soot.jimple.infoflow.android.entryPointCreators;

import java.util.Objects;

import soot.SootClass;
import soot.SootMethod;

/**
 * Contains information used for data transfers between arbitrary components.
 * These components implement common interface methods for setting and retrieving an intent.
 * In the case of activities, there are also methods to save and retrieve an result intent.
 */
public class ComponentExchangeInfo {

	public final SootClass componentDataExchangeInterface;
	public final SootMethod getResultIntentMethod;
	public final SootMethod setResultIntentMethod;
	public final SootMethod getIntentMethod;
	public final SootMethod setIntentMethod;

	@Override
	public int hashCode() {
		return Objects.hash(componentDataExchangeInterface, getIntentMethod, getResultIntentMethod, setIntentMethod,
				setResultIntentMethod);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ComponentExchangeInfo other = (ComponentExchangeInfo) obj;
		return Objects.equals(componentDataExchangeInterface, other.componentDataExchangeInterface)
				&& Objects.equals(getIntentMethod, other.getIntentMethod)
				&& Objects.equals(getResultIntentMethod, other.getResultIntentMethod)
				&& Objects.equals(setIntentMethod, other.setIntentMethod)
				&& Objects.equals(setResultIntentMethod, other.setResultIntentMethod);
	}

	public ComponentExchangeInfo(SootClass componentDataExchangeInterface, SootMethod getIntentMethod,
			SootMethod setIntentMethod, SootMethod getResultIntentMethod, SootMethod setResultIntentMethod) {
		this.componentDataExchangeInterface = componentDataExchangeInterface;
		this.getIntentMethod = getIntentMethod;
		this.setIntentMethod = setIntentMethod;
		this.getResultIntentMethod = getResultIntentMethod;
		this.setResultIntentMethod = setResultIntentMethod;
	}

}
