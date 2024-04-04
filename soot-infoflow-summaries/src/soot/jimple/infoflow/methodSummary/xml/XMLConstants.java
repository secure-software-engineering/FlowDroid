package soot.jimple.infoflow.methodSummary.xml;

public class XMLConstants {
	// xml summary tree
	/*
	 * <summary> <gaps> <gap> </gap> ... </gaps> <methods> <method> <flows> <flow>
	 * <from></from> <to></to> </flow> ... </flows> </method> ... </methods>
	 * </summary>
	 */
	public static final String TREE_SUMMARY = "summary";
	public static final String TREE_METHODS = "methods";
	public static final String TREE_METHOD = "method";
	public static final String TREE_FLOWS = "flows";
	public static final String TREE_FLOW = "flow";
	public static final String TREE_SINK = "to";
	public static final String TREE_SOURCE = "from";
	public static final String TREE_GAPS = "gaps";
	public static final String TREE_GAP = "gap";
	public static final String TREE_CLEARS = "clears";
	public static final String TREE_CLEAR = "clear";
	public static final String TREE_HIERARCHY = "hierarchy";
	public static final String TREE_INTERFACE = "interface";

	public static final String ATTRIBUTE_FORMAT_VERSION = "fileFormatVersion";
	public static final String ATTRIBUTE_IS_INTERFACE = "isInterface";
	public static final String ATTRIBUTE_ID = "num";
	public static final String ATTRIBUTE_METHOD_SIG = "id";
	public static final String ATTRIBUTE_IS_EXCLUDED = "isExcluded";
	public static final String ATTRIBUTE_FLOWTYPE = "sourceSinkType";
	public static final String ATTRIBUTE_PARAMETER_INDEX = "ParameterIndex";
	public static final String ATTRIBUTE_ACCESSPATH = "AccessPath";
	public static final String ATTRIBUTE_ACCESSPATHTYPES = "AccessPathTypes";
	public static final String ATTRIBUTE_BASETYPE = "BaseType";
	public static final String ATTRIBUTE_ERROR = "ERROR";
	public static final String ATTRIBUTE_TAINT_SUB_FIELDS = "taintSubFields";
	public static final String ATTRIBUTE_GAP = "gap";
	public static final String ATTRIBUTE_IS_ALIAS = "isAlias";
	public static final String ATTRIBUTE_TYPE_CHECKING = "typeChecking";
	public static final String ATTRIBUTE_IGNORE_TYPES = "ignoreTypes";
	public static final String ATTRIBUTE_CUT_SUBFIELDS = "cutSubfields";
	public static final String ATTRIBUTE_MATCH_STRICT = "matchStrict";
	public static final String ATTRIBUTE_NAME = "name";
	public static final String ATTRIBUTE_SUPERCLASS = "superClass";
	public static final String ATTRIBUTE_IS_EXCLUSIVE = "isExclusive";
	public static final String ATTRIBUTE_PREVENT_PROPAGATION = "preventPropagation";

	public static final String VALUE_TRUE = "true";
	public static final String VALUE_FALSE = "false";

	public static final String VALUE_PARAMETER = "parameter";
	public static final String VALUE_FIELD = "field";
	public static final String VALUE_RETURN = "return";

	// precise collections:
	public static final String TREE_CONSTRAINTS = "constraints";
	public static final String TREE_KEY = "key";
	public static final String TREE_INDEX = "index";
	public static final String ATTRIBUTE_IMPL_LOC = "ImplicitLocation";
	public static final String ATTRIBUTE_CONSTRAINED = "constrained";
	public static final String ATTRIBUTE_FINAL = "final";
	public static final String ATTRIBUTE_MODE = "mode";
	public static final String MODE_IN_PLACE = "inplace";
	public static final String MODE_APPEND = "append";
	public static final String CONSTRAINT_KEEP = "keep";
	public static final String CONSTRAINT_RO = "read-only";

	public static final String EXCLUDED_ON_CLEAR = "excludedOnClear";

}
