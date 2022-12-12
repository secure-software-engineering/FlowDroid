package soot.jimple.infoflow.android.test.droidBench.forward;

public class ImplicitFlowTest extends soot.jimple.infoflow.android.test.droidBench.ImplicitFlowTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_FORWARDS;
	}
}
