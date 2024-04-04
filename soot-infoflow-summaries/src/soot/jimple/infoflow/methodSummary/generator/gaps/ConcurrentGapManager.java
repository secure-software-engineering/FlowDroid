package soot.jimple.infoflow.methodSummary.generator.gaps;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import soot.jimple.Stmt;
import soot.jimple.infoflow.methodSummary.data.summary.GapDefinition;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;

/**
 * Concurrent implementation of a gap manager
 * 
 * @author Steven Arzt
 *
 */
public class ConcurrentGapManager extends AbstractGapManager {

	private final Map<Stmt, GapDefinition> gaps = new ConcurrentHashMap<>();
	private AtomicInteger lastGapID = new AtomicInteger();

	@Override
	public GapDefinition getOrCreateGapForCall(MethodSummaries flows, Stmt gapCall) {
		return gaps.computeIfAbsent(gapCall, g -> flows.getOrCreateGap(lastGapID.incrementAndGet(),
				gapCall.getInvokeExpr().getMethod().getSignature()));
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
