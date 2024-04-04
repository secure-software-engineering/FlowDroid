package soot.jimple.infoflow.test.junit;

import org.junit.Assert;
import org.junit.Test;
import soot.jimple.Stmt;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.ResultSourceInfo;

import java.util.ArrayList;
import java.util.List;

public abstract class AlwaysShortenTests extends JUnitTests {

    private boolean pathContainsStmt(ResultSourceInfo sourceInfo, String signature) {
        for (Stmt stmt : sourceInfo.getPath())
            if (signature.equals(stmt.toString()))
                return true;
        return false;
    }

    @Test
    public void alwaysShortenTest1() {
        // Tests that always shortens doesn't break neighbors at the call site. See also issue 583.
        IInfoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void alwaysShortenTest1(int)>");
        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        checkInfoflow(infoflow, 1);
        // Make sure we found two distinct source statements
        Assert.assertEquals(2, infoflow.getResults().getResultSet().stream()
                .map(res -> res.getSource().getStmt()).distinct().count());
        // and that the path was shortened
        for (DataFlowResult res : infoflow.getResults().getResultSet()) {
            Assert.assertFalse(pathContainsStmt(res.getSource(), "$stack2 = i + 42"));
        }
    }

    @Test
    public void alwaysShortenTest2() {
        // Tests that always shortens doesn't break neighbors at the call site. See also issue 583.
        IInfoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.OtherTestCode: void alwaysShortenTest2(int)>");
        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        checkInfoflow(infoflow, 1);
        Assert.assertEquals(1, infoflow.getResults().getResultSet().size());
        // Make sure that the path was shortened
        for (DataFlowResult res : infoflow.getResults().getResultSet()) {
            Assert.assertFalse(pathContainsStmt(res.getSource(), "$stack2 = i + 42"));
        }
    }
}
