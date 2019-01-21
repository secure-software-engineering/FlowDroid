package soot.jimple.infoflow.problems.rules;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import soot.ArrayType;
import soot.IntType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.LengthExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.AbstractDataFlowAbstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;
import soot.jimple.infoflow.data.TaintAbstraction;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.solver.ngsolver.SolverState;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule for propagating array accesses
 * 
 * @author Steven Arzt
 *
 */
public class ArrayPropagationRule extends AbstractTaintPropagationRule {

	public ArrayPropagationRule(InfoflowManager manager, TaintAbstraction zeroValue, TaintPropagationResults results) {
		super(manager, zeroValue, results);
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateNormalFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, Stmt destStmt, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll, int flags) {
		final TaintAbstraction source = (TaintAbstraction) state.getTargetVal();
		final Stmt stmt = (Stmt) state.getTarget();

		// Get the assignment
		if (!(stmt instanceof AssignStmt))
			return null;
		AssignStmt assignStmt = (AssignStmt) stmt;

		TaintAbstraction newAbs = null;
		final Value leftVal = assignStmt.getLeftOp();
		final Value rightVal = assignStmt.getRightOp();

		if (rightVal instanceof LengthExpr) {
			LengthExpr lengthExpr = (LengthExpr) rightVal;
			if (getAliasing().mayAlias(source.getAccessPath().getPlainValue(), lengthExpr.getOp())) {
				// Is the length tainted? If only the contents are tainted, we
				// the
				// incoming abstraction does not match
				if (source.getAccessPath().getArrayTaintType() == ArrayTaintType.Contents)
					return null;

				// Taint the array length
				AccessPath ap = getManager().getAccessPathFactory().createAccessPath(leftVal, null, IntType.v(),
						(Type[]) null, true, false, true, ArrayTaintType.ContentsAndLength);
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
		}
		// y = x[i] && x tainted -> x, y tainted
		else if (rightVal instanceof ArrayRef) {
			Value rightBase = ((ArrayRef) rightVal).getBase();
			Value rightIndex = ((ArrayRef) rightVal).getIndex();
			if (source.getAccessPath().getArrayTaintType() != ArrayTaintType.Length
					&& getAliasing().mayAlias(rightBase, source.getAccessPath().getPlainValue())) {
				// We must remove one layer of array typing, e.g., A[][] -> A[]
				Type targetType = source.getAccessPath().getBaseType();
				assert targetType instanceof ArrayType;
				targetType = ((ArrayType) targetType).getElementType();

				// Create the new taint abstraction
				ArrayTaintType arrayTaintType = source.getAccessPath().getArrayTaintType();
				AccessPath ap = getManager().getAccessPathFactory().copyWithNewValue(source.getAccessPath(), leftVal,
						targetType, false, true, arrayTaintType);
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}

			// y = x[i] with i tainted
			else if (source.getAccessPath().getArrayTaintType() != ArrayTaintType.Length
					&& rightIndex == source.getAccessPath().getPlainValue()
					&& getManager().getConfig().getImplicitFlowMode().trackArrayAccesses()) {
				// Create the new taint abstraction
				ArrayTaintType arrayTaintType = ArrayTaintType.ContentsAndLength;
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
						null, false, true, ArrayTaintType.Length);
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
		}

		if (newAbs == null)
			return null;

		Set<AbstractDataFlowAbstraction> res = new HashSet<>();
		res.add(newAbs);

		// Compute the aliases
		if (Aliasing.canHaveAliases(assignStmt, leftVal, newAbs))
			getAliasing().computeAliases(state.derive(newAbs), leftVal, res,
					getManager().getICFG().getMethodOf(assignStmt));

		return res;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, SootMethod dest, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateCallToReturnFlow(
			SolverState<Unit, AbstractDataFlowAbstraction> state, ByReferenceBoolean killSource,
			ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<AbstractDataFlowAbstraction> propagateReturnFlow(
			Collection<AbstractDataFlowAbstraction> callerD1s, TaintAbstraction source, Stmt stmt, Stmt retSite,
			Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

}
