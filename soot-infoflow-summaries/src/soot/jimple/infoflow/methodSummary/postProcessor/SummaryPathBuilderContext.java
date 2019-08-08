package soot.jimple.infoflow.methodSummary.postProcessor;

import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

/**
 * Class for keeping track of the context under which the path reconstruction
 * for a summary takes place. If we, for example, have used pre-existing
 * summaries during the data flow analysis, we need to also model these effects
 * during path reconstruction.
 * 
 * @author Steven Arzt
 *
 */
public class SummaryPathBuilderContext {

	private final ITaintPropagationWrapper taintWrapper;

	/**
	 * Creates a new instance of the {@link SummaryPathBuilderContext} class
	 * 
	 * @param taintWrapper The taint wrapper that was used during the data flow
	 *                     phase, or null if no taint wrapper was used
	 */
	public SummaryPathBuilderContext(ITaintPropagationWrapper taintWrapper) {
		this.taintWrapper = taintWrapper;
	}

	/**
	 * Gets the taint wrapper that was used during the data flow phase
	 * 
	 * @return The taint wrapper that was used during the data flow phase, or null
	 *         if no taint wrapper was used
	 */
	public ITaintPropagationWrapper getTaintWrapper() {
		return taintWrapper;
	}

}
