package soot.jimple.infoflow.river;

import soot.jimple.Stmt;

import java.util.Set;

/**
 * Provides the analysis whether usage context is needed
 *
 * @author Tim Lange
 */
public interface IUsageContextProvider {
    /**
     * Provide an access path that should be queried as an additional flow.
     *
     * @return Flow specifications if a usage context shall be computed. Always non-null.
     */
    Set<AdditionalFlowInfoSpecification> needsAdditionalInformation(Stmt stmt);
}
