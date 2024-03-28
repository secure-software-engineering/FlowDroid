package soot.jimple.infoflow.collections.test;

import soot.jimple.infoflow.collections.test.junit.FlowDroidTest;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static soot.jimple.infoflow.collections.test.Helper.*;

public class SimpleQueueTestCode {
    @FlowDroidTest(expected = 1)
    public void queueTest1() {
        Queue<String> q = new LinkedBlockingQueue<>();
        q.offer(source());
        q.offer("Element");
        sink(q.element());
    }

    @FlowDroidTest(expected = 0)
    public void queueTest2() {
        Queue<String> q = new LinkedBlockingQueue<>();
        q.offer("Element");
        q.offer(source());
        sink(q.element());
    }

    @FlowDroidTest(expected = 1)
    public void priorityQueueTest1() {
        Queue<String> q = new PriorityQueue<>();
        q.offer(source());
        q.offer("Element");
        sink(q.element());
    }

    @FlowDroidTest(expected = 1)
    public void priorityQueueTest2() {
        Queue<String> q = new PriorityQueue<>();
        q.offer("Element");
        q.offer(source());
        // Ordering of the priority queue might have changed the index
        sink(q.element());
    }
}
