package soot.jimple.infoflow.data.accessPaths;

import java.util.Arrays;

import soot.Value;
import soot.jimple.infoflow.data.AccessPathFragment;

/**
 * <p>
 * AccessPath reduction strategy that removes sub-chains that return to exactly
 * the same field.
 * </p>
 * 
 * <p>
 * The following example access path can be shortened by removing the first two
 * fragments.
 * </p>
 * 
 * <pre>
 * <java.lang.Thread: java.lang.ThreadGroup group>
 * <java.lang.ThreadGroup: java.lang.Thread[] threads>
 * <java.lang.Thread: java.lang.ThreadGroup group>
 * <java.lang.ThreadGroup: java.lang.Thread[] threads> *
 * </pre>
 * 
 * @author Steven Arzt
 *
 */
public class SameFieldReductionStrategy implements IAccessPathReductionStrategy {

	@Override
	public AccessPathFragment[] reduceAccessPath(Value base, AccessPathFragment[] fragments) {
		for (int bucketStart = fragments.length - 2; bucketStart >= 0; bucketStart--) {
			// Check if we have a repeating field
			int repeatPos = -1;
			for (int i = bucketStart + 1; i < fragments.length; i++)
				if (fragments[i].getField() == fragments[bucketStart].getField()
						&& Arrays.equals(fragments[i].getContext(), fragments[bucketStart].getContext())) {
					repeatPos = i;
					break;
				}
			int repeatLen = repeatPos - bucketStart;
			if (repeatPos < 0)
				continue;

			// Check that everything between bucketStart and repeatPos
			// really repeats after bucketStart
			boolean matches = true;
			for (int i = 0; i < repeatPos - bucketStart; i++)
				matches &= (repeatPos + i < fragments.length)
						&& fragments[bucketStart + i].getField() == fragments[repeatPos + i].getField();
			if (matches) {
				AccessPathFragment[] newFragments = new AccessPathFragment[fragments.length - repeatLen];
				System.arraycopy(fragments, 0, newFragments, 0, bucketStart + 1);
				System.arraycopy(fragments, repeatPos + 1, newFragments, bucketStart + 1,
						fragments.length - repeatPos - 1);
				fragments = newFragments;
				break;
			}
		}
		return fragments;
	}

}
