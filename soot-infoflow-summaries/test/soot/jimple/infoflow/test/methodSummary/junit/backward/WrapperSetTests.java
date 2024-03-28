package soot.jimple.infoflow.test.methodSummary.junit.backward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;
import soot.jimple.infoflow.Infoflow;

public class WrapperSetTests extends soot.jimple.infoflow.test.methodSummary.junit.WrapperSetTests {
    @Override
    protected AbstractInfoflow createInfoflowInstance() {
        return new BackwardsInfoflow("", false, null);
    }
}
