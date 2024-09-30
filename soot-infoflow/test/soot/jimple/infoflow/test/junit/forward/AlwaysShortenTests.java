package soot.jimple.infoflow.test.junit.forward;

import java.io.File;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.solver.PredecessorShorteningMode;

public class AlwaysShortenTests extends soot.jimple.infoflow.test.junit.AlwaysShortenTests {
	class AlwaysShortenInfoflow extends Infoflow {
		public AlwaysShortenInfoflow(File androidPath, boolean forceAndroidJar, BiDirICFGFactory icfgFactory) {
			super(androidPath, forceAndroidJar, icfgFactory);
		}

		@Override
		protected PredecessorShorteningMode pathConfigToShorteningMode(
				InfoflowConfiguration.PathConfiguration pathConfiguration) {
			return PredecessorShorteningMode.AlwaysShorten;
		}
	}

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		AbstractInfoflow infoflow = new AlwaysShortenInfoflow(null, false, null);
		infoflow.getConfig().getPathConfiguration()
				.setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
		return infoflow;
	}
}
