package soot.jimple.infoflow.android.test.droidBench.backward;

public class EmulatorDetectionTest extends soot.jimple.infoflow.android.test.droidBench.EmulatorDetectionTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_BACKWARDS;
	}
}
