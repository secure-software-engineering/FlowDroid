package soot.jimple.infoflow.collections.analyses;

import java.util.*;

import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

/**
 * Simple intraprocedural list size analysis performing constant propagation on the list size.
 * ONLY USABLE FOR TESTING PURPOSES! Only handles methods used in the test cases.
 *
 * @author Tim Lange
 */
public class ListSizeAnalysis extends ForwardFlowAnalysis<Unit, Map<Local, ListSizeAnalysis.ListSize>> {

    /**
     *  NOT YET INITIALIZED OR NO LIST -> implicitly null
     *    / /  /      \
     *   0  1  2  ...  n
     *   \  \  \      /
     *  NON-CONSTANT SIZE -> BOTTOM
     */
    public static class ListSize {
        // shared instance of bottom
        private static ListSize BOTTOM = new ListSize();

        int size;
        boolean isBottom;


        static ListSize bottom() {
            return BOTTOM;
        }

        ListSize(int size) {
            this.size = size;
        }

        private ListSize() {
            isBottom = true;
        }

        ListSize plusOne() {
            if (isBottom)
                return this;
            return new ListSize(size + 1);
        }

        ListSize minusOne() {
            if (isBottom)
                return this;
            return new ListSize(size - 1);
        }

        public int getSize() {
            return size;
        }

        public boolean isBottom() {
            return isBottom;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListSize other = (ListSize) o;
            return size == other.size && isBottom == other.isBottom;
        }

        @Override
        public int hashCode() {
            return Objects.hash(size, isBottom);
        }

        @Override
        public String toString() {
            return isBottom ? "Size: BOTTOM" : "Size: " + size;
        }
    }

    private final Set<SootClass> classes;

    private static final Set<String> increments = new HashSet<>();
    static {
        increments.add("boolean add(java.lang.Object)");
        increments.add("java.lang.Object push(java.lang.Object)");
        increments.add("void addElement(java.lang.Object)");
        increments.add("boolean offer(java.lang.Object)");
    }

    private static final Set<String> decrements = new HashSet<>();
    static {
        decrements.add("java.lang.Object remove(int)");
        decrements.add("java.lang.Object pop()");
    }

    private static final Set<String> resets = new HashSet<>();
    static {
        resets.add("void clear()");
    }

    private static final Set<String> invalidates = new HashSet<>();
    static {
        invalidates.add("boolean remove(java.lang.Object)");
        invalidates.add("java.util.Iterator iterator()");
        invalidates.add("java.util.ListIterator listIterator()");
        invalidates.add("boolean removeAll(java.util.Collection)");
        invalidates.add("boolean retainAll(java.util.Collection)");
        invalidates.add("java.util.Spliterator spliterator()");
        invalidates.add("java.util.List subList(int,int)");
    }

    public ListSizeAnalysis(DirectedGraph<Unit> graph) {
        super(graph);
        SootClass listClass = Scene.v().getSootClassUnsafe("java.util.List");
        SootClass queueClass = Scene.v().getSootClassUnsafe("java.util.Queue");
        classes = new HashSet<>(Scene.v().getFastHierarchy().getAllSubinterfaces(listClass));
        classes.addAll(Scene.v().getFastHierarchy().getAllImplementersOfInterface(listClass));
        classes.add(listClass);
        classes.addAll(Scene.v().getFastHierarchy().getAllSubinterfaces(queueClass));
        classes.addAll(Scene.v().getFastHierarchy().getAllImplementersOfInterface(queueClass));
        classes.add(queueClass);
        doAnalysis();
    }

    @Override
    protected void flowThrough(Map<Local, ListSize> in, Unit unit, Map<Local, ListSize> out) {
        out.putAll(in);
        Stmt stmt = (Stmt) unit;
        if (stmt instanceof AssignStmt) {
            Value leftOp = ((AssignStmt) stmt).getLeftOp();
            Value rightOp = ((AssignStmt) stmt).getRightOp();

            if (leftOp instanceof Local && rightOp instanceof NewExpr) {
                SootClass sc = ((NewExpr) rightOp).getBaseType().getSootClass();
                if (classes.contains(sc)) {
                    // Init new list
                    out.put((Local) leftOp, new ListSize(0));
                }
            } else {
                // Overwritten
                out.remove(leftOp);
            }

            // Invalidate list size if an alias is created
            out.remove(rightOp);
        }

        if (!stmt.containsInvokeExpr())
            return;

        // Also invalidate list size if it flows into a callee
        for (Value v : stmt.getInvokeExpr().getArgs())
            if (out.containsKey(v))
                out.put((Local) v, ListSize.bottom());

        SootMethod sm = stmt.getInvokeExpr().getMethod();
        if (!classes.contains(sm.getDeclaringClass()))
            return;

        String subsig = sm.getSubSignature();
        if (increments.contains(subsig)) {
            Local base = (Local) ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
            ListSize size = out.get(base);
            if (size != null)
                out.put(base, size.plusOne());
        } else if (decrements.contains(subsig)) {
            Local base = (Local) ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
            ListSize size = out.get(base);
            if (size != null)
                out.put(base, size.minusOne());
        } else if (invalidates.contains(subsig)) {
            Local base = (Local) ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
            ListSize size = out.get(base);
            if (size != null)
                out.put(base, ListSize.bottom());
        } else if (resets.contains(subsig)) {
            Local base = (Local) ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
            ListSize size = out.get(base);
            if (size != null)
                out.put(base, new ListSize(0));
        }
    }

    @Override
    protected Map<Local, ListSize> newInitialFlow() {
        return new HashMap<>();
    }

    @Override
    protected void merge(Map<Local, ListSize> in1, Map<Local, ListSize> in2, Map<Local, ListSize> out) {
        // Must
        for (Local local : in1.keySet()) {
            ListSize in1Const = in1.get(local);
            ListSize in2Const = in2.get(local);
            if (in1Const == null)
                out.put(local, in2Const);
            else if (in2Const == null || in1Const.equals(in2Const))
                out.put(local, in1Const);
            else
                out.put(local, ListSize.bottom());
        }
    }

    @Override
    protected void copy(Map<Local, ListSize> source, Map<Local, ListSize> dest) {
        if (source == dest) {
            return;
        }
        dest.putAll(source);
    }
}
