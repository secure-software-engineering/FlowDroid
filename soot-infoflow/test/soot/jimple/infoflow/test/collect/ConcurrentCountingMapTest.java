package soot.jimple.infoflow.test.collect;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import soot.jimple.infoflow.collect.ConcurrentCountingMap;

public class ConcurrentCountingMapTest {

	@Test
	public void keySetTest() {
		ConcurrentCountingMap<String> map = new ConcurrentCountingMap<>();
		map.increment("Hello");
		map.increment("World");
		for (String k : map.keySet())
			assertEquals(new Integer(1), map.get(k));
	}

}
