package soot.jimple.infoflow.integration.test.junit.river;

import soot.jimple.infoflow.InfoflowConfiguration;

public abstract class RiverBaseJUnitTests extends BaseJUnitTests {

	@Override
	protected void setConfiguration(InfoflowConfiguration config) {
		config.setAdditionalFlowsEnabled(true);
		config.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);
	}

}
