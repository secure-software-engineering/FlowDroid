package soot.jimple.infoflow.problems.rules.backward;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.LengthExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPath.ArrayTaintType;
import soot.jimple.infoflow.data.ContainerContext;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.problems.rules.IArrayContextProvider;
import soot.jimple.infoflow.typing.TypeUtils;
import soot.jimple.infoflow.util.ByReferenceBoolean;

/**
 * Rule for propagating array accesses
 * 
 * @author Steven Arzt
 *
 */
public class BackwardsArrayPropagationRule extends AbstractTaintPropagationRule implements IArrayContextProvider {

	public BackwardsArrayPropagationRule(InfoflowManager manager, Abstraction zeroValue,
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

		Aliasing aliasing = manager.getAliasing();
		if (aliasing == null)
			return null;

		Abstraction newAbs = null;
		final Value leftVal = assignStmt.getLeftOp();
		final Value rightVal = assignStmt.getRightOp();

		Set<Abstraction> res = new HashSet<>();
		// x = a.length -> a length tainted
		if (rightVal instanceof LengthExpr) {
			LengthExpr lengthExpr = (LengthExpr) rightVal;
			if (aliasing.mayAlias(leftVal, source.getAccessPath().getPlainValue())) {
				// Taint the array length
				AccessPath ap = getManager().getAccessPathFactory().createAccessPath(lengthExpr.getOp(),
						lengthExpr.getOp().getType(), true, ArrayTaintType.Length);
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
		}
		// y = new A[i] && y length tainted -> i tainted
		else if (rightVal instanceof NewArrayExpr && getManager().getConfig().getEnableArraySizeTainting()) {
			NewArrayExpr newArrayExpr = (NewArrayExpr) rightVal;
			if (!(newArrayExpr.getSize() instanceof Constant)
					&& source.getAccessPath().getArrayTaintType() != ArrayTaintType.Contents
					&& aliasing.mayAlias(source.getAccessPath().getPlainValue(), leftVal)) {
				// Create the new taint abstraction
				AccessPath ap = getManager().getAccessPathFactory().createAccessPath(newArrayExpr.getSize(), true);
				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
		}
		// y = x[i] && y tainted -> x[i] tainted
		else if (rightVal instanceof ArrayRef) {
			Value rightBase = ((ArrayRef) rightVal).getBase();
			Value rightIndex = ((ArrayRef) rightVal).getIndex();
			// y = x[i]
			if (source.getAccessPath().getArrayTaintType() != ArrayTaintType.Length
					&& aliasing.mayAlias(leftVal, source.getAccessPath().getPlainValue())) {
				// track index
				AccessPath ap;
				if (getManager().getConfig().getImplicitFlowMode().trackArrayAccesses()) {
					ap = getManager().getAccessPathFactory().createAccessPath(rightIndex, false);
					if (ap != null) {
						newAbs = source.deriveNewAbstraction(ap, assignStmt);
						res.add(newAbs);
					}
				}
				// taint whole array
				// We add one layer
				Type baseType = source.getAccessPath().getBaseType();
				Type targetType = TypeUtils.buildArrayOrAddDimension(baseType, baseType.getArrayType());

				// Create the new taint abstraction
				ap = getManager().getAccessPathFactory().copyWithNewValue(source.getAccessPath(), rightBase, targetType,
						false, true, ArrayTaintType.Contents);

				newAbs = source.deriveNewAbstraction(ap, assignStmt);
			}
		}

		if (newAbs == null)
			return null;

		// We have to keep the source if leftval is an array
		killSource.value = !(leftVal instanceof ArrayRef);
		res.add(newAbs);

		if (aliasing.canHaveAliases(assignStmt, leftVal, newAbs))
			aliasing.computeAliases(d1, assignStmt, leftVal, res, manager.getICFG().getMethodOf(assignStmt), newAbs);

		return res;
	}

	@Override
	public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest,
			ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt,
			ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction calleeD1,
			Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
		return null;
	}

	@Override
	public ContainerContext[] getContextForArrayRef(ArrayRef arrayRef, Stmt stmt) {
		return null;
	}
}
