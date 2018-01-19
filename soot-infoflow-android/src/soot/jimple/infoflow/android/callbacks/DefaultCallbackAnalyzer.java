package soot.jimple.infoflow.android.callbacks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Kind;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.callbacks.CallbackDefinition.CallbackType;
import soot.jimple.infoflow.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.infoflow.entryPointCreators.AndroidEntryPointUtils;
import soot.jimple.infoflow.memory.IMemoryBoundedSolver;
import soot.jimple.infoflow.memory.ISolverTerminationReason;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.EdgePredicate;
import soot.jimple.toolkits.callgraph.Filter;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.callgraph.Targets;
import soot.util.HashMultiMap;
import soot.util.MultiMap;
import soot.util.queue.ChunkedQueue;
import soot.util.queue.QueueReader;

/**
 * Default implementation of the callback analyzer class. This implementation
 * aims for precision. It tries to rule out callbacks registered in unreachable
 * code. The mapping between components and callbacks is as precise as possible.
 * 
 * @author Steven Arzt
 *
 */
public class DefaultCallbackAnalyzer extends AbstractCallbackAnalyzer implements IMemoryBoundedSolver {

	private MultiMap<SootClass, SootMethod> callbackWorklist = null;
	private AndroidEntryPointUtils entryPointUtils = new AndroidEntryPointUtils();
	private Set<IMemoryBoundedSolverStatusNotification> notificationListeners = new HashSet<>();
	private ISolverTerminationReason isKilled = null;

	public DefaultCallbackAnalyzer(InfoflowAndroidConfiguration config, Set<SootClass> entryPointClasses)
			throws IOException {
		super(config, entryPointClasses);
	}

	public DefaultCallbackAnalyzer(InfoflowAndroidConfiguration config, Set<SootClass> entryPointClasses,
			String callbackFile) throws IOException {
		super(config, entryPointClasses, callbackFile);
	}

	public DefaultCallbackAnalyzer(InfoflowAndroidConfiguration config, Set<SootClass> entryPointClasses,
			Set<String> androidCallbacks) throws IOException {
		super(config, entryPointClasses, androidCallbacks);
	}

	/**
	 * Helper class that computes reachable methods only from a given set of
	 * starting methods, while ignoring all other methods. It will not take
	 * everything that is already reachable in the callgraph as the seed, but
	 * only work on what is explicitly given as the entry points.
	 * 
	 * This class is heavily based on Soot's {@link ReachableMethods} class by
	 * Ondrej Lhotak.
	 * 
	 * @author Steven Arzt
	 *
	 */
	protected class MyReachableMethods {

		private final SootClass originalComponent;
		private final Set<MethodOrMethodContext> set = new HashSet<MethodOrMethodContext>();
		private final ChunkedQueue<MethodOrMethodContext> reachables = new ChunkedQueue<MethodOrMethodContext>();
		private final QueueReader<MethodOrMethodContext> allReachables = reachables.reader();
		private QueueReader<MethodOrMethodContext> unprocessedMethods;

		/**
		 * Creates a new instance of the {@link MyReachableMethods} class
		 * 
		 * @param originalComponent
		 *            The original component or which we are looking for
		 *            callback registrations. This information is used to more
		 *            precisely model calls to abstract methods.
		 * @param entryPoints
		 *            The entry points from which to find the reachable methods
		 */
		public MyReachableMethods(SootClass originalComponent, Collection<MethodOrMethodContext> entryPoints) {
			this.originalComponent = originalComponent;
			this.unprocessedMethods = reachables.reader();
			addMethods(entryPoints.iterator());
		}

		private void addMethods(Iterator<MethodOrMethodContext> methods) {
			while (methods.hasNext())
				addMethod(methods.next());
		}

		private void addMethod(MethodOrMethodContext m) {
			// Filter out methods in system classes
			if (!SystemClassHandler.isClassInSystemPackage(m.method().getDeclaringClass().getName())) {
				if (set.add(m)) {
					reachables.add(m);
				}
			}
		}

		public void update() {
			while (unprocessedMethods.hasNext()) {
				MethodOrMethodContext m = unprocessedMethods.next();
				Filter filter = new Filter(new EdgePredicate() {

					@Override
					public boolean want(Edge e) {
						if (e.kind() == Kind.CLINIT)
							return false;
						else if (e.kind() == Kind.VIRTUAL) {
							// We only filter calls to this.*
							if (!e.src().isStatic() && e.srcStmt().getInvokeExpr() instanceof InstanceInvokeExpr) {
								SootMethod refMethod = e.srcStmt().getInvokeExpr().getMethod();
								InstanceInvokeExpr iinv = (InstanceInvokeExpr) e.srcStmt().getInvokeExpr();
								if (iinv.getBase() == e.src().getActiveBody().getThisLocal()) {

									// If our parent class P has an abstract
									// method foo() and the lifecycle
									// class L overrides foo(), make sure that
									// all calls to P.foo() in the
									// context of L only go to L.foo().
									SootClass calleeClass = refMethod.getDeclaringClass();
									if (Scene.v().getFastHierarchy().isSubclass(originalComponent, calleeClass)) {
										SootClass targetClass = e.getTgt().method().getDeclaringClass();
										return targetClass == originalComponent || Scene.v().getFastHierarchy()
												.isSubclass(targetClass, originalComponent);
									}
								}

								// We do not expect callback registrations in
								// any
								// calls to system classes
								if (SystemClassHandler.isClassInSystemPackage(refMethod.getDeclaringClass().getName()))
									return false;
							}
						} else if (config.getCallbackConfig().getFilterThreadCallbacks()) {
							// Check for thread call edges
							if (e.kind() == Kind.THREAD || e.kind() == Kind.EXECUTOR)
								return false;

							// Some apps have a custom layer for managing
							// threads,
							// so we need a more generic model
							if (e.tgt().getName().equals("run"))
								if (Scene.v().getFastHierarchy().canStoreType(e.tgt().getDeclaringClass().getType(),
										RefType.v("java.lang.Runnable")))
									return false;
						}
						return true;
					}

				});
				Iterator<Edge> targets = filter.wrap(Scene.v().getCallGraph().edgesOutOf(m));
				addMethods(new Targets(targets));
			}
		}

