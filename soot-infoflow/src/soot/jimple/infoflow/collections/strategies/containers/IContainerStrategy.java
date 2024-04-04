package soot.jimple.infoflow.collections.strategies.containers;

import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContainerContext;

/**
 * Strategy for resolving keys/indices and reasoning about their relation
 *
 * @author Tim Lange
 */
public interface IContainerStrategy {
    /**
     * Checks whether the two arguments intersect
     *
     * @param apKey   key from the access path
     * @param stmtKey key from the statement
     * @return true if args fully intersects, maybe on part match and false on definitely no match
     */
    Tristate intersect(ContainerContext apKey, ContainerContext stmtKey);

    /**
     * Return the union of the given contexts
     *
     * @param ctxt1 key from the access path
     * @param ctxt2 key from the statement
     * @return new context definition
     */
    ContainerContext[] append(ContainerContext[] ctxt1, ContainerContext[] ctxt2);

    /**
     * Retrieves a context for a given key
     *
     * @param key  requested value
     * @param stmt statement containing key
     * @return new context definition
     */
    ContainerContext getKeyContext(Value key, Stmt stmt);

    /**
     * Retrieves a context for a given index
     *
     * @param index requested value
     * @param stmt  statement containing index
     * @return new context definition
     */
    ContainerContext getIndexContext(Value index, Stmt stmt);

    /**
     * Retrieves a context given an implicit key after the given statement
     * (i.e. a list where the key is dependent on the number of add calls before)
     *
     * @param lst  list
     * @param stmt statement that uses value
     * @return new context definition
     */
    ContainerContext getNextPosition(Value lst, Stmt stmt);

    /**
     * Retrieves a context given an implicit key before the given statement
     * (i.e. a list where the key is dependent on the number of add calls before)
     *
     * @param lst  list
     * @param stmt statement that uses value
     * @return new context definition
     */
    ContainerContext getFirstPosition(Value lst, Stmt stmt);

    /**
     * Retrieves a context given an implicit key before the given statement
     * (i.e. a list where the key is dependent on the number of add calls before)
     *
     * @param lst  list
     * @param stmt statement that uses value
     * @return new context definition
     */
    ContainerContext getLastPosition(Value lst, Stmt stmt);

    /**
     * Returns whether ctxt1 is less than ctxt2
     *
     * @param ctxt1 first context
     * @param ctxt2 second context
     * @return true ctxt1 is less than ctxt2
     */
    Tristate lessThanEqual(ContainerContext ctxt1, ContainerContext ctxt2);

    /**
     * Shifts the ctxt to the right
     *
     * @param ctxt current context
     * @param n
     * @return new context definition
     */
    ContainerContext shift(ContainerContext ctxt, int n, boolean exact);

    /**
     * Returns whether the context is still useful or not
     *
     * @param ctxts contexts
     * @return true if context contains no useful information and thus, the collection should be smashed
     */
    boolean shouldSmash(ContainerContext[] ctxts);

    /**
     * Returns whether the given value is used in a read-only fashion
     *
     * @param unit current statement
     * @return true if is read-only
     */
    boolean isReadOnly(Unit unit);
}
