package soot.jimple.infoflow.test.methodSummary.junit.backward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;
import soot.jimple.infoflow.Infoflow;

public class WrapperMapTests extends soot.jimple.infoflow.test.methodSummary.junit.WrapperMapTests {
    @Override
    protected AbstractInfoflow createInfoflowInstance() {
        return new BackwardsInfoflow("", false, null);
    }
}
