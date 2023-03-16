package soot.jimple.infoflow.integration.test.junit;

import org.junit.BeforeClass;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Collections;

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
