package soot.jimple.infoflow.methodSummary.generator.gaps;

import java.util.Collection;
import java.util.Set;

import soot.Local;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

/**
 * Interface for implementations that manage the creation of gaps during the
 * taint propagation and the summary generation
 * 
 * @author Steven Arzt
 *
 */

public interface IGapManager {

	/**
	 * Gets the data object of the given call into a gap method. If no gap
	 * definition exists, a new one is created.
	 * 
	 * @param flows   The flow set in which to register the gap
	 * @param gapCall The gap to be called
	 * @return The data object of the given gap call. If this call site has already
	 *         been processed, the old object is returned. Otherwise, a new object
	 *         is generated.
	 */
	public GapDefinition getOrCreateGapForCall(MethodSummaries flows, Stmt gapCall);

	/**
	 * Gets the data object of the given call into a gap method
	 * 
	 * @param gapCall The gap to be called
	 * @return The data object of the given gap call if it exists, otherwise null
	 */
	public GapDefinition getGapForCall(Stmt gapCall);

	/**
	 * Gets whether the given local is referenced in any gap. This can either be as
	 * a parameter, a base object, or a return value
	 * 
	 * @param local The local to check
	 * @return True if the given local is referenced in at least one gap, otherwise
	 *         false
	 */
	public boolean isLocalReferencedInGap(Local local);

	/**
	 * Gets the gap definitions that references the given local as parameters or
	 * base objects
	 * 
	 * @param local The local for which to find the gap references
	 * @return The gaps that reference the given local
	 */
	public Set<GapDefinition> getGapDefinitionsForLocalUse(Local local);

	/**
	 * Gets the gap definitions that references the given local as return value.
	 * 
	 * @param local The local for which to find the gap references
	 * @return The gaps that reference the given local
	 */
	public Set<GapDefinition> getGapDefinitionsForLocalDef(Local local);

	/**
	 * Checks whether we need to produce a gap for the given method call
	 * 
	 * @param stmt The call statement
	 * @param abs  The abstraction that reaches the given call
	 * @param icfg The interprocedural control flow graph
	 * @return True if we need to create a gap, otherwise false
	 */
	public boolean needsGapConstruction(Stmt stmt, Abstraction abs, IInfoflowCFG icfg);

	/**
	 * Gets all statements on which gaps are defined
	 * 
	 * @return A collection with all statements on which gaps are defined
	 */
	public Collection<Stmt> getAllGapStmts();

}
