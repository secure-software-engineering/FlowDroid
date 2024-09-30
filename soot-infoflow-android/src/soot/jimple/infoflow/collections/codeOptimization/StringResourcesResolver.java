package soot.jimple.infoflow.collections.codeOptimization;

import java.io.File;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.codeOptimization.ICodeOptimizer;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.util.queue.QueueReader;

/**
 * Resolves Android String resources to constants in the code.
 *
 * @author Tim Lange
 */
public class StringResourcesResolver implements ICodeOptimizer {
	private static final String CONTEXT_CLASS = "android.content.Context";
	private static final String RESOURCE_CLASS = "android.content.res.Resources";
	private static final String GET_STRING_SUBSIG = "java.lang.String getString(int)";
	private static final String GET_INT_SUBSIG = "int getInteger(int)";
	private static final String GET_BOOL_SUBSIG = "boolean getBoolean(int)";
	private static final String GET_FLOAT_SUBSIG = "float getFloat(int)";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final SetupApplication setupApplication;

	private File apkFile = null;
	private int runtime = -1;
	private int replaced = -1;

	public StringResourcesResolver(SetupApplication setupApplication) {
		this.setupApplication = setupApplication;
	}

	public int getRuntime() {
		return runtime;
	}

	public int getReplacedStatementCount() {
		return replaced;
	}

	@Override
	public void initialize(InfoflowConfiguration config) {
		if (config instanceof InfoflowAndroidConfiguration)
			this.apkFile = ((InfoflowAndroidConfiguration) config).getAnalysisFileConfig().getTargetAPKFile();
	}

	@Override
	public void run(InfoflowManager manager, Collection<SootMethod> excluded, ISourceSinkManager sourcesSinks,
			ITaintPropagationWrapper taintWrapper) {
		// This optimization enables more constant propagation. It doesn't make sense to
		// perform it without
		// constant propagation enabled.
		if (manager.getConfig().getCodeEliminationMode() == InfoflowConfiguration.CodeEliminationMode.NoCodeElimination)
			return;

		// Only android apps contain resources
		if (apkFile == null)
			return;

		long beforeOptimization = System.nanoTime();

		ARSCFileParser parser = setupApplication.getResources();
		if (parser == null) {
			logger.error("Could get the ARSC file parser! Aborting string resource resolving...");
			return;
		}
		assert parser != null;

		SootClass contextClass = Scene.v().getSootClassUnsafe(CONTEXT_CLASS);
		if (contextClass == null)
			logger.warn("Could not load class " + CONTEXT_CLASS + ".");
		SootClass resourceClass = Scene.v().getSootClassUnsafe(RESOURCE_CLASS);
		if (resourceClass == null)
			logger.warn("Could not load class " + RESOURCE_CLASS + ".");

		// First pass: Collect all statements to be replaced
		ReplacementCandidates rcs = new ReplacementCandidates();
		for (QueueReader<MethodOrMethodContext> rdr = Scene.v().getReachableMethods().listener(); rdr.hasNext();) {
			MethodOrMethodContext sm = rdr.next();
			SootMethod method = sm.method();
			if (method == null || !method.isConcrete() || excluded.contains(method))
				continue;

			UnitPatchingChain chain = method.retrieveActiveBody().getUnits();
			for (Unit unit : chain) {
				Stmt stmt = (Stmt) unit;
				// We only care for calls to a method where the returned value is used
				if (!stmt.containsInvokeExpr() || !(stmt instanceof AssignStmt))
					continue;

				// Check whether the invoke expressions calls a method we care about
				SootMethod callee = stmt.getInvokeExpr().getMethod();
				String subSig = callee.getSubSignature();

				boolean contextGetString = contextClass != null
						&& manager.getHierarchy().isSubclass(callee.getDeclaringClass(), contextClass)
						&& subSig.equals(GET_STRING_SUBSIG);
				if (!contextGetString) {
					boolean resourceGet = resourceClass != null
							&& manager.getHierarchy().isSubclass(callee.getDeclaringClass(), resourceClass)
							&& (subSig.equals(GET_STRING_SUBSIG) || subSig.equals(GET_INT_SUBSIG)
									|| subSig.equals(GET_BOOL_SUBSIG) || subSig.equals(GET_FLOAT_SUBSIG));
					if (!resourceGet)
						continue;
				}

				// Extract the resource id
				Value arg0 = stmt.getInvokeExpr().getArg(0);
				// We expect an integer constant here
				if (!(arg0 instanceof IntConstant))
					continue;
				int resourceId = ((IntConstant) arg0).value;

				// Get the string for the given resource id
				ARSCFileParser.AbstractResource res = parser.findResource(resourceId);
				Constant c;
				if (res instanceof ARSCFileParser.StringResource) {
					String str = ((ARSCFileParser.StringResource) res).getValue();
					c = StringConstant.v(str);
				} else if (res instanceof ARSCFileParser.IntegerResource) {
					int i = ((ARSCFileParser.IntegerResource) res).getValue();
					c = IntConstant.v(i);
				} else if (res instanceof ARSCFileParser.BooleanResource) {
					boolean b = ((ARSCFileParser.BooleanResource) res).getValue();
					c = IntConstant.v(b ? 1 : 0);
				} else if (res instanceof ARSCFileParser.FloatResource) {
					float f = ((ARSCFileParser.FloatResource) res).getValue();
					c = FloatConstant.v(f);
				} else {
					continue;
				}

				// Construct a new constant assignment
				AssignStmt constantAssign = Jimple.v().newAssignStmt(((AssignStmt) stmt).getLeftOp(), c);
				constantAssign.addTag(SimulatedCodeElementTag.TAG);
				rcs.add(method, stmt, constantAssign);
			}
		}

		// Second pass: replace call statements with constant assignments
		rcs.replace(manager.getICFG());

		replaced = rcs.size();
		runtime = (int) Math.round((System.nanoTime() - beforeOptimization) / 1E9);
		logger.info(String.format("Resolved %d android string resources in %d seconds.", replaced, runtime));
	}
}
