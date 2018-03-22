package soot.jimple.infoflow.test.junit;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;

/**
 * Test cases for checking FlowDroid's reflection support
 * 
 * @author Steven Arzt
 *
 */
public class ReflectionTests extends JUnitTests {
	
	@Test
	public void testSimpleMethodCall1() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testSimpleMethodCall1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testSimpleMethodCall2() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testSimpleMethodCall2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testSimpleMethodCall3() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testSimpleMethodCall3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testSimpleMethodCall4() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testSimpleMethodCall3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testConditionalMethodCall1() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testConditionalMethodCall1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testConditionalMethodCall2() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testConditionalMethodCall2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test
	public void testConditionalMethodCall3() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testConditionalMethodCall3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test
	public void testConditionalMethodCall4() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testConditionalMethodCall4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test
	public void testTransfer1() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testTransfer1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testParameterAlias1() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testParameterAlias1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testAliasInCallee1() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testAliasInCallee1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testReflectiveInstance1() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testReflectiveInstance1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testReflectiveInstance2() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testReflectiveInstance2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void testReflectiveInstance3() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void testReflectiveInstance3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void thisClassTest1() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void thisClassTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void thisClassTest2() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void thisClassTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void propoagationClassTest1() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void propoagationClassTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void propoagationClassTest2() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void propoagationClassTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void noArgumentTest1() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void noArgumentTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void noArgumentTest2() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void noArgumentTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void allObjectTest1() {
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setEnableReflection(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.ReflectionTestCode: void allObjectTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

}
