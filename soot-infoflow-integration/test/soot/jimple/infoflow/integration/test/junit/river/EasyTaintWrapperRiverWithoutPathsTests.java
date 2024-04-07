package soot.jimple.infoflow.integration.test.junit.river;

import soot.jimple.infoflow.InfoflowConfiguration;

/**
 * Tests the RiverTestCode with the EasyTaintWrapper and no path reconstruction
 *
 * @author Tim Lange
 */
public class EasyTaintWrapperRiverWithoutPathsTests extends EasyTaintWrapperRiverTests {
    @Override
    protected void setConfiguration(InfoflowConfiguration config) {
        super.setConfiguration(config);
        config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.NoPaths);
    }
}
