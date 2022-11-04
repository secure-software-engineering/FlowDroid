package soot.jimple.infoflow.data.pathBuilders;

import soot.jimple.infoflow.data.SourceContextAndPath;

import static soot.jimple.infoflow.data.pathBuilders.ResultStatus.*;

/**
 * Wrapper class to pass the result of a SourceFindingTask
 */
public class ProcessingResult {
    private static final ProcessingResult NEW_INSTANCE = new ProcessingResult(NEW, null);
    private static final ProcessingResult INFEASIBLE_INSTANCE = new ProcessingResult(INFEASIBLE_OR_MAX_PATHS_REACHED, null);

    private final ResultStatus result;
    private final SourceContextAndPath scap;

    private ProcessingResult(ResultStatus result, SourceContextAndPath scap) {
        this.result = result;
        this.scap = scap;
    }

    public static ProcessingResult NEW() {
        return NEW_INSTANCE;
    }
    public static ProcessingResult INFEASIBLE_OR_MAX_PATHS_REACHED() {
        return INFEASIBLE_INSTANCE;
    }
    public static ProcessingResult CACHED(SourceContextAndPath scap) {
        return new ProcessingResult(CACHED, scap);
    }

    public ResultStatus getResult() {
        return result;
    }

    public SourceContextAndPath getScap() {
        assert result == CACHED;
        return scap;
    }
}
