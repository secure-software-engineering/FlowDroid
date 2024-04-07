package soot.jimple.infoflow.integration.test.junit.river;

import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Collections;

/**
 * Tests the RiverPaperTests with the SummaryTaintWrapper
 *
 * @author Tim Lange
 */
public class SummaryTaintWrapperRiverTests extends RiverTests {
    @Override
    protected ITaintPropagationWrapper getTaintWrapper() {
        try {
            return TaintWrapperFactory.createTaintWrapper(Collections.singleton("../soot-infoflow-summaries/summariesManual"));
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException("Could not initialized Taintwrapper:");
        }
    }
}
