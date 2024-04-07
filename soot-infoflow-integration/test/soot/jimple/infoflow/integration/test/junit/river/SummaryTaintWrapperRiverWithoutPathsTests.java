package soot.jimple.infoflow.integration.test.junit.river;

import soot.jimple.infoflow.InfoflowConfiguration;

/**
 * Tests the RiverTestCode with the SummaryTaintWrapper and no path reconstruction
 *
 * @author Tim Lange
 */
public class SummaryTaintWrapperRiverWithoutPathsTests extends SummaryTaintWrapperRiverTests {
    @Override
    protected void setConfiguration(InfoflowConfiguration config) {
        super.setConfiguration(config);
        config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.NoPaths);
    }
}
