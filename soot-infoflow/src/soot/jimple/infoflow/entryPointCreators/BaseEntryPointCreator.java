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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.ArrayType;
import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LocalGenerator;
import soot.LongType;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.DoubleConstant;
import soot.jimple.EqExpr;
import soot.jimple.FloatConstant;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.util.SystemClassHandler;

/**
 * Common base class for all entry point creators. Implementors must override
 * the createDummyMainInternal method to provide their entry point
 * implementation.
 */
public abstract class BaseEntryPointCreator implements IEntryPointCreator {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected Map<SootClass, Local> localVarsForClasses = new HashMap<>();
	private final Set<SootClass> failedClasses = new HashSet<>();

	private boolean substituteCallParams = false;
	private List<String> substituteClasses;
	private boolean allowSelfReferences = false;
	private boolean ignoreSystemClassParams = true;
	private boolean allowNonPublicConstructors = false;

	private final Set<SootMethod> failedMethods = new HashSet<>();

	/**
	 * Default name of the class containing the dummy main method
	 */
	protected String dummyClassName = "dummyMainClass";
	/**
	 * Default name of the dummy main method
	 */
	protected String dummyMethodName = "dummyMainMethod";

	protected boolean shallowMode = false;
	protected boolean overwriteDummyMainMethod = false;
	protected boolean warnOnConstructorLoop = false;

	protected Value intCounter;
	protected int conditionCounter;

	protected SootMethod mainMethod = null;
	protected Body body = null;
	protected LocalGenerator generator = null;

	/**
	 * Returns a copy of all classes that could not be instantiated properly
	 * 
	 * @return The classes where the constructor could not be generated
	 */
	public Set<SootClass> getFailedClasses() {
		return new HashSet<SootClass>(failedClasses);
	}

	/**
	 * Returns all methods from from methodsToCall, where no call was possible
	 * 
	 * @return A Set of methods that were not called in the main method
	 */
	public Set<SootMethod> getFailedMethods() {
		return new HashSet<SootMethod>(failedMethods);
	}

	public void setSubstituteCallParams(boolean b) {
		substituteCallParams = b;
	}

	@Override
	public void setSubstituteClasses(List<String> l) {
		substituteClasses = l;
	}

	@Override
	public SootMethod createDummyMain() {
		// Load the substitution classes
		if (substituteCallParams)
			for (String className : substituteClasses)
				Scene.v().forceResolve(className, SootClass.BODIES).setApplicationClass();

		// Create the empty main method
		createAdditionalFields();
		createAdditionalMethods();
		createEmptyMainMethod();
		body = mainMethod.getActiveBody();

		// We provide some helper objects
		final Body body = mainMethod.getActiveBody();
		generator = Scene.v().createLocalGenerator(body);

		// Make sure that we have an opaque predicate
		conditionCounter = 0;
		intCounter = generator.generateLocal(IntType.v());
		body.getUnits().add(Jimple.v().newAssignStmt(intCounter, IntConstant.v(conditionCounter)));

		SootMethod m = createDummyMainInternal();
		m.addTag(SimulatedCodeElementTag.TAG);
		return m;
	}

	/**
	 * Implementors need to overwrite this method for providing the actual dummy
	 * main method
	 * 
	 * @return The generated dummy main method
	 */
	protected abstract SootMethod createDummyMainInternal();

	/**
	 * Gets the class that contains the dummy main method. If such a class does not
	 * exist yet, it is created
	 * 
	 * @return The class tha contains the dummy main method
	 */
	protected SootClass getOrCreateDummyMainClass() {
		SootClass mainClass = Scene.v().getSootClassUnsafe(dummyClassName);
		if (mainClass == null) {
			mainClass = Scene.v().makeSootClass(dummyClassName);
			mainClass.setResolvingLevel(SootClass.BODIES);
			Scene.v().addClass(mainClass);
		}
		return mainClass;
	}

