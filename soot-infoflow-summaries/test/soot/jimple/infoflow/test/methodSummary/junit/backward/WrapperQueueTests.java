package soot.jimple.infoflow.test.methodSummary.junit.backward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;
import soot.jimple.infoflow.Infoflow;

public class WrapperQueueTests extends soot.jimple.infoflow.test.methodSummary.junit.WrapperQueueTests {
    @Override
    protected AbstractInfoflow createInfoflowInstance() {
        return new BackwardsInfoflow("", false, null);
    }
}
