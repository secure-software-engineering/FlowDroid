package soot.jimple.infoflow.river;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.handlers.PostAnalysisHandler;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkCondition;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Checks whether conditional sinks met their condition and filter those which did not.
 *
 * @author Tim Lange
 */
public class ConditionalFlowPostProcessor implements PostAnalysisHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    InfoflowManager manager;

    public ConditionalFlowPostProcessor(InfoflowManager manager) {
        this.manager = manager;
    }

    @Override
    public InfoflowResults onResultsAvailable(InfoflowResults results, IInfoflowCFG cfg) {
        if (results.size() == 0)
            return results;

        logger.info("Filtering conditional flows...");
        HashSet<ResultSinkInfo> tbr = new HashSet<>();

        for (DataFlowResult dfRes : results.getResultSet()) {
            Set<SourceSinkCondition> conditions = dfRes.getSink().getDefinition().getConditions();
            if (conditions != null
                    && !conditions.stream().allMatch(cond -> cond.evaluate(dfRes, results)))
                tbr.add(dfRes.getSink());
        }

        logger.info(String.format("Removed %d flows not meeting their conditions", tbr.size()));
        results.removeAll(tbr);

        return results;
    }
}
