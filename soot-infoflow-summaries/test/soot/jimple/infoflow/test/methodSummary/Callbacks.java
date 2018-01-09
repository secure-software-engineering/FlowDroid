package soot.jimple.infoflow.test.methodSummary;

public class Callbacks {
	
	public interface MyCallbacks {
		
		public String transform(String in);
		public void transformObject(Data o);
		
	}
	
	public String paraToCallbackToReturn(String data, MyCallbacks cbs) {
		String foo = cbs.transform(data);
		return foo;
	}

	private MyCallbacks cbs = null;
	
	public void setCallbacks(MyCallbacks cbs) {
		this.cbs = cbs;
	}
	
	public String fieldCallbackToReturn(String data) {
		String foo = cbs.transform(data);
		return foo;
	}

	public Data fieldCallbackToField(Data o) {
		cbs.transformObject(o);
		return o;
	}

}
