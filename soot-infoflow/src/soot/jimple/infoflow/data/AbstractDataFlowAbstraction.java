package soot.jimple.infoflow.data;

import soot.Unit;
import soot.jimple.infoflow.solver.FastSolverLinkedNode;

/**
 * Abstract base class for data flow abstractions in FlowDroid
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractDataFlowAbstraction implements Cloneable, FastSolverLinkedNode<Unit> {

	protected SourceContext sourceContext = null;

	public abstract AbstractDataFlowAbstraction clone();

	@Override
	public AbstractDataFlowAbstraction reduce() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deriveSourceContext(FastSolverLinkedNode<Unit> originalNode) {
		if (originalNode != null) {
			SourceContext originalContext = ((AbstractDataFlowAbstraction) originalNode).sourceContext;
			if (originalContext != null)
				this.sourceContext = originalContext;
		}
	}

	/**
	 * Only use this method if you really need to fake a source context and know
	 * what you are doing.
	 * 
	 * @param sourceContext The new source context
	 */
	public void setSourceContext(SourceContext sourceContext) {
		this.sourceContext = sourceContext;
	}

	@Override
	public SourceContext getSourceContext() {
		return sourceContext;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sourceContext == null) ? 0 : sourceContext.hashCode());
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
		AbstractDataFlowAbstraction other = (AbstractDataFlowAbstraction) obj;
		if (sourceContext == null) {
			if (other.sourceContext != null)
				return false;
		} else if (!sourceContext.equals(other.sourceContext))
			return false;
		return true;
	}

}
