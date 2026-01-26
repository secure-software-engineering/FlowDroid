package soot.jimple.infoflow;

import soot.Local;
import soot.Singletons.Global;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.scalar.LocalSplitter;

/**
 * With more recent soot versions, locals are reused more often.
 * This can cause problems in FlowDroid (e.g. the overwriteParameter test case).
 * The simple solution: We split these locals beforehand
 * @author Marc Miltenberger
 */
public class FlowDroidLocalSplitter extends LocalSplitter {
	public static class SplittedLocal extends JimpleLocal {

		private static final long serialVersionUID = 1L;
		private JimpleLocal originalLocal;

		public SplittedLocal(JimpleLocal oldLocal) {
			super(null, oldLocal.getType());
			//do not intern the name again
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
		//Reuse the old name
		return name;
	}

	@Override
	protected Local createClonedLocal(Local oldLocal) {
		return new SplittedLocal((JimpleLocal) oldLocal);
	}

	public static FlowDroidLocalSplitter v() {
		return new FlowDroidLocalSplitter();
	}

}
