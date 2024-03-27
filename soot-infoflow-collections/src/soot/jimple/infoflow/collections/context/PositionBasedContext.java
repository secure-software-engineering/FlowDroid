package soot.jimple.infoflow.collections.context;

import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContainerContext;

/**
 * Interface for all position-based contexts.
 * Immutable by default.
 *
 * @param <T> concrete position-based context type
 */
public interface PositionBasedContext<T extends PositionBasedContext<?>> extends ContainerContext {
    /**
     * Check whether this and other intersects
     *
     * @param other value-based context
     * @return true on perfect match, maybe on part match and otherwise false
     */
    Tristate intersects(T other);

    /**
     * Check whether this is less or equal to other
     *
     * @param other value-based context
     * @return true on perfect match, maybe on part match and otherwise false
     */
    Tristate lessThanEqual(T other);

    /**
     * Shifts the position(s) by n
     *
     * @param n number of shifts
     * @return new position-based context
     */
    T exactShift(T n);

    /**
     * Unions this with other
     *
     * @param other other position-based context
     * @return new position-based context
     */
    T union(T other);
}
