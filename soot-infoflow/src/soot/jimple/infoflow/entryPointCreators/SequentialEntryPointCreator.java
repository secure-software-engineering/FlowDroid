package soot.jimple.infoflow.entryPointCreators;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.Jimple;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;

/**
 * Simple entry point creator that builds a sequential list of method
 * invocations. Each method is invoked only once.
 * 
 * @author Steven Arzt
 */
public class SequentialEntryPointCreator extends BaseEntryPointCreator {

	private final Collection<String> methodsToCall;

	/**
	 * Creates a new instance of the {@link SequentialEntryPointCreator} class
	 * 
	 * @param methodsToCall A collection containing the methods to be called in the
	 *                      dummy main method. Entries must be valid Soot method
	 *                      signatures.
	 */
	public SequentialEntryPointCreator(Collection<String> methodsToCall) {
		this.methodsToCall = methodsToCall;
		setAllowNonPublicConstructors(true);
	}

	@Override
	public Collection<String> getRequiredClasses() {
		return SootMethodRepresentationParser.v().parseClassNames(methodsToCall, false).keySet();
	}

	@Override
	protected SootMethod createDummyMainInternal() {
		Map<String, Set<String>> classMap = SootMethodRepresentationParser.v().parseClassNames(methodsToCall, false);

		// Create the classes
		for (String className : classMap.keySet()) {
			SootClass createdClass = Scene.v().forceResolve(className, SootClass.BODIES);
			createdClass.setApplicationClass();
			Local localVal = generateClassConstructor(createdClass);
			if (localVal == null) {
				logger.warn("Cannot generate constructor for class: {}", createdClass);
				continue;
			}

			// Create the method calls
			for (String method : classMap.get(className)) {
				SootMethodAndClass methodAndClass = SootMethodRepresentationParser.v().parseSootMethodString(method);
				SootMethod methodToInvoke = findMethod(Scene.v().getSootClass(methodAndClass.getClassName()),
						methodAndClass.getSubSignature());

				if (methodToInvoke == null)
					logger.warn("Method %s not found, skipping", methodAndClass);
				else if (methodToInvoke.isConcrete() && !methodToInvoke.isConstructor()) {
					// Load the method
					methodToInvoke.retrieveActiveBody();
					buildMethodCall(methodToInvoke, localVal);
				}
			}
		}

		// Jimple needs an explicit return statement
		body.getUnits().add(Jimple.v().newReturnVoidStmt());

		return mainMethod;
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
