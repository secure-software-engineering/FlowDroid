package soot.jimple.infoflow.problems.rules.forward;

import java.util.Collection;
import java.util.Collections;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule that skips over certain often-used system classes which do not modify
 * the taint state
 * 
 * @author Steven Arzt
 *
 */
public class SkipSystemClassRule extends AbstractTaintPropagationRule {

	private final SootMethod objectCons;
	private final SootMethod objectClinit;
	private final SootMethod objectGetClass;
	private final SootMethod threadCons;

	public SkipSystemClassRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);

		// Get the system methods
		this.objectCons = Scene.v().getObjectType().getSootClass().getMethodUnsafe("void <init>()");
		this.objectClinit = Scene.v().getObjectType().getSootClass().getMethodUnsafe("void <clinit>()");
		this.objectGetClass = Scene.v().getObjectType().getSootClass().getMethodUnsafe("java.lang.Class getClass()");
		this.threadCons = Scene.v().grabMethod("<java.lang.Thread: void <init>()>");
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		// If this call goes to one of the well-known system methods, we skip it
		if (isSystemClassDest(dest))
			killAll.value = true;

		return null;
	}

	/**
	 * Gets whether the given destination method is one of our well-known system
	 * methods
	 * 
	 * @param dest
	 *            The destination method of the call
	 * @return True if the given method is one of the well-known system methods,
	 *         otherwise false
	 */
	private boolean isSystemClassDest(SootMethod dest) {
		return dest == objectCons || dest == objectClinit || dest == objectGetClass || dest == threadCons;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		// If we don't have any callees, we may not interfere with the normal
		// propagation
		Collection<SootMethod> callees = getManager().getICFG().getCalleesOfCallAt(stmt);
		if (callees.isEmpty())
			return null;

		// If we have killed a taint that would have gone into a system method, we need
		// to pass it on inside the caller.
		for (SootMethod callee : getManager().getICFG().getCalleesOfCallAt(stmt))
			if (!isSystemClassDest(callee))
				return null;
		return Collections.singleton(source);
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction calleeD1, Abstraction source, Stmt stmt,
                                                       Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

}
