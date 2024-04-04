package soot.jimple.infoflow.collections.problems.rules.forward;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import soot.ArrayType;
import soot.IntType;
import soot.Type;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.LengthExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.collections.ICollectionsSupport;
import soot.jimple.infoflow.collections.strategies.containers.IContainerStrategy;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.ContainerContext;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.IArrayContextProvider;
import soot.jimple.infoflow.problems.rules.forward.ArrayPropagationRule;
import soot.jimple.infoflow.util.ByReferenceBoolean;

public class ArrayWithIndexPropagationRule extends ArrayPropagationRule implements IArrayContextProvider {
	public ArrayWithIndexPropagationRule(InfoflowManager manager, Abstraction zeroValue,
			TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		// Get the assignment
		if (!(stmt instanceof AssignStmt))
			return null;
		AssignStmt assignStmt = (AssignStmt) stmt;

		boolean contentTainted = source.getAccessPath().getArrayTaintType() != AccessPath.ArrayTaintType.Length;

		Abstraction newAbs = null;
		Aliasing aliasing = getAliasing();
		final Value leftVal = assignStmt.getLeftOp();
		if (leftVal instanceof ArrayRef) {
			Value leftBase = ((ArrayRef) leftVal).getBase();
			Value leftIndex = ((ArrayRef) leftVal).getIndex();
			// Strong update if possible
			if (contentTainted && aliasing.mayAlias(leftBase, source.getAccessPath().getPlainValue())
					&& matchesIndex(source, leftIndex, stmt).isTrue()) {
				if (source.getAccessPath().getArrayTaintType() == AccessPath.ArrayTaintType.ContentsAndLength) {
					AccessPath ap = manager.getAccessPathFactory().copyWithNewValue(source.getAccessPath(),
							source.getAccessPath().getPlainValue(), source.getAccessPath().getBaseType(), false, true,
							AccessPath.ArrayTaintType.Length);
					killSource.value = true;
					return Collections.singleton(source.deriveNewAbstraction(ap, stmt));
				} else {
					killAll.value = true;
					return null;
				}
			}
		}

		final Value rightVal = assignStmt.getRightOp();

		if (rightVal instanceof LengthExpr) {
			LengthExpr lengthExpr = (LengthExpr) rightVal;
			if (getAliasing().mayAlias(source.getAccessPath().getPlainValue(), lengthExpr.getOp())) {
				// Is the length tainted? If only the contents are tainted, we
				// the
				// incoming abstraction does not match
				if (source.getAccessPath().getArrayTaintType() == AccessPath.ArrayTaintType.Contents)
					return null;

				// Taint the array length
				AccessPath ap = getManager().getAccessPathFactory().createAccessPath(leftVal, IntType.v(), null, true,
						false, true, AccessPath.ArrayTaintType.ContentsAndLength);
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
		}
		// y = x[i] && x tainted -> x, y tainted
		if (rightVal instanceof ArrayRef) {
			Value rightBase = ((ArrayRef) rightVal).getBase();
			Value rightIndex = ((ArrayRef) rightVal).getIndex();
			if (contentTainted && getAliasing().mayAlias(rightBase, source.getAccessPath().getPlainValue())
					&& !matchesIndex(source, rightIndex, stmt).isFalse()) {
				// We must remove one layer of array typing, e.g., A[][] -> A[]
				Type targetType = source.getAccessPath().getBaseType();
				assert targetType instanceof ArrayType;
				if (targetType instanceof ArrayType)
					targetType = ((ArrayType) targetType).getElementType();
				else
					targetType = null;

				// Create the new taint abstraction
				AccessPath.ArrayTaintType arrayTaintType = source.getAccessPath().getArrayTaintType();
				AccessPath ap = getManager().getAccessPathFactory().copyWithNewValue(source.getAccessPath(), leftVal,
						targetType, false, true, arrayTaintType);
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
			// y = x[i] with i tainted
			else if (source.getAccessPath().getArrayTaintType() != AccessPath.ArrayTaintType.Length
					&& rightIndex == source.getAccessPath().getPlainValue()
					&& getManager().getConfig().getImplicitFlowMode().trackArrayAccesses()) {
				// Create the new taint abstraction
				AccessPath.ArrayTaintType arrayTaintType = AccessPath.ArrayTaintType.ContentsAndLength;
				AccessPath ap = getManager().getAccessPathFactory().copyWithNewValue(source.getAccessPath(), leftVal,
						null, false, true, arrayTaintType);
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
		}
		// y = new A[i] with i tainted
		else if (rightVal instanceof NewArrayExpr && getManager().getConfig().getEnableArraySizeTainting()) {
			NewArrayExpr newArrayExpr = (NewArrayExpr) rightVal;
			if (getAliasing().mayAlias(source.getAccessPath().getPlainValue(), newArrayExpr.getSize())) {
				// Create the new taint abstraction
				AccessPath ap = getManager().getAccessPathFactory().copyWithNewValue(source.getAccessPath(), leftVal,
						null, false, true, AccessPath.ArrayTaintType.Length);
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
		}

		if (newAbs == null)
			return null;

		Set<Abstraction> res = Collections.singleton(newAbs);
		// Compute the aliases
		if (manager.getAliasing().canHaveAliases(assignStmt, leftVal, newAbs))
			getAliasing().computeAliases(d1, assignStmt, leftVal, res, getManager().getICFG().getMethodOf(assignStmt),
					newAbs);

		return res;
	}

	private Tristate matchesIndex(Abstraction incoming, Value index, Stmt stmt) {
		ContainerContext[] apCtxt = incoming.getAccessPath().getBaseContext();
		if (apCtxt == null)
			return Tristate.MAYBE();

		IContainerStrategy strategy = ((ICollectionsSupport) manager.getTaintWrapper()).getContainerStrategy();
		ContainerContext indexCtxt = strategy.getIndexContext(index, stmt);
		return strategy.intersect(apCtxt[0], indexCtxt);
	}

	@Override
	public ContainerContext[] getContextForArrayRef(ArrayRef arrayRef, Stmt stmt) {
		IContainerStrategy strategy = ((ICollectionsSupport) manager.getTaintWrapper()).getContainerStrategy();
		ContainerContext ctxt = strategy.getIndexContext(arrayRef.getIndex(), stmt);
		return ctxt.containsInformation() ? new ContainerContext[] { ctxt } : null;
	}
}
