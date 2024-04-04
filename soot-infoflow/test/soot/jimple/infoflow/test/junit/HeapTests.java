/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.test.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration.AliasingAlgorithm;
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.SequentialEntryPointCreator;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.jimple.infoflow.taintWrappers.AbstractTaintWrapper;
import soot.jimple.infoflow.taintWrappers.IReversibleTaintWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

/**
 * tests aliasing of heap references
 */
public abstract class HeapTests extends JUnitTests {

	@Test(timeout = 300000)
	public void testForEarlyTermination() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testForEarlyTermination()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testForLoop() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testForLoop()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testForWrapper() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testForWrapper()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void simpleTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void simpleTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void argumentTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void argumentTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void negativeTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void doubleCallTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void doubleCallTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void heapTest0() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void methodTest0()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void heapTest0b() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void methodTest0b()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void heapTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void methodTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testExample1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ForwardBackwardTest: void testMethod()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testReturn() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void methodReturn()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testTwoLevels() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void twoLevelTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void multiAliasTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void multiAliasNoRecusiveAPTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void overwriteAliasTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void arrayAliasTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void arrayAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void arrayAliasTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void arrayAliasTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void functionAliasTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void functionAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void functionAliasTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void functionAliasTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void multiLevelTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiLevelTaint()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void multiLevelTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiLevelTaint2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void negativeMultiLevelTest() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeMultiLevelTaint()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void negativeMultiLevelTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeMultiLevelTaint2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void threeLevelTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void threeLevelTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void threeLevelShortAPTest() {
		IInfoflow infoflow = initInfoflow();

		infoflow.getConfig().getAccessPathConfiguration().setAccessPathLength(1);
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void threeLevelTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void recursionTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void recursionTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void activationUnitTest1() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void activationUnitTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void activationUnitTest3() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void activationUnitTest4() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void activationUnitTest4b() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest4b()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void activationUnitTest5() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest5()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void returnAliasTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void returnAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void callPerformanceTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void callPerformanceTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void aliasesTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().getAccessPathConfiguration().setAccessPathLength(5);
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testAliases()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	private class MyTaintWrapper extends AbstractTaintWrapper implements IReversibleTaintWrapper {
		@Override
		public boolean isExclusiveInternal(Stmt stmt, AccessPath taintedPath) {
			return stmt.containsInvokeExpr() && (stmt.getInvokeExpr().getMethod().getName().equals("foo2")
					|| stmt.getInvokeExpr().getMethod().getName().equals("bar2"));
		}

		@Override
		public Set<AccessPath> getTaintsForMethodInternal(Stmt stmt, AccessPath taintedPath) {
			if (!stmt.containsInvokeExpr())
				return Collections.singleton(taintedPath);

			Set<AccessPath> res = new HashSet<AccessPath>();
			res.add(taintedPath);

			// We use a path length of 1, i.e. do not work with member
			// fields,
			// hence the commented-out code
			if (stmt.getInvokeExpr().getMethod().getName().equals("foo2")) {
				InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();
				if (taintedPath.getPlainValue() == iinv.getArg(0)) {
					RefType rt = (RefType) iinv.getBase().getType();
					AccessPath ap = manager.getAccessPathFactory().createAccessPath(iinv.getBase(),
							new SootField[] { rt.getSootClass().getFieldByName("b1") }, true);
					res.add(ap);
				}
				if (taintedPath.getPlainValue() == iinv.getArg(1)) {
					RefType rt = (RefType) iinv.getBase().getType();
					AccessPath ap = manager.getAccessPathFactory().createAccessPath(iinv.getBase(),
							new SootField[] { rt.getSootClass().getFieldByName("b2") }, true);
					res.add(ap);
				}
			} else if (stmt.getInvokeExpr().getMethod().getName().equals("bar2")) {
				InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();
				if (taintedPath.getPlainValue() == iinv.getArg(0)) {
					RefType rt = (RefType) iinv.getBase().getType();
					AccessPath ap = manager.getAccessPathFactory().createAccessPath(iinv.getBase(),
							new SootField[] { rt.getSootClass().getFieldByName("b1") }, true);
					res.add(ap);
				} else if (taintedPath.getPlainValue() == iinv.getBase()) {
					DefinitionStmt def = (DefinitionStmt) stmt;
					AccessPath ap = manager.getAccessPathFactory()
							.createAccessPath(def.getLeftOp(), new SootField[] { Scene.v()
									.getSootClass("soot.jimple.infoflow.test.HeapTestCode$A").getFieldByName("b") },
									true);
					res.add(ap);
				}
			}

			return res;
		}

		@Override
		public Set<Abstraction> getInverseTaintsForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
			// Compute the tainted access paths
			Set<AccessPath> aps = getInverseTaintsForMethodInternal(stmt, taintedPath.getAccessPath());
			if (aps == null || aps.isEmpty())
				return null;

			// Convert the access paths into full abstractions
			Set<Abstraction> res = new HashSet<Abstraction>(aps.size());
			for (AccessPath ap : aps)
				if (ap == taintedPath.getAccessPath())
					res.add(taintedPath);
				else
					res.add(taintedPath.deriveNewAbstraction(ap, stmt));
			return res;
		}

		public Set<AccessPath> getInverseTaintsForMethodInternal(Stmt stmt, AccessPath taintedPath) {
			if (!stmt.containsInvokeExpr())
				return Collections.singleton(taintedPath);

			Set<AccessPath> res = new HashSet<AccessPath>();
			res.add(taintedPath);

			if (stmt.getInvokeExpr().getMethod().getName().equals("foo2")) {
				InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();

				if (iinv.getBase() == taintedPath.getPlainValue()) {
					RefType rt = (RefType) iinv.getBase().getType();
					boolean wholeObjectIsTainted = taintedPath.getTaintSubFields()
							&& taintedPath.getFragmentCount() == 0;

					if (wholeObjectIsTainted || taintedPath.firstFieldMatches(rt.getSootClass().getFieldByName("b1"))) {
						AccessPath ap = manager.getAccessPathFactory().createAccessPath(iinv.getArg(0), true);
						res.add(ap);
					}
					if (wholeObjectIsTainted || taintedPath.firstFieldMatches(rt.getSootClass().getFieldByName("b2"))) {
						AccessPath ap = manager.getAccessPathFactory().createAccessPath(iinv.getArg(1), true);
						res.add(ap);
					}
				}
			} else if (stmt.getInvokeExpr().getMethod().getName().equals("bar2")) {
				InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();

				if (iinv.getBase() == taintedPath.getPlainValue()) {
					RefType rt = (RefType) iinv.getBase().getType();
					boolean wholeObjectIsTainted = taintedPath.getTaintSubFields()
							&& taintedPath.getFragmentCount() == 0;

					if (wholeObjectIsTainted || taintedPath.firstFieldMatches(rt.getSootClass().getFieldByName("b1"))) {
						AccessPath ap = manager.getAccessPathFactory().createAccessPath(iinv.getArg(0), true);
						res.add(ap);
					}
				} else if (stmt instanceof AssignStmt) {
					AssignStmt assignStmt = (AssignStmt) stmt;

					if (assignStmt.getLeftOp() == taintedPath.getPlainValue() && taintedPath.firstFieldMatches(
							Scene.v().getSootClass("soot.jimple.infoflow.test.HeapTestCode$A").getFieldByName("b"))) {
						AccessPath ap = manager.getAccessPathFactory().createAccessPath(iinv.getBase(), true);
						res.add(ap);
						res.remove(taintedPath);
					}
				}
			}

			return res;
		}

		@Override
		public boolean supportsCallee(SootMethod method) {
			return false;
		}

		@Override
		public boolean supportsCallee(Stmt callSite) {
			return false;
		}

		@Override
		public Set<Abstraction> getAliasesForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
			return null;
		}
	}

	@Test(timeout = 300000)
	public void wrapperAliasesTest() {
		IInfoflow infoflow = initInfoflow();
		ITaintPropagationWrapper taintWrapper = new MyTaintWrapper();
		infoflow.setTaintWrapper(taintWrapper);

		infoflow.getConfig().getAccessPathConfiguration().setAccessPathLength(3);
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testWrapperAliases()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void negativeAliasesTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().getAccessPathConfiguration().setAccessPathLength(4);
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeTestAliases()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void aliasPerformanceTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().getAccessPathConfiguration().setAccessPathLength(3);
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasPerformanceTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void aliasPerformanceTestFIS() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().getAccessPathConfiguration().setAccessPathLength(3);
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().setAliasingAlgorithm(AliasingAlgorithm.PtsBased);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasPerformanceTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 3); // PTS-based alias analysis is not
									// flow-sensitive
	}

	@Test(timeout = 300000)
	public void backwardsParameterTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void backwardsParameterTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void aliasTaintLeakTaintTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasTaintLeakTaintTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void fieldBaseOverwriteTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void fieldBaseOverwriteTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void doubleAliasTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void doubleAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void doubleAliasTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void doubleAliasTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void tripleAliasTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void tripleAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 3);
	}

	@Test(timeout = 300000)
	public void singleAliasTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void singleAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void negativeSingleAliasTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeSingleAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void intAliasTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void intAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void staticAliasTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void staticAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void staticAliasTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void staticAliasTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void unAliasParameterTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void unAliasParameterTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void overwriteParameterTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteParameterTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void multiAliasBaseTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiAliasBaseTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void innerClassTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(false);
		infoflow.getConfig().getAccessPathConfiguration().setUseThisChainReduction(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void innerClassTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void innerClassTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(false);
		infoflow.getConfig().getAccessPathConfiguration().setUseThisChainReduction(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void innerClassTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void innerClassTest3() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(false);
		infoflow.getConfig().getAccessPathConfiguration().setUseThisChainReduction(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void innerClassTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void innerClassTest4() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void innerClassTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void innerClassTest5() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(false);
		infoflow.getConfig().getAccessPathConfiguration().setUseThisChainReduction(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void innerClassTest5()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void innerClassTest6() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(false);
		infoflow.getConfig().getAccessPathConfiguration().setUseThisChainReduction(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void innerClassTest6()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void datastructureTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void datastructureTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void datastructureTest2() {

		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);
		infoflow.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void datastructureTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);

		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void staticAccessPathTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void staticAccessPathTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void separatedTreeTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void separatedTreeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void overwriteAliasedVariableTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasedVariableTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void overwriteAliasedVariableTest2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasedVariableTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void overwriteAliasedVariableTest3() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasedVariableTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void overwriteAliasedVariableTest4() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasedVariableTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void overwriteAliasedVariableTest5() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasedVariableTest5()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void overwriteAliasedVariableTest6() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasedVariableTest6()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Ignore("fails, looks like a conceptual problem")
	@Test(timeout = 300000)
	public void aliasFlowTest() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasFlowTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void aliasStrongUpdateTest() {
		final String sinkMethod = "<soot.jimple.infoflow.test.HeapTestCode: "
				+ "void leakData(soot.jimple.infoflow.test.HeapTestCode$Data)>";
		final String sourceMethod = "<soot.jimple.infoflow.test.HeapTestCode: "
				+ "soot.jimple.infoflow.test.HeapTestCode$Data getSecretData()>";

		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasStrongUpdateTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, Collections.singleton(sourceMethod),
				Collections.singleton(sinkMethod));

		Assert.assertTrue(infoflow.isResultAvailable());
		InfoflowResults map = infoflow.getResults();
		Assert.assertEquals(1, map.size());
		Assert.assertTrue(map.containsSinkMethod(sinkMethod));
		Assert.assertTrue(map.isPathBetweenMethods(sinkMethod, sourceMethod));
	}

	@Test(timeout = 300000)
	public void aliasStrongUpdateTest2() {
		final String sinkMethod = "<soot.jimple.infoflow.test.HeapTestCode: "
				+ "void leakData(soot.jimple.infoflow.test.HeapTestCode$Data)>";
		final String sourceMethod = "<soot.jimple.infoflow.test.HeapTestCode: "
				+ "soot.jimple.infoflow.test.HeapTestCode$Data getSecretData()>";

		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasStrongUpdateTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, Collections.singleton(sourceMethod),
				Collections.singleton(sinkMethod));

		Assert.assertTrue(infoflow.isResultAvailable());
		InfoflowResults map = infoflow.getResults();
		Assert.assertEquals(1, map.size());
		Assert.assertTrue(map.containsSinkMethod(sinkMethod));
		Assert.assertTrue(map.isPathBetweenMethods(sinkMethod, sourceMethod));
	}

	@Test(timeout = 300000)
	public void aliasStrongUpdateTest3() {
		final String sinkMethod = "<soot.jimple.infoflow.test.HeapTestCode: "
				+ "void leakData(soot.jimple.infoflow.test.HeapTestCode$Data)>";

		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);
		onlyForwards(infoflow, "not supported backward");
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasStrongUpdateTest3()>");
		ISourceSinkManager ssm = null;
		if (infoflow instanceof Infoflow) {
			ssm = new ISourceSinkManager() {

				@Override
				public SinkInfo getSinkInfo(Stmt sCallSite, InfoflowManager manager, AccessPath ap) {
					if (sCallSite.containsInvokeExpr()) {
						SootMethod sm = sCallSite.getInvokeExpr().getMethod();
						if (sm.getSignature().equals(sinkMethod))
							return new SinkInfo(new MethodSourceSinkDefinition(new SootMethodAndClass(sm)));
					}
					return null;
				}

				@Override
				public SourceInfo getSourceInfo(Stmt sCallSite, InfoflowManager manager) {
					if (sCallSite instanceof AssignStmt) {
						AssignStmt assignStmt = (AssignStmt) sCallSite;
						if (assignStmt.getRightOp().toString().contains("taintedBySourceSinkManager"))
							return new SourceInfo(manager.getAccessPathFactory().createAccessPath(assignStmt.getLeftOp(), true));
						else
							return null;
					}
					return null;
				}

				@Override
				public void initialize() {
					//
				}

			};
		}
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);

		Assert.assertTrue(infoflow.isResultAvailable());
		InfoflowResults map = infoflow.getResults();
		Assert.assertEquals(1, map.size());
		Assert.assertTrue(map.containsSinkMethod(sinkMethod));
	}

	@Test(timeout = 300000)
	public void arrayLengthAliasTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void arrayLengthAliasTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void arrayLengthAliasTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void arrayLengthAliasTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void arrayLengthAliasTest3() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void arrayLengthAliasTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void arrayLengthAliasTest4() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void arrayLengthAliasTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void taintPrimitiveFieldTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void taintPrimitiveFieldTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void taintPrimitiveFieldTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void taintPrimitiveFieldTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void multiContextTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiContextTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void recursiveFollowReturnsPastSeedsTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void recursiveFollowReturnsPastSeedsTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void doubleAliasTest1() {
		IInfoflow infoflow = initInfoflow(false);
		infoflow.getConfig().getPathConfiguration().setPathReconstructionMode(PathReconstructionMode.Fast);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void doubleAliasTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(2, infoflow.getResults().numConnections());
	}

	@Test(timeout = 300000)
	public void contextTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void contextTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void contextTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void contextTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void contextTest3() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void contextTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void summaryTest1() {
		// This test is highly dependent upon when a summary is created vs.
		// when it is used, so we run it multiple times.
		for (int i = 0; i < 10; i++) {
			IInfoflow infoflow = initInfoflow();
			List<String> epoints = new ArrayList<String>();
			epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void summaryTest1()>");
			infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
			checkInfoflow(infoflow, 1);
		}
	}

	@Test(timeout = 300000)
	public void delayedReturnTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void delayedReturnTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void aliasWithOverwriteTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasWithOverwriteTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void aliasWithOverwriteTest2() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasWithOverwriteTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void aliasWithOverwriteTest3() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasWithOverwriteTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void aliasWithOverwriteTest4() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasWithOverwriteTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void aliasWithOverwriteTest5() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasWithOverwriteTest5()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void innerFieldReductionTestNegative() {
		IInfoflow infoflow = initInfoflow();
		onlyForwards(infoflow, "We lose the object sensitivity here due to the recursive data structure"
				+ "reduction. See also the comment above the code of innerFieldReductionTestNegative.");
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void innerFieldReductionTestNegative()>");
		infoflow.computeInfoflow(appPath, libPath, new SequentialEntryPointCreator(epoints), sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void innerFieldReductionTestNegative2() {
		IInfoflow infoflow = initInfoflow();
		onlyBackwards(infoflow, "We lose the object sensitivity here due to the recursive data structure"
				+ "reduction. See also the comment above the code of innerFieldReductionTestNegative.");
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void innerFieldReductionTestNegative2()>");
		infoflow.computeInfoflow(appPath, libPath, new SequentialEntryPointCreator(epoints), sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void activationStatementTest1() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationStatementTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}


	@Test(timeout = 300000)
	public void callSiteCreatesAlias() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void callSiteCreatesAlias()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void lhsNotUpwardsInAliasFlow() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void lhsNotUpwardsInAliasFlow()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void identityStmtIsNotAGoodHandoverPoint() {
		IInfoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		infoflow.setTaintPropagationHandler(new TaintPropagationHandler() {
			@Override
			public void notifyFlowIn(Unit stmt, Abstraction taint, InfoflowManager manager, FlowFunctionType type) {
				Assert.assertTrue(taint.isAbstractionActive());
			}

			@Override
			public Set<Abstraction> notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing, InfoflowManager manager, FlowFunctionType type) {
				return outgoing;
			}
		});
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void identityStmtIsNotAGoodHandoverPoint()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}


	@Test(timeout = 300000)
	public void testRecursiveAccessPath() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testRecursiveAccessPath()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testRemoveEntailedAbstractions1() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void removeEntailedAbstractionsTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(2, infoflow.getResults().getResultSet().stream()
				.map(res -> res.getSource().getStmt()).distinct().count());
	}

	@Test(timeout = 300000)
	public void testRemoveEntailedAbstractions2() {
		IInfoflow infoflow = initInfoflow();
		infoflow.getConfig().setInspectSources(false);
		infoflow.getConfig().getAccessPathConfiguration().setAccessPathLength(1);
		infoflow.getConfig().setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void removeEntailedAbstractionsTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		Assert.assertEquals(2, infoflow.getResults().getResultSet().stream()
				.map(res -> res.getSource().getStmt()).distinct().count());
	}
}
