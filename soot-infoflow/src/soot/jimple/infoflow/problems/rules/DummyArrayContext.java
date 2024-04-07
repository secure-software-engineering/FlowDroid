package soot.jimple.infoflow.problems.rules;

import soot.jimple.ArrayRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.ContainerContext;

public class DummyArrayContext implements IArrayContextProvider {
    public ContainerContext[] getContextForArrayRef(ArrayRef arrayRef, Stmt stmt) {
        return null;
    }
}