	/**
	 * Creates a new, empty main method containing the given body
	 * 
	 * @return The newly generated main method
	 */
	protected void createEmptyMainMethod() {
		// If we already have a main class, we need to make sure to use a fresh
		// method name
		int methodIndex = 0;
		String methodName = dummyMethodName;
		SootClass mainClass = getOrCreateDummyMainClass();
		if (!overwriteDummyMainMethod)
			while (mainClass.declaresMethodByName(methodName))
				methodName = dummyMethodName + "_" + methodIndex++;

		Type stringArrayType = ArrayType.v(RefType.v("java.lang.String"), 1);

		Body body;
		mainMethod = mainClass.getMethodByNameUnsafe(methodName);

		// Remove the existing main method if necessary. Do not clear the
		// existing one, this would take much too long.
		if (mainMethod != null) {
			mainClass.removeMethod(mainMethod);
			mainMethod = null;
		}

		// Create the method
		mainMethod = Scene.v().makeSootMethod(methodName, Collections.singletonList(stringArrayType), VoidType.v());

		// Create the body
		body = Jimple.v().newBody();
		body.setMethod(mainMethod);
		mainMethod.setActiveBody(body);

		// Add the method to the class
		mainClass.addMethod(mainMethod);

		// First add class to scene, then make it an application class
		// as addClass contains a call to "setLibraryClass"
		mainClass.setApplicationClass();
		mainMethod.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

		// Add a parameter reference to the body
		LocalGenerator lg = Scene.v().createLocalGenerator(body);
		Local paramLocal = lg.generateLocal(stringArrayType);
		body.getUnits()
				.addFirst(Jimple.v().newIdentityStmt(paramLocal, Jimple.v().newParameterRef(stringArrayType, 0)));
	}

	/**
	 * Creates additional fields in the entry point class that are required by the
	 * dummy main method
	 */
	protected void createAdditionalFields() {
		// empty in default implementation
	}

	/**
	 * Creates additional methods in the entry point class that are required by the
	 * dummy main method
	 */
	protected void createAdditionalMethods() {
		// empty in default implementation
	}

	/**
	 * Gets a field name that is not already in use by some field
	 * 
	 * @param baseName The base name, i.e., prefix of the new field
	 * @return A field name that is still free
	 */
	protected String getNonCollidingFieldName(String baseName) {
		String fieldName = baseName;
		int fieldIdx = 0;
		final SootClass mainClass = getOrCreateDummyMainClass();
		while (mainClass.declaresFieldByName(fieldName))
			fieldName = baseName + "_" + fieldIdx++;
		return fieldName;
	}

	/**
	 * Builds a new invocation statement that invokes the given method
	 * 
	 * @param methodToCall The method to call
	 * @param classLocal   The local containing an instance of the class on which to
	 *                     invoke the method
	 * @return The newly created invocation statement
	 */
	protected Stmt buildMethodCall(SootMethod methodToCall, Local classLocal) {
		return buildMethodCall(methodToCall, classLocal, Collections.<SootClass>emptySet());
	}

