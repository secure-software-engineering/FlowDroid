package soot.jimple.infoflow.test.methodSummary.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

public abstract class WrapperListTests extends JUnitTests {

	private static File files = new File("testSummaries");
	protected final ITaintPropagationWrapper wrapper;

	public WrapperListTests() {
		wrapper = (ITaintPropagationWrapper) TaintWrapperFactory.createTaintWrapper(files);
	}

	@Test
	public void concreteArrayListPos0Test() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadPos0Test()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void concreteArrayListPos1Test() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadPos1Test()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void concreteArrayListNegativeTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadNegativeTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test
	public void listTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void writeReadTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void listIteratorTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void iteratorTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void listsubListTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void subListTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void concreteLinkedListNegativeTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add(
				"<soot.jimple.infoflow.test.methodSummary.ListTestCode: void linkedListConcreteWriteReadNegativeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		infoflow.setTaintWrapper(wrapper);
		negativeCheckInfoflow(infoflow);
	}

	@Test
	public void concreteLinkedListTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void linkedListConcreteWriteReadTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void writeReadLinkedListTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void linkedListWriteReadTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void concreteLinkedListIteratorTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void linkedListIteratorTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void subLinkedListTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void linkedListSubListTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void stackGetTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadStackGetTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void stackPeekTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadStackPeekTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void stackPopTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadStackPopTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void stackNegativeTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add(
				"<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadStackNegativeTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Override
	protected IInfoflow initInfoflow() {
		return initInfoflow(false);
	}

	@Override
	protected IInfoflow initInfoflow(boolean useTaintWrapper) {
//		IInfoflow result = new BackwardsInfoflow();
		IInfoflow result = new Infoflow();
		WrapperListTestConfig testConfig = new WrapperListTestConfig();
		result.setSootConfig(testConfig);
		result.setTaintWrapper(wrapper);
		return result;
	}

}