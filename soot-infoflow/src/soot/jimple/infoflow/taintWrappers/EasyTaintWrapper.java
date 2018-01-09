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
package soot.jimple.infoflow.taintWrappers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import heros.TwoElementSet;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.infoflow.util.SystemClassHandler;

/**
 * A list of methods is passed which contains signatures of instance methods
 * that taint their base objects if they are called with a tainted parameter.
 * When a base object is tainted, all return values are tainted, too. For static
 * methods, only the return value is assumed to be tainted when the method is
 * called with a tainted parameter value.
 * 
 * @author Christian Fritz, Steven Arzt
 *
 */
public class EasyTaintWrapper extends AbstractTaintWrapper implements Cloneable {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<String, Set<String>> classList;
	private final Map<String, Set<String>> excludeList;
	private final Map<String, Set<String>> killList;
	private final Set<String> includeList;

	private LoadingCache<SootMethod, MethodWrapType> methodWrapCache = CacheBuilder.newBuilder()
			.build(new CacheLoader<SootMethod, MethodWrapType>() {

				@Override
				public MethodWrapType load(SootMethod arg0) throws Exception {
					return getMethodWrapType(arg0.getSubSignature(), arg0.getDeclaringClass());
				}

			});

	private boolean aggressiveMode = false;
	private boolean alwaysModelEqualsHashCode = true;

	/**
	 * The possible effects this taint wrapper can have on a method invocation
	 */
	private enum MethodWrapType {
		/**
		 * This method can create a new taint
		 */
		CreateTaint,
		/**
		 * This method can kill a taint
		 */
		KillTaint,
		/**
		 * This method has been explicitly excluded from taint wrapping, i.e., it
		 * neither creates nor kills taints even if the same method in the parent class
		 * or an interfaces does.
		 */
		Exclude,
		/**
		 * This method has not been named in the taint wrapper configuration
		 */
		NotRegistered
	}

	/**
	 * Creates a new instanceof the {@link EasyTaintWrapper} class. This constructor
	 * assumes that all classes are included and get wrapped. However, only the
	 * methods in the given map create new taints
	 * 
	 * @param classList
	 *            The method for which to create new taints. This is a mapping from
	 *            class names to sets of subsignatures.
	 */
	public EasyTaintWrapper(Map<String, Set<String>> classList) {
		this(classList, new HashMap<String, Set<String>>(), new HashMap<String, Set<String>>(), new HashSet<String>());
	}

	public EasyTaintWrapper(Map<String, Set<String>> classList, Map<String, Set<String>> excludeList) {
		this(classList, excludeList, new HashMap<String, Set<String>>(), new HashSet<String>());
	}

	public EasyTaintWrapper(Map<String, Set<String>> classList, Map<String, Set<String>> excludeList,
			Map<String, Set<String>> killList) {
		this(classList, excludeList, killList, new HashSet<String>());
	}

	public EasyTaintWrapper(Map<String, Set<String>> classList, Map<String, Set<String>> excludeList,
			Map<String, Set<String>> killList, Set<String> includeList) {
		this.classList = classList;
		this.excludeList = excludeList;
		this.killList = killList;
		this.includeList = includeList;
	}

	public EasyTaintWrapper() throws IOException {
		this(soot.jimple.infoflow.util.ResourceUtils.getResourceStream("/EasyTaintWrapperSource.txt"));
	}

	public EasyTaintWrapper(String f) throws IOException {
		this(new FileReader(new File(f).getAbsoluteFile()));
	}

	public EasyTaintWrapper(InputStream stream) throws IOException {
		this(new InputStreamReader(stream));
	}

