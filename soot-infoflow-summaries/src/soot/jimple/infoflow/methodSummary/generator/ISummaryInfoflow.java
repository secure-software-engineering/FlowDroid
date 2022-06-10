package soot.jimple.infoflow.methodSummary.generator;

import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowManager;

public interface ISummaryInfoflow extends IInfoflow {
    InfoflowManager getManager();
}
