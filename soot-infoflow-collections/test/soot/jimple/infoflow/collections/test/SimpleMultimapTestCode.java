package soot.jimple.infoflow.collections.test;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

import java.util.Collection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

public class SimpleMultimapTestCode {
    class A {
        String str;
        String unrelated;
    }

    @FlowDroidTest(expected = 1)
    public void testMultimapPutGet1() {
        Multimap<String, A> mmap = HashMultimap.create();
        A a = new A();
        a.str = source();
        mmap.put("K", a);
        Collection<A> c = mmap.get("K");
        sink(c.iterator().next().str);
    }

    @FlowDroidTest(expected = 0)
    public void testMultimapPutGet2() {
        Multimap<String, A> mmap = HashMultimap.create();
        A a = new A();
        a.str = source();
        mmap.put("K", a);
        Collection<A> c = mmap.get("K");
        sink(c.iterator().next().unrelated);
    }

    @FlowDroidTest(expected = 0)
    public void testMultimapPutGet3() {
        Multimap<String, A> mmap = HashMultimap.create();
        A a = new A();
        a.str = source();
        mmap.put("K", a);
        Collection<A> c = mmap.get("L");
        sink(c.iterator().next().str);
    }
}
