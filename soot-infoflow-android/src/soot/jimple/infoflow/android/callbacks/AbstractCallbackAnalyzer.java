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
package soot.jimple.infoflow.android.callbacks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.AnySubType;
import soot.Body;
import soot.FastHierarchy;
import soot.Local;
import soot.PointsToSet;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.callbacks.CallbackDefinition.CallbackType;
import soot.jimple.infoflow.android.callbacks.filters.ICallbackFilter;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.android.source.parsers.xml.ResourceUtils;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.infoflow.values.IValueProvider;
import soot.jimple.infoflow.values.SimpleConstantValueProvider;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

/**
 * Analyzes the classes in the APK file to find custom implementations of the
 * well-known Android callback and handler interfaces.
 * 
 * @author Steven Arzt
 *
 */
public abstract class AbstractCallbackAnalyzer {

	private static final String SIG_CAR_CREATE = "<android.car.Car: android.car.Car createCar(android.content.Context,android.content.ServiceConnection)>";

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final SootClass scContext = Scene.v().getSootClassUnsafe("android.content.Context");

	protected final SootClass scBroadcastReceiver = Scene.v()
			.getSootClassUnsafe(AndroidEntryPointConstants.BROADCASTRECEIVERCLASS);
	protected final SootClass scServiceConnection = Scene.v()
			.getSootClassUnsafe(AndroidEntryPointConstants.SERVICECONNECTIONINTERFACE);

	protected final SootClass scFragmentTransaction = Scene.v().getSootClassUnsafe("android.app.FragmentTransaction");
	protected final SootClass scFragment = Scene.v().getSootClassUnsafe(AndroidEntryPointConstants.FRAGMENTCLASS);

	protected final SootClass scSupportFragmentTransaction = Scene.v()
			.getSootClassUnsafe("android.support.v4.app.FragmentTransaction");
	protected final SootClass scSupportFragment = Scene.v().getSootClassUnsafe("android.support.v4.app.Fragment");

	protected final InfoflowAndroidConfiguration config;
	protected final Set<SootClass> entryPointClasses;
	protected final Set<String> androidCallbacks;

	protected final MultiMap<SootClass, CallbackDefinition> callbackMethods = new HashMultiMap<>();
	protected final MultiMap<SootClass, Integer> layoutClasses = new HashMultiMap<>();
	protected final Set<SootClass> dynamicManifestComponents = new HashSet<>();
	protected final MultiMap<SootClass, SootClass> fragmentClasses = new HashMultiMap<>();
	protected final Map<SootClass, Integer> fragmentIDs = new HashMap<>();

	protected final List<ICallbackFilter> callbackFilters = new ArrayList<>();
	protected final Set<SootClass> excludedEntryPoints = new HashSet<>();

	protected IValueProvider valueProvider = new SimpleConstantValueProvider();

	public AbstractCallbackAnalyzer(InfoflowAndroidConfiguration config, Set<SootClass> entryPointClasses)
			throws IOException {
		this(config, entryPointClasses, "AndroidCallbacks.txt");
	}

	public AbstractCallbackAnalyzer(InfoflowAndroidConfiguration config, Set<SootClass> entryPointClasses,
			String callbackFile) throws IOException {
		this(config, entryPointClasses, loadAndroidCallbacks(callbackFile));
	}

	public AbstractCallbackAnalyzer(InfoflowAndroidConfiguration config, Set<SootClass> entryPointClasses,
			InputStream inputStream) throws IOException {
		this(config, entryPointClasses, loadAndroidCallbacks(new InputStreamReader(inputStream)));
	}

	public AbstractCallbackAnalyzer(InfoflowAndroidConfiguration config, Set<SootClass> entryPointClasses,
			Reader reader) throws IOException {
		this(config, entryPointClasses, loadAndroidCallbacks(reader));
	}

