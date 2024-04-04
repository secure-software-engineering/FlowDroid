package soot.jimple.infoflow.android.test.droidBench.forward;

public class CallbackTest extends soot.jimple.infoflow.android.test.droidBench.CallbackTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_FORWARDS;
	}
}
