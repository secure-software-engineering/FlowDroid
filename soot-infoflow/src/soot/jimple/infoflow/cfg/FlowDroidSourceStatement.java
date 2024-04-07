package soot.jimple.infoflow.cfg;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

/**
 * Tags statements which were identified as sources in the initial seed
 * collection phase. This tag can be used later to efficiently query whether a
 * statement is a source without querying a {@link ISourceSinkManager}. Note
 * that {@link MethodSourceSinkDefinition} with
 * {@link soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition.CallType}
 * Callback or Return are not tagged because this tag is mainly used to adhere
 * to {@link InfoflowConfiguration#getInspectSources()}.
 *
 * @author Tim Lange
 */
public class FlowDroidSourceStatement implements Tag {
	public static final String TAG_NAME = "fd_source";

	public static final FlowDroidSourceStatement INSTANCE = new FlowDroidSourceStatement();

	private FlowDroidSourceStatement() {
	}

	@Override
	public String getName() {
		return TAG_NAME;
	}

	@Override
	public byte[] getValue() throws AttributeValueException {
		return null;
	}
}
