package soot.jimple.infoflow.data.pathBuilders;

/**
 * Enum to describe the ProcessingResults
 */
public enum ResultStatus {
    // Describes that the predecessor should be queued
    NEW,
    // Describes that the predecessor was already queued, but we might need to merge paths
    CACHED,
    // Describes that nothing further should be queued
    INFEASIBLE_OR_MAX_PATHS_REACHED
}
