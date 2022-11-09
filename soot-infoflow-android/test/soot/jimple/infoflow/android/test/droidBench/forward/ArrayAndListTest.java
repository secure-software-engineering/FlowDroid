package soot.jimple.infoflow.android.test.droidBench.forward;

public class ArrayAndListTest extends soot.jimple.infoflow.android.test.droidBench.ArrayAndListTest {
    @Override
    protected TestResultMode getTestResultMode() {
        return TestResultMode.FLOWDROID_FORWARDS;
    }
}
