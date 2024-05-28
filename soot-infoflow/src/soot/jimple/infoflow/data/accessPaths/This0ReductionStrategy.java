package soot.jimple.infoflow.data.accessPaths;

import soot.RefType;
import soot.SootField;
import soot.Value;
import soot.jimple.infoflow.data.AccessPathFragment;

/**
 * <p>
 * Strategy that reduces back and forth references between an inner class and
 * its enclosing class to direct accesses.
 * </p>
 * 
 * <pre>
 * a.inner.this$0.c -> a.c
 * </pre>
 * 
 * @author Steven Arzt
 *
 */
public class This0ReductionStrategy implements IAccessPathReductionStrategy {

	@Override
	public AccessPathFragment[] reduceAccessPath(Value base, AccessPathFragment[] fragments) {
		for (int i = 0; i < fragments.length; i++) {
			// Is this a reference to an outer class?
			SootField curField = fragments[i].getField();
			if (curField.getName().startsWith("this$")) {
				// Get the name of the outer class
				String outerClassName = ((RefType) curField.getType()).getClassName();

				// Check the base object
				int startIdx = -1;
				if (base != null && base.getType() instanceof RefType
						&& ((RefType) base.getType()).getClassName().equals(outerClassName)) {
					startIdx = 0;
				} else {
					// Scan forward to find the same reference
					for (int j = 0; j < i; j++) {
						SootField nextField = fragments[j].getField();
						if (nextField.getType() instanceof RefType
								&& ((RefType) nextField.getType()).getClassName().equals(outerClassName)) {
							startIdx = j;
							break;
						}
					}
				}

				if (startIdx >= 0) {
					AccessPathFragment[] newFragments = new AccessPathFragment[fragments.length - (i - startIdx) - 1];
					System.arraycopy(fragments, 0, newFragments, 0, startIdx);
					System.arraycopy(fragments, i + 1, newFragments, startIdx, fragments.length - i - 1);
					fragments = newFragments;
					break;
				}
			}
		}
		return fragments;
	}

}
