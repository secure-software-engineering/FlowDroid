package soot.jimple.infoflow.data.accessPaths;

import soot.Value;
import soot.jimple.infoflow.data.AccessPathFragment;

/**
 * <p>
 * Strategy for reducing a sequence of access path fragments. AP reduction
 * ideally transforms an incoming AP into its canonical form. This helps avoid
 * infinite recursion.
 * </p>
 * 
 * <p>
 * Examples for reduction:
 * </p>
 * 
 * <pre>
 * list.next.prev -> list
 * </pre>
 * 
 * @author Steven Arzt
 *
 */
public interface IAccessPathReductionStrategy {

	/**
	 * Reduces the given sequence of access path fragments
	 * 
	 * @param base      The local in which the access path is rooted
	 * @param fragments The sequence of access path fragments to reduce
	 * @return The reduced sequence of access path fragments if the reduction was
	 *         applicable, otherwise the original sequence
	 */
	public AccessPathFragment[] reduceAccessPath(Value base, AccessPathFragment[] fragments);

}
