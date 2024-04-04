package soot.jimple.infoflow.android.test.droidBench.backward;

public class GeneralJavaTest extends soot.jimple.infoflow.android.test.droidBench.GeneralJavaTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_BACKWARDS;
	}
}
