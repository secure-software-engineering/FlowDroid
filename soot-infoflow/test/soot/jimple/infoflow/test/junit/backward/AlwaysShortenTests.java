package soot.jimple.infoflow.test.junit.backward;

import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.BackwardsInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.solver.PredecessorShorteningMode;

public class AlwaysShortenTests extends soot.jimple.infoflow.test.junit.AlwaysShortenTests {
    class AlwaysShortenBackwardsInfoflow extends BackwardsInfoflow {
        public AlwaysShortenBackwardsInfoflow(String androidPath, boolean forceAndroidJar, BiDirICFGFactory icfgFactory) {
            super(androidPath, forceAndroidJar, icfgFactory);
        }

        @Override
        protected PredecessorShorteningMode pathConfigToShorteningMode(InfoflowConfiguration.PathConfiguration pathConfiguration) {
            return PredecessorShorteningMode.AlwaysShorten;
        }
    }

    @Override
    protected AbstractInfoflow createInfoflowInstance() {
        AbstractInfoflow infoflow = new AlwaysShortenBackwardsInfoflow("", false, null);
        infoflow.getConfig().getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
        return infoflow;
    }
}
