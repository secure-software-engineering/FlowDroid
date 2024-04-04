package soot.jimple.infoflow.android.test.droidBench.backward;

public class AndroidSpecificTest extends soot.jimple.infoflow.android.test.droidBench.AndroidSpecificTest {
    @Override
    protected TestResultMode getTestResultMode() {
        return TestResultMode.FLOWDROID_BACKWARDS;
    }
}
