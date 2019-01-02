package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.junit.BeforeClass;

import soot.jimple.infoflow.methodSummary.data.sourceSink.AbstractFlowSinkSource;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSink;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSource;
import soot.jimple.infoflow.methodSummary.data.summary.MethodFlow;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;

public abstract class TestHelper {

	protected static String appPath;
	protected static String libPath;

	protected final static String INT_TYPE = "int";
	protected final static String OBJECT_TYPE = "java.lang.Object";
	protected static final String OBJECT_ARRAY_TYPE = "java.lang.Object[]";
	protected static final String INT_ARRAY_TYPE = "int[]";
	protected static final String LIST_TYPE = "java.util.List";
	protected static final String STRING_TYPE = "java.lang.String";

	protected static final String DATACLASS_SIG = "soot.jimple.infoflow.test.methodSummary.Data";
	protected final static String DATACLASS_INT_FIELD = "<" + DATACLASS_SIG + ": int value>";
	protected final static String DATACLASS_INT_FIELD2 = "<" + DATACLASS_SIG + ": int value2>";
	protected final static String DATACLASS_OBJECT_FIELD = "<" + DATACLASS_SIG + ": " + OBJECT_TYPE + " objectField>";
	protected final static String DATACLASS_STRING_FIELD = "<" + DATACLASS_SIG + ": " + STRING_TYPE + " stringField>";
	protected final static String DATACLASS_STRING_FIELD2 = "<" + DATACLASS_SIG + ": " + STRING_TYPE + " stringField2>";

	protected static final String APICLASS_SIG = "soot.jimple.infoflow.test.methodSummary.ApiClass";
	protected final static String APICLASS_DATA_FIELD = "<" + APICLASS_SIG
			+ ": soot.jimple.infoflow.test.methodSummary.Data dataField>";

	protected final static String LINKEDLIST_FIRST = "<java.util.LinkedList: java.util.LinkedList$Node first>";
	protected final static String LINKEDLIST_LAST = "<java.util.LinkedList: java.util.LinkedList$Node last>";
	protected final static String LINKEDLIST_ITEM = "<java.util.LinkedList$Node: java.lang.Object item>";

	@BeforeClass
	public static void setUp() throws IOException {
		final String sep = System.getProperty("path.separator");

		File f = new File(".");
		File testSrc1 = new File(f, "testBin");
		File testSrc2 = new File(f, "build" + File.separator + "testclasses");

		if (!(testSrc1.exists() || testSrc2.exists()))
			fail("Test aborted - none of the test sources are available");

		appPath = testSrc1.getCanonicalPath() + sep + testSrc2.getCanonicalPath();
		libPath = System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar";
	}

	protected boolean containsFlow(Set<MethodFlow> flows, SourceSinkType sourceTyp, String[] sourceFields,
			SourceSinkType sinkTyp, String[] sinkFields) {
		return containsFlow(flows, sourceTyp, -1, sourceFields, sinkTyp, -1, sinkFields);
	}

	protected boolean containsFlow(Set<MethodFlow> flows, SourceSinkType sourceTyp, String[] sourceFields,
			SourceSinkType sinkTyp, int sinkParameterIdx, String[] sinkFields) {
		return containsFlow(flows, sourceTyp, -1, sourceFields, sinkTyp, sinkParameterIdx, sinkFields);
	}

	protected boolean containsFlow(Set<MethodFlow> flows, SourceSinkType sourceTyp, int sourceParamterIdx,
			String[] sourceFields, SourceSinkType sinkTyp, String[] sinkFields) {
		return containsFlow(flows, sourceTyp, sourceParamterIdx, sourceFields, sinkTyp, -1, sinkFields);
	}

	/**
	 * Checks whether the given set of flows contains a specific flow
	 * 
	 * @param flows             The set of flows
	 * @param sourceTyp         The type of the source (parameter, field, etc.)
	 * @param sourceParamterIdx The parameter index of the source
	 * @param sourceFields      The array of fields in the source
	 * @param sinkTyp           The type of the sink (parameter, field, etc.)
	 * @param sinkParamterIdx   The parameter index of the sink
	 * @param sinkFields        The array of fields in the sink
	 * @return True if the given flow is contained in the given set of flows,
	 *         otherwise false
	 */
	protected boolean containsFlow(Set<MethodFlow> flows, SourceSinkType sourceTyp, int sourceParamterIdx,
			String[] sourceFields, SourceSinkType sinkTyp, int sinkParamterIdx, String[] sinkFields) {
		return containsFlow(flows, sourceTyp, sourceParamterIdx, sourceFields, null, sinkTyp, sinkParamterIdx,
				sinkFields, null);
	}

