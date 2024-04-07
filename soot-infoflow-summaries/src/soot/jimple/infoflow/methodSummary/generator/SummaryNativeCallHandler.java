package soot.jimple.infoflow.methodSummary.generator;

import java.util.Collections;
import java.util.Set;

import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.nativeCallHandler.AbstractNativeCallHandler;
import soot.jimple.infoflow.nativeCallHandler.DefaultNativeCallHandler;
import soot.jimple.infoflow.nativeCallHandler.INativeCallHandler;

/**
 * Handler for dealing with native calls during summary generation
 * 
 * @author Steven Arzt
 *
 */
public class SummaryNativeCallHandler extends AbstractNativeCallHandler {

	private final INativeCallHandler fallbackHandler;

	/**
	 * Creates a new instance of the SummaryNativeCallHandler class
	 */
	public SummaryNativeCallHandler() {
		this(new DefaultNativeCallHandler());
	}

	/**
	 * Creates a new instance of the SummaryNativeCallHandler class
	 * 
	 * @param fallbackHandler The fallback native code handler to use. If the
	 *                        fallback handler supports a callee, its taints are
	 *                        used. Otherwise, the summary handler applies an
	 *                        over-approximation
	 */
	public SummaryNativeCallHandler(INativeCallHandler fallbackHandler) {
		this.fallbackHandler = fallbackHandler;
	}

	@Override
	public void initialize(InfoflowManager manager) {
		super.initialize(manager);
		fallbackHandler.initialize(manager);
	}

	@Override
	public Set<Abstraction> getTaintedValues(Stmt call, Abstraction source, Value[] params) {
		// Check the fallback handler first, before doing an over-approximation
		if (fallbackHandler.supportsCall(call))
			return fallbackHandler.getTaintedValues(call, source, params);

		// Check whether we have an incoming access path
		boolean found = false;
		for (Value val : call.getInvokeExpr().getArgs())
			if (val == source.getAccessPath().getPlainValue()) {
				found = true;
				break;
			}
		if (!found)
			return Collections.emptySet();

		// We over-approximate native method calls
		if (call instanceof DefinitionStmt) {
			DefinitionStmt defStmt = (DefinitionStmt) call;
			return Collections.singleton(source.deriveNewAbstraction(
					manager.getAccessPathFactory().createAccessPath(defStmt.getLeftOp(), true), call));
		}

		return Collections.emptySet();
	}

	@Override
	public boolean supportsCall(Stmt call) {
		// We over-approximate everything
		return true;
	}

}
