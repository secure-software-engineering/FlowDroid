package soot.jimple.infoflow.river;

import java.util.Iterator;
import java.util.Set;

import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;

public class Utils {

	/**
	 * Check whether baseLocal is tainted in the outgoing set. Assumes baseLocal is
	 * an object and the check happens at a call site.
	 *
	 * @param outgoing  outgoing taint set
	 * @param baseLocal base local
	 * @return corresponding abstraction if baseLocal is tainted else null
	 */
	public static Abstraction getTaintFromLocal(Set<Abstraction> outgoing, Value baseLocal) {
		for (Abstraction abs : outgoing)
			if (abs.getAccessPath().getPlainValue() == baseLocal)
				return abs;

		return null;
	}

	/**
	 * Check whether the access path is read at unit.
	 *
	 * @param unit unit
	 * @param ap   access path
	 * @return true if ap is read at unit
	 */
	public static boolean isReadAt(Unit unit, AccessPath ap) {
		Iterator<ValueBox> it = unit.getUseBoxesIterator();
		while (it.hasNext())
			if (it.next().getValue() == ap.getPlainValue())
				return true;

		return false;
	}

}
