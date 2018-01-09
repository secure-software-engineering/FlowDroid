package soot.jimple.infoflow.solver;

/**
 * Enumeration containing all modes with which predecessor chains can be
 * shortened in the solvers
 * 
 * @author Steven Arzt
 *
 */
public enum PredecessorShorteningMode {

	/**
	 * Never shorten any predecessor chains. This option ensures that path data is
	 * fully available in all cases, but consumes more resources.
	 */
	NeverShorten,

	/**
	 * Shorten predecessor chains by skipping over elements that are equal to their
	 * respective predecessor.
	 */
	ShortenIfEqual,

	/**
	 * Always maximally shorten predecessor chains. This option uses the fewest
	 * resources, but does not allow for path reconstruction (more precisely: will
	 * only yield a one-element path).
	 */
	AlwaysShorten

}