	/**
	 * Builds a new invocation statement that invokes the given method
	 * 
	 * @param methodToCall  The method to call
	 * @param classLocal    The local containing an instance of the class on which
	 *                      to invoke the method
	 * @param parentClasses The classes for which we already have instances that
	 *                      shall be reused
	 * @return The newly created invocation statement
	 */
	protected Stmt buildMethodCall(SootMethod methodToCall, Local classLocal, Set<SootClass> parentClasses) {
		// If we don't have a method, we cannot call it (sad but true)
		if (methodToCall == null)
			return null;

		if (classLocal == null && !methodToCall.isStatic()) {
			logger.warn("Cannot call method {}, because there is no local for base object: {}", methodToCall,
					methodToCall.getDeclaringClass());
			failedMethods.add(methodToCall);
			return null;
		}

		final InvokeExpr invokeExpr;
		List<Value> args = new LinkedList<Value>();
		if (methodToCall.getParameterCount() > 0) {
			for (Type tp : methodToCall.getParameterTypes()) {
				Set<SootClass> constructionStack = new HashSet<SootClass>();
				if (!allowSelfReferences)
					constructionStack.add(methodToCall.getDeclaringClass());
				args.add(getValueForType(tp, constructionStack, parentClasses));
			}

			if (methodToCall.isStatic())
				invokeExpr = Jimple.v().newStaticInvokeExpr(methodToCall.makeRef(), args);
			else {
				assert classLocal != null : "Class local method was null for non-static method call";
				if (methodToCall.isConstructor())
					invokeExpr = Jimple.v().newSpecialInvokeExpr(classLocal, methodToCall.makeRef(), args);
				else if (methodToCall.getDeclaringClass().isInterface())
					invokeExpr = Jimple.v().newInterfaceInvokeExpr(classLocal, methodToCall.makeRef(), args);
				else
					invokeExpr = Jimple.v().newVirtualInvokeExpr(classLocal, methodToCall.makeRef(), args);
			}
		} else {
			if (methodToCall.isStatic()) {
				invokeExpr = Jimple.v().newStaticInvokeExpr(methodToCall.makeRef());
			} else {
				assert classLocal != null : "Class local method was null for non-static method call";
				if (methodToCall.isConstructor())
					invokeExpr = Jimple.v().newSpecialInvokeExpr(classLocal, methodToCall.makeRef());
				else if (methodToCall.getDeclaringClass().isInterface())
					invokeExpr = Jimple.v().newInterfaceInvokeExpr(classLocal, methodToCall.makeRef(), args);
				else
					invokeExpr = Jimple.v().newVirtualInvokeExpr(classLocal, methodToCall.makeRef());
			}
		}

		Stmt stmt;
		if (!(methodToCall.getReturnType() instanceof VoidType)) {
			Local returnLocal = generator.generateLocal(methodToCall.getReturnType());
			stmt = Jimple.v().newAssignStmt(returnLocal, invokeExpr);

		} else {
			stmt = Jimple.v().newInvokeStmt(invokeExpr);
		}
		body.getUnits().add(stmt);

		// Clean up. If we re-use parent objects, do not destroy those. We can
		// only clean up what we have created.
		for (Value val : args)
			if (val instanceof Local && val.getType() instanceof RefType) {
				if (!parentClasses.contains(((RefType) val.getType()).getSootClass())) {
					body.getUnits().add(Jimple.v().newAssignStmt(val, NullConstant.v()));
					localVarsForClasses.remove(((RefType) val.getType()).getSootClass());
				}
			}

		return stmt;
	}

	/**
	 * Creates a value of the given type to be used as a substitution in method
	 * invocations or fields
	 * 
	 * @param tp                The type for which to get a value
	 * @param constructionStack The set of classes we're currently constructing.
	 *                          Attempts to create a parameter of one of these
	 *                          classes will trigger the constructor loop check and
	 *                          the respective parameter will be substituted by
	 *                          null.
	 * @param parentClasses     If the given type is compatible with one of the
	 *                          types in this list, the already-created object is
	 *                          used instead of creating a new one.
	 * @return The generated value, or null if no value could be generated
	 */
	protected Value getValueForType(Type tp, Set<SootClass> constructionStack, Set<SootClass> parentClasses) {
		return getValueForType(tp, constructionStack, parentClasses, null);
	}

	/**
	 * Creates a value of the given type to be used as a substitution in method
	 * invocations or fields
	 * 
	 * @param tp                The type for which to get a value
	 * @param constructionStack The set of classes we're currently constructing.
	 *                          Attempts to create a parameter of one of these
	 *                          classes will trigger the constructor loop check and
	 *                          the respective parameter will be substituted by
	 *                          null.
	 * @param parentClasses     If the given type is compatible with one of the
	 *                          types in this list, the already-created object is
	 *                          used instead of creating a new one.
	 * @param generatedLocals   The set that receives all (temporary) locals created
	 *                          to provide a value of the requested type
	 * @return The generated value, or null if no value could be generated
	 */
	protected Value getValueForType(Type tp, Set<SootClass> constructionStack, Set<SootClass> parentClasses,
			Set<Local> generatedLocals) {
		// Depending on the parameter type, we try to find a suitable
		// concrete substitution
		if (isSimpleType(tp.toString()))
			return getSimpleDefaultValue(tp);
		else if (tp instanceof RefType) {
			SootClass classToType = ((RefType) tp).getSootClass();

			if (classToType != null) {
				// If we have a parent class compatible with this type, we use
				// it before we check any other option
				for (SootClass parent : parentClasses)
					if (isCompatible(parent, classToType)) {
						Value val = this.localVarsForClasses.get(parent);
						if (val != null)
							return val;
					}

				// If this is a system class, we may want to skip it
				if (ignoreSystemClassParams && SystemClassHandler.v().isClassInSystemPackage(classToType.getName()))
					return NullConstant.v();

				// Create a new instance to plug in here
				Value val = generateClassConstructor(classToType, constructionStack, parentClasses, generatedLocals);

				// If we cannot create a parameter, we try a null reference.
				// Better than not creating the whole invocation...
				if (val == null)
					return NullConstant.v();

				// Keep track of the locals we generate
				if (generatedLocals != null && val instanceof Local)
					generatedLocals.add((Local) val);

				return val;
			}
		} else if (tp instanceof ArrayType) {
			Value arrVal = buildArrayOfType((ArrayType) tp, constructionStack, parentClasses, generatedLocals);
			if (arrVal == null) {
				logger.warn("Array parameter substituted by null");
				return NullConstant.v();
			}
			return arrVal;
		} else {
			logger.warn("Unsupported parameter type: {}", tp.toString());
			return null;
		}
		throw new RuntimeException("Should never see me");
	}