	public EasyTaintWrapper(Reader reader) throws IOException {
		BufferedReader bufReader = new BufferedReader(reader);
		try {
			String line = bufReader.readLine();
			List<String> methodList = new LinkedList<String>();
			List<String> excludeList = new LinkedList<String>();
			List<String> killList = new LinkedList<String>();
			this.includeList = new HashSet<String>();
			while (line != null) {
				if (!line.isEmpty() && !line.startsWith("%"))
					if (line.startsWith("~"))
						excludeList.add(line.substring(1));
					else if (line.startsWith("-"))
						killList.add(line.substring(1));
					else if (line.startsWith("^"))
						includeList.add(line.substring(1));
					else
						methodList.add(line);
				line = bufReader.readLine();
			}
			this.classList = SootMethodRepresentationParser.v().parseClassNames(methodList, true);
			this.excludeList = SootMethodRepresentationParser.v().parseClassNames(excludeList, true);
			this.killList = SootMethodRepresentationParser.v().parseClassNames(killList, true);
			logger.info("Loaded wrapper entries for {} classes and {} exclusions.", classList.size(),
					excludeList.size());
		} finally {
			bufReader.close();
		}
	}

	public EasyTaintWrapper(File f) throws IOException {
		this(new FileReader(f));
	}

	public EasyTaintWrapper(EasyTaintWrapper taintWrapper) {
		this(taintWrapper.classList, taintWrapper.excludeList, taintWrapper.killList, taintWrapper.includeList);
	}

	@Override
	public Set<AccessPath> getTaintsForMethodInternal(Stmt stmt, AccessPath taintedPath) {
		if (!stmt.containsInvokeExpr())
			return Collections.emptySet();

		final Set<AccessPath> taints = new HashSet<AccessPath>();
		final SootMethod method = stmt.getInvokeExpr().getMethod();

		// If the callee is a phantom class or has no body, we pass on the taint
		if (method.isPhantom() || !method.hasActiveBody()) {
			// Exception: Tainted value is overwritten
			if (!(!taintedPath.isStaticFieldRef()
					&& stmt instanceof DefinitionStmt
					&& ((DefinitionStmt) stmt).getLeftOp() == taintedPath.getPlainValue()))
				taints.add(taintedPath);
		}

		// For the moment, we don't implement static taints on wrappers. Pass it
		// on
		// not to break anything
		if (taintedPath.isStaticFieldRef())
			return Collections.singleton(taintedPath);

		// Do we handle equals() and hashCode() separately?
		final String subSig = stmt.getInvokeExpr().getMethodRef().getSubSignature().getString();
		boolean taintEqualsHashCode = alwaysModelEqualsHashCode
				&& (subSig.equals("boolean equals(java.lang.Object)") || subSig.equals("int hashCode()"));

		// We need to handle some API calls explicitly as they do not really fit
		// the model of our rules
		if (!taintedPath.isEmpty()
				&& method.getDeclaringClass().getName().equals("java.lang.String")
				&& subSig.equals("void getChars(int,int,char[],int)"))
			return handleStringGetChars(stmt.getInvokeExpr(), taintedPath);

		// If this is not one of the supported classes, we skip it
		boolean isSupported = includeList == null || includeList.isEmpty();
		if (!isSupported)
			for (String supportedClass : this.includeList)
				if (method.getDeclaringClass().getName().startsWith(supportedClass)) {
					isSupported = true;
					break;
				}
		if (!isSupported && !aggressiveMode && !taintEqualsHashCode)
			return taints;

		// Check for a cached wrap type
		final MethodWrapType wrapType = methodWrapCache.getUnchecked(method);

		if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr iiExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
			if (taintedPath.isEmpty() || iiExpr.getBase().equals(taintedPath.getPlainValue())) {
				// If the base object is tainted, we have to check whether we
				// must kill the taint
				if (wrapType == MethodWrapType.KillTaint)
					return Collections.emptySet();

				// If the base object is tainted, all calls to its methods
				// always return
				// tainted values
				if (stmt instanceof DefinitionStmt) {
					DefinitionStmt def = (DefinitionStmt) stmt;

					// Check for exclusions
					if (wrapType != MethodWrapType.Exclude && SystemClassHandler.isTaintVisible(taintedPath, method))
						taints.add(manager.getAccessPathFactory().createAccessPath(def.getLeftOp(), true));
				}

				// If the base object is tainted, we pass this taint on
				taints.add(taintedPath);
			}
		}

