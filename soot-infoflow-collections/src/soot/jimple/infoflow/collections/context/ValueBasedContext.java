package soot.jimple.infoflow.collections.context;

import soot.jimple.infoflow.collections.util.Tristate;
import soot.jimple.infoflow.data.ContainerContext;

/**
 * Interface for all value-based contexts
 * Immutable by default.
 *
 * @param <T> concrete value-based context type
 */
public interface ValueBasedContext<T extends ValueBasedContext<?>> extends ContainerContext {
    /**
     * Check whether this and other intersects
     *
     * @param other value-based context
     * @return true on perfect match, maybe on part match and otherwise false
     */
    Tristate intersect(T other);
}
