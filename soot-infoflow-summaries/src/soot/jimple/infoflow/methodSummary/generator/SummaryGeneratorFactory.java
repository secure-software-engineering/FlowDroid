package soot.jimple.infoflow.methodSummary.generator;

import soot.jimple.infoflow.InfoflowConfiguration.ImplicitFlowMode;

public class SummaryGeneratorFactory {

	/**
	 * summary generator settings
	 */
	private final int accessPathLength = 4;
	private final boolean enableImplicitFlows = false;
	private final boolean enableExceptionTracking = true;
	private final boolean flowSensitiveAliasing = true;
	private final boolean useRecursiveAccessPaths = true;
	private final boolean loadFullJAR = true;

	/**
	 * Initializes the summary generator object
	 * 
	 * @return The initialized summary generator object
	 */
	public SummaryGenerator initSummaryGenerator() {
		SummaryGenerator s = new SummaryGenerator();
		s.getConfig().getAccessPathConfiguration().setAccessPathLength(accessPathLength);
		s.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(useRecursiveAccessPaths);
		s.getConfig().setEnableExceptionTracking(enableExceptionTracking);
		s.getConfig().setImplicitFlowMode(
				enableImplicitFlows ? ImplicitFlowMode.AllImplicitFlows : ImplicitFlowMode.NoImplicitFlows);
		s.getConfig().setFlowSensitiveAliasing(flowSensitiveAliasing);
		s.getConfig().setLoadFullJAR(loadFullJAR);

		return s;
	}

}
