package soot.jimple.infoflow.cfg;

import soot.tagkit.Tag;

/**
 * Marks a class that is identified as a system class
 *
 * @author Marc Miltenberger
 */
public class FlowDroidSystemClassChecked implements Tag {
	public static final String TAG_NAME = "fd_sysclass";

	private static final FlowDroidSystemClassChecked INSTANCE_TRUE = new FlowDroidSystemClassChecked(true);
	private static final FlowDroidSystemClassChecked INSTANCE_FALSE = new FlowDroidSystemClassChecked(false);

	private FlowDroidSystemClassChecked(boolean b) {
		this.result = b;
	}

	public static FlowDroidSystemClassChecked v(boolean b) {
		if (b)
			return INSTANCE_TRUE;
		else
			return INSTANCE_FALSE;
	}

	public final boolean result;

	@Override
	public String getName() {
		return TAG_NAME;
	}

}
