package soot.jimple.infoflow.android.test.droidBench.forward;

public class FieldAndObjectSensitivityTest extends soot.jimple.infoflow.android.test.droidBench.FieldAndObjectSensitivityTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_FORWARDS;
	}
}