	/**
	 * Checks whether the given set of flows contains a specific flow
	 * 
	 * @param flows             The set of flows
	 * @param sourceTyp         The type of the source (parameter, field, etc.)
	 * @param sourceParamterIdx The parameter index of the source
	 * @param sourceFields      The array of fields in the source
	 * @param sinkTyp           The type of the sink (parameter, field, etc.)
	 * @param sinkParamterIdx   The parameter index of the sink
	 * @param sinkFields        The array of fields in the sink
	 * @return True if the given flow is contained in the given set of flows,
	 *         otherwise false
	 */
	protected boolean containsFlow(Set<MethodFlow> flows, SourceSinkType sourceTyp, int sourceParamterIdx,
			String[] sourceFields, String sourceGapSignature, SourceSinkType sinkTyp, int sinkParamterIdx,
			String[] sinkFields, String sinkGapSignature) {
		for (MethodFlow mf : flows) {
			// We might have the flow as a direct match or as a reverse flow with an alias
			if (flowMatches(sourceTyp, sourceParamterIdx, sourceFields, sourceGapSignature, sinkTyp, sinkParamterIdx,
					sinkFields, sinkGapSignature, mf))
				return true;

			if (mf.isAlias()) {
				MethodFlow reverseFlow = mf.reverse();
				if (flowMatches(sourceTyp, sourceParamterIdx, sourceFields, sourceGapSignature, sinkTyp,
						sinkParamterIdx, sinkFields, sinkGapSignature, reverseFlow))
					return true;
			}
		}

		return false;
	}

	/**
	 * Checks whether the given flow matches the properties of a specific flow
	 * 
	 * @param mf                The flow to check
	 * @param sourceTyp         The type of the source (parameter, field, etc.)
	 * @param sourceParamterIdx The parameter index of the source
	 * @param sourceFields      The array of fields in the source
	 * @param sinkTyp           The type of the sink (parameter, field, etc.)
	 * @param sinkParamterIdx   The parameter index of the sink
	 * @param sinkFields        The array of fields in the sink
	 * @return True if the given flow is contained in the given set of flows,
	 *         otherwise false
	 */
	protected boolean flowMatches(SourceSinkType sourceTyp, int sourceParamterIdx, String[] sourceFields,
			String sourceGapSignature, SourceSinkType sinkTyp, int sinkParamterIdx, String[] sinkFields,
			String sinkGapSignature, MethodFlow mf) {
		FlowSource source = mf.source();
		FlowSink sink = mf.sink();

		if (source.getType() == sourceTyp && sink.getType() == sinkTyp) {
			if (checkParamter(source, sourceTyp, sourceParamterIdx) && checkParamter(sink, sinkTyp, sinkParamterIdx)) {
				if (checkFields(source, sourceFields) && checkFields(sink, sinkFields))
					if (checkGap(source, sourceGapSignature) && checkGap(sink, sinkGapSignature))
						return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether the given source or sink has the specified gap
	 * 
	 * @param sourceSink   The source or sink to check
	 * @param gapSignature The signature of the gap method
	 * @return True if the given source or sink is associated with a gap that has
	 *         the given method signature, otherwise false
	 */
	private boolean checkGap(AbstractFlowSinkSource sourceSink, String gapSignature) {
		if (sourceSink.getGap() == null)
			return gapSignature == null || gapSignature.isEmpty();
		return sourceSink.getGap().getSignature().equals(gapSignature);
	}

	private boolean checkParamter(AbstractFlowSinkSource s, SourceSinkType sType, int parameterIdx) {
		if (sType.equals(SourceSinkType.Parameter)) {
			if (s.getType() == SourceSinkType.Parameter) {
				return s.getParameterIndex() == parameterIdx;
			}
			return false;
		}
		return true;
	}

	/**
	 * Checks whether the given fields are equal to the fields in the access path of
	 * the given source or sink
	 * 
	 * @param s      The source or sink to check
	 * @param fields The fields to compare
	 * @return True if the given fields are equal to the fields in the access path
	 *         of the given source or sink, otherwise false
	 */
	private boolean checkFields(AbstractFlowSinkSource s, String[] fields) {
		if (fields == null || fields.length == 0) {
			if (!s.hasAccessPath())
				return true;
			return false;
		}
		if (!s.hasAccessPath() && fields != null && fields.length > 0)
			return false;

		if (s.getAccessPath().length != fields.length)
			return false;
		for (int i = 0; i < fields.length; i++) {
			if (!s.getAccessPath()[i].replaceAll("[<>]", "").equals(fields[i].replaceAll("[<>]", "")))
				return false;
		}

		return true;
	}

	/**
	 * Gets the {@link SummaryGenerator} instance to be used for producing the
	 * summaries
	 * 
	 * @return The {@link SummaryGenerator} instance
	 */
	protected abstract SummaryGenerator getSummary();

	/**
	 * Gets the classpath to use for creating summaries
	 * 
	 * @return The classpath to use for creating summaries
	 */
	protected String getClasspath() {
		return appPath + System.getProperty("path.separator") + libPath;
	}

	/**
	 * Creates a full summary for the given method
	 * 
	 * @param methodSignature The signature of the method for which to compute the
	 *                        flow summaries
	 * @return The summary object computed for the given method
	 */
	protected MethodSummaries createSummaries(String methodSignature) {
		return getSummary().createMethodSummary(getClasspath(), methodSignature);
	}

}
