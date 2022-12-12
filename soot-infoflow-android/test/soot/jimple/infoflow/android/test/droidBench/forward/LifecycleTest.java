package soot.jimple.infoflow.android.test.droidBench.forward;

public class LifecycleTest extends soot.jimple.infoflow.android.test.droidBench.LifecycleTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_FORWARDS;
	}
}
