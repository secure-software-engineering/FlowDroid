package soot.jimple.infoflow.android.test.droidBench.backward;

public class ThreadingTest extends soot.jimple.infoflow.android.test.droidBench.ThreadingTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_BACKWARDS;
	}
}
