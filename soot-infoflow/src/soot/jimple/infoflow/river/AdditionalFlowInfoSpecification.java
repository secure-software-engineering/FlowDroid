package soot.jimple.infoflow.river;

import soot.Local;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;

/**
 * Specification for searching backwards for additional information on a data
 * flow
 *
 * @author Steven Arzt
 *
 */
public class AdditionalFlowInfoSpecification {

    private final AccessPath accessPath;
    private final Local local;
    private final Stmt stmt;

    public AdditionalFlowInfoSpecification() {
        accessPath = null;
        local = null;
        stmt = null;
    }

    public AdditionalFlowInfoSpecification(Local base, Stmt stmt) {
        this.accessPath = null;
        this.local = base;
        this.stmt = stmt;
    }

    public AdditionalFlowInfoSpecification(AccessPath accessPath, Stmt stmt) {
        this.accessPath = accessPath;
        this.local = null;
        this.stmt = stmt;
    }

    public Local getLocal() {
        return local;
    }

    public Stmt getStmt() {
        return stmt;
    }

    /**
     * Gets the access path represented by this specification
     *
     * @param manager The data flow manager
     * @return The generated access path
     */
    public AccessPath toAccessPath(InfoflowManager manager) {
        if (accessPath == null) {
            return manager.getAccessPathFactory().createAccessPath(local, true);
        }
        return accessPath;
    }

    @Override
    public String toString() {
        return String.format("%s.%s", local.toString(), accessPath.toString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((accessPath == null) ? 0 : accessPath.hashCode());
        result = prime * result + ((local == null) ? 0 : local.hashCode());
        result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
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
        AdditionalFlowInfoSpecification other = (AdditionalFlowInfoSpecification) obj;
        if (accessPath == null) {
            if (other.accessPath != null)
                return false;
        } else if (!accessPath.equals(other.accessPath))
            return false;
        if (local == null) {
            if (other.local != null)
                return false;
        } else if (!local.equals(other.local))
            return false;
        if (stmt == null) {
            if (other.stmt != null)
                return false;
        } else if (!stmt.equals(other.stmt))
            return false;
        return true;
    }

}
