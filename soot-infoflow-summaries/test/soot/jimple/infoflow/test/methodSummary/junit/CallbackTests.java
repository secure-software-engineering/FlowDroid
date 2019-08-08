package soot.jimple.infoflow.test.methodSummary.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.methodSummary.DefaultSummaryConfig;
import soot.jimple.infoflow.methodSummary.data.summary.MethodSummaries;
import soot.jimple.infoflow.methodSummary.data.summary.SourceSinkType;
import soot.jimple.infoflow.methodSummary.generator.SummaryGenerator;
import soot.options.Options;

public class CallbackTests extends TestHelper {

	static final String className = "soot.jimple.infoflow.test.methodSummary.Callbacks";

	private static final String FIELD_CALLBACK = "<soot.jimple.infoflow.test.methodSummary.Callbacks: soot.jimple.infoflow.test.methodSummary.Callbacks$MyCallbacks cbs>";

	@Test(timeout = 100000)
	public void paraToCallbackToReturn() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.Callbacks: java.lang.String paraToCallbackToReturn(java.lang.String,soot.jimple.infoflow.test.methodSummary.Callbacks$MyCallbacks)>";
		MethodSummaries flow = createSummaries(mSig);

		// Parameter 1 to gap base object
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 1, null, "", SourceSinkType.GapBaseObject,
				0, null,
				"<soot.jimple.infoflow.test.methodSummary.Callbacks$MyCallbacks: java.lang.String transform(java.lang.String)>"));
		// Parameter 0 to gap argument 0
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 0, null, "", SourceSinkType.Parameter, 0,
				null,
				"<soot.jimple.infoflow.test.methodSummary.Callbacks$MyCallbacks: java.lang.String transform(java.lang.String)>"));
		// Gap return value to method return value
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Return, -1, null,
				"<soot.jimple.infoflow.test.methodSummary.Callbacks$MyCallbacks: java.lang.String transform(java.lang.String)>",
				SourceSinkType.Return, -1, null, ""));

		assertEquals(3, flow.getFlowCount());
	}

	@Test(timeout = 100000)
	public void fieldCallbackToReturn() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.Callbacks: java.lang.String fieldCallbackToReturn(java.lang.String)>";
		MethodSummaries flow = createSummaries(mSig);

		// Field to gap base object
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Field, -1, new String[] { FIELD_CALLBACK }, "",
				SourceSinkType.GapBaseObject, 0, null,
				"<soot.jimple.infoflow.test.methodSummary.Callbacks$MyCallbacks: java.lang.String transform(java.lang.String)>"));
		// Parameter 0 to gap argument 0
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 0, null, "", SourceSinkType.Parameter, 0,
				null,
				"<soot.jimple.infoflow.test.methodSummary.Callbacks$MyCallbacks: java.lang.String transform(java.lang.String)>"));
		// Gap return value to method return value
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Return, -1, null,
				"<soot.jimple.infoflow.test.methodSummary.Callbacks$MyCallbacks: java.lang.String transform(java.lang.String)>",
				SourceSinkType.Return, -1, null, ""));

		assertEquals(3, flow.getFlowCount());
	}

	@Test(timeout = 100000)
	public void fieldCallbackToField() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.Callbacks: soot.jimple.infoflow.test.methodSummary.Data fieldCallbackToField(soot.jimple.infoflow.test.methodSummary.Data)>";
		MethodSummaries flow = createSummaries(mSig);

		// Field to gap base object
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Field, -1, new String[] { FIELD_CALLBACK }, "",
				SourceSinkType.GapBaseObject, 0, null,
				"<soot.jimple.infoflow.test.methodSummary.Callbacks$MyCallbacks: void transformObject(soot.jimple.infoflow.test.methodSummary.Data)>"));
		// Parameter 0 to gap argument 0
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 0, null, "", SourceSinkType.Parameter, 0,
				null,
				"<soot.jimple.infoflow.test.methodSummary.Callbacks$MyCallbacks: void transformObject(soot.jimple.infoflow.test.methodSummary.Data)>"));
		// Gap parameter 0 to method return value
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 0, null,
				"<soot.jimple.infoflow.test.methodSummary.Callbacks$MyCallbacks: void transformObject(soot.jimple.infoflow.test.methodSummary.Data)>",
				SourceSinkType.Return, -1, null, ""));

		assertEquals(3, flow.getFlowCount());
	}

	@Test(timeout = 100000)
	public void apiClassMakeString() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ApiClass: java.lang.String makeString(soot.jimple.infoflow.test.methodSummary.IGapClass,java.lang.String)>";
		MethodSummaries flow = createSummaries(mSig);

		// Parameter 0 to gap base object
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 0, null, "", SourceSinkType.GapBaseObject,
				0, null,
				"<soot.jimple.infoflow.test.methodSummary.IGapClass: java.lang.String callTheGap(java.lang.String)>"));
		// Parameter 1 to gap argument 0
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 1, null, "", SourceSinkType.Parameter, 0,
				null,
				"<soot.jimple.infoflow.test.methodSummary.IGapClass: java.lang.String callTheGap(java.lang.String)>"));
		// Gap return value to method return value
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Return, -1, null,
				"<soot.jimple.infoflow.test.methodSummary.IGapClass: java.lang.String callTheGap(java.lang.String)>",
				SourceSinkType.Return, -1, null, ""));

		assertEquals(3, flow.getFlowCount());
	}

	@Test(timeout = 100000)
	public void apifillDataObject() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ApiClass: void fillDataObject(soot.jimple.infoflow.test.methodSummary.IGapClass,java.lang.String,soot.jimple.infoflow.test.methodSummary.Data)>";
		MethodSummaries flow = createSummaries(mSig);

		// Parameter 0 to gap base object
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 0, null, "", SourceSinkType.GapBaseObject,
				0, null,
				"<soot.jimple.infoflow.test.methodSummary.IGapClass: void fillDataString(java.lang.String,soot.jimple.infoflow.test.methodSummary.Data)>"));
		// Parameter 1 to gap argument 0
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 1, null, "", SourceSinkType.Parameter, 0,
				null,
				"<soot.jimple.infoflow.test.methodSummary.IGapClass: void fillDataString(java.lang.String,soot.jimple.infoflow.test.methodSummary.Data)>"));
		// Parameter 2 to gap argument 1
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 2, null, "", SourceSinkType.Parameter, 1,
				null,
				"<soot.jimple.infoflow.test.methodSummary.IGapClass: void fillDataString(java.lang.String,soot.jimple.infoflow.test.methodSummary.Data)>"));

		assertEquals(3, flow.getFlowCount());
	}

	@Test(timeout = 100000)
	public void gapToGap() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ApiClass: java.lang.String gapToGap(soot.jimple.infoflow.test.methodSummary.IUserCodeClass,java.lang.String)>";
		MethodSummaries flow = createSummaries(mSig);

		// Parameter 0 to gap 0+1 base object
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 0, null, "", SourceSinkType.GapBaseObject,
				0, null,
				"<soot.jimple.infoflow.test.methodSummary.IUserCodeClass: java.lang.String callTheGap(java.lang.String)>"));
		// Parameter 1 to gap 0 argument 0
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 1, null, "", SourceSinkType.Parameter, 0,
				null,
				"<soot.jimple.infoflow.test.methodSummary.IUserCodeClass: java.lang.String callTheGap(java.lang.String)>"));
		// Gap 0+1 base back to parameter 0
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Field, -1, null,
				"<soot.jimple.infoflow.test.methodSummary.IUserCodeClass: java.lang.String callTheGap(java.lang.String)>",
				SourceSinkType.Parameter, 0, null, ""));
		// Gap 0 return to gap 1 parameter 0
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Return, -1, null,
				"<soot.jimple.infoflow.test.methodSummary.IUserCodeClass: java.lang.String callTheGap(java.lang.String)>",
				SourceSinkType.Parameter, 0, null,
				"<soot.jimple.infoflow.test.methodSummary.IUserCodeClass: java.lang.String callTheGap(java.lang.String)>"));
		// Gap 1 return to method return value
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Return, -1, null,
				"<soot.jimple.infoflow.test.methodSummary.IUserCodeClass: java.lang.String callTheGap(java.lang.String)>",
				SourceSinkType.Return, -1, null, null));
		// Gap 0 base object to gap 1 base object
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Field, -1, null,
				"<soot.jimple.infoflow.test.methodSummary.IUserCodeClass: java.lang.String callTheGap(java.lang.String)>",
				SourceSinkType.GapBaseObject, -1, null,
				"<soot.jimple.infoflow.test.methodSummary.IUserCodeClass: java.lang.String callTheGap(java.lang.String)>"));

		assertEquals(6, flow.getFlowCount());
	}

	@Test(timeout = 100000)
	public void callToCall() {
		String mSig = "<soot.jimple.infoflow.test.methodSummary.ApiClass: java.lang.String callToCall(soot.jimple.infoflow.test.methodSummary.IGapClass,java.lang.String)>";
		MethodSummaries flow = createSummaries(mSig);

		// Parameter 0 to gap 0 base object
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 0, null, "", SourceSinkType.GapBaseObject,
				0, null, "<soot.jimple.infoflow.test.methodSummary.IGapClass: java.lang.String retrieveString()>"));
		// Gap 0 base object to gap 1 base object
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Field, 0, null,
				"<soot.jimple.infoflow.test.methodSummary.IGapClass: java.lang.String retrieveString()>",
				SourceSinkType.GapBaseObject, 0, null,
				"<soot.jimple.infoflow.test.methodSummary.IGapClass: java.lang.String callTheGap(java.lang.String)>"));
		// Parameter 1 to gap 1 argument 0
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Parameter, 1, null, "", SourceSinkType.Parameter, 0,
				null,
				"<soot.jimple.infoflow.test.methodSummary.IGapClass: java.lang.String callTheGap(java.lang.String)>"));
		// Gap 1 return to method return value
		assertTrue(containsFlow(flow.getAllFlows(), SourceSinkType.Return, -1, null,
				"<soot.jimple.infoflow.test.methodSummary.IGapClass: java.lang.String callTheGap(java.lang.String)>",
				SourceSinkType.Return, -1, null, null));

		assertEquals(5, flow.getFlowCount());
	}

	@Override
	protected SummaryGenerator getSummary() {
		SummaryGenerator sg = new SummaryGenerator();
		List<String> sub = new LinkedList<String>();
		sub.add("java.util.ArrayList");
		sg.setSubstitutedWith(sub);
		sg.getConfig().getAccessPathConfiguration().setAccessPathLength(5);
		sg.getConfig().getAccessPathConfiguration().setUseRecursiveAccessPaths(true);
		sg.setSootConfig(new DefaultSummaryConfig() {

			@Override
			public void setSootOptions(Options options, InfoflowConfiguration config) {
				super.setSootOptions(options, config);
				Options.v().set_exclude(Collections.singletonList("soot.jimple.infoflow.test.methodSummary.GapClass"));
			}

		});
		return sg;
	}

}
