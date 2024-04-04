package soot.jimple.infoflow.test.collect;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import soot.jimple.infoflow.collect.AtomicBitSet;

public class AtomicBitSetTest {

	@Test
	public void simpleTest() {
		AtomicBitSet bs = new AtomicBitSet(10);
		bs.set(3);
		assertTrue(bs.get(3));
		assertFalse(bs.get(5));
	}

	@Test
	public void sizeTest() {
		AtomicBitSet bs = new AtomicBitSet(5);
		bs.set(7);
		assertTrue(bs.get(7));
		assertFalse(bs.get(3));
		assertFalse(bs.get(10));
	}

	@Test
	public void largeTest() {
		AtomicBitSet bs = new AtomicBitSet(120);
		bs.set(7);
		bs.set(115);
		assertTrue(bs.get(7));
		assertTrue(bs.get(115));
		assertFalse(bs.get(3));
		assertFalse(bs.get(10));
		assertFalse(bs.get(116));
	}

}
