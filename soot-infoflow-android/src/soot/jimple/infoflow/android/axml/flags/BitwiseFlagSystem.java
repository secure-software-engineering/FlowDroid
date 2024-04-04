package soot.jimple.infoflow.android.axml.flags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * In some cases, bitmasks might be used in a way where there is a precendence
 * of certain masks to save space. For "normal" bitmasks, the traditional way of
 * checking is much faster.
 * 
 * Simple Example: Option A (can be used alone or in combination with others):
 * 01 Option B (cannot be used with option A): 10 Option C (only valid when used
 * with Option A): 11
 * 
 * In this case, we could do checking of B via ((v & 10) == 1) && ((v & 1) != 1)
 * but sometimes there are a lot of options (e.g. inputType)
 * 
 * @param <T> the keys used to distinguish flags
 */
public class BitwiseFlagSystem<T> {
	private List<T> keys = new ArrayList<T>();
	private List<Integer> values = new ArrayList<Integer>();

	/**
	 * Associate the given key with the bits set in set bits. The first registration
	 * wins.
	 * 
	 * @param key     the key
	 * @param setBits the bits set
	 */
	public void register(T key, int setBits) {
		keys.add(key);
		values.add(setBits);
	}

	/**
	 * Returns all matching flags
	 * 
	 * @param value input value
	 */
	public final Collection<T> getFlags(int value) {
		List<T> matchedResults = new ArrayList<>(4);
		List<Integer> matchedValues = new ArrayList<>(4);
		for (int i = 0; i < keys.size(); i++) {
			int v = values.get(i);
			if ((v & value) == v) {
				if (!hadAnyMatch(v, matchedValues)) {
					matchedResults.add(keys.get(i));
					matchedValues.add(v);
				}

			}
		}
		return matchedResults;
	}

	private static boolean hadAnyMatch(int value, List<Integer> matchedValues) {
		for (int c : matchedValues) {
			if ((c & value) == value) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether all the given flags are set
	 * 
	 * @param inputValue input value
	 * @param flag       the flags to check
	 */
	@SafeVarargs
	public final boolean isSet(int inputValue, T... flag) {
		List<T> flagsLeft = new ArrayList<T>(flag.length);
		for (T i : flag)
			flagsLeft.add(i);

		for (T t : getFlags(inputValue)) {
			Iterator<T> it = flagsLeft.iterator();
			while (it.hasNext()) {
				if (it.next().equals(t)) {
					it.remove();
					if (flagsLeft.isEmpty())
						return true;
				}
			}
		}
		return false;
	}
}
