package soot.jimple.infoflow.android.test.droidBench.forward;

public class EmulatorDetectionTest extends soot.jimple.infoflow.android.test.droidBench.EmulatorDetectionTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_FORWARDS;
	}
}