	public AbstractCallbackAnalyzer(InfoflowAndroidConfiguration config, Set<SootClass> entryPointClasses,
			Set<String> androidCallbacks) throws IOException {
		this.config = config;
		this.entryPointClasses = entryPointClasses;
		this.androidCallbacks = androidCallbacks;
	}

	/**
	 * Loads the set of interfaces that are used to implement Android callback
	 * handlers from a file on disk
	 * 
	 * @param androidCallbackFile
	 *            The file from which to load the callback definitions
	 * @return A set containing the names of the interfaces that are used to
	 *         implement Android callback handlers
	 */
	private static Set<String> loadAndroidCallbacks(String androidCallbackFile) throws IOException {
		String fileName = androidCallbackFile;
		if (!new File(fileName).exists()) {
			fileName = "../soot-infoflow-android/AndroidCallbacks.txt";
			if (!new File(fileName).exists()) {
				return loadAndroidCallbacks(
						new InputStreamReader(ResourceUtils.getResourceStream("/AndroidCallbacks.txt")));
			}
		}
		return loadAndroidCallbacks(new FileReader(fileName));
	}

	/**
	 * Loads the set of interfaces that are used to implement Android callback
	 * handlers from a file on disk
	 * 
	 * @param reader
	 *            A file reader
	 * @return A set containing the names of the interfaces that are used to
	 *         implement Android callback handlers
	 */
	public static Set<String> loadAndroidCallbacks(Reader reader) throws IOException {
		Set<String> androidCallbacks = new HashSet<String>();
		BufferedReader bufReader = new BufferedReader(reader);
		try {
			String line;
			while ((line = bufReader.readLine()) != null)
				if (!line.isEmpty())
					androidCallbacks.add(line);

		} finally {
			bufReader.close();
		}
		return androidCallbacks;
	}

	/**
	 * Collects the callback methods for all Android default handlers implemented in
	 * the source code.
	 */
	public void collectCallbackMethods() {
		// Initialize the filters
		for (ICallbackFilter filter : callbackFilters)
			filter.reset();
	}

	/**
	 * Analyzes the given method and looks for callback registrations
	 * 
	 * @param lifecycleElement
	 *            The lifecycle element (activity, etc.) with which to associate the
	 *            found callbacks
	 * @param method
	 *            The method in which to look for callbacks
	 */
	protected void analyzeMethodForCallbackRegistrations(SootClass lifecycleElement, SootMethod method) {
		// Do not analyze system classes
		if (SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
			return;
		if (!method.isConcrete())
			return;

		// Iterate over all statement and find callback registration methods
		Set<SootClass> callbackClasses = new HashSet<SootClass>();
		for (Unit u : method.retrieveActiveBody().getUnits()) {
			Stmt stmt = (Stmt) u;
			// Callback registrations are always instance invoke expressions
			if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();

				final SootMethodRef mref = iinv.getMethodRef();
				for (int i = 0; i < iinv.getArgCount(); i++) {
					final Type type = mref.parameterType(i);
					if (!(type instanceof RefType))
						continue;
					String param = type.toString();
					if (androidCallbacks.contains(param)) {
						Value arg = iinv.getArg(i);

						// This call must be to a system API in order to
						// register an OS-level callback
						if (!SystemClassHandler.isClassInSystemPackage(iinv.getMethod().getDeclaringClass().getName()))
							continue;

						// We have a formal parameter type that corresponds to one of the Android
						// callback interfaces. Look for definitions of the parameter to estimate the
						// actual type.
						if (arg instanceof Local) {
							Set<Type> possibleTypes = Scene.v().getPointsToAnalysis().reachingObjects((Local) arg)
									.possibleTypes();
							for (Type possibleType : possibleTypes) {
								RefType baseType;
								if (possibleType instanceof RefType)
									baseType = (RefType) possibleType;
								else if (possibleType instanceof AnySubType)
									baseType = ((AnySubType) possibleType).getBase();
								else {
									logger.warn("Unsupported type detected in callback analysis");
									continue;
								}

								SootClass targetClass = baseType.getSootClass();
								if (!SystemClassHandler.isClassInSystemPackage(targetClass.getName()))
									callbackClasses.add(targetClass);
							}

							// If we don't have pointsTo information, we take
							// the type of the local
							if (possibleTypes.isEmpty()) {
								Type argType = ((Local) arg).getType();
								RefType baseType;
								if (argType instanceof RefType)
									baseType = (RefType) argType;
								else if (argType instanceof AnySubType)
									baseType = ((AnySubType) argType).getBase();
								else {
									logger.warn("Unsupported type detected in callback analysis");
									continue;
								}

								SootClass targetClass = baseType.getSootClass();
								if (!SystemClassHandler.isClassInSystemPackage(targetClass.getName()))
									callbackClasses.add(targetClass);
							}
						}
					}
				}
			}
		}

		// Analyze all found callback classes
		for (SootClass callbackClass : callbackClasses)
			analyzeClassInterfaceCallbacks(callbackClass, callbackClass, lifecycleElement);
	}

