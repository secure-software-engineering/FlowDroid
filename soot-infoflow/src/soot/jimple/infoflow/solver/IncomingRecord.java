package soot.jimple.infoflow.solver;

import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;

/**
 * Record for the incoming set inside an IFDS solver
 *
 * @param <N> node
 * @param <D> data flow fact
 */
public class IncomingRecord<N, D extends FastSolverLinkedNode<D, N>> {
    public final N n;  // call site
    public final D d1; // calling context in caller
    public final D d2; // fact at the call site
    public final D d3; // calling context in callee

    public IncomingRecord(N n, D d1, D d2, D d3) {
        this.n = n;
        this.d1 = d1;
        this.d2 = d2;
        this.d3 = d3;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((d1 == null) ? 0 : d1.hashCode());
        result = prime * result + ((d2 == null) ? 0 : d2.hashCode());
        result = prime * result + ((d3 == null) ? 0 : d3.hashCode());
        result = prime * result + ((n == null) ? 0 : n.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IncomingRecord<?, ?> other = (IncomingRecord<?, ?>) obj;
        if (d1 == null) {
            if (other.d1 != null)
                return false;
        } else if (!d1.equals(other.d1))
            return false;
        if (d2 == null) {
            if (other.d2 != null)
                return false;
        } else if (!d2.equals(other.d2))
            return false;
        if (d3 == null) {
            if (other.d3 != null)
                return false;
        } else if (!d3.equals(other.d3))
            return false;
        if (n == null) {
            if (other.n != null)
                return false;
        } else if (!n.equals(other.n))
            return false;
        return true;
    }
}