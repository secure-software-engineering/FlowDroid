package soot.jimple.infoflow.collections.test;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

public class AliasListTestCode {
    List<String> alias;

    static class Data {
        String stringField;
    }

    @FlowDroidTest(expected = 1)
    public void testListAdd1() {
        alias = null;

        Data d = new Data();
        List<Data> lst = new ArrayList<>();
        lst.add(d);
        Data alias = lst.get(0);
        alias.stringField = source();
        sink(d.stringField);
    }

    @FlowDroidTest(expected = 2)
    public void testShiftOnAlias1() {
        alias = null;

        List<String> lst = new ArrayList<>();
        lst.add(source()); // lst@0 tainted
        if (new Random().nextBoolean())
            alias = lst;
        else
            alias = new ArrayList<>();
        alias.add(0, "element"); // lst@0 must shift to the right
        sink(alias.get(0)); // right (because we cannot strong update)
        sink(alias.get(1)); // right
        sink(alias.get(2));
    }


    @FlowDroidTest(expected = 2)
    public void testShiftOnAlias2() {
        alias = null;

        List<String> lst = new ArrayList<>();
        lst.add(source()); // lst@0 tainted
        if (new Random().nextBoolean())
            alias = lst;
        else
            alias = new ArrayList<>();
        alias.add(0, "element"); // lst@0 must shift to the right
        sink(lst.get(0)); // right (because we cannot strong update)
        sink(lst.get(1)); // right
        sink(lst.get(2));
    }
}
