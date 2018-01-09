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

    public AtomicBitSet(int length) {
        int intLength = (length + 31) / 32;
        array = new AtomicIntegerArray(intLength);
    }

    public boolean set(long n) {
        int bit = 1 << n;
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
