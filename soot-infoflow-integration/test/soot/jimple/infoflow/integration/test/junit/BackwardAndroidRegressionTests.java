package soot.jimple.infoflow.integration.test.junit;

import soot.jimple.infoflow.InfoflowConfiguration;

public class BackwardAndroidRegressionTests extends AndroidRegressionTests {
    @Override
    protected void setConfiguration(InfoflowConfiguration config) {
        config.setDataFlowDirection(InfoflowConfiguration.DataFlowDirection.Backwards);
    }
}
