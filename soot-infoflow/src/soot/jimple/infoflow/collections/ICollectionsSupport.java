package soot.jimple.infoflow.collections;

import java.util.Set;

import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

public interface ICollectionsSupport extends ITaintPropagationWrapper {
	Set<Abstraction> getTaintsForMethodApprox(Stmt stmt, Abstraction d1, Abstraction incoming);

	IContainerStrategy getContainerStrategy();
}
