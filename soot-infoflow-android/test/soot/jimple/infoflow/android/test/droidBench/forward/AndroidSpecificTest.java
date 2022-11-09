package soot.jimple.infoflow.android.test.droidBench.forward;

public class AndroidSpecificTest extends soot.jimple.infoflow.android.test.droidBench.AndroidSpecificTest {
    @Override
    protected TestResultMode getTestResultMode() {
        return TestResultMode.FLOWDROID_FORWARDS;
    }
}
