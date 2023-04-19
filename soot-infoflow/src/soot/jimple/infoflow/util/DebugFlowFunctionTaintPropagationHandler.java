package soot.jimple.infoflow.util;

import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;

import java.util.Set;

/**
 * Prints all propagations to stdout. Useful to debug small test cases.
 *
 * @author Tim Lange
 */
public class DebugFlowFunctionTaintPropagationHandler implements TaintPropagationHandler {

    String prefix;

    public DebugFlowFunctionTaintPropagationHandler() {
        this.prefix = "";
    }

    public DebugFlowFunctionTaintPropagationHandler(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void notifyFlowIn(Unit stmt, Abstraction taint, InfoflowManager manager, FlowFunctionType type) {
        // no-op
    }

    @Override
    public Set<Abstraction> notifyFlowOut(Unit stmt, Abstraction d1, Abstraction incoming, Set<Abstraction> outgoing, InfoflowManager manager, FlowFunctionType type) {
        String typeString = "";
        switch (type) {
            case CallToReturnFlowFunction:
                typeString = "CallToReturn";
                break;
            case ReturnFlowFunction:
                typeString = "Return";
                break;
            case CallFlowFunction:
                typeString = "Call";
                break;
            case NormalFlowFunction:
                typeString = "Normal";
                break;
        }
            if (type == FlowFunctionType.ReturnFlowFunction && outgoing != null) {
                Unit out = stmt;
                for (Abstraction abs : outgoing) {
                    if (abs.getCorrespondingCallSite() != null) {
                        out = abs.getCorrespondingCallSite();
                        break;
                    }
                }
                System.out.println(this.prefix + " " + typeString + " @ " + out + ":\n\tIn: " + incoming + "\n\tOut: " + outgoing + "\n");
            } else
                System.out.println(this.prefix + " " + typeString + " @ " + stmt + ":\n\tIn: " + incoming + "\n\tOut: " + outgoing + "\n");

        return outgoing;
    }
}
