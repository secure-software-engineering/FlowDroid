package soot.jimple.infoflow.collections.test.junit;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;

public class IteratorTests extends FlowDroidTests {
	@Override
	protected void setConfiguration(InfoflowConfiguration config) {

	}

	private static final String testCodeClass = "soot.jimple.infoflow.collections.test.IteratorTestCode";

	@Test(timeout = 30000)
	public void testListSublist1() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testListSublist2() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testListIterator1() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testListIterator2() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testListListIterator1() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testListListIterator2() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testIterator1() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testIterator2() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testIterator3() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testIterator4() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testIterator5() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testIterator6() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testIterator7() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testIterator8() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testIterator9() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testIterator10() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testIterator11() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}

	@Test(timeout = 30000)
	public void testSpliteratorFlow() {
		IInfoflow infoflow = initInfoflow();
		String epoint = "<" + testCodeClass + ": void " + getCurrentMethod() + "()>";
		infoflow.computeInfoflow(appPath, libPath, Collections.singleton(epoint), sources, sinks);
		Assert.assertEquals(getExpectedResultsForMethod(epoint), infoflow.getResults().size());
	}
}
