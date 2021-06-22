package soot.jimple.infoflow.android.callbacks;

import java.io.IOException;
import java.util.Set;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;

/**
 * A callback analyzer that favors performance over precision.
 * 
 * @author Steven Arzt
 *
 */
public class FastCallbackAnalyzer extends AbstractCallbackAnalyzer {

	public FastCallbackAnalyzer(InfoflowAndroidConfiguration config, Set<SootClass> entryPointClasses)
			throws IOException {
		super(config, entryPointClasses);
	}

	public FastCallbackAnalyzer(InfoflowAndroidConfiguration config, Set<SootClass> entryPointClasses,
			String callbackFile) throws IOException {
		super(config, entryPointClasses, callbackFile);
	}

	public FastCallbackAnalyzer(InfoflowAndroidConfiguration config, Set<SootClass> entryPointClasses,
			Set<String> androidCallbacks) throws IOException {
		super(config, entryPointClasses, androidCallbacks);
	}

	@Override
	public void collectCallbackMethods() {
		super.collectCallbackMethods();
		logger.info("Collecting callbacks in FAST mode...");

		// Find the mappings between classes and layouts
		findClassLayoutMappings();

		for (SootClass sc : Scene.v().getApplicationClasses()) {
			if (sc.isConcrete()) {
				for (SootMethod sm : sc.getMethods()) {
					if (sm.isConcrete()) {
						analyzeMethodForCallbackRegistrations(null, sm);
						analyzeMethodForDynamicBroadcastReceiver(sm);
						analyzeMethodForServiceConnection(sm);
					}
				}

				// Check for method overrides
				analyzeMethodOverrideCallbacks(sc);
			}
		}
	}

	/**
	 * Finds the mappings between classes and their respective layout files
	 */
	private void findClassLayoutMappings() {
		for (SootClass sc : Scene.v().getApplicationClasses()) {
			if (sc.isConcrete()) {
				for (SootMethod sm : sc.getMethods()) {
					if (!sm.isConcrete())
						continue;

					for (Unit u : sm.retrieveActiveBody().getUnits()) {
						if (u instanceof Stmt) {
							Stmt stmt = (Stmt) u;
							if (stmt.containsInvokeExpr()) {
								InvokeExpr inv = stmt.getInvokeExpr();
								if (invokesSetContentView(inv)) {
									for (Value val : inv.getArgs()) {
										Integer intValue = valueProvider.getValue(sm, stmt, val, Integer.class);
										if (intValue != null) {
											this.layoutClasses.put(sm.getDeclaringClass(), intValue);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void excludeEntryPoint(SootClass entryPoint) {
		// not supported
	}

}
