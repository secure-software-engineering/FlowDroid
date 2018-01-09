package soot.jimple.infoflow.handlers;

/**
 * Handler that allows clients to execute certain tasks before the actual data
 * flow analysis is executed.
 * 
 * @author Steven Arzt
 *
 */
public interface PreAnalysisHandler {
	
	/**
	 * This method is called before the callgraph is constructed
	 */
	public void onBeforeCallgraphConstruction();
	
	/**
	 * This method is called after the callgraph has been constructed, but
	 * before the actual data flow analysis is carried out.
	 */
	public void onAfterCallgraphConstruction();

}
