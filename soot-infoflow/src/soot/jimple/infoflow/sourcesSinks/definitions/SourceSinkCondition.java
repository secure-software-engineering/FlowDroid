package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Set;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;

/**
 * Abstract base class for conditions under which a source/sink definition is
 * valid
 *
 * @author Steven Arzt
 *
 */
public abstract class SourceSinkCondition {

    /**
     * Evaluates the condition on the given data flow result
     *
     * @param result
     *            The data flow result
     * @param results
     *            All results of this data flow analysis
     * @return True if the given data flow result matches the condition, otherwise
     *         false
     */
    public abstract boolean evaluate(DataFlowResult result, InfoflowResults results);

    /**
     * Gets all methods referenced by this condition
     *
     * @return The methods referenced by this condition, or null if this condition
     *         does not reference any methods
     */
    public Set<SootMethod> getReferencedMethods() {
        return null;
    }

    /**
     * Gets all classes referenced by this condition
     *
     * @return The classes referenced by this condition, or null if this condition
     *         does not reference any classes
     */
    public Set<SootClass> getReferencedClasses() {
        return null;
    }

}
