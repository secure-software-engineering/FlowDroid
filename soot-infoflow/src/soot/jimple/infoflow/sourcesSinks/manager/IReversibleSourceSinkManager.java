package soot.jimple.infoflow.sourcesSinks.manager;

import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;

/**
 * A source sink manager which also allows to treat sinks as sources and vice versa.
 * Backwards search uses this type of SourceSinkManager
 *
 * @author Tim Lange
 */
public interface IReversibleSourceSinkManager extends ISourceSinkManager {
    /**
     * Determines whether this is a source resulting in a leak for backwards search.
     * Creating a SinkInfo using source signatures.
     *
     * @param sCallSite
     *            a Stmt which should include an invokeExrp calling a method
     * @param manager
     *            The manager object for interacting with the solver
     * @param ap
     *            The access path to check. Pass null to check whether the given
     *            statement can be a sink for any given access path.
     * @return A SourceInfo object containing additional information if this
     *         call is a source, otherwise null
     */
    public SinkInfo getInverseSourceInfo(Stmt sCallSite, InfoflowManager manager, AccessPath ap);

    /**
     * Determines if a method called by the Stmt is a sink method or not. If
     * so, additional information is returned to introduce a taint.
     * Creating a SourceInfo using sink signatures
     *
     * @param sCallSite
     *            The call site to check
     * @param manager
     *            The manager object for interacting with the solver
     * @return A SinkInfo object containing additional information if this call
     *         is a sink, otherwise null
     */
    public SourceInfo getInverseSinkInfo(Stmt sCallSite, InfoflowManager manager);
}
