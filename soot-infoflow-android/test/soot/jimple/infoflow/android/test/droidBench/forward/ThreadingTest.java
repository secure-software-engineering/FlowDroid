package soot.jimple.infoflow.android.test.droidBench.forward;

public class ThreadingTest extends soot.jimple.infoflow.android.test.droidBench.ThreadingTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_FORWARDS;
	}
}
