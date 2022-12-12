package soot.jimple.infoflow.android.test.droidBench.backward;

public class ArrayAndListTest extends soot.jimple.infoflow.android.test.droidBench.ArrayAndListTest {
    @Override
    protected TestResultMode getTestResultMode() {
        return TestResultMode.FLOWDROID_BACKWARDS;
    }
}
