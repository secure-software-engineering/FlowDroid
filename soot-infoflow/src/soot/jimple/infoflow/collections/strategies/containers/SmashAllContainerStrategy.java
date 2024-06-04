package soot.jimple.infoflow.collections.strategies.containers;

import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.context.UnknownContext;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContainerContext;

/**
 * A container strategy that smashed all containers, i.e., emulates FlowDroid's
 * behavior without container models
 * 
 * @author Steven Arzt
 *
 */
public class SmashAllContainerStrategy implements IContainerStrategy {

	@Override
	public Tristate intersect(ContainerContext apKey, ContainerContext stmtKey) {
		return Tristate.MAYBE();
	}

	@Override
	public ContainerContext[] append(ContainerContext[] ctxt1, ContainerContext[] ctxt2) {
		return new ContainerContext[] { UnknownContext.v() };
	}

	@Override
	public ContainerContext getKeyContext(Value key, Stmt stmt) {
		return UnknownContext.v();
	}

	@Override
	public ContainerContext getIndexContext(Value index, Stmt stmt) {
		return UnknownContext.v();
	}

	@Override
	public ContainerContext getNextPosition(Value lst, Stmt stmt) {
		return UnknownContext.v();
	}

	@Override
	public ContainerContext getFirstPosition(Value lst, Stmt stmt) {
		return UnknownContext.v();
	}

	@Override
	public ContainerContext getLastPosition(Value lst, Stmt stmt) {
		return UnknownContext.v();
	}

	@Override
	public Tristate lessThanEqual(ContainerContext ctxt1, ContainerContext ctxt2) {
		return Tristate.MAYBE();
	}

	@Override
	public ContainerContext shift(ContainerContext ctxt, int n, boolean exact) {
		return UnknownContext.v();
	}

	@Override
	public boolean shouldSmash(ContainerContext[] ctxts) {
		return true;
	}

	@Override
	public boolean isReadOnly(Unit unit) {
		return false;
	}

}
