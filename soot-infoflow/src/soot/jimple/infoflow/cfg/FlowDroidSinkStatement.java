package soot.jimple.infoflow.cfg;

import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

/**
 * Tags statements which were identified as sinks in the initial seed collection phase.
 * This tag can be used later to efficiently query whether a statement is a sink without
 * querying a {@link ISourceSinkManager}.
 *
 * @author Tim Lange
 */
public class FlowDroidSinkStatement implements Tag {
    public static final String TAG_NAME = "fd_sink";

    public static final FlowDroidSinkStatement INSTANCE = new FlowDroidSinkStatement();

    private FlowDroidSinkStatement() { }

    @Override
    public String getName() {
        return TAG_NAME;
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return null;
    }
}