	/**
	 * Constructs an array of the given type with a single element of this type in
	 * the given method
	 * 
	 * @param tp                The type of which to create the array
	 * @param constructionStack Set of classes currently being built to avoid
	 *                          constructor loops
	 * @param parentClasses     If a requested type is compatible with one of the
	 *                          types in this list, the already-created object is
	 *                          used instead of creating a new one.
	 * @param generatedLocals   A set that receives the (temporary) locals that were
	 *                          generated to create the requested array
	 * @return The local referencing the newly created array, or null if the array
	 *         generation failed
	 */
	private Value buildArrayOfType(ArrayType tp, Set<SootClass> constructionStack, Set<SootClass> parentClasses,
			Set<Local> generatedLocals) {
		// Generate a single element in the array
		Value singleElement = getValueForType(tp.getElementType(), constructionStack, parentClasses);

		// Generate a new single-element array
		Local local = generator.generateLocal(tp);
		NewArrayExpr newArrayExpr = Jimple.v().newNewArrayExpr(tp.getElementType(), IntConstant.v(1));
		AssignStmt assignArray = Jimple.v().newAssignStmt(local, newArrayExpr);
		body.getUnits().add(assignArray);

		// Assign the element to the first element of the array
		AssignStmt assign = Jimple.v().newAssignStmt(Jimple.v().newArrayRef(local, IntConstant.v(0)), singleElement);
		body.getUnits().add(assign);
		return local;
	}

	/**
	 * Generates code which creates a new instance of the given class.
	 * 
	 * @param createdClass The class of which to create an instance
	 * @return The local containing the new object instance if the operation
	 *         completed successfully, otherwise null.
	 */
	protected Local generateClassConstructor(SootClass createdClass) {
		return this.generateClassConstructor(createdClass, new HashSet<SootClass>(), Collections.<SootClass>emptySet(),
				null);
	}

	/**
	 * Generates code which creates a new instance of the given class.
	 * 
	 * @param createdClass  The class of which to create an instance
	 * @param parentClasses If a constructor call requires an object of a type which
	 *                      is compatible with one of the types in this list, the
	 *                      already-created object is used instead of creating a new
	 *                      one.
	 * @return The local containing the new object instance if the operation
	 *         completed successfully, otherwise null.
	 */
	protected Local generateClassConstructor(SootClass createdClass, Set<SootClass> parentClasses) {
		return this.generateClassConstructor(createdClass, new HashSet<SootClass>(), parentClasses, null);
	}

	/**
	 * Determines whether a class is accepted for generating a constructor.
	 * 
	 * @param clazz The class of which to create an instance
	 * @return Whether the class is accepted for generating a constructor
	 */
	protected boolean acceptClass(SootClass clazz) {
		// We cannot create instances of phantom classes as we do not have any
		// constructor information for them
		if (clazz.isPhantom() || clazz.isPhantomClass()) {
			logger.warn("Cannot generate constructor for phantom class {}", clazz.getName());
			return false;
		}

		return true;
	}

