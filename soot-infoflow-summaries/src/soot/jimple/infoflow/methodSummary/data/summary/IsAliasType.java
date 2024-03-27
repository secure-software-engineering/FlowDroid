package soot.jimple.infoflow.methodSummary.data.summary;

public enum IsAliasType {
    /* Always valid in alias flow */
    TRUE,
    /* Always not valid in alias flow */
    FALSE,
    /* Only valid if the incoming abstraction has a context */
    WITH_CONTEXT
}
