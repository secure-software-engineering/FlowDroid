package soot.jimple.infoflow;

import java.util.Map;

import soot.Body;
import soot.Local;
import soot.Singletons.Global;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.scalar.LocalSplitter;
import soot.toolkits.scalar.UnusedLocalEliminator;

/**
 * With more recent soot versions, locals are reused more often. This can cause
 * problems in FlowDroid (e.g. the overwriteParameter test case). The simple
 * solution: We split these locals beforehand
 * 
 * @author Marc Miltenberger
 */
public class FlowDroidLocalSplitter extends LocalSplitter {
	public static class SplittedLocal extends JimpleLocal {

		private static final long serialVersionUID = 1L;
		private JimpleLocal originalLocal;

		public SplittedLocal(JimpleLocal oldLocal) {
			super(null, oldLocal.getType());
			// do not intern the name again
			setName(oldLocal.getName());
			if (oldLocal.isUserDefinedLocal()) {
				setUserDefinedLocal();
			}

			this.originalLocal = oldLocal;
			while (originalLocal instanceof SplittedLocal) {
				originalLocal = ((SplittedLocal) originalLocal).originalLocal;
			}
		}

		public JimpleLocal getOriginalLocal() {
			return originalLocal;
		}

	}

	public FlowDroidLocalSplitter() {
		super((Global) null);
	}

	@Override
	protected String getNewName(String name, int count) {
		// Reuse the old name
		return name;
	}

	@Override
	protected Local createClonedLocal(Local oldLocal) {
		return new SplittedLocal((JimpleLocal) oldLocal);
	}

	public static FlowDroidLocalSplitter v() {
		return new FlowDroidLocalSplitter();
	}

	@Override
	protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
		super.internalTransform(body, phaseName, options);
		UnusedLocalEliminator.v().transform(body);
	}

}
