package soot.jimple.infoflow.methodSummary.generator.gaps;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import soot.jimple.Stmt;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;

/**
 * Class that manages the creation of gaps during the taint propagation and the
 * summary generation
 * 
 * @author Steven Arzt
 *
 */
public class GapManager extends AbstractGapManager {

	private final Map<Stmt, GapDefinition> gaps = new HashMap<>();
	private int lastGapID = 0;

	@Override
	public synchronized GapDefinition getOrCreateGapForCall(MethodSummaries flows, Stmt gapCall) {
		GapDefinition gd = this.gaps.get(gapCall);
		if (gd == null) {
			// Generate a new gap ID
			// Register it in the summary object
			gd = flows.getOrCreateGap(lastGapID++, gapCall.getInvokeExpr().getMethod().getSignature());
			this.gaps.put(gapCall, gd);
		}
		return gd;
	}

	@Override
	public GapDefinition getGapForCall(Stmt gapCall) {
		return this.gaps.get(gapCall);
	}

	@Override
	public Collection<Stmt> getAllGapStmts() {
		return gaps.keySet();
	}

}
