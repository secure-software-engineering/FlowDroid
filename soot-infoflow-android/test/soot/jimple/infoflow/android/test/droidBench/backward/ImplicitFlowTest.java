package soot.jimple.infoflow.android.test.droidBench.backward;

public class ImplicitFlowTest extends soot.jimple.infoflow.android.test.droidBench.ImplicitFlowTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_BACKWARDS;
	}
}
