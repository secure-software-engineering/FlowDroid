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
package soot.jimple.infoflow.android.source;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.InfoflowConfiguration.LayoutMatchingMode;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition.CallbackType;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointUtils;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.ResPackage;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.callbacks.CallbackDefinition;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType;
import soot.jimple.infoflow.sourcesSinks.manager.BaseSourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.IOneSourceAtATimeManager;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.scalar.ConstantPropagatorAndFolder;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.Tag;

/**
 * SourceManager implementation for AndroidSources
 *
 * @author Steven Arzt
 */
public class AndroidSourceSinkManager extends BaseSourceSinkManager
		implements ISourceSinkManager, IOneSourceAtATimeManager {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected final static String Activity_FindViewById = "<android.app.Activity: android.view.View findViewById(int)>";
	protected final static String View_FindViewById = "<android.view.View: android.view.View findViewById(int)>";

	protected SootMethod smActivityFindViewById;
	protected SootMethod smViewFindViewById;

	protected final Map<Integer, AndroidLayoutControl> layoutControls;
	protected List<ARSCFileParser.ResPackage> resourcePackages;
	protected String appPackageName = "";
	protected final Set<SootMethod> analyzedLayoutMethods = new HashSet<SootMethod>();
	protected SootClass[] iccBaseClasses = null;
	protected AndroidEntryPointUtils entryPointUtils = new AndroidEntryPointUtils();

	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with
	 * either strong or weak matching.
	 *
	 * @param sources The list of source methods
	 * @param sinks   The list of sink methods
	 * @param config  The configuration of the data flow analyzer
	 */
	public AndroidSourceSinkManager(Collection<? extends ISourceSinkDefinition> sources,
			Collection<? extends ISourceSinkDefinition> sinks, InfoflowAndroidConfiguration config) {
		this(sources, sinks, Collections.<AndroidCallbackDefinition>emptySet(), config, null);
	}

	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with
	 * strong matching, i.e. the methods in the code must exactly match those in the
	 * list.
	 *
	 * @param sources         The list of source methods
	 * @param sinks           The list of sink methods
	 * @param callbackMethods The list of callback methods whose parameters are
	 *                        sources through which the application receives data
	 *                        from the operating system
	 * @param weakMatching    True for weak matching: If an entry in the list has no
	 *                        return type, it matches arbitrary return types if the
	 *                        rest of the method signature is compatible. False for
	 *                        strong matching: The method signature in the code
	 *                        exactly match the one in the list.
	 * @param config          The configuration of the data flow analyzer
	 * @param layoutControls  A map from reference identifiers to the respective
	 *                        Android layout controls
	 */
	public AndroidSourceSinkManager(Collection<? extends ISourceSinkDefinition> sources,
			Collection<? extends ISourceSinkDefinition> sinks, Set<AndroidCallbackDefinition> callbackMethods,
			InfoflowAndroidConfiguration config, Map<Integer, AndroidLayoutControl> layoutControls) {
		super(sources, sinks, callbackMethods, config);
		this.layoutControls = layoutControls;
	}

	@Override
	public void initialize() {
		super.initialize();

		// Get some frequently-used methods
		this.smActivityFindViewById = Scene.v().grabMethod(Activity_FindViewById);
		this.smViewFindViewById = Scene.v().grabMethod(View_FindViewById);

		// For ICC methods (e.g., startService), the classes name of these
		// methods may change through user's definition. We match all the
		// ICC methods through their base class name.
		if (iccBaseClasses == null)
			iccBaseClasses = new SootClass[] { Scene.v().getSootClass("android.content.Context"), // activity,
					// service
					// and
					// broadcast
					Scene.v().getSootClass("android.content.ContentResolver"), // provider
					Scene.v().getSootClass("android.app.Activity") // some
					// methods
					// (e.g.,
					// onActivityResult)
					// only
					// defined
					// in
					// Activity
					// class
			};

	}

	/**
	 * Finds the given resource in the given package
	 *
	 * @param resName     The name of the resource to retrieve
	 * @param resID
	 * @param packageName The name of the package in which to look for the resource
	 * @return The specified resource if available, otherwise null
	 */
	private AbstractResource findResource(String resName, String resID, String packageName) {
		// Find the correct package
		for (ARSCFileParser.ResPackage pkg : this.resourcePackages) {
			// If we don't have any package specification, we pick the app's
			// default package
			boolean matches = (packageName == null || packageName.isEmpty())
					&& pkg.getPackageName().equals(this.appPackageName);
			matches |= pkg.getPackageName().equals(packageName);
			if (!matches)
				continue;

			// We have found a suitable package, now look for the resource
			for (ARSCFileParser.ResType type : pkg.getDeclaredTypes())
				if (type.getTypeName().equals(resID)) {
					AbstractResource res = type.getFirstResource(resName);
					return res;
				}
		}
		return null;
	}

	/**
	 * Finds the last assignment to the given local representing a resource ID by
	 * searching upwards from the given statement
	 *
	 * @param stmt  The statement from which to look backwards
	 * @param local The variable for which to look for assignments
	 * @return The last value assigned to the given variable
	 */
	private Integer findLastResIDAssignment(Stmt stmt, Local local, BiDiInterproceduralCFG<Unit, SootMethod> cfg,
			Set<Stmt> doneSet) {
		if (!doneSet.add(stmt))
			return null;

		// If this is an assign statement, we need to check whether it changes
		// the variable we're looking for
		if (stmt instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) stmt;
			if (assign.getLeftOp() == local) {
				// ok, now find the new value from the right side
				if (assign.getRightOp() instanceof IntConstant)
					return ((IntConstant) assign.getRightOp()).value;
				else if (assign.getRightOp() instanceof FieldRef) {
					SootField field = ((FieldRef) assign.getRightOp()).getField();
					for (Tag tag : field.getTags())
						if (tag instanceof IntegerConstantValueTag)
							return ((IntegerConstantValueTag) tag).getIntValue();
						else
							logger.error(String.format("Constant %s was of unexpected type", field.toString()));
				} else if (assign.getRightOp() instanceof InvokeExpr) {
					InvokeExpr inv = (InvokeExpr) assign.getRightOp();
					if (inv.getMethod().getName().equals("getIdentifier")
							&& inv.getMethod().getDeclaringClass().getName().equals("android.content.res.Resources")
							&& this.resourcePackages != null) {
						// The right side of the assignment is a call into the
						// well-known
						// Android API method for resource handling
						if (inv.getArgCount() != 3) {
							logger.error(String.format("Invalid parameter count (%d) for call to getIdentifier",
									inv.getArgCount()));
							return null;
						}

						// Find the parameter values
						String resName = "";
						String resID = "";
						String packageName = "";

						// In the trivial case, these values are constants
						if (inv.getArg(0) instanceof StringConstant)
							resName = ((StringConstant) inv.getArg(0)).value;
						if (inv.getArg(1) instanceof StringConstant)
							resID = ((StringConstant) inv.getArg(1)).value;

						Value thirdArg = inv.getArg(2);
						if (thirdArg instanceof StringConstant)
							packageName = ((StringConstant) thirdArg).value;
						else if (thirdArg instanceof Local)
							packageName = findLastStringAssignment(stmt, (Local) thirdArg, cfg);
						else if (thirdArg instanceof NullConstant)
							return null;
						else {
							logger.error(String.format("Unknown parameter type %s in call to getIdentifier",
									inv.getArg(2).getClass().getName()));
							return null;
						}

						// Find the resource
						ARSCFileParser.AbstractResource res = findResource(resName, resID, packageName);
						if (res != null)
							return res.getResourceID();
					}
				}
			}
		}

		// Continue the search upwards
		for (Unit pred : cfg.getPredsOf(stmt)) {
			if (!(pred instanceof Stmt))
				continue;
			Integer lastAssignment = findLastResIDAssignment((Stmt) pred, local, cfg, doneSet);
			if (lastAssignment != null)
				return lastAssignment;
		}
		return null;
	}

	/**
	 * Sets the resource packages to be used for finding sensitive layout controls
	 * as sources
	 *
	 * @param resourcePackages The resource packages to be used for looking up
	 *                         layout controls
	 */
	public void setResourcePackages(List<ResPackage> resourcePackages) {
		this.resourcePackages = resourcePackages;
	}

	/**
	 * Sets the name of the app's base package
	 *
	 * @param appPackageName The name of the app's base package
	 */
	public void setAppPackageName(String appPackageName) {
		this.appPackageName = appPackageName;
	}

	/**
	 * Finds the last assignment to the given String local by searching upwards from
	 * the given statement
	 *
	 * @param stmt  The statement from which to look backwards
	 * @param local The variable for which to look for assignments
	 * @return The last value assigned to the given variable
	 */
	private String findLastStringAssignment(Stmt stmt, Local local, BiDiInterproceduralCFG<Unit, SootMethod> cfg) {
		LinkedList<Stmt> workList = new LinkedList<Stmt>();
		Set<Stmt> seen = new HashSet<Stmt>();
		workList.add(stmt);
		while (!workList.isEmpty()) {
			stmt = workList.removeFirst();

			if (stmt instanceof AssignStmt) {
				AssignStmt assign = (AssignStmt) stmt;
				if (assign.getLeftOp() == local) {
					// ok, now find the new value from the right side
					if (assign.getRightOp() instanceof StringConstant)
						return ((StringConstant) assign.getRightOp()).value;
				}
			}

			// Continue the search upwards
			for (Unit pred : cfg.getPredsOf(stmt)) {
				if (!(pred instanceof Stmt))
					continue;

				Stmt s = (Stmt) pred;
				if (seen.add(s))
					workList.add(s);
			}
		}
		return null;
	}

	/**
	 * Gets the layout control that is referenced at the given call site
	 * 
	 * @param sCallSite A call to <code>findViewById()</code> or a similar method
	 * @param cfg       The bidirectional control flow graph
	 * @return The layout control that is being accessed at the given statement, or
	 *         <code>null</code> if no such control could be found
	 */
	protected AndroidLayoutControl getLayoutControl(Stmt sCallSite, IInfoflowCFG cfg) {
		// If we don't have a layout control list, we cannot perform any
		// more specific checks
		if (this.layoutControls == null)
			return null;

		// Perform a constant propagation inside this method exactly
		// once
		SootMethod uiMethod = cfg.getMethodOf(sCallSite);
		if (analyzedLayoutMethods.add(uiMethod))
			ConstantPropagatorAndFolder.v().transform(uiMethod.getActiveBody());

		// If we match specific controls, we need to get the ID of
		// control and look up the respective data object
		InvokeExpr iexpr = sCallSite.getInvokeExpr();
		if (iexpr.getArgCount() != 1) {
			logger.error("Framework method call with unexpected number of arguments");
			return null;
		}

		Integer id = valueProvider.getValue(uiMethod, sCallSite, iexpr.getArg(0), Integer.class);
		if (id == null && iexpr.getArg(0) instanceof Local) {
			id = findLastResIDAssignment(sCallSite, (Local) iexpr.getArg(0), cfg,
					new HashSet<Stmt>(cfg.getMethodOf(sCallSite).getActiveBody().getUnits().size()));
		}
		if (id == null) {
			logger.debug("Could not find assignment to local " + ((Local) iexpr.getArg(0)).getName() + " in method "
					+ cfg.getMethodOf(sCallSite).getSignature());
			return null;
		}

		AndroidLayoutControl control = this.layoutControls.get(id);
		if (control == null)
			return null;
		return control;
	}

	@Override
	protected ISourceSinkDefinition getUISourceDefinition(Stmt sCallSite, IInfoflowCFG cfg) {
		// If we match input controls, we need to check whether this is a call
		// to one of the well-known resource handling functions in Android
		if (sourceSinkConfig.getLayoutMatchingMode() == LayoutMatchingMode.NoMatch || !sCallSite.containsInvokeExpr())
			return null;

		// If nobody cares about the value obtained from the UI, we can ignore
		// the call
		if (!(sCallSite instanceof AssignStmt))
			return null;

		InvokeExpr ie = sCallSite.getInvokeExpr();
		SootMethod callee = ie.getMethod();

		// Is this a call to resource-handling method?
		boolean isResourceCall = callee == smActivityFindViewById || callee == smViewFindViewById;
		if (!isResourceCall) {
			for (SootMethod cfgCallee : cfg.getCalleesOfCallAt(sCallSite)) {
				if (cfgCallee == smActivityFindViewById || cfgCallee == smViewFindViewById) {
					isResourceCall = true;
					break;
				}
			}
		}

		// We need special treatment for the Android support classes
		if (!isResourceCall) {
			if ((callee.getDeclaringClass().getName().startsWith("android.support.v")
					|| callee.getDeclaringClass().getName().startsWith("androidx."))
					&& callee.getSubSignature().equals(smActivityFindViewById.getSubSignature()))
				isResourceCall = true;
		}

		if (isResourceCall) {
			// If we match all controls, we don't care about the specific
			// control we're dealing with
			if (sourceSinkConfig.getLayoutMatchingMode() == LayoutMatchingMode.MatchAll) {
				return MethodSourceSinkDefinition.createReturnSource(CallType.MethodCall);
			}

			AndroidLayoutControl control = getLayoutControl(sCallSite, cfg);
			if (control != null) {
				if (sourceSinkConfig.getLayoutMatchingMode() == LayoutMatchingMode.MatchSensitiveOnly
						&& control.isSensitive()) {
					return control.getSourceDefinition();
				}
			}
		}
		return null;
	}

	@Override
	protected boolean isEntryPointMethod(SootMethod method) {
		return entryPointUtils.isEntryPointMethod(method);
	}

	@Override
	protected ISourceSinkDefinition getSinkDefinition(Stmt sCallSite, InfoflowManager manager, AccessPath ap) {
		ISourceSinkDefinition definition = super.getSinkDefinition(sCallSite, manager, ap);
		if (definition != null)
			return definition;

		if (sCallSite.containsInvokeExpr()) {
			final SootMethod callee = sCallSite.getInvokeExpr().getMethod();
			final String subSig = callee.getSubSignature();
			final SootClass sc = callee.getDeclaringClass();

			// Do not consider ICC methods as sinks if only the base object is
			// tainted
			boolean isParamTainted = false;
			if (ap != null) {
				if (!sc.isInterface() && !ap.isStaticFieldRef()) {
					for (int i = 0; i < sCallSite.getInvokeExpr().getArgCount(); i++) {
						if (sCallSite.getInvokeExpr().getArg(i) == ap.getPlainValue()) {
							isParamTainted = true;
							break;
						}
					}
				}
			}

			if (isParamTainted || ap == null) {
				for (SootClass clazz : iccBaseClasses) {
					if (Scene.v().getOrMakeFastHierarchy().isSubclass(sc, clazz)) {
						SootMethod sm = clazz.getMethodUnsafe(subSig);
						if (sm != null) {
							ISourceSinkDefinition def = this.sinkMethods.get(sm);
							if (def != null)
								return def;
							break;
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	protected CallbackDefinition getCallbackDefinition(SootMethod method) {
		CallbackDefinition def = super.getCallbackDefinition(method);
		if (def instanceof AndroidCallbackDefinition) {
			AndroidCallbackDefinition d = (AndroidCallbackDefinition) def;
			// If this is a UI element, we only consider it as a
			// source if we actually want to taint all UI elements
			if (d.getCallbackType() == CallbackType.Widget
					&& sourceSinkConfig.getLayoutMatchingMode() != LayoutMatchingMode.MatchAll)
				return null;
		}
		return def;
	}
}