		// if param is tainted && classList contains classname && if list.
		// contains
		// signature of method -> add propagation
		if (isSupported && wrapType == MethodWrapType.CreateTaint) {
			// If we are inside a conditional, we always taint
			boolean doTaint = taintedPath.isEmpty();

			// Otherwise, we have to check whether we have a tainted parameter
			if (!doTaint)
				for (Value param : stmt.getInvokeExpr().getArgs()) {
					if (param.equals(taintedPath.getPlainValue())) {
						// If we call a method on an instance with a tainted
						// parameter, this
						// instance (base object) and (if it exists) any left
						// side of the
						// respective assignment is assumed to be tainted.
						if (!taintEqualsHashCode) {
							doTaint = true;
							break;
						}
					}
				}

			if (doTaint) {
				// If make sure to also taint the left side of an assignment
				// if the object just got tainted
				if (stmt instanceof DefinitionStmt)
					taints.add(
							manager.getAccessPathFactory().createAccessPath(((DefinitionStmt) stmt).getLeftOp(), true));

				// Taint the base object
				if (stmt.getInvokeExprBox().getValue() instanceof InstanceInvokeExpr)
					taints.add(manager.getAccessPathFactory().createAccessPath(
							((InstanceInvokeExpr) stmt.getInvokeExprBox().getValue()).getBase(), true));

				// The originally tainted parameter or base object as such
				// stays tainted
				taints.add(taintedPath);
			}
		}

