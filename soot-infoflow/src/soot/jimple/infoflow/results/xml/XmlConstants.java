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

		public static final String performanceData = "PerformanceData";
		public static final String performanceEntry = "PerformanceEntry";

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
		public static final String terminationState = "TerminationState";
		public static final String statement = "Statement";
		public static final String linenumber = "LineNumber";
		public static final String method = "Method";

		public static final String value = "Value";
		public static final String type = "Type";
		public static final String taintSubFields = "TaintSubFields";

		public static final String category = "Category";

		public static final String name = "Name";

		public static final String methodSourceSinkDefinition = "MethodSourceSinkDefinition";

	}

	class Values {

		public static final String TRUE = "true";
		public static final String FALSE = "false";

		public static final String PERF_CALLGRAPH_SECONDS = "CallgraphConstructionSeconds";
		public static final String PERF_TAINT_PROPAGATION_SECONDS = "TaintPropagationSeconds";
		public static final String PERF_PATH_RECONSTRUCTION_SECONDS = "PathReconstructionSeconds";
		public static final String PERF_TOTAL_RUNTIME_SECONDS = "TotalRuntimeSeconds";
		public static final String PERF_MAX_MEMORY_CONSUMPTION = "MaxMemoryConsumption";

		public static final String PERF_SOURCE_COUNT = "SourceCount";
		public static final String PERF_SINK_COUNT = "SinkCount";

	}

}
