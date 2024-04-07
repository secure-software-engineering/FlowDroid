package soot.jimple.infoflow.collections.test;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class ArrayTestCode {
    @FlowDroidTest(expected = 1)
    public void testArray1() {
        String[] arr = new String[2];
        arr[0] = source();
        sink(arr[0]);
    }

    @FlowDroidTest(expected = 0)
    public void testArray2() {
        String[] arr = new String[2];
        arr[0] = source();
        sink(arr[1]);
    }

    @FlowDroidTest(expected = 0)
    public void testListToArray1() {
        List<String> lst = new ArrayList<>();
        lst.add("Other");
        lst.add(source());
        Object[] arr = lst.toArray();
        sink(arr[0]);
    }

    @FlowDroidTest(expected = 1)
    public void testListToArray2() {
        List<String> lst = new ArrayList<>();
        lst.add("Other");
        lst.add(source());
        String[] arr = lst.toArray(new String[0]);
        sink(arr[1]);
    }

    @FlowDroidTest(expected = 1)
    public void testArrayToList1() {
        String[] arr = new String[2];
        arr[0] = source();
        List<String> lst = Arrays.asList(arr);
        sink(lst.get(0));
    }

    @FlowDroidTest(expected = 0)
    public void testArrayToList2() {
        String[] arr = new String[2];
        arr[0] = source();
        List<String> lst = Arrays.asList(arr);
        sink(lst.get(1));
    }
}
