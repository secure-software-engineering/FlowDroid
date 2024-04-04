package soot.jimple.infoflow.collections.util;

/**
 * Three-state boolean class
 */
public class Tristate {
    private enum State {
        TRUE,
        FALSE,
        MAYBE
    }

    private final State internal;
    private Tristate(State s) {
        this.internal = s;
    }

    private static final Tristate TRUE_INSTANCE = new Tristate(State.TRUE);
    private static final Tristate FALSE_INSTANCE = new Tristate(State.FALSE);
    private static final Tristate MAYBE_INSTANCE = new Tristate(State.MAYBE);

    public static Tristate TRUE() {
        return TRUE_INSTANCE;
    }

    public static Tristate FALSE() {
        return FALSE_INSTANCE;
    }

    public static Tristate MAYBE() {
        return MAYBE_INSTANCE;
    }

    public static Tristate fromBoolean(boolean b) {
        return b ? TRUE() : FALSE();
    }

    public boolean isTrue() {
        return internal == State.TRUE;
    }

    public boolean isFalse() {
        return internal == State.FALSE;
    }

    public boolean isMaybe() {
        return internal == State.MAYBE;
    }

    public Tristate and(Tristate other) {
        if (this.isTrue() && other.isTrue())
            return TRUE_INSTANCE;

        if (this.isFalse() || other.isFalse())
            return FALSE_INSTANCE;

        return MAYBE_INSTANCE;
    }

    public Tristate negate() {
        switch (internal) {
            case MAYBE:
                return MAYBE_INSTANCE;
            case TRUE:
                return FALSE_INSTANCE;
            case FALSE:
                return TRUE_INSTANCE;
            default:
                throw new RuntimeException("Case missing!");
        }
    }

    @Override
    public String toString() {
        return internal.toString();
    }
}
