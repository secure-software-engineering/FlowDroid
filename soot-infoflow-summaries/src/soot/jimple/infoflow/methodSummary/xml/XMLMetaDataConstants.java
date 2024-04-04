package soot.jimple.infoflow.methodSummary.xml;

public class XMLMetaDataConstants {
	// xml meta data summary tree
	/*
	 * <summaryMetaData> <exclusiveModels> <exclusiveModel> </exclusiveModel> ...
	 * </exclusiveModels> </summaryMetaData>
	 */
	public static final String TREE_SUMMARY_META_DATA = "summaryMetaData";
	public static final String TREE_EXCLUSIVE_MODELS = "exclusiveModels";
	public static final String TREE_EXCLUSIVE_MODEL = "exclusiveModel";

	public static final String TREE_HIERARCHY = "hierarchy";
	public static final String TREE_CLASS = "class";

	public static final String ATTRIBUTE_FORMAT_VERSION = "fileFormatVersion";
	public static final String ATTRIBUTE_TYPE = "type";
	public static final String ATTRIBUTE_NAME = "name";
	public static final String ATTRIBUTE_SUPERCLASS = "superClass";

	public static final String VALUE_CLASS = "class";
	public static final String VALUE_PACKAGE = "package";

}
