package soot.jimple.infoflow.collect;

import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Atomic and concurrent implementation of a BitSet. Original implementation
 * taken from:
 * http://stackoverflow.com/questions/12424633/atomicbitset-implementation-for-java
 * 
 * @author Steven Arzt
 *
 */
public class AtomicBitSet {
	private final AtomicIntegerArray array;
	private int largestInt;

	public AtomicBitSet(int length) {
		this.largestInt = length;
		int intLength = (length + 31) / 32;
		array = new AtomicIntegerArray(intLength);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int val = 0;
		for (int i = 0; i < array.length(); i++) {
			int c = array.get(i);

			for (int bit = 0; bit < 32; bit++) {
				if ((c & (1 << bit)) != 0) {
					sb.append(val).append(", ");
				}
				val++;
			}
		}
		if (sb.length() > 0)
			sb.setLength(sb.length() - 2);
		return sb.toString();
	}

	public String toBitString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length(); i++) {
			int c = array.get(i);

			for (int bit = 0; bit < 32; bit++) {
				if ((c & (1 << bit)) != 0)
					sb.append("1, ");
				else
					sb.append("0, ");
			}
		}
		if (sb.length() > 0)
			sb.setLength(sb.length() - 2);
		return sb.toString();
	}

	public boolean unset(long n) {
		int bit = 1 << n;
		int idx = (int) (n >>> 5);
		while (true) {
			int num = array.get(idx);
			int num2 = num & ~bit;

			// If the bit is already unset in the current value, we are too late
			if (num == num2)
				return false;

			if (array.compareAndSet(idx, num, num2))
				return true;
		}
	}

	public boolean set(long n) {
		int bit = 1 << (n);
		int idx = (int) (n >>> 5);
		while (true) {
			int num = array.get(idx);
			int num2 = num | bit;

			// If the bit is already set in the current value, we are too late
			if (num == num2)
				return false;

			if (array.compareAndSet(idx, num, num2))
				return true;
		}
	}

	public int getLargestInt() {
		return largestInt;
	}

	public int size() {
		return array.length();
	}

	public boolean get(long n) {
		int bit = 1 << n;
		int idx = (int) (n >>> 5);
		int num = array.get(idx);
		return (num & bit) != 0;
	}
}
