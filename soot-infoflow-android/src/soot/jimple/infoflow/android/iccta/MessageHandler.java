package soot.jimple.infoflow.android.iccta;

import java.util.Collection;

import soot.Scene;
import soot.SootClass;

public final class MessageHandler {

	private MessageHandler() {
	}

	public static Collection<SootClass> getAllHandlers() {
		SootClass handler = Scene.v().getSootClass("android.os.Handler");
		Collection<SootClass> h = Scene.v().getOrMakeFastHierarchy().getSubclassesOf(handler);

		return h;
	}
}