	/**
	 * Generates code which creates a new instance of the given class. Note that if
	 * {@link #allowNonPublicConstructors} is <code>true<code>, private or protected
	 * constructors may be used.
	 * 
	 * @param createdClass      The class of which to create an instance
	 * @param constructionStack The stack of classes currently under construction.
	 *                          This is used to detect constructor loops. If a
	 *                          constructor requires a parameter of a type that is
	 *                          already on the stack, this value is substituted by
	 *                          null.
	 * @param parentClasses     If a constructor call requires an object of a type
	 *                          which is compatible with one of the types in this
	 *                          list, the already-created object is used instead of
	 *                          creating a new one.
	 * @param tempLocals        The set that receives all generated temporary locals
	 *                          that were necessary for calling the constructor of
	 *                          the requested class
	 * @return The local containing the new object instance if the operation
	 *         completed successfully, otherwise null.
	 */
	protected Local generateClassConstructor(SootClass createdClass, Set<SootClass> constructionStack,
			Set<SootClass> parentClasses, Set<Local> tempLocals) {
		if (createdClass == null || this.failedClasses.contains(createdClass))
			return null;

		// If we already have a class local of that type, we re-use it
		Local existingLocal = localVarsForClasses.get(createdClass);
		if (existingLocal != null)
			return existingLocal;

		if (!acceptClass(createdClass)) {
			failedClasses.add(createdClass);
			return null;
		}

		// if sootClass is simpleClass:
		if (isSimpleType(createdClass.toString())) {
			Local varLocal = generator.generateLocal(getSimpleTypeFromType(createdClass.getType()));

			AssignStmt aStmt = Jimple.v().newAssignStmt(varLocal, getSimpleDefaultValue(createdClass.getType()));
			body.getUnits().add(aStmt);
			return varLocal;
		}

		boolean isInnerClass = createdClass.getName().contains("$");
		SootClass outerClass = isInnerClass
				? Scene.v().getSootClassUnsafe(
						createdClass.getName().substring(0, createdClass.getName().lastIndexOf("$")))
				: null;

		// Make sure that we don't run into loops
		if (!constructionStack.add(createdClass)) {
			if (warnOnConstructorLoop) {
				logger.warn("Ran into a constructor generation loop for class " + createdClass
						+ ", substituting with null...");
			}
			Local tempLocal = generator.generateLocal(RefType.v(createdClass));
			AssignStmt assignStmt = Jimple.v().newAssignStmt(tempLocal, NullConstant.v());
			body.getUnits().add(assignStmt);
			return tempLocal;
		}
		if (createdClass.isInterface() || createdClass.isAbstract()) {
			return generateSubstitutedClassConstructor(createdClass, constructionStack, parentClasses);
		} else {
			// Find a constructor we can invoke. We do this first as we don't
			// want
			// to change anything in our method body if we cannot create a class
			// instance anyway.
			List<SootMethod> constructors = new ArrayList<>();
			for (SootMethod currentMethod : createdClass.getMethods()) {
				if (!currentMethod.isConstructor())
					continue;
				if (!allowNonPublicConstructors && (currentMethod.isPrivate() || currentMethod.isProtected()))
					continue;
				constructors.add(currentMethod);
			}

			// Prefer public constructors. The fewer parameters a constructor has, the
			// better for us.
			Collections.sort(constructors, new Comparator<SootMethod>() {

				@Override
				public int compare(SootMethod o1, SootMethod o2) {
					if ((o1.isPrivate() || o1.isProtected()) != (o2.isPrivate() || o2.isProtected()))
						return (o1.isPrivate() || o1.isProtected()) ? 1 : -1;
					if (o1.getParameterCount() == o2.getParameterCount()) {
						int o1Prims = 0, o2Prims = 0;
						for (int i = 0; i < o1.getParameterCount(); i++)
							if (o1.getParameterType(i) instanceof PrimType)
								o1Prims++;
						for (int i = 0; i < o2.getParameterCount(); i++)
							if (o2.getParameterType(i) instanceof PrimType)
								o2Prims++;
						return o1Prims - o2Prims;
					}
					return o1.getParameterCount() - o2.getParameterCount();
				}

			});

			if (!constructors.isEmpty()) {
				SootMethod currentMethod = constructors.remove(0);
				List<Value> params = new LinkedList<Value>();
				for (Type type : currentMethod.getParameterTypes()) {
					// We need to reset the construction stack. Just because we
					// already created a class instance for parameter 1, there is no reason for
					// not being able to create the same class instance again for parameter 2.
					Set<SootClass> newStack = new HashSet<>(constructionStack);

					// We need to check whether we have a reference to the
					// outer class. In this case, we do not generate a new
					// instance, but use the one we already have.
					SootClass typeClass = type instanceof RefType ? ((RefType) type).getSootClass() : null;
					if (typeClass != null && isInnerClass && typeClass == outerClass
							&& this.localVarsForClasses.containsKey(outerClass))
						params.add(this.localVarsForClasses.get(outerClass));
					else if (shallowMode) {
						if (isSimpleType(type.toString()))
							params.add(getSimpleDefaultValue(type));
						else
							params.add(NullConstant.v());
					} else {
						Value val = getValueForType(type, newStack, parentClasses, tempLocals);
						params.add(val);
					}
				}

				// Build the "new" expression
				NewExpr newExpr = Jimple.v().newNewExpr(RefType.v(createdClass));
				Local tempLocal = generator.generateLocal(RefType.v(createdClass));
				AssignStmt assignStmt = Jimple.v().newAssignStmt(tempLocal, newExpr);
				body.getUnits().add(assignStmt);

				// Create the constructor invocation
				InvokeExpr vInvokeExpr;
				if (params.isEmpty() || params.contains(null))
					vInvokeExpr = Jimple.v().newSpecialInvokeExpr(tempLocal, currentMethod.makeRef());
				else
					vInvokeExpr = Jimple.v().newSpecialInvokeExpr(tempLocal, currentMethod.makeRef(), params);

				// We don't need return values
				body.getUnits().add(Jimple.v().newInvokeStmt(vInvokeExpr));
				if (tempLocals != null)
					tempLocals.add(tempLocal);
				return tempLocal;
			}

			this.failedClasses.add(createdClass);
			return null;
		}
	}

