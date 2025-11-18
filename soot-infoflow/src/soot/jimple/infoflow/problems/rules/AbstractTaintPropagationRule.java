package soot.jimple.infoflow.problems.rules;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;

/**
 * Abstract base class for all taint propagation rules
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractTaintPropagationRule implements ITaintPropagationRule {

	protected InfoflowManager manager;
	protected Abstraction zeroValue;
	protected TaintPropagationResults results;

	@Override
	public void init(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
		this.manager = manager;
		this.zeroValue = zeroValue;
		this.results = results;
	}

	protected InfoflowManager getManager() {
		return this.manager;
	}

	protected Aliasing getAliasing() {
		return this.manager.getAliasing();
	}

	protected Abstraction getZeroValue() {
		return this.zeroValue;
	}

	protected TaintPropagationResults getResults() {
		return this.results;
	}

}
