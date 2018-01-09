package soot.jimple.infoflow.aliasing;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;

public abstract class AbstractBulkAliasStrategy extends AbstractAliasStrategy {

	public AbstractBulkAliasStrategy(InfoflowManager manager) {
		super(manager);
	}

	@Override
	public boolean isInteractive() {
		return false;
	}
	
	@Override
	public boolean mayAlias(AccessPath ap1, AccessPath ap2) {
		return false;
	}
	

	@Override
	public boolean isLazyAnalysis() {
		return false;
	}

}
