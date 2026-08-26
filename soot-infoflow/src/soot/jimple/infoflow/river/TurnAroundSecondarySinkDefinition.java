package soot.jimple.infoflow.river;

/**
 * This is used as a sink to denote a connection from a primary flow sink to a
 * turnaround point.
 * 
 * @author Marc Miltenberger
 */
public class TurnAroundSecondarySinkDefinition extends SecondarySinkDefinition {
	public static TurnAroundSecondarySinkDefinition INSTANCE = new TurnAroundSecondarySinkDefinition();

}
