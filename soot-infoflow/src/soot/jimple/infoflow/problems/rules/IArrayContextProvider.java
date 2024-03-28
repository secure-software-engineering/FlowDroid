package soot.jimple.infoflow.problems.rules;

import soot.jimple.ArrayRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.ContainerContext;

public interface IArrayContextProvider {
    /**
     * Returns the context definition for an array
     *
     * @param arrayRef array reference
     * @param stmt
     * @return context definition
     */
    ContainerContext[] getContextForArrayRef(ArrayRef arrayRef, Stmt stmt);
}
