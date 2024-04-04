package soot.jimple.infoflow.collections.analyses;

import java.util.HashSet;
import java.util.Set;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import heros.solver.IDESolver;
import soot.FastHierarchy;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

/**
 * Intraprocedural analysis that reasons whether a list view (i.e. iterator or
 * sublist) modifies the list.
 *
 * @author Tim Lange
 */
public final class ReadOnlyListViewAnalysis {
	private final LoadingCache<UnitGraph, SimpleLocalUses> graphToUses = IDESolver.DEFAULT_CACHE_BUILDER
			.build(new CacheLoader<UnitGraph, SimpleLocalUses>() {
				@Override
				public SimpleLocalUses load(UnitGraph ug) throws Exception {
					return new SimpleLocalUses(ug, new SimpleLocalDefs(ug));
				}
			});

	private final SootClass iteratorClass;
	private final Set<String> iteratorReadOperations;
	private final SootClass listClass;
	private final Set<String> listReadOperations;
	private final FastHierarchy fh;
	private final IInfoflowCFG icfg;

	public ReadOnlyListViewAnalysis(IInfoflowCFG icfg) throws IllegalStateException {
		this.icfg = icfg;
		this.fh = Scene.v().getOrMakeFastHierarchy();
		this.iteratorClass = Scene.v().getSootClassUnsafe("java.util.Iterator");
		this.listClass = Scene.v().getSootClassUnsafe("java.util.List");
		if (this.iteratorClass == null || this.listClass == null)
			throw new IllegalStateException("Soot is not yet loaded!");

		// Iterator signatures
		this.iteratorReadOperations = new HashSet<>();
		iteratorReadOperations.add("void forEachRemaining(java.util.Consumer)");
		iteratorReadOperations.add("boolean hasNext()");
		iteratorReadOperations.add("boolean hasPrevious()");
		iteratorReadOperations.add("java.lang.Object next()");
		iteratorReadOperations.add("int nextIndex()");
		iteratorReadOperations.add("java.lang.Object previous()");
		iteratorReadOperations.add("int previousIndex()");
		iteratorReadOperations.add("boolean equals(java.lang.Object)");
		iteratorReadOperations.add("int hashCode()");
		iteratorReadOperations.add("java.lang.Object clone()");

		// List signatures
		this.listReadOperations = new HashSet<>();
		listReadOperations.add("boolean contains(java.lang.Object)");
		listReadOperations.add("int indexOf(java.lang.Object)");
		listReadOperations.add("int lastIndexOf(java.lang.Object)");
		listReadOperations.add("java.util.Iterator iterator()");
		listReadOperations.add("java.util.ListIterator listIterator()");
		listReadOperations.add("java.util.ListIterator listIterator(int)");
		listReadOperations.add("java.util.List subList()");
		listReadOperations.add("java.util.List subList(int,int)");
		listReadOperations.add("java.lang.Object get(int)");
		listReadOperations.add("boolean isEmpty()");
		listReadOperations.add("int size()");
		listReadOperations.add("java.lang.Object[] toArray()");
		listReadOperations.add("java.lang.Object[] toArray(java.lang.Object[])");
		listReadOperations.add("boolean equals(java.lang.Object)");
		listReadOperations.add("int hashCode()");
		listReadOperations.add("java.lang.Object clone()");
		// The 2 methods below do write but keep the index in place such that
		// we only over-approximate the index by missing a strong update but never
		// invalidate the given index.
		listReadOperations.add("void replaceAll(java.util.function.UnaryOperator)");
		listReadOperations.add("java.lang.Object set(int,java.lang.Object)");
	}

	private boolean isHarmfulIteratorOperation(SootMethod sm) {
		return fh.canStoreClass(sm.getDeclaringClass(), iteratorClass)
				&& !iteratorReadOperations.contains(sm.getSubSignature());
	}

	private boolean isHarmfulListOperation(SootMethod sm) {
		return fh.canStoreClass(sm.getDeclaringClass(), listClass)
				&& !listReadOperations.contains(sm.getSubSignature());
	}

	/**
	 * Checks whether this assignment creates an iterator that is only used to read
	 * values but never mutates the underlying collection
	 *
	 * @param unit current unit
	 * @return true if iterator is read-only
	 */
	public boolean isReadOnlyIterator(Unit unit) {
		// If this unit is no assign statement, the return value is ignored
		// and there won't be any mutation on an alias
		if (!(unit instanceof AssignStmt))
			return true;

		UnitGraph ug = (UnitGraph) icfg.getOrCreateUnitGraph(icfg.getMethodOf(unit));
		return isReadOnlyIteratorInternal(graphToUses.getUnchecked(ug), (AssignStmt) unit);
	}

	private boolean isSupportedClass(Type type) {
		if (type instanceof RefType) {
			SootClass sc = ((RefType) type).getSootClass();
			return fh.canStoreClass(sc, iteratorClass) || fh.canStoreClass(sc, listClass);
		}

		return false;
	}

	private boolean isReadOnlyIteratorInternal(SimpleLocalUses du, AssignStmt assign) {
		for (UnitValueBoxPair uv : du.getUsesOf(assign)) {
			Stmt stmt = (Stmt) uv.getUnit();
			Value use = uv.getValueBox().getValue();
			// We assume an iterator also mutates the collection if
			// 1. a method is called on it that is unknown
			// 2. it is leaving the method as an argument
			// 3. the iterator leaves the method through a return statement
			// 4. is assigned to a field
			if (stmt.containsInvokeExpr()) {
				if (isHarmfulIteratorOperation(stmt.getInvokeExpr().getMethod())
						|| isHarmfulListOperation(stmt.getInvokeExpr().getMethod()))
					return false;
				if (stmt.getInvokeExpr().getArgs().contains(use))
					return false;
			} else if (stmt instanceof ReturnStmt) {
				// If the return is a use, the iterator leaves the method
				return false;
			}

			if (stmt instanceof AssignStmt) {
				Value lhs = ((AssignStmt) stmt).getLeftOp();
				boolean isLocal = lhs instanceof Local;
				boolean isSupportedClass = isSupportedClass(lhs.getType());

				// Fields are not supported, so we will bail out if a list or iterator is
				// assigned
				// to a field. For other types, we don't care because they can't change the
				// list.
				if (!isLocal)
					if (isSupportedClass)
						return false;
					else
						continue;
				// If the lhs can't mutate the list, we can stop here
				if (!isSupportedClass)
					continue;

				// All e = it.next() will be caught by the previous case s.t.
				// here we should only see assignments or casts
				if (!isReadOnlyIteratorInternal(du, (AssignStmt) stmt))
					return false;
			}
		}

		// If we have seen all statements, we can assume the iterator is read-only
		return true;
	}
}
