package soot.jimple.infoflow.results.xml;

/**
 * Class containing the tag and attribute names for serializing data flow
 * results to XML.
 * 
 * @author Steven Arzt
 *
 */
class XmlConstants {
	
	class Tags {
		
		public static final String root = "DataFlowResults";
		
		public static final String results = "Results";
		public static final String result = "Result";
		
		public static final String sink = "Sink";
		public static final String accessPath = "AccessPath";
		
		public static final String fields = "Fields";
		public static final String field = "Field";
		
		public static final String sources = "Sources";
		public static final String source = "Source";
		
		public static final String taintPath = "TaintPath";
		public static final String pathElement = "PathElement";
		
		
	}
	
	class Attributes {
		
		public static final String fileFormatVersion = "FileFormatVersion";
		public static final String statement = "Statement";
		public static final String method = "Method";
		
		public static final String value = "Value";
		public static final String type = "Type";
		public static final String taintSubFields = "TaintSubFields";
		
		
	}
	
	class Values {
		
		public static final String TRUE = "true";
		public static final String FALSE = "false";
		
	}

}
