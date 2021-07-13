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
package soot.jimple.infoflow.entryPointCreators;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.IntType;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.jimple.EqExpr;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.toolkits.scalar.NopEliminator;

/**
 * Entry point creator that performs Java method invocations. The invocation
 * order is simulated to be arbitrary. If you only need a sequential list, use
 * the {@link SequentialEntryPointCreator} instead.
 * 
 * @author Steven Arzt
 */
public class DefaultEntryPointCreator extends BaseEntryPointCreator {

	private static final Logger logger = LoggerFactory.getLogger(DefaultEntryPointCreator.class);

	private final Collection<String> methodsToCall;

	/**
	 * Creates a new instanceof the {@link DefaultEntryPointCreator} class
	 * 
	 * @param methodsToCall A collection containing the methods to be called in the
	 *                      dummy main method. Note that the order of the method
	 *                      calls is simulated to be arbitrary. Entries must be
	 *                      valid Soot method signatures.
	 */
	public DefaultEntryPointCreator(Collection<String> methodsToCall) {
		this.methodsToCall = methodsToCall;
		setAllowNonPublicConstructors(true);
	}

	@Override
	protected SootMethod createDummyMainInternal() {
		Map<String, Set<String>> classMap = SootMethodRepresentationParser.v().parseClassNames(methodsToCall, false);

		// create new class:
		HashMap<String, Local> localVarsForClasses = new HashMap<String, Local>();

		// create constructors:
		for (String className : classMap.keySet()) {
			SootClass createdClass = Scene.v().forceResolve(className, SootClass.BODIES);
			createdClass.setApplicationClass();

			Local localVal = generateClassConstructor(createdClass);
			if (localVal == null) {
				logger.warn("Cannot generate constructor for class: {}", createdClass);
				continue;
			}
			localVarsForClasses.put(className, localVal);
		}

		// add entrypoint calls
		int conditionCounter = 0;
		final Jimple jimple = Jimple.v();
		NopStmt startStmt = jimple.newNopStmt();
		NopStmt endStmt = jimple.newNopStmt();
		Value intCounter = generator.generateLocal(IntType.v());
		body.getUnits().add(startStmt);
		for (Entry<String, Set<String>> entry : classMap.entrySet()) {
			Local classLocal = localVarsForClasses.get(entry.getKey());
			for (String method : entry.getValue()) {
				SootMethodAndClass methodAndClass = SootMethodRepresentationParser.v().parseSootMethodString(method);
				SootMethod currentMethod = findMethod(Scene.v().getSootClass(methodAndClass.getClassName()),
						methodAndClass.getSubSignature());
				if (currentMethod == null) {
					logger.warn("Entry point not found: {}", method);
					continue;
				}

				EqExpr cond = jimple.newEqExpr(intCounter, IntConstant.v(conditionCounter));
				conditionCounter++;
				NopStmt thenStmt = jimple.newNopStmt();
				IfStmt ifStmt = jimple.newIfStmt(cond, thenStmt);
				body.getUnits().add(ifStmt);
				buildMethodCall(currentMethod, classLocal);
				body.getUnits().add(thenStmt);
			}
		}
		body.getUnits().add(endStmt);
		GotoStmt gotoStart = jimple.newGotoStmt(startStmt);
		body.getUnits().add(gotoStart);

		body.getUnits().add(Jimple.v().newReturnVoidStmt());
		NopEliminator.v().transform(body);
		eliminateSelfLoops();
		return mainMethod;
	}

	@Override
	public Collection<String> getRequiredClasses() {
		return SootMethodRepresentationParser.v().parseClassNames(methodsToCall, false).keySet();
	}

	@Override
	public Collection<SootMethod> getAdditionalMethods() {
		return null;
	}

	@Override
	public Collection<SootField> getAdditionalFields() {
		return null;
	}

}
