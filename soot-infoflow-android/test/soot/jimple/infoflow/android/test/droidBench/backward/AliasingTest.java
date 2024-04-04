package soot.jimple.infoflow.android.test.droidBench.backward;

public class AliasingTest extends soot.jimple.infoflow.android.test.droidBench.AliasingTest {
    @Override
    protected TestResultMode getTestResultMode() {
        return TestResultMode.FLOWDROID_BACKWARDS;
    }
}
