package soot.jimple.infoflow.test.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;

import soot.jimple.infoflow.util.extensiblelist.ExtensibleList;

public class ExtensibleListTest {

	@Test
	public void test() {
		ExtensibleList<Integer> l1 = new ExtensibleList<>();
		l1.addAll(1, 2, 3);
		assertEquals(3, l1.size());
		assertEquals(3, (int) l1.removeLast());
		assertEquals(2, l1.size());
		assertEquals(2, (int) l1.getLast());

		ExtensibleList<Integer> cmp = new ExtensibleList<>();
		cmp.addAll(1, 2);
		assertEquals(cmp, l1);

		ExtensibleList<Integer> l2 = new ExtensibleList<>(l1);
		l2.addAll(3, 4);
		ExtensibleList<Integer> l3 = new ExtensibleList<>(l2);
		l3.addAll(5);

		testListForward(l2, 1, 2, 3, 4);
		l2.addAll(-1, -2);
		l1.add(-2);
		testListForward(l3, 1, 2, 3, 4, 5);

		l3 = l3.addFirstSlow(9);
		testListForward(l3, 9, 1, 2, 3, 4, 5);
	}

	@Test
	public void testSafety() {
		ExtensibleList<Integer> l1 = new ExtensibleList<>();
		l1.addAll(1, 2, 3);

		ExtensibleList<Integer> l2 = new ExtensibleList<>(l1);
		l2.add(4);
		assertEquals(4, (int) l2.removeLast());

		ExtensibleList<Integer> l3 = (ExtensibleList<Integer>) l1.removeLast();
		testListForward(l3, 1, 2);
		testListForward(l1, 1, 2, 3);
	}

	@Test
	public void testSafety2() {
		ExtensibleList<Integer> l1 = new ExtensibleList<>();
		l1.addAll(1, 2, 3);

		ExtensibleList<Integer> l2 = new ExtensibleList<>(l1);
		testListForward(l2, 1, 2, 3);
		l2 = (ExtensibleList<Integer>) l2.removeLast();
		testListForward(l2, 1, 2);
		testListForward(l1, 1, 2, 3);
	}

	@Test
	public void testRemoveLast() {
		ExtensibleList<Integer> l1 = new ExtensibleList<>();
		l1.addAll(1, 2, 3);

		assertEquals(3, l1.removeLast());
		assertEquals(2, l1.size());

		l1.add(42);

		assertEquals(42, l1.removeLast());
		assertEquals(2, l1.size());
	}

	private void testListForward(ExtensibleList<Integer> l, int... test) {
		assertEquals(test.length, l.size());
		if (test.length > 0)
			if (!l.getFirstSlow().equals(test[0]))
				throw new AssertionError();
		int r = l.getLast();
		assertEquals(r, test[test.length - 1]);
		ExtensibleList<Integer> cmp = new ExtensibleList<>();
		for (int i : test)
			cmp.add(i);
		assertEquals(l, cmp);
		assertEquals(l.hashCode(), cmp.hashCode());

		Iterator<Integer> it = l.reverseIterator();
		for (int i = test.length - 1; i >= 0; i--) {
			int c = test[i];
			assertTrue(it.hasNext());
			int actual = (int) it.next();
			assertEquals(c, actual);
		}
		assertFalse(it.hasNext());
	}
}
