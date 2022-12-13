package soot.jimple.infoflow.android.test.droidBench.forward;

public class GeneralJavaTest extends soot.jimple.infoflow.android.test.droidBench.GeneralJavaTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_FORWARDS;
	}
}
