package soot.jimple.infoflow.config;

/**
 * Allows to configure the strategy to use with collections and maps.
 */
public enum PreciseCollectionStrategy {
	/**
	 * Noe support for precise container access (default) 
	 */
	NONE,
	
	/**
	 * Supports maps with constant access, e.g.
	 * map.put("a", getSource());
	 * sink(map.get("b"));
	 * will not trigger a leak.
	 * Note that you will need to use the Summary taint wrapper in order to get proper support. 
	 */
	CONSTANT_MAP_SUPPORT
}
