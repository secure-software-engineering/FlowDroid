package soot.jimple.infoflow.collections.test;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

import java.util.*;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

public class SimpleListTestCode {
    @FlowDroidTest(expected = 1)
    public void testListAdd1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 0)
    public void testListAdd2() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        sink(lst.get(0));
    }

    @FlowDroidTest(expected = 1)
    public void testListAdd3(int x) {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        if (x == 1)
            lst.add(tainted);
        else
            lst.add("Not tainted");
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 0)
    public void testListRemove1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.remove(1);
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 1)
    public void testListRemove2() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        String removed = lst.remove(1);
        sink(removed);
    }

    @FlowDroidTest(expected = 1)
    public void testListRemove3() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.remove(0);
        sink(lst.get(0));
    }

    @FlowDroidTest(expected = 0)
    public void testListRemove4() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add("Some String");
        lst.remove(0);
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 1)
    public void testListRemove5() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add("Some String");
        lst.remove("String");
        sink(lst.get(0));
    }

    @FlowDroidTest(expected = 0)
    public void testListClear() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.clear();
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 1)
    public void testListInsert1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add(1, "Other");
        lst.add("xxx");
        sink(lst.get(2));
    }

    @FlowDroidTest(expected = 0)
    public void testListInsert2() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add(1, "Other");
        lst.add("xxx");
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 1)
    public void testListInsert3() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add("Some String2");
        lst.add("Some String3");
        lst.add(1, tainted);
        lst.add("xxx");
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 1)
    public void testListInsert4() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add(new Random().nextInt(), "Other");
        lst.add("xxx");
        sink(lst.get(1)); // index 1 or 2 might be tainted
    }

    @FlowDroidTest(expected = 1)
    public void testListInsert5() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add(new Random().nextInt(), "Other");
        lst.add("xxx");
        sink(lst.get(2)); // index 1 or 2 might be tainted
    }

    @FlowDroidTest(expected = 0)
    public void testListInsert6() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("Some String");
        lst.add(tainted);
        lst.add(new Random().nextInt(), "Other");
        lst.add("xxx");
        sink(lst.get(3)); // index 1 or 2 might be tainted
    }

    @FlowDroidTest(expected = 1)
    public void testListInsertInLoop1() {
        List<String> lst = new ArrayList<>();
        lst.add(source()); // tainted @ idx=0
        while (new Random().nextBoolean()) {
            lst.add(0, "Some element"); // tainted idx gets shifted by 1 each iteration
        }
        sink(lst.get(0));
    }

    @FlowDroidTest(expected = 1)
    public void testListInsertInLoop2() {
        List<String> lst = new ArrayList<>();
        lst.add(source()); // tainted @ idx=0
        while (new Random().nextBoolean()) {
            lst.add(0, "Some element"); // tainted idx gets shifted by 1 each iteration
            lst.add(0, "Some element"); // tainted idx gets shifted by 1 each iteration
        }
        sink(lst.get(0));
    }

    @FlowDroidTest(expected = 1)
    public void testListReplaceAll1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add(tainted);
        lst.replaceAll(e -> e);
        sink(lst.get(0));
    }

    @FlowDroidTest(expected = 0)
    public void testListReplaceAll2() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add(tainted);
        lst.replaceAll(e -> "Overwritten");
        sink(lst.get(0));
    }

    @FlowDroidTest(expected = 0)
    public void testListSet1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("xxx");
        lst.add(tainted);
        lst.set(1, "XXX");
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 1)
    public void testListSet2() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("xxx");
        lst.add("yyy");
        lst.set(1, tainted);
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 2)
    public void testListRemoveAll1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("xxx");
        lst.add(tainted);
        lst.removeAll(Collections.emptyList());
        sink(lst.get(0));
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 2)
    public void testListRetainAll1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("xxx");
        lst.add(tainted);
        lst.retainAll(Collections.emptyList());
        sink(lst.get(0));
        sink(lst.get(1));
    }

    @FlowDroidTest(expected = 1)
    public void testListSort1() {
        List<String> lst = new ArrayList<>();
        String tainted = source();
        lst.add("xxx");
        lst.add(tainted);
        lst.sort(String::compareTo);
        sink(lst.get(0));
    }

    @FlowDroidTest(expected = 3)
    public void testListAddItself1() {
        List lst = new ArrayList<>();
        lst.add(source());
        while (new Random().nextBoolean()) {
            lst.add(lst);
        }
        sink(lst.get(0));
        sink(lst.get(1));
        sink(lst.get(2));
    }

    @FlowDroidTest(expected = 1)
    public void testListAddAll1() {
        List<String> lst = new ArrayList<>();
        lst.add("First el");
        lst.add(source());
        List<String> otherLst = new ArrayList<>();
        otherLst.add("First el");
        otherLst.add("Second el");
        otherLst.addAll(lst);
        sink(otherLst.get(0));
        sink(otherLst.get(1));
        sink(otherLst.get(2));
        sink(otherLst.get(3)); // Correct one
    }

    @FlowDroidTest(expected = 1)
    public void testListAddAll2() {
        List<String> lst = new ArrayList<>();
        lst.add("First el");
        lst.add(source());
        List<String> otherLst = new ArrayList<>();
        otherLst.add("First el");
        otherLst.addAll(lst);
        otherLst.add("Third el");
        sink(otherLst.get(0));
        sink(otherLst.get(1));
        sink(otherLst.get(2)); // Correct one
        sink(otherLst.get(3));
    }

    @FlowDroidTest(expected = 1)
    public void testListAddAll3() {
        List<String> lst = new ArrayList<>();
        lst.add("First el");
        lst.add(source());
        List<String> otherLst = new ArrayList<>();
        otherLst.add("First el");
        otherLst.add("Third el");
        lst.addAll(otherLst);
        sink(lst.get(0));
        sink(lst.get(1)); // Correct one
        sink(lst.get(2));
        sink(lst.get(3));
    }

    @FlowDroidTest(expected = 2)
    public void testListAddAll4() {
        List<String> lst = new ArrayList<>();
        lst.add(source());
        while (new Random().nextBoolean()) {
            lst.addAll(lst);
        }
        sink(lst.get(0));
        sink(lst.get(1));
    }
}