	/**
	 * Generates a call to a constructor for a an interface or an abstract class
	 * that is substituted with an actual implementation
	 * 
	 * @param createdClass      The class for which to create a constructor call
	 * @param constructionStack The stack for making sure that we do not run into
	 *                          loops
	 * @param parentClasses     If a constructor call requires an object of a type
	 *                          which is compatible with one of the types in this
	 *                          list, the already-created object is used instead of
	 *                          creating a new one.
	 * @return The local containing the new object instance if the operation
	 *         completed successfully, otherwise null.
	 */
	private Local generateSubstitutedClassConstructor(SootClass createdClass, Set<SootClass> constructionStack,
			Set<SootClass> parentClasses) {
		// This feature must be enabled explicitly
		if (!substituteCallParams) {
			logger.warn("Cannot create valid constructor for {}, because it is {} and cannot substitute with subclass",
					createdClass,
					(createdClass.isInterface() ? "an interface" : (createdClass.isAbstract() ? "abstract" : "")));
			this.failedClasses.add(createdClass);
			return null;
		}

		// Find a matching implementor of the interface
		List<SootClass> classes;
		if (createdClass.isInterface())
			classes = Scene.v().getActiveHierarchy().getImplementersOf(createdClass);
		else
			classes = Scene.v().getActiveHierarchy().getSubclassesOf(createdClass);

		// Generate an instance of the substitution class. If we fail,
		// try the next substitution. If we don't find any possible
		// substitution, we're in trouble
		for (SootClass sClass : classes)
			if (substituteClasses.contains(sClass.toString())) {
				Local cons = generateClassConstructor(sClass, constructionStack, parentClasses, null);
				if (cons == null)
					continue;
				return cons;
			}
		logger.warn("Cannot create valid constructor for {}, because it is {} and cannot substitute with subclass",
				createdClass,
				(createdClass.isInterface() ? "an interface" : (createdClass.isAbstract() ? "abstract" : "")));
		this.failedClasses.add(createdClass);
		return null;
	}

	protected Type getSimpleTypeFromType(Type type) {
		if (type.toString().equals("java.lang.String")) {
			assert type instanceof RefType;
			return RefType.v(((RefType) type).getSootClass());
		}
		if (type.toString().equals("void"))
			return soot.VoidType.v();
		if (type.toString().equals("char"))
			return soot.CharType.v();
		if (type.toString().equals("byte"))
			return soot.ByteType.v();
		if (type.toString().equals("short"))
			return soot.ShortType.v();
		if (type.toString().equals("int"))
			return soot.IntType.v();
		if (type.toString().equals("float"))
			return soot.FloatType.v();
		if (type.toString().equals("long"))
			return soot.LongType.v();
		if (type.toString().equals("double"))
			return soot.DoubleType.v();
		if (type.toString().equals("boolean"))
			return soot.BooleanType.v();
		throw new RuntimeException("Unknown simple type: " + type);
	}

