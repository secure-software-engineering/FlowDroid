package soot.jimple.infoflow.collections.test;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

import java.util.*;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class CollectionsTestCode {
    @FlowDroidTest(expected = 2)
    public void testSort1() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        lst.add("unrelated");
        Collections.sort(lst);
        sink(lst.get(0));
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 2)
    public void testSort2() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        lst.add("unrelated");
        Collections.sort(lst, Comparator.comparingInt(String::length));
        sink(lst.get(0));
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 2)
    public void testShuffle1() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        lst.add("unrelated");
        Collections.shuffle(lst);
        sink(lst.get(0));
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 2)
    public void testShuffle2() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        lst.add("unrelated");
        Collections.shuffle(lst, new Random());
        sink(lst.get(0));
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 2)
    public void testRotate1() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        lst.add("unrelated");
        Collections.rotate(lst, 2);
        sink(lst.get(0));
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 2)
    public void testReverse1() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        lst.add("unrelated");
        Collections.reverse(lst);
        sink(lst.get(0));
        sink(lst.get(1));
    }
}