	/**
	 * Checks whether all filters accept the association between the callback class
	 * and its parent component
	 * 
	 * @param lifecycleElement
	 *            The hosting component's class
	 * @param targetClass
	 *            The class implementing the callbacks
	 * @return True if all filters accept the given component-callback mapping,
	 *         otherwise false
	 */
	private boolean filterAccepts(SootClass lifecycleElement, SootClass targetClass) {
		for (ICallbackFilter filter : callbackFilters)
			if (!filter.accepts(lifecycleElement, targetClass))
				return false;
		return true;
	}

	/**
	 * Checks whether all filters accept the association between the callback method
	 * and its parent component
	 * 
	 * @param lifecycleElement
	 *            The hosting component's class
	 * @param targetMethod
	 *            The method implementing the callback
	 * @return True if all filters accept the given component-callback mapping,
	 *         otherwise false
	 */
	private boolean filterAccepts(SootClass lifecycleElement, SootMethod targetMethod) {
		for (ICallbackFilter filter : callbackFilters)
			if (!filter.accepts(lifecycleElement, targetMethod))
				return false;
		return true;
	}

	/**
	 * Checks whether the given method dynamically registers a new broadcast
	 * receiver
	 * 
	 * @param method
	 *            The method to check
	 */
	protected void analyzeMethodForDynamicBroadcastReceiver(SootMethod method) {
		// Do not analyze system classes
		if (SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
			return;
		if (!method.isConcrete() || !method.hasActiveBody())
			return;

		final FastHierarchy fastHierarchy = Scene.v().getFastHierarchy();
		final RefType contextType = scContext.getType();
		for (Unit u : method.getActiveBody().getUnits()) {
			Stmt stmt = (Stmt) u;
			if (stmt.containsInvokeExpr()) {
				final InvokeExpr iexpr = stmt.getInvokeExpr();
				final SootMethodRef methodRef = iexpr.getMethodRef();
				if (methodRef.name().equals("registerReceiver") && iexpr.getArgCount() > 0
						&& fastHierarchy.canStoreType(methodRef.declaringClass().getType(), contextType)) {
					Value br = iexpr.getArg(0);
					if (br.getType() instanceof RefType) {
						RefType rt = (RefType) br.getType();
						if (!SystemClassHandler.isClassInSystemPackage(rt.getSootClass().getName()))
							dynamicManifestComponents.add(rt.getSootClass());
					}
				}
			}
		}
	}

	/**
	 * Checks whether the given method dynamically registers a new service
	 * connection
	 * 
	 * @param method
	 *            The method to check
	 */
	protected void analyzeMethodForServiceConnection(SootMethod method) {
		// Do not analyze system classes
		if (SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
			return;
		if (!method.isConcrete() || !method.hasActiveBody())
			return;

		for (Unit u : method.getActiveBody().getUnits()) {
			Stmt stmt = (Stmt) u;
			if (stmt.containsInvokeExpr()) {
				final InvokeExpr iexpr = stmt.getInvokeExpr();
				final SootMethodRef methodRef = iexpr.getMethodRef();
				if (methodRef.getSignature().equals(SIG_CAR_CREATE)) {
					Value br = iexpr.getArg(1);

					// We need all possible types for the parameter
					if (br instanceof Local && Scene.v().hasPointsToAnalysis()) {
						PointsToSet pts = Scene.v().getPointsToAnalysis().reachingObjects((Local) br);
						for (Type tp : pts.possibleTypes()) {
							if (tp instanceof RefType) {
								RefType rt = (RefType) tp;
								if (!SystemClassHandler.isClassInSystemPackage(rt.getSootClass().getName()))
									dynamicManifestComponents.add(rt.getSootClass());
							}
						}
					}

					// Just to be sure, also add the declared type
					if (br.getType() instanceof RefType) {
						RefType rt = (RefType) br.getType();
						if (!SystemClassHandler.isClassInSystemPackage(rt.getSootClass().getName()))
							dynamicManifestComponents.add(rt.getSootClass());
					}
				}
			}
		}
	}

	/**
	 * Checks whether the given method executes a fragment transaction that creates
	 * new fragment
	 * 
	 * @author Goran Piskachev
	 * @param method
	 *            The method to check
	 */
	protected void analyzeMethodForFragmentTransaction(SootClass lifecycleElement, SootMethod method) {
		if (scFragment == null || scFragmentTransaction == null)
			if (scSupportFragment == null || scSupportFragmentTransaction == null)
				return;
		if (!method.isConcrete() || !method.hasActiveBody())
			return;

		// first check if there is a Fragment manager, a fragment transaction
		// and a call to the add method which adds the fragment to the transaction
		boolean isFragmentManager = false;
		boolean isFragmentTransaction = false;
		boolean isAddTransaction = false;
		for (Unit u : method.getActiveBody().getUnits()) {
			Stmt stmt = (Stmt) u;
			if (stmt.containsInvokeExpr()) {
				final String methodName = stmt.getInvokeExpr().getMethod().getName();
				if (methodName.equals("getFragmentManager"))
					isFragmentManager = true;
				else if (methodName.equals("beginTransaction"))
					isFragmentTransaction = true;
				else if (methodName.equals("add") || methodName.equals("replace"))
					isAddTransaction = true;
				else if (methodName.equals("inflate") && stmt.getInvokeExpr().getArgCount() > 1) {
					Value arg = stmt.getInvokeExpr().getArg(0);
					Integer fragmentID = valueProvider.getValue(method, stmt, arg, Integer.class);
					if (fragmentID != null)
						fragmentIDs.put(lifecycleElement, fragmentID);
				}
			}
		}

		// now get the fragment class from the second argument of the add method
		// from the transaction
		if (isFragmentManager && isFragmentTransaction && isAddTransaction)
			for (Unit u : method.getActiveBody().getUnits()) {
				Stmt stmt = (Stmt) u;
				if (stmt.containsInvokeExpr()) {
					InvokeExpr invExpr = stmt.getInvokeExpr();
					if (invExpr instanceof InstanceInvokeExpr) {
						InstanceInvokeExpr iinvExpr = (InstanceInvokeExpr) invExpr;

						// Make sure that we referring to the correct class and
						// method
						isFragmentTransaction = scFragmentTransaction != null && Scene.v().getFastHierarchy()
								.canStoreType(iinvExpr.getBase().getType(), scFragmentTransaction.getType());
						isFragmentTransaction |= scSupportFragmentTransaction != null && Scene.v().getFastHierarchy()
								.canStoreType(iinvExpr.getBase().getType(), scSupportFragmentTransaction.getType());
						isAddTransaction = stmt.getInvokeExpr().getMethod().getName().equals("add")
								|| stmt.getInvokeExpr().getMethod().getName().equals("replace");

						if (isFragmentTransaction && isAddTransaction) {
							// We take all fragments passed to the method
							for (int i = 0; i < stmt.getInvokeExpr().getArgCount(); i++) {
								Value br = stmt.getInvokeExpr().getArg(i);

								// Is this a fragment?
								if (br.getType() instanceof RefType) {
									RefType rt = (RefType) br.getType();

									boolean addFragment = scFragment != null
											&& Scene.v().getFastHierarchy().canStoreType(rt, scFragment.getType());
									addFragment |= scSupportFragment != null && Scene.v().getFastHierarchy()
											.canStoreType(rt, scSupportFragment.getType());
									if (addFragment)
										fragmentClasses.put(method.getDeclaringClass(), rt.getSootClass());
								}
							}
						}
					}
				}
			}
	}

	/**
	 * Gets whether the call in the given statement can end up in the respective
	 * method inherited from one of the given classes.
	 * 
	 * @param stmt
	 *            The statement containing the call sites
	 * @param classNames
	 *            The base classes in which the call can potentially end up
	 * @return True if the given call can end up in a method inherited from one of
	 *         the given classes, otherwise falae
	 */
	protected boolean isInheritedMethod(Stmt stmt, String... classNames) {
		if (!stmt.containsInvokeExpr())
			return false;

		// Look at the direct callee
		SootMethod tgt = stmt.getInvokeExpr().getMethod();
		for (String className : classNames)
			if (className.equals(tgt.getDeclaringClass().getName()))
				return true;

		// If we have a callgraph, we can use that.
		if (Scene.v().hasCallGraph()) {
			Iterator<Edge> edgeIt = Scene.v().getCallGraph().edgesOutOf(stmt);
			while (edgeIt.hasNext()) {
				Edge edge = edgeIt.next();
				String targetClass = edge.getTgt().method().getDeclaringClass().getName();
				for (String className : classNames)
					if (className.equals(targetClass))
						return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether this invocation calls Android's Activity.setContentView method
	 * 
	 * @param inv
	 *            The invocaton to check
	 * @return True if this invocation calls setContentView, otherwise false
	 */
	protected boolean invokesSetContentView(InvokeExpr inv) {
		String methodName = SootMethodRepresentationParser.v()
				.getMethodNameFromSubSignature(inv.getMethodRef().getSubSignature().getString());
		if (!methodName.equals("setContentView"))
			return false;

		// In some cases, the bytecode points the invocation to the current
		// class even though it does not implement setContentView, instead
		// of using the superclass signature
		SootClass curClass = inv.getMethod().getDeclaringClass();
		while (curClass != null) {
			if (curClass.getName().equals("android.app.Activity")
					|| curClass.getName().equals("android.support.v7.app.ActionBarActivity")
					|| curClass.getName().equals("android.support.v7.app.AppCompatActivity"))
				return true;
			if (curClass.declaresMethod("void setContentView(int)"))
				return false;
			curClass = curClass.hasSuperclass() ? curClass.getSuperclass() : null;
		}
		return false;
	}

	/**
	 * Checks whether this invocation calls Android's LayoutInflater.inflate method
	 * 
	 * @param inv
	 *            The invocaton to check
	 * @return True if this invocation calls inflate, otherwise false
	 */
	protected boolean invokesInflate(InvokeExpr inv) {
		String methodName = SootMethodRepresentationParser.v()
				.getMethodNameFromSubSignature(inv.getMethodRef().getSubSignature().getString());
		if (!methodName.equals("inflate"))
			return false;

		// In some cases, the bytecode points the invocation to the current
		// class even though it does not implement setContentView, instead
		// of using the superclass signature
		SootClass curClass = inv.getMethod().getDeclaringClass();
		while (curClass != null) {
			if (curClass.getName().equals("android.app.Fragment"))
				return true;
			if (curClass.declaresMethod("android.view.View inflate(int,android.view.ViewGroup,boolean)"))
				return false;
			curClass = curClass.hasSuperclass() ? curClass.getSuperclass() : null;
		}
		return false;
	}

	protected void analyzeMethodOverrideCallbacks(SootClass sootClass) {
		if (!sootClass.isConcrete())
			return;
		if (sootClass.isInterface())
			return;

		// Do not start the search in system classes
		if (config.getIgnoreFlowsInSystemPackages() && SystemClassHandler.isClassInSystemPackage(sootClass.getName()))
			return;

		// There are also some classes that implement interesting callback
		// methods.
		// We model this as follows: Whenever the user overwrites a method in an
		// Android OS class, we treat it as a potential callback.
		Map<String, SootMethod> systemMethods = new HashMap<>(10000);
		for (SootClass parentClass : Scene.v().getActiveHierarchy().getSuperclassesOf(sootClass)) {
			if (SystemClassHandler.isClassInSystemPackage(parentClass.getName()))
				for (SootMethod sm : parentClass.getMethods())
					if (!sm.isConstructor())
						systemMethods.put(sm.getSubSignature(), sm);
		}

		// Iterate over all user-implemented methods. If they are inherited
		// from a system class, they are callback candidates.
		for (SootClass parentClass : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(sootClass)) {
			if (SystemClassHandler.isClassInSystemPackage(parentClass.getName()))
				continue;
			for (SootMethod method : parentClass.getMethods()) {
				if (!method.hasTag(SimulatedCodeElementTag.TAG_NAME)) {
					// Check whether this is a real callback method
					SootMethod parentMethod = systemMethods.get(method.getSubSignature());
					if (parentMethod != null)
						checkAndAddMethod(method, parentMethod, sootClass, CallbackType.Default);
				}
			}
		}
	}

	private SootMethod getMethodFromHierarchyEx(SootClass c, String methodSignature) {
		SootMethod m = c.getMethodUnsafe(methodSignature);
		if (m != null)
			return m;
		SootClass superClass = c.getSuperclassUnsafe();
		if (superClass != null)
			return getMethodFromHierarchyEx(superClass, methodSignature);
		return null;
	}

	private void analyzeClassInterfaceCallbacks(SootClass baseClass, SootClass sootClass, SootClass lifecycleElement) {
		// We cannot create instances of abstract classes anyway, so there is no
		// reason to look for interface implementations
		if (!baseClass.isConcrete())
			return;

		// Do not analyze system classes
		if (SystemClassHandler.isClassInSystemPackage(baseClass.getName()))
			return;
		if (SystemClassHandler.isClassInSystemPackage(sootClass.getName()))
			return;

		// Check the filters
		if (!filterAccepts(lifecycleElement, baseClass))
			return;
		if (!filterAccepts(lifecycleElement, sootClass))
			return;

		// If we are a class, one of our superclasses might implement an Android
		// interface
		SootClass superClass = sootClass.getSuperclassUnsafe();
		if (superClass != null)
			analyzeClassInterfaceCallbacks(baseClass, superClass, lifecycleElement);

		// Do we implement one of the well-known interfaces?
		for (SootClass i : collectAllInterfaces(sootClass)) {
			if (androidCallbacks.contains(i.getName())) {
				CallbackType callbackType = isUICallback(i) ? CallbackType.Widget : CallbackType.Default;

				for (SootMethod sm : i.getMethods()) {
					SootMethod callbackImplementation = getMethodFromHierarchyEx(baseClass, sm.getSubSignature());
					if (callbackImplementation != null)
						checkAndAddMethod(callbackImplementation, sm, lifecycleElement, callbackType);
				}
			}
		}
	}

	/**
	 * Gets whether the given callback interface or class represents a UI callback
	 * 
	 * @param i
	 *            The callback interface or class to check
	 * @return True if the given callback interface or class represents a UI
	 *         callback, otherwise false
	 */
	private boolean isUICallback(SootClass i) {
		return i.getName().startsWith("android.widget") || i.getName().startsWith("android.view")
				|| i.getName().startsWith("android.content.DialogInterface$");
	}

	/**
	 * Checks whether the given Soot method comes from a system class. If not, it is
	 * added to the list of callback methods.
	 * 
	 * @param method
	 *            The method to check and add
	 * @param parentMethod
	 *            The original method in the Android framework that declared the
	 *            callback. This can, for example, be the method in the interface.
	 * @param lifecycleClass
	 *            The base class (activity, service, etc.) to which this callback
	 *            method belongs
	 * @param callbackType
	 *            The type of callback to be registered
	 * @return True if the method is new, i.e., has not been seen before, otherwise
	 *         false
	 */
	protected boolean checkAndAddMethod(SootMethod method, SootMethod parentMethod, SootClass lifecycleClass,
			CallbackType callbackType) {
		// Do not call system methods
		if (SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName()))
			return false;

		// Skip empty methods
		if (method.isConcrete() && isEmpty(method.retrieveActiveBody()))
			return false;

		// Skip constructors
		if (method.isConstructor() || method.isStaticInitializer())
			return false;

		// Check the filters
		if (!filterAccepts(lifecycleClass, method.getDeclaringClass()))
			return false;
		if (!filterAccepts(lifecycleClass, method))
			return false;

		return this.callbackMethods.put(lifecycleClass, new CallbackDefinition(method, parentMethod, callbackType));
	}

	private boolean isEmpty(Body activeBody) {
		for (Unit u : activeBody.getUnits())
			if (!(u instanceof IdentityStmt || u instanceof ReturnVoidStmt))
				return false;
		return true;
	}

	private Set<SootClass> collectAllInterfaces(SootClass sootClass) {
		Set<SootClass> interfaces = new HashSet<SootClass>(sootClass.getInterfaces());
		for (SootClass i : sootClass.getInterfaces())
			interfaces.addAll(collectAllInterfaces(i));
		return interfaces;
	}

	public MultiMap<SootClass, CallbackDefinition> getCallbackMethods() {
		return this.callbackMethods;
	}

	public MultiMap<SootClass, Integer> getLayoutClasses() {
		return this.layoutClasses;
	}

	public MultiMap<SootClass, SootClass> getFragmentClasses() {
		return this.fragmentClasses;
	}

	public Set<SootClass> getDynamicManifestComponents() {
		return this.dynamicManifestComponents;
	}

	/**
	 * Adds a new filter that checks every callback before it is associated with the
	 * respective host component
	 * 
	 * @param filter
	 *            The filter to add
	 */
	public void addCallbackFilter(ICallbackFilter filter) {
		this.callbackFilters.add(filter);
	}

	/**
	 * Excludes an entry point from all further processing. No more callbacks will
	 * be collected for the given entry point
	 * 
	 * @param entryPoint
	 *            The entry point to exclude
	 */
	public void excludeEntryPoint(SootClass entryPoint) {
		this.excludedEntryPoints.add(entryPoint);
	}

	/**
	 * Checks whether the given class is an excluded entry point
	 * 
	 * @param entryPoint
	 *            The entry point to check
	 * @return True if the given class is an excluded entry point, otherwise false
	 */
	public boolean isExcludedEntryPoint(SootClass entryPoint) {
		return this.excludedEntryPoints.contains(entryPoint);
	}

	/**
	 * Sets the provider that shall be used for obtaining constant values during the
	 * callback analysis
	 * 
	 * @param valueProvider
	 *            The value provider to use
	 */
	public void setValueProvider(IValueProvider valueProvider) {
		this.valueProvider = valueProvider;
	}

}