	protected static boolean isSimpleType(String t) {
		if (t.equals("java.lang.String") || t.equals("void") || t.equals("char") || t.equals("byte")
				|| t.equals("short") || t.equals("int") || t.equals("float") || t.equals("long") || t.equals("double")
				|| t.equals("boolean")) {
			return true;
		} else {
			return false;
		}
	}

	protected Value getSimpleDefaultValue(Type t) {
		if (t == RefType.v("java.lang.String"))
			return StringConstant.v("");
		if (t instanceof CharType)
			return IntConstant.v(0);
		if (t instanceof ByteType)
			return IntConstant.v(0);
		if (t instanceof ShortType)
			return IntConstant.v(0);
		if (t instanceof IntType)
			return IntConstant.v(0);
		if (t instanceof FloatType)
			return FloatConstant.v(0);
		if (t instanceof LongType)
			return LongConstant.v(0);
		if (t instanceof DoubleType)
			return DoubleConstant.v(0);
		if (t instanceof BooleanType)
			return IntConstant.v(0);

		// also for arrays etc.
		return NullConstant.v();
	}

	/**
	 * Finds a method with the given signature in the given class or one of its
	 * super classes
	 * 
	 * @param currentClass The current class in which to start the search
	 * @param subsignature The subsignature of the method to find
	 * @return The method with the given signature if it has been found, otherwise
	 *         null
	 */
	protected SootMethod findMethod(SootClass currentClass, String subsignature) {
		SootMethod m = currentClass.getMethodUnsafe(subsignature);
		if (m != null) {
			return m;
		}
		if (currentClass.hasSuperclass()) {
			return findMethod(currentClass.getSuperclass(), subsignature);
		}
		return null;
	}

	/**
	 * Checks whether an object of type "actual" can be inserted where an object of
	 * type "expected" is required.
	 * 
	 * @param actual   The actual type (the substitution candidate)
	 * @param expected The requested type
	 * @return True if the two types are compatible and "actual" can be used as a
	 *         substitute for "expected", otherwise false
	 */
	protected boolean isCompatible(SootClass actual, SootClass expected) {
		return Scene.v().getOrMakeFastHierarchy().canStoreType(actual.getType(), expected.getType());
	}

	/**
	 * Eliminates all loops of length 0 (if a goto <if a>)
	 */
	protected void eliminateSelfLoops() {
		// Get rid of self-loops
		for (Iterator<Unit> unitIt = body.getUnits().iterator(); unitIt.hasNext();) {
			Unit u = unitIt.next();
			if (u instanceof IfStmt) {
				IfStmt ifStmt = (IfStmt) u;
				if (ifStmt.getTarget() == ifStmt)
					unitIt.remove();
			}
		}
	}

	/**
	 * Sets the name that shall be used for the new class containing the dummy main
	 * method
	 * 
	 * @param dummyMethodName The name for the new class containing the dummy main
	 *                        method
	 */
	public void setDummyClassName(String dummyClassName) {
		this.dummyClassName = dummyClassName;
	}

	/**
	 * Sets the name that shall be used for the new dummy main method
	 * 
	 * @param dummyMethodName The name for the new dummy main method
	 */
	public void setDummyMethodName(String dummyMethodName) {
		this.dummyMethodName = dummyMethodName;
	}

	/**
	 * Sets whether a call to a method A.foo() may receive an instance of A as a
	 * parameter. If this is not allowed, other type-compatible class instances are
	 * taken. If they don't exist, null is used.
	 * 
	 * @param value True if method calls may receive instances of their containing
	 *              class as parameter values, otherwise false
	 */
	public void setAllowSelfReferences(boolean value) {
		this.allowSelfReferences = value;
	}

	/**
	 * Sets whether shallow mode shall be used. Normally, if a call to a method a()
	 * is to be created, this class first creates instances of all required
	 * parameter objects. If these, in turn, require other objects, they are
	 * instantiated as well. In shallow mode, this does not happen. Instead, all
	 * values on the first level are replaced with default values (e.g., null for
	 * objects).
	 * 
	 * @param shallowMode True if shallow mode shall be used, otherwise false
	 */
	public void setShallowMode(boolean shallowMode) {
		this.shallowMode = shallowMode;
	}

