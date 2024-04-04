package soot.jimple.infoflow.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Extended version of the {@link AtomicInteger} class
 * 
 * @author Steven Arzt
 *
 */
public class ExtendedAtomicInteger extends AtomicInteger {

	/**
	 * 
	 */
	private static final long serialVersionUID = -365647246646024478L;

	public ExtendedAtomicInteger() {
		super();
	}

	public ExtendedAtomicInteger(int initialValue) {
		super(initialValue);
	}

	/**
	 * Atomically subtracts the given value from the current value
	 * 
	 * @param diff The value to subtract from the current value
	 */
	public void subtract(int diff) {
		while (true) {
			int curValue = get();
			if (compareAndSet(curValue, curValue - diff))
				break;
		}
	}

}