		return taints;
	}

	/**
	 * Explicitly handles String.getChars() which does not really fit our
	 * declarative model
	 * 
	 * @param invokeExpr
	 *            The invocation of String.getChars()
	 * @param taintedPath
	 *            The tainted access path
	 * @return The set of new taints to pass on in the taint propagation
	 */
	private Set<AccessPath> handleStringGetChars(InvokeExpr invokeExpr, AccessPath taintedPath) {
		// If the base object is tainted, the third argument gets tainted as
		// well
		if (((InstanceInvokeExpr) invokeExpr).getBase() == taintedPath.getPlainValue())
			return new TwoElementSet<AccessPath>(taintedPath,
					manager.getAccessPathFactory().createAccessPath(invokeExpr.getArg(2), true));
		return Collections.singleton(taintedPath);
	}

	/**
	 * Checks whether at least one method in the given class is registered in the
	 * taint wrapper
	 * 
	 * @param parentClass
	 *            The class to check
	 * @param newTaints
	 *            Check the list for creating new taints
	 * @param killTaints
	 *            Check the list for killing taints
	 * @param excludeTaints
	 *            Check the list for excluding taints
	 * @return True if at least one method of the given class has been registered
	 *         with the taint wrapper, otherwise
	 */
	private boolean hasWrappedMethodsForClass(SootClass parentClass, boolean newTaints, boolean killTaints,
			boolean excludeTaints) {
		if (newTaints && classList.containsKey(parentClass.getName()))
			return true;
		if (excludeTaints && excludeList.containsKey(parentClass.getName()))
			return true;
		if (killTaints && killList.containsKey(parentClass.getName()))
			return true;
		return false;
	}

	/**
	 * Gets the type of action the taint wrapper shall perform on a given method
	 * 
	 * @param subSig
	 *            The subsignature of the method to look for
	 * @param parentClass
	 *            The parent class in which to start looking
	 * @return The type of action to be performed on the given method
	 */
	private MethodWrapType getMethodWrapType(String subSig, SootClass parentClass) {
		// If this is not one of the supported classes, we skip it
		boolean isSupported = false;
		for (String supportedClass : this.includeList)
			if (parentClass.getName().startsWith(supportedClass)) {
				isSupported = true;
				break;
			}

		// Do we always model equals() and hashCode()?
		if (alwaysModelEqualsHashCode
				&& (subSig.equals("boolean equals(java.lang.Object)") || subSig.equals("int hashCode()")))
			return MethodWrapType.CreateTaint;

		// Do not process unsupported classes
		if (!isSupported)
			return MethodWrapType.NotRegistered;

		if (parentClass.isInterface())
			return getInterfaceWrapType(subSig, parentClass);
		else {
			// We have to walk up the hierarchy to also include all methods
			// registered for superclasses
			List<SootClass> superclasses = Scene.v().getActiveHierarchy().getSuperclassesOfIncluding(parentClass);
			for (SootClass sclass : superclasses) {
				MethodWrapType wtClass = getMethodWrapTypeDirect(sclass.getName(), subSig);
				if (wtClass != MethodWrapType.NotRegistered)
					return wtClass;

				for (SootClass ifc : sclass.getInterfaces()) {
					MethodWrapType wtIface = getInterfaceWrapType(subSig, ifc);
					if (wtIface != MethodWrapType.NotRegistered)
						return wtIface;
				}
			}
		}

		return MethodWrapType.NotRegistered;
	}

	/**
	 * Checks whether the taint wrapper has an entry for the given combination of
	 * class/interface and method subsignature. This method does not take the
	 * hierarchy into account.
	 * 
	 * @param className
	 *            The name of the class to look for
	 * @param subSignature
	 *            The method subsignature to look for
	 * @return The type of wrapping if the taint wrapper has been configured with
	 *         the given class or interface name and method subsignature, otherwise
	 *         NotRegistered.
	 */
	private MethodWrapType getMethodWrapTypeDirect(String className, String subSignature) {
		if (alwaysModelEqualsHashCode
				&& (subSignature.equals("boolean equals(java.lang.Object)") || subSignature.equals("int hashCode()")))
			return MethodWrapType.CreateTaint;

		Set<String> cEntries = classList.get(className);
		Set<String> eEntries = excludeList.get(className);
		Set<String> kEntries = killList.get(className);

		if (cEntries != null && cEntries.contains(subSignature))
			return MethodWrapType.CreateTaint;
		if (eEntries != null && eEntries.contains(subSignature))
			return MethodWrapType.Exclude;
		if (kEntries != null && kEntries.contains(subSignature))
			return MethodWrapType.KillTaint;
		return MethodWrapType.NotRegistered;
	}

	/**
	 * Checks whether the taint wrapper has been configured for the given method in
	 * the given interface or one of its parent interfaces.
	 * 
	 * @param subSig
	 *            The method subsignature to look for
	 * @param ifc
	 *            The interface where to start the search
	 * @return The configured type of wrapping if the given method is implemented in
	 *         the given interface or one of its super interfaces, otherwise
	 *         NotRegistered
	 */
	private MethodWrapType getInterfaceWrapType(String subSig, SootClass ifc) {
		if (ifc.isPhantom())
			return getMethodWrapTypeDirect(ifc.getName(), subSig);

		assert ifc.isInterface() : "Class "
				+ ifc.getName()
				+ " is not an interface, though returned "
				+ "by getInterfaces().";
		for (SootClass pifc : Scene.v().getActiveHierarchy().getSuperinterfacesOfIncluding(ifc)) {
			MethodWrapType wt = getMethodWrapTypeDirect(pifc.getName(), subSig);
			if (wt != MethodWrapType.NotRegistered)
				return wt;
		}
		return MethodWrapType.NotRegistered;
	}

	@Override
	public boolean isExclusiveInternal(Stmt stmt, AccessPath taintedPath) {
		SootMethod method = stmt.getInvokeExpr().getMethod();

		// Do we have an entry for at least one entry in the given class?
		if (hasWrappedMethodsForClass(method.getDeclaringClass(), true, true, false))
			return true;

		// In aggressive mode, we always taint the return value if the base
		// object is tainted.
		if (aggressiveMode && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr iiExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
			if (iiExpr.getBase().equals(taintedPath.getPlainValue()))
				return true;
		}

		final MethodWrapType wrapType = methodWrapCache.getUnchecked(method);
		return wrapType != MethodWrapType.NotRegistered;
	}

	/**
	 * Sets whether the taint wrapper shall always assume the return value of a call
	 * "a = x.foo()" to be tainted if the base object is tainted, even if the
	 * respective method is not in the data file.
	 * 
	 * @param aggressiveMode
	 *            True if return values shall always be tainted if the base object
	 *            on which the method is invoked is tainted, otherwise false
	 */
	public void setAggressiveMode(boolean aggressiveMode) {
		this.aggressiveMode = aggressiveMode;
	}

	/**
	 * Gets whether the taint wrapper shall always consider return values as tainted
	 * if the base object of the respective invocation is tainted
	 * 
	 * @return True if return values shall always be tainted if the base object on
	 *         which the method is invoked is tainted, otherwise false
	 */
	public boolean getAggressiveMode() {
		return this.aggressiveMode;
	}

	/**
	 * Sets whether the equals() and hashCode() methods shall always be modeled,
	 * regardless of the target type.
	 * 
	 * @param alwaysModelEqualsHashCode
	 *            True if the equals() and hashCode() methods shall always be
	 *            modeled, regardless of the target type, otherwise false
	 */
	public void setAlwaysModelEqualsHashCode(boolean alwaysModelEqualsHashCode) {
		this.alwaysModelEqualsHashCode = alwaysModelEqualsHashCode;
	}

	/**
	 * Gets whether the equals() and hashCode() methods shall always be modeled,
	 * regardless of the target type.
	 * 
	 * @return True if the equals() and hashCode() methods shall always be modeled,
	 *         regardless of the target type, otherwise false
	 */
	public boolean getAlwaysModelEqualsHashCode() {
		return this.alwaysModelEqualsHashCode;
	}

	/**
	 * Registers a prefix of class names to be included when generating taints. All
	 * classes whose names don't start with a registered prefix will be skipped.
	 * 
	 * @param prefix
	 *            The prefix to register
	 */
	public void addIncludePrefix(String prefix) {
		this.includeList.add(prefix);
	}

	/**
	 * Adds a method to which the taint wrapping rules shall apply
	 * 
	 * @param className
	 *            The class containing the method to be wrapped
	 * @param subSignature
	 *            The subsignature of the method to be wrapped
	 */
	public void addMethodForWrapping(String className, String subSignature) {
		Set<String> methods = this.classList.get(className);
		if (methods == null) {
			methods = new HashSet<String>();
			this.classList.put(className, methods);
		}
		methods.add(subSignature);
	}

	@Override
	public EasyTaintWrapper clone() {
		return new EasyTaintWrapper(this);
	}

	@Override
	public boolean supportsCallee(SootMethod method) {
		// Be conservative in aggressive mode
		if (aggressiveMode)
			return true;

		// Check for special models
		final String subSig = method.getSubSignature();
		if (alwaysModelEqualsHashCode
				&& (subSig.equals("boolean equals(java.lang.Object)") || subSig.equals("int hashCode()")))
			return true;

		for (String supportedClass : this.includeList)
			if (method.getDeclaringClass().getName().startsWith(supportedClass))
				return true;
		return false;
	}

	@Override
	public boolean supportsCallee(Stmt callSite) {
		// We need an invocation expression
		if (!callSite.containsInvokeExpr())
			return false;

		SootMethod method = callSite.getInvokeExpr().getMethod();
		if (!supportsCallee(method))
			return false;

		// We need a method that can create a taint
		if (!aggressiveMode) {
			// Check for a cached wrap type
			final MethodWrapType wrapType = methodWrapCache.getUnchecked(method);
			if (wrapType != MethodWrapType.CreateTaint)
				return false;
		}

		// We need at least one non-constant argument or a tainted base
		if (callSite.getInvokeExpr() instanceof InstanceInvokeExpr)
			return true;
		for (Value val : callSite.getInvokeExpr().getArgs())
			if (!(val instanceof Constant))
				return true;
		return false;
	}

	@Override
	public Set<Abstraction> getAliasesForMethod(Stmt stmt, Abstraction d1, Abstraction taintedPath) {
		// We do not provide any aliases
		return null;
	}

	/**
	 * Attempts to locate the definition file for the easy taint wrapper in its
	 * default location
	 * 
	 * @return The default definition file for the easy taint wrapper if it could be
	 *         found, otherwise null
	 */
	public static File locateDefaultDefinitionFile() {
		// GIT project structure
		File twSourceFile = new File("../soot-infoflow/EasyTaintWrapperSource.txt");
		if (twSourceFile.exists())
			return twSourceFile;

		// Current folder
		twSourceFile = new File("EasyTaintWrapperSource.txt");
		if (twSourceFile.exists())
			return twSourceFile;

		// Give up.
		return null;
	}

}
