package soot.jimple.infoflow.test.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import soot.Body;
import soot.Local;
import soot.LocalGenerator;
import soot.Modifier;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration.CodeEliminationMode;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;

/**
 * Bytecode-specific tests. We do not analyze code that we compile from Java,
 * but specifically craft Jimple code with the properties we need.
 * 
 * @author Steven Arzt
 *
 */
public abstract class BytecodeTests extends JUnitTests {

	public static class Container {
		public String data;

		public Container() {
			data = "";
		}
	}

	/**
	 * Source method for the test cases
	 * 
	 * @return A dummy container
	 */
	public Container source() {
		return new Container();
	}

	/**
	 * Sink method for the test cases
	 * 
	 * @param c A dummy container
	 */
	public void sink(Container c) {

	}

	/**
	 * Dummy method such that the analysis has an entry point
	 */
	public static void dummy() {
		// Make sure we have a reference to the container class
		Container c = new Container();
		System.out.println(c);
	}

	@Test
	public void flowSensitivityTest1() {
		List<String> sources = Collections.singletonList("<soot.jimple.infoflow.test.junit.BytecodeTests: "
				+ "soot.jimple.infoflow.test.junit.BytecodeTests$Container source()>");
		List<String> sinks = Collections.singletonList("<soot.jimple.infoflow.test.junit.BytecodeTests: "
				+ "void sink(soot.jimple.infoflow.test.junit.BytecodeTests$Container)>");

		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.junit.BytecodeTests: void dummy()>");
		infoflow.addPreprocessor(new PreAnalysisHandler() {

			@Override
			public void onBeforeCallgraphConstruction() {
				// Create the bytecode we need for our test case
				createBytecodeFlowSensitivityTest1();
			}

			@Override
			public void onAfterCallgraphConstruction() {
				// nothing to do here
			}

		});
		infoflow.getConfig().setCodeEliminationMode(CodeEliminationMode.NoCodeElimination);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	private void createBytecodeFlowSensitivityTest1() {
		SootClass testClass = new SootClass("TestClass");
		final Scene sc = Scene.v();
		sc.addClass(testClass);
		testClass.setApplicationClass();

		// Get a reference to the source method
		SootMethod smSource = Scene.v().getMethod("<soot.jimple.infoflow.test.junit.BytecodeTests: "
				+ "soot.jimple.infoflow.test.junit.BytecodeTests$Container source()>");

		// Get a reference to the sink method
		SootMethod smSink = Scene.v().getMethod("<soot.jimple.infoflow.test.junit.BytecodeTests: "
				+ "void sink(soot.jimple.infoflow.test.junit.BytecodeTests$Container)>");

		// Get the constructor of the container class
		SootMethod consContainer = Scene.v()
				.getMethod("<soot.jimple.infoflow.test.junit.BytecodeTests$Container: " + "void <init>()>");

		// Create the first method that calls the second one and passes on the
		// parameter
		RefType containerType = Scene.v().getRefType("soot.jimple.infoflow.test.junit.BytecodeTests$Container");
		SootMethod smOnCreate = Scene.v().makeSootMethod("onCreate", Collections.<Type>singletonList(containerType),
				VoidType.v());
		testClass.addMethod(smOnCreate);
		smOnCreate.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

		Body bOnCreate = Jimple.v().newBody(smOnCreate);
		smOnCreate.setActiveBody(bOnCreate);
		LocalGenerator localGenOnCreate = Scene.v().createLocalGenerator(bOnCreate);

		Local thisOnCreate = localGenOnCreate.generateLocal(testClass.getType());
		Local param0onCreate = localGenOnCreate.generateLocal(containerType);

		bOnCreate.getUnits().add(Jimple.v().newIdentityStmt(thisOnCreate, Jimple.v().newThisRef(testClass.getType())));
		bOnCreate.getUnits()
				.add(Jimple.v().newIdentityStmt(param0onCreate, Jimple.v().newParameterRef(containerType, 0)));

		// Create the second method that receives the parameter
		SootMethod smTarget = Scene.v().makeSootMethod("callTarget", Collections.<Type>singletonList(containerType),
				VoidType.v());
		testClass.addMethod(smTarget);
		smTarget.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

		Body bTarget = Jimple.v().newBody(smTarget);
		smTarget.setActiveBody(bTarget);
		LocalGenerator localGenTarget = Scene.v().createLocalGenerator(bTarget);

		Local thisTarget = localGenTarget.generateLocal(testClass.getType());
		Local param0Target = localGenTarget.generateLocal(containerType);

		bTarget.getUnits().add(Jimple.v().newIdentityStmt(thisTarget, Jimple.v().newThisRef(testClass.getType())));
		bTarget.getUnits().add(Jimple.v().newIdentityStmt(param0Target, Jimple.v().newParameterRef(containerType, 0)));

		// Overwrite the parameter with a source value
		bTarget.getUnits().add(Jimple.v().newAssignStmt(param0Target,
				Jimple.v().newVirtualInvokeExpr(thisTarget, smSource.makeRef())));

		bTarget.getUnits().add(Jimple.v().newReturnVoidStmt());

		// Call the second method from the first one
		bOnCreate.getUnits()
				.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(smTarget.makeRef(), param0onCreate)));

		// Call the sink with the parameter object in the first method
		bOnCreate.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(thisOnCreate,
				smSink.makeRef(), Collections.singletonList(param0onCreate))));

		bOnCreate.getUnits().add(Jimple.v().newReturnVoidStmt());

		// Our entry point must call the first method
		SootMethod smEntryPoint = Scene.v().getMethod("<soot.jimple.infoflow.test.junit.BytecodeTests: void dummy()>");
		Body bEntryPoint = smEntryPoint.retrieveActiveBody();

		LocalGenerator localGenEntryPoint = Scene.v().createLocalGenerator(bEntryPoint);
		Local containerEntryPoint = localGenEntryPoint.generateLocal(containerType);

		bEntryPoint.getUnits().insertBefore(
				Jimple.v().newAssignStmt(containerEntryPoint, Jimple.v().newNewExpr(containerType)),
				smEntryPoint.getActiveBody().getUnits().getLast());

		bEntryPoint.getUnits().insertBefore(
				Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(containerEntryPoint, consContainer.makeRef())),
				smEntryPoint.getActiveBody().getUnits().getLast());

		Stmt stmtCall = Jimple.v().newInvokeStmt(
				Jimple.v().newStaticInvokeExpr(smOnCreate.makeRef(), Collections.singletonList(containerEntryPoint)));
		bEntryPoint.getUnits().insertBefore(stmtCall, smEntryPoint.getActiveBody().getUnits().getLast());
	}

}
