package soot.jimple.infoflow.test.junit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.options.Options;

/**
 * These tests check whether exceptional control- and data flows are handled
 * correctly.
 * 
 * @author Steven Arzt
 */
public class ExceptionTests extends JUnitTests {

	@Test
	public void exceptionControlFlowTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionControlFlowTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void exceptionControlWrappedFlowTest1() throws IOException {
		IInfoflow infoflow = initInfoflow();
		infoflow.setTaintWrapper(new EasyTaintWrapper(new File("EasyTaintWrapperSource.txt")));
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionControlFlowTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void exceptionControlFlowTestNoJDK1() throws IOException {
		IInfoflow infoflow = initInfoflow();
		infoflow.setTaintWrapper(new EasyTaintWrapper(new File("EasyTaintWrapperSource.txt")));
		infoflow.setSootConfig(new IInfoflowConfig() {

			@Override
			public void setSootOptions(Options options, InfoflowConfiguration config) {
				List<String> excludeList = new ArrayList<String>();
				excludeList.add("java.");
				excludeList.add("javax.");
				options.set_exclude(excludeList);
				options.set_prepend_classpath(false);
				Options.v().set_ignore_classpath_errors(true);
			}

		});

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionControlFlowTest1()>");
		infoflow.computeInfoflow(appPath, null, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void exceptionControlFlowTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionControlFlowTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void exceptionControlFlowTest3() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionControlFlowTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void exceptionDataFlowTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionDataFlowTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void exceptionDataFlowTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionDataFlowTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void disabledExceptionTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void disabledExceptionTest()>");
		infoflow.getConfig().setEnableExceptionTracking(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test
	public void callMethodParamReturnTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void callMethodParamReturnTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	@Ignore("FlowDroid cannot know that the exception will always happen")
	public void callMethodParamReturnTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void callMethodParamReturnTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test
	public void callMethodParamReturnTest2b() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void callMethodParamReturnTest2b()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void callMethodParamReturnTest3() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void callMethodParamReturnTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test
	public void npeTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void npeTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void npeTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void npeTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

}
