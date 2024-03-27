package soot.jimple.infoflow.data;

/**
 * A context definition for distinguishing individual elements inside a
 * container
 * 
 * @author Steven Arzt
 *
 */
public interface ContainerContext {
    /**
     * Returns whether the context definition
     *
     * @return true if it still restricts
     */
    boolean containsInformation();

    /**
     * Gets whether the given context is imprecise, for example, because the context is built
     * conditionally through multiple paths or the value itself contains some approximation.
     * Users of this context should respect this flag and refrain from performing strong updates.
     *
     * @return true if the context is imprecise
     */
    boolean isImprecise();

    /**
     * Returns whether this entails other.
     *
     * @param other other context definition
     * @return true if this entails other
     */
    boolean entails(ContainerContext other);

    // There are no restrictions to what a context could be. We expect the context factory to be tightly coupled with
    // the users of the contexts.
}
