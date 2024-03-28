package soot.jimple.infoflow.collections.test;

import static soot.jimple.infoflow.collections.test.Helper.sink;
import static soot.jimple.infoflow.collections.test.Helper.source;

import java.util.Stack;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

public class SimpleStackTestCode {
    class A {
        String f;
    }

    @FlowDroidTest(expected = 1)
    public void testStackPushPop1() {
        Stack<String> s = new Stack<>();
        String tainted = source();
        s.push("First");
        s.push(tainted);
        s.push("Third");
        s.pop();
        String res = s.pop();
        sink(res);
    }

    @FlowDroidTest(expected = 0)
    public void testStackPushPop2() {
        Stack<String> s = new Stack<>();
        String tainted = source();
        // s -> 0
        s.push("First");
        // s -> 1
        s.push(tainted);
        // s -> 2
        s.push("Third");
        // s -> 3
        s.pop();
        // s -> 2
        s.pop();
        // s -> 1
        String res = s.pop();
        // s -> 0
        sink(res);
    }

    @FlowDroidTest(expected = 1)
    public void testStackPushPopPeek1() {
        Stack<String> s = new Stack<>();
        String tainted = source();
        // s -> 0
        s.push("First");
        // s -> 1
        s.push(tainted);
        // s -> 2
        s.push("Third");
        // s -> 3
        s.pop();
        // s -> 2
        String res = s.peek();
        // s -> 2
        sink(res);
    }

    @FlowDroidTest(expected = 1)
    public void testStackPushReturn1() {
        A a = new A();
        a.f = source();

        Stack<A> s = new Stack<>();
        A b = s.push(a);
        sink(b.f);
    }
}
