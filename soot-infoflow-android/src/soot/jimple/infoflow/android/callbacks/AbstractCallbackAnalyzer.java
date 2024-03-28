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
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import heros.solver.Pair;
import soot.AnySubType;
import soot.Body;
import soot.FastHierarchy;
import soot.Local;
import soot.PointsToSet;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.ClassConstant;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition.CallbackType;
import soot.jimple.infoflow.android.callbacks.filters.ICallbackFilter;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.android.source.parsers.xml.ResourceUtils;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.jimple.infoflow.typing.TypeUtils;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.infoflow.values.IValueProvider;
import soot.jimple.infoflow.values.SimpleConstantValueProvider;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraphFactory;
import soot.toolkits.scalar.SimpleLocalDefs;
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
	protected final SootClass scAndroidXFragmentTransaction = Scene.v()
			.getSootClassUnsafe("androidx.fragment.app.FragmentTransaction");
	protected final SootClass scSupportFragment = Scene.v().getSootClassUnsafe("android.support.v4.app.Fragment");
	protected final SootClass scAndroidXFragment = Scene.v().getSootClassUnsafe("androidx.fragment.app.Fragment");

	protected final SootClass scSupportViewPager = Scene.v().getSootClassUnsafe("android.support.v4.view.ViewPager");
	protected final SootClass scAndroidXViewPager = Scene.v().getSootClassUnsafe("androidx.viewpager.widget.ViewPager");

	protected final SootClass scFragmentStatePagerAdapter = Scene.v()
			.getSootClassUnsafe("android.support.v4.app.FragmentStatePagerAdapter");
	protected final SootClass scFragmentPagerAdapter = Scene.v()
			.getSootClassUnsafe("android.support.v4.app.FragmentPagerAdapter");

	protected final SootClass scAndroidXFragmentStatePagerAdapter = Scene.v()
			.getSootClassUnsafe("androidx.fragment.app.FragmentStatePagerAdapter");
	protected final SootClass scAndroidXFragmentPagerAdapter = Scene.v()
			.getSootClassUnsafe("androidx.fragment.app.FragmentPagerAdapter");

	protected final InfoflowAndroidConfiguration config;
	protected final Set<SootClass> entryPointClasses;
	protected final Set<String> androidCallbacks;

	protected final MultiMap<SootClass, AndroidCallbackDefinition> callbackMethods = new HashMultiMap<>();
	protected final MultiMap<SootClass, Integer> layoutClasses = new HashMultiMap<>();
	protected final Set<SootClass> dynamicManifestComponents = new HashSet<>();
	protected final MultiMap<SootClass, SootClass> fragmentClasses = new HashMultiMap<>();
	protected final MultiMap<SootClass, SootClass> fragmentClassesRev = new HashMultiMap<>();
	protected final Map<SootClass, Integer> fragmentIDs = new HashMap<>();

	protected final List<ICallbackFilter> callbackFilters = new ArrayList<>();
	protected final Set<SootClass> excludedEntryPoints = new HashSet<>();

	protected IValueProvider valueProvider = new SimpleConstantValueProvider();

	protected LoadingCache<SootField, List<Type>> arrayToContentTypes = CacheBuilder.newBuilder()
			.build(new CacheLoader<SootField, List<Type>>() {

				@Override
				public List<Type> load(SootField field) throws Exception {
					// Find all assignments to this field
					List<Type> typeList = new ArrayList<>();
					field.getDeclaringClass().getMethods().stream().filter(m -> m.isConcrete())
							.map(m -> m.retrieveActiveBody()).forEach(b -> {
								// Find all locals that reference the field
								Set<Local> arrayLocals = new HashSet<>();
								for (Unit u : b.getUnits()) {
									if (u instanceof AssignStmt) {
										AssignStmt assignStmt = (AssignStmt) u;
										Value rop = assignStmt.getRightOp();
										Value lop = assignStmt.getLeftOp();
										if (rop instanceof FieldRef && ((FieldRef) rop).getField() == field) {
											arrayLocals.add((Local) lop);
										} else if (lop instanceof FieldRef && ((FieldRef) lop).getField() == field) {
											arrayLocals.add((Local) rop);
										}
									}
								}

								// Find casts
								for (Unit u : b.getUnits()) {
									if (u instanceof AssignStmt) {
										AssignStmt assignStmt = (AssignStmt) u;
										Value rop = assignStmt.getRightOp();
										Value lop = assignStmt.getLeftOp();

										if (rop instanceof CastExpr) {
											CastExpr ce = (CastExpr) rop;
											if (arrayLocals.contains(ce.getOp()))
												arrayLocals.add((Local) lop);
											else if (arrayLocals.contains(lop))
												arrayLocals.add((Local) ce.getOp());
										}
									}
								}

								// Find the assignments to the array locals
								for (Unit u : b.getUnits()) {
									if (u instanceof AssignStmt) {
										AssignStmt assignStmt = (AssignStmt) u;
										Value rop = assignStmt.getRightOp();
										Value lop = assignStmt.getLeftOp();
										if (lop instanceof ArrayRef) {
											ArrayRef arrayRef = (ArrayRef) lop;
											if (arrayLocals.contains(arrayRef.getBase())) {
												Type t = rop.getType();
												if (t instanceof RefType)
													typeList.add(rop.getType());
											}
										}
									}
								}
							});
					return typeList;
				}

			});

	private MultiMap<SootMethod, Stmt> javaScriptInterfaces = new HashMultiMap<SootMethod, Stmt>();

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
	 * @param androidCallbackFile The file from which to load the callback
	 *                            definitions
	 * @return A set containing the names of the interfaces that are used to
	 *         implement Android callback handlers
	 */
	private static Set<String> loadAndroidCallbacks(String androidCallbackFile) throws IOException {
		String fileName = androidCallbackFile;
		if (!new File(fileName).exists()) {
			fileName = "../soot-infoflow-android/AndroidCallbacks.txt";
			if (!new File(fileName).exists()) {
				try (InputStream is = ResourceUtils.getResourceStream("/AndroidCallbacks.txt")) {
					return loadAndroidCallbacks(new InputStreamReader(is));
				}
			}
		}
		try (FileReader fr = new FileReader(fileName)) {
			return loadAndroidCallbacks(fr);
		}
	}

	/**
	 * Loads the set of interfaces that are used to implement Android callback
	 * handlers from a file on disk
	 *
	 * @param reader A file reader
	 * @return A set containing the names of the interfaces that are used to
	 *         implement Android callback handlers
	 */
	public static Set<String> loadAndroidCallbacks(Reader reader) throws IOException {
		Set<String> androidCallbacks = new HashSet<String>();
		try (BufferedReader bufReader = new BufferedReader(reader)) {
			String line;
			while ((line = bufReader.readLine()) != null)
				if (!line.isEmpty())
					androidCallbacks.add(line);
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
	 * @param lifecycleElement The lifecycle element (activity, etc.) with which to
	 *                         associate the found callbacks
	 * @param method           The method in which to look for callbacks
	 */
	protected void analyzeMethodForCallbackRegistrations(SootClass lifecycleElement, SootMethod method) {
		// Do not analyze system classes
		if (SystemClassHandler.v().isClassInSystemPackage(method.getDeclaringClass()))
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
					final Type type = mref.getParameterType(i);
					if (!(type instanceof RefType))
						continue;
					String param = type.toString();
					if (androidCallbacks.contains(param)) {
						Value arg = iinv.getArg(i);

						// This call must be to a system API in order to
						// register an OS-level callback
						if (!SystemClassHandler.v()
								.isClassInSystemPackage(iinv.getMethod().getDeclaringClass()))
							continue;
						// We have a formal parameter type that corresponds to one of the Android
						// callback interfaces. Look for definitions of the parameter to estimate the
						// actual type.
						if (arg instanceof Local) {
							Set<Type> possibleTypes = Scene.v().getPointsToAnalysis().reachingObjects((Local) arg)
									.possibleTypes();
							// If we don't have pointsTo information, we take
							// the type of the local
							if (possibleTypes.isEmpty()) {
								Type argType = ((Local) arg).getType();
								checkAndAddCallback(callbackClasses, argType);
							} else {
								for (Type possibleType : possibleTypes)
									checkAndAddCallback(callbackClasses, possibleType);
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
	 * Adds the class that implements the given type to the set of callback classes.
	 * This method deals with <code>AnyType</code> types as well.
	 * 
	 * @param callbackClasses The set to which to add the callback classes
	 * @param argType         The type to add
	 */
	protected void checkAndAddCallback(Set<SootClass> callbackClasses, Type argType) {
		RefType baseType;
		if (argType instanceof RefType) {
			baseType = (RefType) argType;
			SootClass targetClass = baseType.getSootClass();
			if (!SystemClassHandler.v().isClassInSystemPackage(targetClass))
				callbackClasses.add(targetClass);
		} else if (argType instanceof AnySubType) {
			baseType = ((AnySubType) argType).getBase();
			SootClass baseClass = ((RefType) baseType).getSootClass();
			for (SootClass sc : TypeUtils.getAllDerivedClasses(baseClass)) {
				if (!SystemClassHandler.v().isClassInSystemPackage(sc))
					callbackClasses.add(sc);
			}
		} else {
			logger.warn("Unsupported type detected in callback analysis");
		}
	}

	/**
	 * Checks whether all filters accept the association between the callback class
	 * and its parent component
	 *
	 * @param lifecycleElement The hosting component's class
	 * @param targetClass      The class implementing the callbacks
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
	 * @param lifecycleElement The hosting component's class
	 * @param targetMethod     The method implementing the callback
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
	 * @param method The method to check
	 */
	protected void analyzeMethodForDynamicBroadcastReceiver(SootMethod method) {
		// Do not analyze system classes
		if (SystemClassHandler.v().isClassInSystemPackage(method.getDeclaringClass()))
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
				if (methodRef.getName().equals("registerReceiver") && iexpr.getArgCount() > 0
						&& fastHierarchy.canStoreType(methodRef.getDeclaringClass().getType(), contextType)) {
					Value br = iexpr.getArg(0);
					if (br.getType() instanceof RefType) {
						RefType rt = (RefType) br.getType();
						if (!SystemClassHandler.v().isClassInSystemPackage(rt.getSootClass()))
							dynamicManifestComponents.add(rt.getSootClass());
					}
				}
			}
		}
	}

	protected void analyzeMethodForJavascriptInterfaces(SootMethod method) {
		// Do not analyze system classes
		if (SystemClassHandler.v().isClassInSystemPackage(method.getDeclaringClass().getName()))
			return;
		if (!method.isConcrete() || !method.hasActiveBody())
			return;

		final FastHierarchy fastHierarchy = Scene.v().getFastHierarchy();
		final RefType webViewType = RefType.v("android.webkit.WebView");
		for (Unit u : method.getActiveBody().getUnits()) {
			Stmt stmt = (Stmt) u;
			if (stmt.containsInvokeExpr()) {
				final InvokeExpr iexpr = stmt.getInvokeExpr();
				final SootMethodRef methodRef = iexpr.getMethodRef();
				if (methodRef.getName().equals("addJavascriptInterface") && iexpr.getArgCount() == 2
						&& fastHierarchy.canStoreType(methodRef.getDeclaringClass().getType(), webViewType)) {
					this.javaScriptInterfaces.put(method, stmt);
				}
			}
		}
	}

	/**
	 * Checks whether the given method dynamically registers a new service
	 * connection
	 *
	 * @param method The method to check
	 */
	protected void analyzeMethodForServiceConnection(SootMethod method) {
		// Do not analyze system classes
		if (SystemClassHandler.v().isClassInSystemPackage(method.getDeclaringClass()))
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
								if (!SystemClassHandler.v().isClassInSystemPackage(rt.getSootClass()))
									dynamicManifestComponents.add(rt.getSootClass());
							}
						}
					}

					// Just to be sure, also add the declared type
					if (br.getType() instanceof RefType) {
						RefType rt = (RefType) br.getType();
						if (!SystemClassHandler.v().isClassInSystemPackage(rt.getSootClass()))
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
	 * @param method The method to check
	 */
	protected void analyzeMethodForFragmentTransaction(SootClass lifecycleElement, SootMethod method) {
		if (scFragment == null || scFragmentTransaction == null)
			if (scSupportFragment == null || scSupportFragmentTransaction == null)
				if (scAndroidXFragment == null || scAndroidXFragmentTransaction == null)
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
				if (methodName.equals("getFragmentManager") || methodName.equals("getSupportFragmentManager"))
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
						isFragmentTransaction |= scAndroidXFragmentTransaction != null && Scene.v().getFastHierarchy()
								.canStoreType(iinvExpr.getBase().getType(), scAndroidXFragmentTransaction.getType());
						isAddTransaction = stmt.getInvokeExpr().getMethod().getName().equals("add")
								|| stmt.getInvokeExpr().getMethod().getName().equals("replace");

						if (isFragmentTransaction && isAddTransaction) {
							// We take all fragments passed to the method
							for (int i = 0; i < stmt.getInvokeExpr().getArgCount(); i++) {
								Value br = stmt.getInvokeExpr().getArg(i);

								// Is this a fragment?
								if (br.getType() instanceof RefType) {
									RefType rt = (RefType) br.getType();
									if (br instanceof ClassConstant)
										rt = (RefType) ((ClassConstant) br).toSootType();

									boolean addFragment = scFragment != null
											&& Scene.v().getFastHierarchy().canStoreType(rt, scFragment.getType());
									addFragment |= scSupportFragment != null && Scene.v().getFastHierarchy()
											.canStoreType(rt, scSupportFragment.getType());
									addFragment |= scAndroidXFragment != null && Scene.v().getFastHierarchy()
											.canStoreType(rt, scAndroidXFragment.getType());
									if (addFragment)
										checkAndAddFragment(method.getDeclaringClass(), rt.getSootClass());
								}
							}
						}
					}
				}
			}
	}

	/**
	 * Check whether a method registers a FragmentStatePagerAdapter to a ViewPager.
	 * This pattern is very common for tabbed apps.
	 *
	 * @param clazz
	 * @param method
	 *
	 * @author Julius Naeumann
	 */
	protected void analyzeMethodForViewPagers(SootClass clazz, SootMethod method) {
		// We need at least one fragment base class
		if (scSupportViewPager == null && scAndroidXViewPager == null)
			return;
		// We need at least one class with a method to register a fragment
		if (scFragmentStatePagerAdapter == null && scAndroidXFragmentStatePagerAdapter == null
				&& scFragmentPagerAdapter == null && scAndroidXFragmentPagerAdapter == null)
			return;

		if (!method.isConcrete())
			return;

		Body body = method.retrieveActiveBody();

		// look for invocations of ViewPager.setAdapter
		for (Unit u : body.getUnits()) {
			Stmt stmt = (Stmt) u;
			if (!stmt.containsInvokeExpr())
				continue;

			InvokeExpr invExpr = stmt.getInvokeExpr();
			if (!(invExpr instanceof InstanceInvokeExpr))
				continue;
			InstanceInvokeExpr iinvExpr = (InstanceInvokeExpr) invExpr;

			// check whether class is of ViewPager type
			if (!safeIsType(iinvExpr.getBase(), scSupportViewPager)
					&& !safeIsType(iinvExpr.getBase(), scAndroidXViewPager))
				continue;

			// check whether setAdapter method is called
			if (!stmt.getInvokeExpr().getMethod().getName().equals("setAdapter")
					|| stmt.getInvokeExpr().getArgCount() != 1)
				continue;

			// get argument
			Value pa = stmt.getInvokeExpr().getArg(0);
			if (!(pa.getType() instanceof RefType))
				continue;
			RefType rt = (RefType) pa.getType();

			// check whether argument is of type FragmentStatePagerAdapter
			if (!safeIsType(pa, scFragmentStatePagerAdapter) && !safeIsType(pa, scAndroidXFragmentStatePagerAdapter)
					&& !safeIsType(pa, scFragmentPagerAdapter) && !safeIsType(pa, scAndroidXFragmentPagerAdapter))
				continue;

			// now analyze getItem() to find possible Fragments
			SootMethod getItem = rt.getSootClass().getMethodUnsafe("android.support.v4.app.Fragment getItem(int)");
			if (getItem == null)
				getItem = rt.getSootClass().getMethodUnsafe("androidx.fragment.app.Fragment getItem(int)");
			if (getItem == null || !getItem.isConcrete())
				continue;

			Body b = getItem.retrieveActiveBody();
			if (b == null)
				continue;

			// iterate and add any returned Fragment classes
			for (Unit getItemUnit : b.getUnits()) {
				if (getItemUnit instanceof ReturnStmt) {
					ReturnStmt rs = (ReturnStmt) getItemUnit;
					Value rv = rs.getOp();
					Type type = rv.getType();
					if (type instanceof RefType) {
						SootClass rtClass = ((RefType) type).getSootClass();
						if (rv instanceof Local && (rtClass.getName().startsWith("android.")
								|| rtClass.getName().startsWith("androidx.")))
							analyzeFragmentCandidates(rs, getItem, (Local) rv);
						else
							checkAndAddFragment(method.getDeclaringClass(), rtClass);
					}
				}
			}
		}
	}

	/**
	 * Attempts to find fragments that are not returned immediately, but that
	 * require a more complex backward analysis. This analysis is best-effort, we do
	 * not attempt to solve every possible case.
	 * 
	 * @param s The statement at which the fragment is returned
	 * @param m The method in which the fragment is returned
	 * @param l The local that contains the fragment
	 */
	private void analyzeFragmentCandidates(Stmt s, SootMethod m, Local l) {
		ExceptionalUnitGraph g = ExceptionalUnitGraphFactory.createExceptionalUnitGraph(m.getActiveBody());
		SimpleLocalDefs lds = new SimpleLocalDefs(g);

		List<Pair<Local, Stmt>> toSearch = new ArrayList<>();
		Set<Pair<Local, Stmt>> doneSet = new HashSet<>();
		toSearch.add(new Pair<>(l, s));

		while (!toSearch.isEmpty()) {
			Pair<Local, Stmt> pair = toSearch.remove(0);
			if (doneSet.add(pair)) {
				List<Unit> defs = lds.getDefsOfAt(pair.getO1(), pair.getO2());
				for (Unit def : defs) {
					if (def instanceof AssignStmt) {
						AssignStmt assignStmt = (AssignStmt) def;
						Value rop = assignStmt.getRightOp();
						if (rop instanceof ArrayRef) {
							ArrayRef arrayRef = (ArrayRef) rop;

							// Look for all assignments to the array
							toSearch.add(new Pair<>((Local) arrayRef.getBase(), assignStmt));
						} else if (rop instanceof FieldRef) {
							FieldRef fieldRef = (FieldRef) rop;
							try {
								List<Type> typeList = arrayToContentTypes.get(fieldRef.getField());
								typeList.stream().map(t -> ((RefType) t).getSootClass())
										.forEach(c -> checkAndAddFragment(m.getDeclaringClass(), c));
							} catch (ExecutionException e) {
								logger.error(String.format("Could not load potential types for field %s",
										fieldRef.getField().getSignature()), e);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Checks whether the given value is of the type of the given class
	 * 
	 * @param val   The value to check
	 * @param clazz The class from which to get the type
	 * @return True if the given value is of the type of the given class
	 */
	private boolean safeIsType(Value val, SootClass clazz) {
		return clazz != null && Scene.v().getFastHierarchy().canStoreType(val.getType(), clazz.getType());
	}

	/**
	 * Gets whether the call in the given statement can end up in the respective
	 * method inherited from one of the given classes.
	 *
	 * @param stmt       The statement containing the call sites
	 * @param classNames The base classes in which the call can potentially end up
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
	 * @param inv The invocaton to check
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
			final String curClassName = curClass.getName();
			if (curClassName.equals("android.app.Activity")
					|| curClassName.equals("android.support.v7.app.ActionBarActivity")
					|| curClassName.equals("android.support.v7.app.AppCompatActivity")
					|| curClassName.equals("androidx.appcompat.app.AppCompatActivity"))
				return true;
			// As long as the class is subclass of android.app.Activity,
			// it can be sure that the setContentView method is what we expected.
			// Following 2 statements make the overriding of method
			// setContentView ignored.
			// if (curClass.declaresMethod("void setContentView(int)"))
			// return false;
			curClass = curClass.hasSuperclass() ? curClass.getSuperclass() : null;
		}
		return false;
	}

	/**
	 * Checks whether this invocation calls Android's LayoutInflater.inflate method
	 *
	 * @param inv The invocaton to check
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
			final String curClassName = curClass.getName();
			if (curClassName.equals("android.app.Fragment") || curClassName.equals("android.view.LayoutInflater"))
				return true;
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
		if (config.getIgnoreFlowsInSystemPackages()
				&& SystemClassHandler.v().isClassInSystemPackage(sootClass))
			return;

		// There are also some classes that implement interesting callback
		// methods.
		// We model this as follows: Whenever the user overwrites a method in an
		// Android OS class, we treat it as a potential callback.
		Map<String, SootMethod> systemMethods = new HashMap<>(10000);
		for (SootClass parentClass : Scene.v().getActiveHierarchy().getSuperclassesOf(sootClass)) {
			if (SystemClassHandler.v().isClassInSystemPackage(parentClass))
				for (SootMethod sm : parentClass.getMethods())
					if (!sm.isConstructor())
						systemMethods.put(sm.getSubSignature(), sm);
		}

		// Iterate over all user-implemented methods. If they are inherited
		// from a system class, they are callback candidates.
		for (SootClass parentClass : Scene.v().getActiveHierarchy().getSuperclassesOfIncluding(sootClass)) {
			if (SystemClassHandler.v().isClassInSystemPackage(parentClass))
				continue;
			for (SootMethod method : parentClass.getMethods()) {
				if (!method.hasTag(SimulatedCodeElementTag.TAG_NAME)) {
					// Check whether this is a real callback method
					SootMethod parentMethod = systemMethods.get(method.getSubSignature());
					if (parentMethod != null) {
						if (checkAndAddMethod(method, parentMethod, sootClass, CallbackType.Default)) {
							// We only keep the latest override in the class hierarchy
							systemMethods.remove(parentMethod.getSubSignature());
						}
					}
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

	protected void analyzeClassInterfaceCallbacks(SootClass baseClass, SootClass sootClass,
			SootClass lifecycleElement) {
		// We cannot create instances of abstract classes anyway, so there is no
		// reason to look for interface implementations
		if (!baseClass.isConcrete())
			return;

		// Do not analyze system classes
		if (SystemClassHandler.v().isClassInSystemPackage(baseClass))
			return;
		if (SystemClassHandler.v().isClassInSystemPackage(sootClass))
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
			this.checkAndAddCallback(i, baseClass, lifecycleElement);
		}
		for (SootClass c : collectAllSuperClasses(sootClass)) {
			this.checkAndAddCallback(c, baseClass, lifecycleElement);
		}
	}

	/**
	 * Checks if the given class/interface appears in android Callbacks. If yes, add
	 * callback method to the list of callback methods
	 *
	 * @param sc               the class/interface to check for existence in
	 *                         AndroidCallbacks
	 * @param baseClass        the class implementing/extending sc
	 * @param lifecycleElement the component to which the callback method belongs
	 */
	private void checkAndAddCallback(SootClass sc, SootClass baseClass, SootClass lifecycleElement) {
		if (androidCallbacks.contains(sc.getName())) {
			CallbackType callbackType = isUICallback(sc) ? CallbackType.Widget : CallbackType.Default;
			for (SootMethod sm : sc.getMethods()) {
				SootMethod callbackImplementation = getMethodFromHierarchyEx(baseClass, sm.getSubSignature());
				if (callbackImplementation != null)
					checkAndAddMethod(callbackImplementation, sm, lifecycleElement, callbackType);
			}
		}
	}

	/**
	 * Gets whether the given callback interface or class represents a UI callback
	 *
	 * @param i The callback interface or class to check
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
	 * @param method         The method to check and add
	 * @param parentMethod   The original method in the Android framework that
	 *                       declared the callback. This can, for example, be the
	 *                       method in the interface.
	 * @param lifecycleClass The base class (activity, service, etc.) to which this
	 *                       callback method belongs
	 * @param callbackType   The type of callback to be registered
	 * @return True if the method is new, i.e., has not been seen before, otherwise
	 *         false
	 */
	protected boolean checkAndAddMethod(SootMethod method, SootMethod parentMethod, SootClass lifecycleClass,
			CallbackType callbackType) {
		// Do not call system methods
		if (SystemClassHandler.v().isClassInSystemPackage(method.getDeclaringClass()))
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

		return this.callbackMethods.put(lifecycleClass,
				new AndroidCallbackDefinition(method, parentMethod, callbackType));
	}

	/**
	 * Registers a fragment that belongs to a given component
	 *
	 * @param componentClass The component (usually an activity) to which the
	 *                       fragment belongs
	 * @param fragmentClass  The fragment class
	 */
	protected void checkAndAddFragment(SootClass componentClass, SootClass fragmentClass) {
		this.fragmentClasses.put(componentClass, fragmentClass);
		this.fragmentClassesRev.put(fragmentClass, componentClass);
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

	private Set<SootClass> collectAllSuperClasses(SootClass sootClass) {
		Set<SootClass> classes = new HashSet<SootClass>();
		if (sootClass.hasSuperclass()) {
			classes.add(sootClass.getSuperclass());
			classes.addAll(collectAllSuperClasses(sootClass.getSuperclass()));
		}
		return classes;
	}

	public MultiMap<SootClass, AndroidCallbackDefinition> getCallbackMethods() {
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
	 * @param filter The filter to add
	 */
	public void addCallbackFilter(ICallbackFilter filter) {
		this.callbackFilters.add(filter);
	}

	/**
	 * Excludes an entry point from all further processing. No more callbacks will
	 * be collected for the given entry point
	 *
	 * @param entryPoint The entry point to exclude
	 */
	public void excludeEntryPoint(SootClass entryPoint) {
		this.excludedEntryPoints.add(entryPoint);
	}

	/**
	 * Checks whether the given class is an excluded entry point
	 *
	 * @param entryPoint The entry point to check
	 * @return True if the given class is an excluded entry point, otherwise false
	 */
	public boolean isExcludedEntryPoint(SootClass entryPoint) {
		return this.excludedEntryPoints.contains(entryPoint);
	}

	/**
	 * Sets the provider that shall be used for obtaining constant values during the
	 * callback analysis
	 *
	 * @param valueProvider The value provider to use
	 */
	public void setValueProvider(IValueProvider valueProvider) {
		this.valueProvider = valueProvider;
	}

	/**
	 * Returns a set of all statements which add a javascript interface
	 * @return the statement list
	 */
	public MultiMap<SootMethod, Stmt> getJavaScriptInterfaces() {
		return javaScriptInterfaces;
	}

}
