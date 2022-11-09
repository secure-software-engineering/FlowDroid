package soot.jimple.infoflow.android.test.droidBench.backward;

public class FieldSourceTest extends soot.jimple.infoflow.android.test.droidBench.FieldSourceTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_BACKWARDS;
	}
}
