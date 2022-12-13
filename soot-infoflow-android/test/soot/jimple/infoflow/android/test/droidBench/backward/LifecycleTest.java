package soot.jimple.infoflow.android.test.droidBench.backward;

public class LifecycleTest extends soot.jimple.infoflow.android.test.droidBench.LifecycleTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_BACKWARDS;
	}
}
