package soot.jimple.infoflow.android.iccta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.tagkit.Tag;

/**
 * One ICC Link contain one source component and one destination component. this
 * class is used to collect all the assist methods which instrument source
 * component.
 *
 */
public class IccInstrumentSource {

	private static IccInstrumentSource s = null;

	private IccInstrumentSource() {
	}

	public static IccInstrumentSource v() {
		if (s == null) {
			s = new IccInstrumentSource();
		}
		return s;
	}

	// call this method for all your need to instrument the source class
	public void instrumentSource(IccLink link, SootMethod redirectMethod) {
		insertRedirectMethodCallAfterIccMethod(link, redirectMethod);
	}

	public void insertRedirectMethodCallAfterIccMethodForContentProvider(IccLink link, SootMethod redirectMethod) {
		List<Value> args = null;

		Stmt fromStmt = (Stmt) link.getFromU();

		if (fromStmt == null) {
			return;
		}

		if (redirectMethod == null) {
			return;
		}

		if (fromStmt instanceof AssignStmt) {
			AssignStmt as = (AssignStmt) fromStmt;
			Unit redirectAssignU = (Unit) Jimple.v().newAssignStmt(as.getLeftOp(),
					Jimple.v().newStaticInvokeExpr(redirectMethod.makeRef(), args));
			link.getFromSM().retrieveActiveBody().getUnits().insertAfter(redirectAssignU, link.getFromU());
		} else {
			Unit redirectCallU = (Unit) Jimple.v()
					.newInvokeStmt(Jimple.v().newStaticInvokeExpr(redirectMethod.makeRef(), args));

			link.getFromSM().retrieveActiveBody().getUnits().insertAfter(redirectCallU, link.getFromU());
		}
	}

	/**
	 * We have the intent in a register at this point, create a new statement to
	 * call the static method with the intent as parameter
	 * 
	 * @param link
	 * @param redirectMethod
	 */
	public void insertRedirectMethodCallAfterIccMethod(IccLink link, SootMethod redirectMethod) {
		List<Value> args = new ArrayList<Value>();

		Stmt fromStmt = (Stmt) link.getFromU();

		if (fromStmt == null) {
			return;
		}

		// specially deal with startActivityForResult since they have two
		// parameters
		if (fromStmt.toString().contains("startActivityForResult")) {

			Value expr = fromStmt.getInvokeExprBox().getValue();
			Value arg0 = null;
			if (expr instanceof JVirtualInvokeExpr) {
				JVirtualInvokeExpr jviExpr = (JVirtualInvokeExpr) expr;
				arg0 = jviExpr.getBase();
			} else if (expr instanceof JSpecialInvokeExpr) {
				JSpecialInvokeExpr jsiExpr = (JSpecialInvokeExpr) expr;
				arg0 = jsiExpr.getBase();
			}

			// Value arg0 =
			// fromStmt.getInvokeExpr().getUseBoxes().get(0).getValue();
			Value arg1 = fromStmt.getInvokeExpr().getArg(0);
			args.add(arg0);
			args.add(arg1);
		} else if (fromStmt.toString().contains("bindService")) {
			Value arg0 = fromStmt.getInvokeExpr().getArg(0); // intent
			Value arg1 = fromStmt.getInvokeExpr().getArg(1); // serviceConnection
			args.add(arg1);
			args.add(arg0);
		} else {
			Value arg0 = fromStmt.getInvokeExpr().getArg(0);
			args.add(arg0);
		}

		if (redirectMethod == null) {
			return;
		}

		Unit redirectCallU = (Unit) Jimple.v()
				.newInvokeStmt(Jimple.v().newStaticInvokeExpr(redirectMethod.makeRef(), args));

		PatchingChain<Unit> units = link.getFromSM().retrieveActiveBody().getUnits();

		copyTags(link.getFromU(), redirectCallU);
		link.getFromSM().retrieveActiveBody().getUnits().insertAfter(redirectCallU, link.getFromU());

		// remove the real ICC methods call stmt
		// link.getFromSM().retrieveActiveBody().getUnits().remove(link.getFromU());
		// Please refer to AndroidIPCManager.postProcess() for this removing
		// process.

		// especially for createChooser method
		for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
			Stmt stmt = (Stmt) iter.next();

			if (stmt.toString().contains(
					"<android.content.Intent: android.content.Intent createChooser(android.content.Intent,java.lang.CharSequence)>")) {
				List<ValueBox> vbs = stmt.getUseAndDefBoxes();
				Unit assignU = Jimple.v().newAssignStmt(vbs.get(0).getValue(), vbs.get(1).getValue());
				copyTags(stmt, assignU);
				units.insertAfter(assignU, stmt);
				units.remove(stmt);
			}
		}
	}

	/**
	 * Copy all the tags of {from} to {to}, if {to} already contain the copied
	 * tag, then overwrite it.
	 * 
	 * @param from
	 * @param to
	 */
	public static void copyTags(Unit from, Unit to) {
		List<Tag> tags = from.getTags();

		for (Tag tag : tags) {
			to.removeTag(tag.getName()); // exception??

			to.addTag(tag);
		}
	}
}
