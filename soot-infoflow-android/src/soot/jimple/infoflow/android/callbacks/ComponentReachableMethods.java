package soot.jimple.infoflow.android.callbacks;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import soot.FastHierarchy;
import soot.Kind;
import soot.MethodOrMethodContext;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.EdgePredicate;
import soot.jimple.toolkits.callgraph.Filter;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.callgraph.Targets;
import soot.util.queue.ChunkedQueue;
import soot.util.queue.QueueReader;

/**
 * Helper class that computes reachable methods only from a given set of
 * starting methods, while ignoring all other methods. It will not take
 * everything that is already reachable in the callgraph as the seed, but only
 * work on what is explicitly given as the entry points.
 * 
 * This class is heavily based on Soot's {@link ReachableMethods} class by
 * Ondrej Lhotak.
 * 
 * @author Steven Arzt
 *
 */
public class ComponentReachableMethods {

	private final InfoflowAndroidConfiguration config;
	private final SootClass originalComponent;
	protected final Set<MethodOrMethodContext> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final ChunkedQueue<MethodOrMethodContext> reachables = new ChunkedQueue<MethodOrMethodContext>();
	private final QueueReader<MethodOrMethodContext> allReachables = reachables.reader();
	private QueueReader<MethodOrMethodContext> unprocessedMethods;
	private final SystemClassHandler systemClassHandler = SystemClassHandler.v();

	/**
	 * Creates a new instance of the {@link ComponentReachableMethods} class
	 * 
	 * @param config            The configuration of the data flow solver
	 * @param originalComponent The original component or which we are looking for
	 *                          callback registrations. This information is used to
	 *                          more precisely model calls to abstract methods.
	 * @param entryPoints       The entry points from which to find the reachable
	 *                          methods
	 */
	public ComponentReachableMethods(InfoflowAndroidConfiguration config, SootClass originalComponent,
			Collection<MethodOrMethodContext> entryPoints) {
		this.config = config;
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
		if (!systemClassHandler.isClassInSystemPackage(m.method().getDeclaringClass())) {
			if (set.add(m)) {
				reachables.add(m);
			}
		}
	}

	public void update() {
		final Scene sc = Scene.v();
		final FastHierarchy fh = Scene.v().getFastHierarchy();
		final RefType runnable = RefType.v("java.lang.Runnable");
		final EdgePredicate predicate = new EdgePredicate() {

			@Override
			public boolean want(Edge e) {
				if (e.kind() == Kind.CLINIT)
					return false;
				else if (e.kind() == Kind.VIRTUAL) {
					// We only filter calls to this.*
					InvokeExpr inv = e.srcStmt().getInvokeExprUnsafe();
					if (!e.src().isStatic() && inv instanceof InstanceInvokeExpr) {
						SootMethod refMethod = inv.getMethod();
						InstanceInvokeExpr iinv = (InstanceInvokeExpr) inv;
						if (iinv.getBase() == e.src().getActiveBody().getThisLocal()) {

							// If our parent class P has an abstract
							// method foo() and the lifecycle
							// class L overrides foo(), make sure that
							// all calls to P.foo() in the
							// context of L only go to L.foo().
							SootClass calleeClass = refMethod.getDeclaringClass();
							if (fh.isSubclass(originalComponent, calleeClass)) {
								SootClass targetClass = e.getTgt().method().getDeclaringClass();
								return targetClass == originalComponent
										|| fh.isSubclass(targetClass, originalComponent);
							}
						}

						// We do not expect callback registrations in
						// any
						// calls to system classes
						if (systemClassHandler.isClassInSystemPackage(refMethod.getDeclaringClass()))
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
						if (sc.getFastHierarchy().canStoreType(e.tgt().getDeclaringClass().getType(), runnable))
							return false;
				}
				return true;
			}

		};
		final CallGraph cg = sc.getCallGraph();

		while (unprocessedMethods.hasNext()) {
			MethodOrMethodContext m = unprocessedMethods.next();
			Iterator<Edge> of = cg.edgesOutOf(m);
			if (of != null && of.hasNext()) {
				Filter filter = new Filter(predicate);
				Iterator<Edge> targets = filter.wrap(of);
				if (targets.hasNext()) {
					addMethods(new Targets(targets));
				}
			}

		}
	}

	/**
	 * Returns a QueueReader object containing all methods found reachable so far,
	 * and which will be informed of any new methods that are later found to be
	 * reachable.
	 */
	public QueueReader<MethodOrMethodContext> listener() {
		return allReachables.clone();
	}

	/**
	 * Returns a QueueReader object which will contain ONLY NEW methods which will
	 * be found to be reachable, but not those that have already been found to be
	 * reachable.
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