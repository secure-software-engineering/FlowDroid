package soot.jimple.infoflow.android.test.droidBench.backward;

public class ReflectionTest extends soot.jimple.infoflow.android.test.droidBench.ReflectionTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_BACKWARDS;
	}
}
