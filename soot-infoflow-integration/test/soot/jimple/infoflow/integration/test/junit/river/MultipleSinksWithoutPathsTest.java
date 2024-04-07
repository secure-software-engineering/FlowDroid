package soot.jimple.infoflow.integration.test.junit.river;

import soot.jimple.infoflow.InfoflowConfiguration;

/**
 * Tests the merging and separating of SourceSinkDefinitions referecing the same statement or method
 * with path reconstruction disabled
 */
public class MultipleSinksWithoutPathsTest extends MultipleSinkTests {
    @Override
    protected void setConfiguration(InfoflowConfiguration config) {
        super.setConfiguration(config);
        config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.NoPaths);
    }
}
