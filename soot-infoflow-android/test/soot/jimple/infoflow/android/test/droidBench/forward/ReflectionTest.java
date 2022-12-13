package soot.jimple.infoflow.android.test.droidBench.forward;

public class ReflectionTest extends soot.jimple.infoflow.android.test.droidBench.ReflectionTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_FORWARDS;
	}
}
