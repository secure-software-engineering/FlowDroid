package soot.jimple.infoflow.methodSummary.data.sourceSink;

public enum ConstraintType {
    // Constraint should match
    TRUE,
    // No constraint
    FALSE,
    // Constraint should not match
    NO_MATCH,
    // Constraint moves to the right, only valid for position-based containers
    SHIFT_RIGHT,
    // Constraint moves to the left, only valid for position-based containers
    SHIFT_LEFT,
    // Tells to keep the constraint from the source field
    KEEP,
    // Tells to keep the constraint from the source field only
    // if the rhs is used in a read-only fashion
    READONLY,
    // Tells to add the current constraint to the given one
    APPEND
}
