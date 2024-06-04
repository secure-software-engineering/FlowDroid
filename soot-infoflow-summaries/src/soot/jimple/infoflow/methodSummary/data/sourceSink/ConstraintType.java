package soot.jimple.infoflow.methodSummary.data.sourceSink;

public enum ConstraintType {
	// Constraint should match
	TRUE(false),
	// No constraint
	FALSE(false),
	// Constraint should not match
	NO_MATCH(false),
	// Constraint moves to the right, only valid for position-based containers
	SHIFT_RIGHT(true),
	// Constraint moves to the left, only valid for position-based containers
	SHIFT_LEFT(true),
	// Tells to keep the constraint from the source field
	KEEP(true),
	// Tells to keep the constraint from the source field only
	// if the rhs is used in a read-only fashion
	READONLY(true),
	// Tells to add the current constraint to the given one
	APPEND(true);

	final boolean isAction;

	ConstraintType(boolean isAction) {
		this.isAction = isAction;
	}

	/**
	 * Gets whether this constraint encodes an action
	 * 
	 * @return True if this constraint encodes an action, false if it only encodes a
	 *         matching property
	 */
	public boolean isAction() {
		return isAction;
	}

}
