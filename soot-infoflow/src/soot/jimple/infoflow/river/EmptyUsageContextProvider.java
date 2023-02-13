package soot.jimple.infoflow.river;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Collections;
import java.util.Set;

/**
 * Default implementation that does not request any usage contexts
 *
 * @author Tim Lange
 */
public class EmptyUsageContextProvider implements IUsageContextProvider {

    @Override
    public Set<AdditionalFlowInfoSpecification> needsAdditionalInformation(Stmt stmt, Set<Abstraction> taints) {
        return Collections.emptySet();
    }

    @Override
    public boolean isStatementWithAdditionalInformation(Stmt stmt, Abstraction abs) {
        return false;
    }
}
