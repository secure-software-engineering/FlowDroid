package soot.jimple.infoflow.test.methodSummary.junit.forward;

import org.junit.Assume;
import org.junit.Test;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;

public class WrapperSetTests extends soot.jimple.infoflow.test.methodSummary.junit.WrapperSetTests {
    @Override
    protected AbstractInfoflow createInfoflowInstance() {
        return new Infoflow("", false, null);
    }
}
