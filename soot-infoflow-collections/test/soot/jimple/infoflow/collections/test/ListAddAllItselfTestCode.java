package soot.jimple.infoflow.collections.test;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

import java.util.ArrayList;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

public class ListAddAllItselfTestCode {
    @FlowDroidTest(expected = 1)
    public void testListAllAllItselfFiniteLoop1() {
        ArrayList<String> lst = new ArrayList<>();
        lst.add(source());
        for (int i = 0; i < 3; i++) {
            lst.addAll(lst);
        }
        sink(lst.get(0));
    }
}