	/**
	 * Gets whether shallow mode shall be used. Normally, if a call to a method a()
	 * is to be created, this class first creates instances of all required
	 * parameter objects. If these, in turn, require other objects, they are
	 * instantiated as well. In shallow mode, this does not happen. Instead, all
	 * values on the first level are replaced with default values (e.g., null for
	 * objects).
	 * 
	 * @return True if shallow mode shall be used, otherwise false
	 */
	public boolean getShallowMode() {
		return this.shallowMode;
	}

	/**
	 * Sets whether the entry point creator shall always pass "null" if a method
	 * expects an object of a system class.
	 * 
	 * @param ignoreSystemClassParams
	 */
	public void setIgnoreSystemClassParams(boolean ignoreSystemClassParams) {
		this.ignoreSystemClassParams = ignoreSystemClassParams;
	}

	/**
	 * Sets whether the entry point creator may use private or protected
	 * constructors in order to generate a class instance in the dummyMain method.
	 * 
	 * @param allowNonPublicConstructors
	 */
	public void setAllowNonPublicConstructors(boolean allowNonPublicConstructors) {
		this.allowNonPublicConstructors = allowNonPublicConstructors;
	}

	/**
	 * Sets whether the dummy main method shall be overwritten if it already exists.
	 * If this flag is set to "false", a new, non-conflicting method and class name
	 * is chosen.
	 * 
	 * @param reuseDummyMainValue True if existing methods that conflict with the
	 *                            entry point to be created shall be overwritten,
	 *                            false to automatically chose a new,
	 *                            non-conflicting name.
	 */
	public void setOverwriteDummyMainMethod(boolean overwriteDummyMainValue) {
		this.overwriteDummyMainMethod = overwriteDummyMainValue;
	}

	/**
	 * Gets whether the dummy main method shall be overwritten if it already exists.
	 * If this flag is set to "false", a new, non-conflicting method and class name
	 * is chosen.
	 * 
	 * @return True if existing methods that conflict with the entry point to be
	 *         created shall be overwritten, false to automatically chose a new,
	 *         non-conflicting name.
	 */
	public boolean getOverwriteDummyMainMethod() {
		return this.overwriteDummyMainMethod;
	}

	/**
	 * Sets whether a warning shall be written to the log when a constructor call
	 * cannot be generated because the analysis ran into a loop when trying to
	 * generate parameter values.
	 * 
	 * @param warnOnConstructorLoop True if a warning shall be written to the log
	 *                              when a constructor generation loop is
	 *                              encountered, otherwise false
	 */
	public void setWarnOnConstructorLoop(boolean warnOnConstructorLoop) {
		this.warnOnConstructorLoop = warnOnConstructorLoop;
	}

	/**
	 * Gets whether a warning shall be written to the log when a constructor call
	 * cannot be generated because the analysis ran into a loop when trying to
	 * generate parameter values.
	 * 
	 * @return True if a warning shall be written to the log when a constructor
	 *         generation loop is encountered, otherwise false
	 */
	public boolean getWarnOnConstructorLoop() {
		return this.warnOnConstructorLoop;
	}

	/**
	 * Resets the internal state to make sure that the entry point creator is
	 * re-usable. Note that this method will not reset the sets of failed classes
	 * and methods, because it doesn't make much sense to try them again and fail
	 * again on later re-runs.
	 */
	protected void reset() {
		localVarsForClasses.clear();
		conditionCounter = 0;
	}

	/**
	 * Creates an opaque predicate that jumps to the given target
	 * 
	 * @param target The target to which the opaque predicate shall jump
	 */
	protected void createIfStmt(Unit target) {
		if (target == null) {
			return;
		}
		final Jimple jimple = Jimple.v();
		EqExpr cond = jimple.newEqExpr(intCounter, IntConstant.v(conditionCounter++));
		IfStmt ifStmt = jimple.newIfStmt(cond, target);
		body.getUnits().add(ifStmt);
	}

	@Override
	public SootMethod getGeneratedMainMethod() {
		return mainMethod;
	}

}
