package soot.jimple.infoflow.collections.test;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

import java.util.Vector;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

public class SimpleVectorTestCode {

    @FlowDroidTest(expected = 1)
    public void testVectorFirstElement1() {
        Vector<String> v = new Vector<>();
        v.addElement(source());
        v.addElement("Test");
        sink(v.firstElement());
    }

    @FlowDroidTest(expected = 0)
    public void testVectorFirstElement2() {
        Vector<String> v = new Vector<>();
        v.addElement("Test");
        v.addElement(source());
        sink(v.firstElement());
    }

    @FlowDroidTest(expected = 1)
    public void testVectorHierarchySummaries1() {
        Vector<String> v = new Vector<>();
        v.add("Test");
        v.add(source());
        sink(v.get(1));
    }

    @FlowDroidTest(expected = 1)
    public void testVectorInsertElementAt1() {
        Vector<String> v = new Vector<>();
        v.addElement("Test");
        v.insertElementAt(source(), 0);
        sink(v.firstElement());
    }

    @FlowDroidTest(expected = 0)
    public void testVectorInsertElementAt2() {
        Vector<String> v = new Vector<>();
        v.addElement("Test");
        v.addElement("Test");
        v.insertElementAt(source(), 1);
        sink(v.firstElement());
    }

    @FlowDroidTest(expected = 0)
    public void testVectorRemoveAllElements1() {
        Vector<String> v = new Vector<>();
        v.addElement(source());
        v.addElement("Test");
        v.removeAllElements();
        sink(v.firstElement());
    }

    @FlowDroidTest(expected = 0)
    public void testVectorRemoveElementAt1() {
        Vector<String> v = new Vector<>();
        v.addElement(source());
        v.addElement("Test");
        v.removeElementAt(0);
        sink(v.firstElement());
    }

    @FlowDroidTest(expected = 1)
    public void testVectorRemoveElementAt2() {
        Vector<String> v = new Vector<>();
        v.addElement("Test");
        v.addElement(source());
        v.removeElementAt(0);
        sink(v.firstElement());
    }

    @FlowDroidTest(expected = 0)
    public void testVectorSetElementAt1() {
        Vector<String> v = new Vector<>();
        v.addElement("Test");
        v.addElement(source());
        v.setElementAt("XXX", 1);
        sink(v.get(1));
    }

    @FlowDroidTest(expected = 1)
    public void testVectorSetElementAt2() {
        Vector<String> v = new Vector<>();
        v.addElement("Test");
        v.addElement("XXX");
        v.setElementAt(source(), 1);
        sink(v.get(1));
    }
}
