package soot.jimple.infoflow.test.methodSummary.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

public class WrapperListTests extends JUnitTests {
	private static File files = new File("testSummaries");
	protected final ITaintPropagationWrapper wrapper;

	public WrapperListTests() throws FileNotFoundException, XMLStreamException {
		wrapper = (ITaintPropagationWrapper) TaintWrapperFactory.createTaintWrapper(files);
	}

	@Test
	public void concreteArrayListPos0Test() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadPos0Test()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void concreteArrayListPos1Test() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadPos1Test()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void concreteArrayListNegativeTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadNegativeTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test
	public void listTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void writeReadTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void listIteratorTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void iteratorTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void listsubListTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void subListTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void concreteLinkedListNegativeTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add(
				"<soot.jimple.infoflow.test.methodSummary.ListTestCode: void linkedListConcreteWriteReadNegativeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		infoflow.setTaintWrapper(wrapper);
		negativeCheckInfoflow(infoflow);
	}

	@Test
	public void concreteLinkedListTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void linkedListConcreteWriteReadTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void writeReadLinkedListTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void linkedListWriteReadTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void concreteLinkedListIteratorTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void linkedListIteratorTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void subLinkedListTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void linkedListSubListTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void stackGetTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadStackGetTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void stackPeekTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadStackPeekTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void stackPopTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadStackPopTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void stackNegativeTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add(
				"<soot.jimple.infoflow.test.methodSummary.ListTestCode: void concreteWriteReadStackNegativeTest()>");
		infoflow.setTaintWrapper(wrapper);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Override
	protected Infoflow initInfoflow() {
		return initInfoflow(false);
	}

	@Override
	protected Infoflow initInfoflow(boolean useTaintWrapper) {
		Infoflow result = new Infoflow();
		WrapperListTestConfig testConfig = new WrapperListTestConfig();
		result.setSootConfig(testConfig);
		result.setTaintWrapper(wrapper);
		return result;
	}

}