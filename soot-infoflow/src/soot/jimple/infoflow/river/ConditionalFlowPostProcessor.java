package soot.jimple.infoflow.river;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.handlers.PostAnalysisHandler;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkCondition;

/**
 * Checks whether conditional sinks met their condition and filter those which
 * did not.
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

		logger.info("Filtering conditional flows, starting with {} flows...", results.numConnections());
		HashSet<ResultSinkInfo> tbr = new HashSet<>();

		for (DataFlowResult dfRes : results.getResultSet()) {
			Set<SourceSinkCondition> conditions = dfRes.getSink().getDefinition().getConditions();
			// One of the conditions must match. Within both, a class name and a signature
			// must match.
			if (conditions != null && !conditions.isEmpty()
					&& conditions.stream().noneMatch(cond -> cond.evaluate(dfRes, results)))
				tbr.add(dfRes.getSink());
		}

		logger.info(String.format("Removed %d flows not meeting their conditions", tbr.size()));
		results.removeAll(tbr);

		return results;
	}
}
