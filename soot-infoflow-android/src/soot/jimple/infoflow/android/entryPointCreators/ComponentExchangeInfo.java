package soot.jimple.infoflow.android.entryPointCreators;

import soot.SootClass;
import soot.SootMethod;

public class ComponentExchangeInfo {

	public final SootClass componentDataExchangeInterface;
	public final SootMethod getResultIntentMethod;
	public final SootMethod getIntentMethod;
	public final SootMethod setIntentMethod;

	public ComponentExchangeInfo(SootClass componentDataExchangeInterface, SootMethod getIntentMethod,
			SootMethod setIntentMethod, SootMethod getResultIntentMethod) {
		this.componentDataExchangeInterface = componentDataExchangeInterface;
		this.getIntentMethod = getIntentMethod;
		this.setIntentMethod = setIntentMethod;
		this.getResultIntentMethod = getResultIntentMethod;
	}

}
