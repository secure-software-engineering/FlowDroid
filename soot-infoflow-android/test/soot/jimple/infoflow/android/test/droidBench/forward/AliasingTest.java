package soot.jimple.infoflow.android.test.droidBench.forward;

public class AliasingTest extends soot.jimple.infoflow.android.test.droidBench.AliasingTest {
    @Override
    protected TestResultMode getTestResultMode() {
        return TestResultMode.FLOWDROID_FORWARDS;
    }
}
