package soot.jimple.infoflow.integration.test.junit.river;

import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import java.io.IOException;

/**
 * Tests the RiverPaperTests with the EasyTaintWrapper
 *
 * @author Tim Lange
 */
public class EasyTaintWrapperRiverTests extends RiverTests {
    @Override
    protected ITaintPropagationWrapper getTaintWrapper() {
        try {
            EasyTaintWrapper easyWrapper = EasyTaintWrapper.getDefault();
            // Add methods used in the test cases
            easyWrapper.addMethodForWrapping("java.io.BufferedOutputStream", "void <init>(java.io.OutputStream)");
            easyWrapper.addMethodForWrapping("java.io.BufferedWriter", "void <init>(java.io.Writer)");
            easyWrapper.addMethodForWrapping("java.io.OutputStreamWriter", "void <init>(java.io.OutputStream)");
            easyWrapper.addMethodForWrapping("java.io.Writer", "void write(java.lang.String)");
            return easyWrapper;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
