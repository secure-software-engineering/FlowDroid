package soot.jimple.infoflow.android.test.droidBench.forward;

import org.junit.Ignore;

@Ignore("Buggy, call graph problem")
public class ReflectionICCTest extends soot.jimple.infoflow.android.test.droidBench.ReflectionICCTest {
	@Override
	protected TestResultMode getTestResultMode() {
		return TestResultMode.FLOWDROID_FORWARDS;
	}
}
