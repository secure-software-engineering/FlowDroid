package soot.jimple.infoflow.test.methodSummary.junit;

public abstract class ApiTestHelper extends TestHelper {
	protected final static String CLASS_SIG = "soot.jimple.infoflow.test.methodSummary.ApiClass";
	protected final static String DATA_SIG = "soot.jimple.infoflow.test.methodSummary.Data";
	
	protected final static String DATA_TYPE = "soot.jimple.infoflow.test.methodSummary.Data";
	protected final static String NON_PRIMITIVE_VAR1 = "<"+CLASS_SIG + ": " + DATA_TYPE + " dataField>";
	protected final static String OBJECT_FIELD = "<"+CLASS_SIG + ": " + OBJECT_TYPE + " objectField>";
	protected final static String NON_PRIMITIVE_VAR2 = CLASS_SIG + ": " + DATA_TYPE + " dataField2";
	protected final static String PRIMITIVE_VAR = CLASS_SIG + ": " + INT_TYPE + " primitiveVariable";
	
	protected final static String DATA_STRING_FIELD = "<" + DATA_SIG + ": java.lang.String stringField>";
	
}
