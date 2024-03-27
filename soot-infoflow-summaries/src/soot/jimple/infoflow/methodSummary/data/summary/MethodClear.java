package soot.jimple.infoflow.methodSummary.data.summary;

import java.util.Map;
import java.util.Objects;

import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowClear;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowConstraint;
import soot.jimple.infoflow.methodSummary.taintWrappers.Taint;

/**
 * A taint clearing definition. This class models the fact that a library method
 * clears an existing taint.
 * 
 * @author Steven Arzt
 *
 */
public class MethodClear extends AbstractMethodSummary {
	private final FlowClear clearDefinition;
	private final boolean preventPropagation;

	public MethodClear(String methodSig, FlowClear clearDefinition, FlowConstraint[] constraints, IsAliasType isAlias, boolean preventPropagation) {
		super(methodSig, constraints, isAlias);
		this.clearDefinition = clearDefinition;
		this.preventPropagation = preventPropagation;
	}

	public MethodClear(String methodSig, FlowClear clearDefinition, FlowConstraint[] constraints, boolean preventPropagation) {
		this(methodSig, clearDefinition, constraints, IsAliasType.FALSE, preventPropagation);
	}

	public boolean preventPropagation() {
		return preventPropagation;
	}

	/**
	 * Gets the definition of the taint that shall be cleared
	 * 
	 * @return The definition of the taint that shall be cleared
	 */
	public FlowClear getClearDefinition() {
		return clearDefinition;
	}

	@Override
	public boolean isAlias(Taint t) {
		return isAlias(t, clearDefinition);
	}

	@Override
	public MethodClear replaceGaps(Map<Integer, GapDefinition> replacementMap) {
		if (replacementMap == null)
			return this;
		return new MethodClear(methodSig, clearDefinition.replaceGaps(replacementMap), getConstraints(), isAlias, preventPropagation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), clearDefinition, preventPropagation);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		MethodClear that = (MethodClear) o;
		return preventPropagation == that.preventPropagation && Objects.equals(clearDefinition, that.clearDefinition);
	}
}