		/**
		 * Returns a QueueReader object containing all methods found reachable
		 * so far, and which will be informed of any new methods that are later
		 * found to be reachable.
		 */
		public QueueReader<MethodOrMethodContext> listener() {
			return allReachables.clone();
		}

		/**
		 * Returns a QueueReader object which will contain ONLY NEW methods
		 * which will be found to be reachable, but not those that have already
		 * been found to be reachable.
		 */
		public QueueReader<MethodOrMethodContext> newListener() {
			return reachables.reader();
		}

		/** Returns true iff method is reachable. */
		public boolean contains(MethodOrMethodContext m) {
			return set.contains(m);
		}

		/** Returns the number of methods that are reachable. */
		public int size() {
			return set.size();
		}

	}

	/**
	 * Collects the callback methods for all Android default handlers
	 * implemented in the source code. Note that this operation runs inside
	 * Soot, so this method only registers a new phase that will be executed
	 * when Soot is next run
	 */
	@Override
	public void collectCallbackMethods() {
		super.collectCallbackMethods();

		Transform transform = new Transform("wjtp.ajc", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				// Notify the listeners that the solver has been started
				for (IMemoryBoundedSolverStatusNotification listener : notificationListeners)
					listener.notifySolverStarted(DefaultCallbackAnalyzer.this);

				// Do we have to start from scratch or do we have a worklist to
				// process?
				if (callbackWorklist == null) {
					logger.info("Collecting callbacks in DEFAULT mode...");
					callbackWorklist = new HashMultiMap<>();

					// Find the mappings between classes and layouts
					findClassLayoutMappings();

					// Process the callback classes directly reachable from the
					// entry points
					for (SootClass sc : entryPointClasses) {
						// Check whether we're still running
						if (isKilled != null)
							break;

						List<MethodOrMethodContext> methods = new ArrayList<MethodOrMethodContext>(
								getLifecycleMethods(sc));

						// Check for callbacks registered in the code
						analyzeRechableMethods(sc, methods);

						// Check for method overrides
						analyzeMethodOverrideCallbacks(sc);
					}
					logger.info("Callback analysis done.");
				} else {
					// Incremental mode, only process the worklist
					logger.info(String.format("Running incremental callback analysis for %d components...",
							callbackWorklist.size()));
					for (Iterator<SootClass> classIt = callbackWorklist.keySet().iterator(); classIt.hasNext();) {
						// Check whether we're still running
						if (isKilled != null)
							break;

						SootClass componentClass = classIt.next();
						Set<SootMethod> callbacks = callbackWorklist.get(componentClass);

						// Check whether we're already beyond the maximum number
						// of callbacks
						// for the current component
						if (config.getCallbackConfig().getMaxCallbacksPerComponent() > 0
								&& callbacks.size() > config.getCallbackConfig().getMaxCallbacksPerComponent()) {
							callbackMethods.remove(componentClass);
							entryPointClasses.remove(componentClass);
							classIt.remove();
							continue;
						}

						List<MethodOrMethodContext> entryClasses = new ArrayList<>(callbacks.size());
						for (SootMethod sm : callbacks)
							entryClasses.add(sm);

						analyzeRechableMethods(componentClass, entryClasses);
						classIt.remove();
					}
					logger.info("Incremental callback analysis done.");
				}

				// Notify the listeners that the solver has been terminated
				for (IMemoryBoundedSolverStatusNotification listener : notificationListeners)
					listener.notifySolverTerminated(DefaultCallbackAnalyzer.this);
			}

		});
		PackManager.v().getPack("wjtp").add(transform);
	}

	/**
	 * Gets all lifecycle methods in the given entry point class
	 * 
	 * @param sc
	 *            The class in which to look for lifecycle methods
	 * @return The set of lifecycle methods in the given class
	 */
	private Collection<? extends MethodOrMethodContext> getLifecycleMethods(SootClass sc) {
		switch (entryPointUtils.getComponentType(sc)) {
		case Activity:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getActivityLifecycleMethods());
		case Service:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getServiceLifecycleMethods());
		case Application:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getApplicationLifecycleMethods());
		case BroadcastReceiver:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getBroadcastLifecycleMethods());
		case Fragment:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getFragmentLifecycleMethods());
		case ContentProvider:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getContentproviderLifecycleMethods());
		case GCMBaseIntentService:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getGCMIntentServiceMethods());
		case GCMListenerService:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getGCMListenerServiceMethods());
		case ServiceConnection:
			return getLifecycleMethods(sc, AndroidEntryPointConstants.getServiceConnectionMethods());
		case Plain:
			return Collections.emptySet();
		}
		return Collections.emptySet();
	}

	/**
	 * This method takes a lifecycle class and the list of lifecycle method
	 * subsignatures. For each subsignature, it checks whether the given class
	 * or one of its superclass overwrites the respective methods. All findings
	 * are collected in a set and returned.
	 * 
	 * @param sc
	 *            The class in which to look for lifecycle method
	 *            implementations
	 * @param methods
	 *            The list of lifecycle method subsignatures for the type of
	 *            component that the given class corresponds to
	 * @return The set of implemented lifecycle methods in the given class
	 */
	private Collection<? extends MethodOrMethodContext> getLifecycleMethods(SootClass sc, List<String> methods) {
		Set<MethodOrMethodContext> lifecycleMethods = new HashSet<>();
		SootClass currentClass = sc;
		while (currentClass != null) {
			for (String sig : methods) {
				SootMethod sm = currentClass.getMethodUnsafe(sig);
				if (sm != null)
					if (!SystemClassHandler.isClassInSystemPackage(sm.getDeclaringClass().getName()))
						lifecycleMethods.add(sm);
			}
			currentClass = currentClass.hasSuperclass() ? currentClass.getSuperclass() : null;
		}
		return lifecycleMethods;
	}

	private void analyzeRechableMethods(SootClass lifecycleElement, List<MethodOrMethodContext> methods) {
		// Make sure to exclude all other edges in the callgraph except for the
		// edges start in the lifecycle methods we explicitly pass in
		MyReachableMethods rm = new MyReachableMethods(lifecycleElement, methods);
		rm.update();

		// Scan for listeners in the class hierarchy
		QueueReader<MethodOrMethodContext> reachableMethods = rm.listener();
		while (reachableMethods.hasNext()) {
			// Check whether we're still running
			if (isKilled != null)
				break;

			SootMethod method = reachableMethods.next().method();
			analyzeMethodForCallbackRegistrations(lifecycleElement, method);
			analyzeMethodForDynamicBroadcastReceiver(method);
			analyzeMethodForFragmentTransaction(lifecycleElement, method);
		}
	}

	@Override
	protected boolean checkAndAddMethod(SootMethod method, SootMethod parentMethod, SootClass lifecycleClass,
			CallbackType callbackType) {
		if (!this.excludedEntryPoints.contains(lifecycleClass)) {
			if (super.checkAndAddMethod(method, parentMethod, lifecycleClass, callbackType)) {
				// Has this entry point been excluded?
				this.callbackWorklist.put(lifecycleClass, method);
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds the mappings between classes and their respective layout files
	 */
	private void findClassLayoutMappings() {
		Iterator<MethodOrMethodContext> rmIterator = Scene.v().getReachableMethods().listener();
		while (rmIterator.hasNext()) {
			SootMethod sm = rmIterator.next().method();
			if (!sm.isConcrete())
				continue;
			if (SystemClassHandler.isClassInSystemPackage(sm.getDeclaringClass().getName()))
				continue;

			for (Unit u : sm.retrieveActiveBody().getUnits())
				if (u instanceof Stmt) {
					Stmt stmt = (Stmt) u;
					if (stmt.containsInvokeExpr()) {
						InvokeExpr inv = stmt.getInvokeExpr();
						if (invokesSetContentView(inv) || invokesInflate(inv)) { // check
																					// also
																					// for
																					// inflate
																					// to
																					// look
																					// for
																					// the
																					// fragments
							for (Value val : inv.getArgs()) {
								Integer intValue = valueProvider.getValue(sm, stmt, val, Integer.class);
								if (intValue != null)
									this.layoutClasses.put(sm.getDeclaringClass(), intValue);
							}
						}
					}
				}
		}
	}

	@Override
	public void forceTerminate(ISolverTerminationReason reason) {
		this.isKilled = reason;
	}

	@Override
	public boolean isTerminated() {
		return isKilled != null;
	}

	@Override
	public boolean isKilled() {
		return isKilled != null;
	}

	@Override
	public void reset() {
		this.isKilled = null;
	}

	@Override
	public void addStatusListener(IMemoryBoundedSolverStatusNotification listener) {
		this.notificationListeners.add(listener);
	}

	@Override
	public void excludeEntryPoint(SootClass entryPoint) {
		super.excludeEntryPoint(entryPoint);
		this.callbackWorklist.remove(entryPoint);
		this.callbackMethods.remove(entryPoint);
	}

	@Override
	public ISolverTerminationReason getTerminationReason() {
		return isKilled;
	}

}
