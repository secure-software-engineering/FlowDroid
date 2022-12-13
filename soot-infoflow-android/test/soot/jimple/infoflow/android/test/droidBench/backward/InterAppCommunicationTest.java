package soot.jimple.infoflow.android.test.droidBench.backward;

public class InterAppCommunicationTest extends soot.jimple.infoflow.android.test.droidBench.InterAppCommunicationTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_BACKWARDS;
	}
}
