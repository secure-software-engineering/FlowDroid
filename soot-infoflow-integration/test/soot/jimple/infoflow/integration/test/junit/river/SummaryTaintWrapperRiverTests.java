package soot.jimple.infoflow.integration.test.junit.river;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import javax.xml.stream.XMLStreamException;

import soot.jimple.infoflow.methodSummary.taintWrappers.TaintWrapperFactory;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

/**
 * Tests the RiverPaperTests with the SummaryTaintWrapper
 *
 * @author Tim Lange
 */
public class SummaryTaintWrapperRiverTests extends RiverTests {
	@Override
	protected ITaintPropagationWrapper getTaintWrapper() {
		try {
			return TaintWrapperFactory.createTaintWrapperFromFiles(Collections
					.singleton(new File(getIntegrationRoot(), "../soot-infoflow-summaries/summariesManual")));
		} catch (IOException | XMLStreamException e) {
			throw new RuntimeException("Could not initialized Taintwrapper:");
		}
	}
}
