package soot.jimple.infoflow.android.iccta;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.util.Chain;

public class IccLink {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	String fromSMString;
	String iccMethod;
	SootMethod fromSM;
	Unit fromU;
	int instruction;
	String exit_kind;
	String destinationC;

	Chain<Unit> units = null;

	public IccLink() {
	}

	public void linkWithTarget() {
		if (fromSM == null) {
			try {
				fromSM = Scene.v().getMethod(fromSMString);
				Body body = fromSM.retrieveActiveBody();
				units = body.getUnits();

				int i = 0;

				for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
					Stmt stmt = (Stmt) iter.next();

					if (i == instruction) {
						if (!iccMethod.contains(stmt.getInvokeExpr().getMethod().getName())) {
							throw new RuntimeException("The reached point is not an ICC method.");
						} else {
							fromU = stmt;
						}
					}

					i++;
				}

			} catch (Exception ex) {
				logger.warn("Linking the target: " + fromSMString + " is ignored.", ex);
			}
		}
	}

	public String toString() {
		return fromSMString + " [" + instruction + ":" + iccMethod + "] " + destinationC;
	}

	public String getFromSMString() {
		return fromSMString;
	}

	public void setFromSMString(String fromSMString) {
		this.fromSMString = fromSMString;
	}

	public SootMethod getFromSM() {
		return fromSM;
	}

	public void setFromSM(SootMethod fromSM) {
		this.fromSM = fromSM;
	}

	public Unit getFromU() {
		return fromU;
	}

	public void setFromU(Unit fromU) {
		this.fromU = fromU;
	}

	public int getInstruction() {
		return instruction;
	}

	public void setInstruction(int instruction) {
		this.instruction = instruction;
	}

	public String getExit_kind() {
		return exit_kind;
	}

	public void setExit_kind(String exit_kind) {
		this.exit_kind = exit_kind;
	}

	public String getDestinationC() {
		return destinationC;
	}

	public void setDestinationC(String destinationC) {
		this.destinationC = destinationC;
	}

	public Chain<Unit> getUnits() {
		return units;
	}

	public void setUnits(Chain<Unit> units) {
		this.units = units;
	}

	public String getIccMethod() {
		return iccMethod;
	}

	public void setIccMethod(String iccMethod) {
		this.iccMethod = iccMethod;
	}
}
