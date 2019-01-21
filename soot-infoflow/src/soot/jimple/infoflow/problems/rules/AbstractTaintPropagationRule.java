package soot.jimple.infoflow.problems.rules;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;

/**
 * Abstract base class for all taint propagation rules
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractTaintPropagationRule implements ITaintPropagationRule {

	protected final InfoflowManager manager;
	protected final TaintAbstraction zeroValue;
	protected final TaintPropagationResults results;

	public AbstractTaintPropagationRule(InfoflowManager manager, TaintAbstraction zeroValue,
			TaintPropagationResults results) {
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

	protected TaintAbstraction getZeroValue() {
		return this.zeroValue;
	}

	protected TaintPropagationResults getResults() {
		return this.results;
	}

}
