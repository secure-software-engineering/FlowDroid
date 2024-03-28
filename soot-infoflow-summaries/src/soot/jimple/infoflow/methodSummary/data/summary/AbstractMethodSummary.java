package soot.jimple.infoflow.methodSummary.data.summary;

import soot.jimple.infoflow.methodSummary.data.sourceSink.AbstractFlowSinkSource;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowConstraint;
import soot.jimple.infoflow.methodSummary.taintWrappers.Taint;

import java.util.Arrays;
import java.util.Map;

/**
 * Abstract base class for all classes that represent method summaries
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractMethodSummary {

	protected final String methodSig;
	protected final FlowConstraint[] constraints;
	protected final IsAliasType isAlias;

	/**
	 * Creates a new instance of the {@link AbstractMethodSummary} class
	 * 
	 * @param methodSig
	 *            The signature of the method containing the flow
	 */
	AbstractMethodSummary(String methodSig, FlowConstraint[] constraints, IsAliasType isAlias) {
		this.methodSig = methodSig;
		this.constraints = constraints;
		this.isAlias = isAlias;
	}

	public String methodSig() {
		return methodSig;
	}

	public FlowConstraint[] getConstraints() {
		return constraints;
	}

	/**
	 * Gets whether the source and the sink of this data flow alias
	 *
	 * @return True the source and the sink of this data flow alias, otherwise false
	 */
	public boolean isAlias() {
		return this.isAlias == IsAliasType.TRUE;
	}

	public abstract boolean isAlias(Taint t);

	protected boolean isAlias(Taint t, AbstractFlowSinkSource flow) {
		if (this.isAlias == IsAliasType.TRUE)
			return true;

		boolean hasContext;
		int fromLen = flow.getAccessPathLength();
		if (fromLen == 0)
			hasContext = t.getBaseContext() != null;
		else if (t.getAccessPathLength() >= fromLen)
			hasContext = t.getAccessPath().getContext(fromLen - 1) != null;
		else
			hasContext = false;

		return (hasContext && isAlias == IsAliasType.WITH_CONTEXT);
	}

	/**
	 * Replaces the gaps in this flow definition according to the given map
	 * 
	 * @param replacementMap
	 *            A mapping from gap id to new gap data object
	 * @return A copy of this flow definition in which the gaps that also occur
	 *         in the given map have been replaced with the values from the map
	 */
	public abstract AbstractMethodSummary replaceGaps(Map<Integer, GapDefinition> replacementMap);

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((methodSig == null) ? 0 : methodSig.hashCode());
		result = prime * result + ((constraints == null) ? 0 : Arrays.hashCode(constraints));
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
		AbstractMethodSummary other = (AbstractMethodSummary) obj;
		if (methodSig == null) {
			if (other.methodSig != null)
				return false;
		} else if (!methodSig.equals(other.methodSig))
			return false;
		if (!Arrays.equals(constraints, other.constraints))
			return false;
		return true;
	}

}
