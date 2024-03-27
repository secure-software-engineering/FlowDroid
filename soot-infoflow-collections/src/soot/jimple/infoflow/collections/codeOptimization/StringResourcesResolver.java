package soot.jimple.infoflow.collections.codeOptimization;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.codeOptimization.ICodeOptimizer;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.toolkits.scalar.ConstantPropagatorAndFolder;
import soot.util.queue.QueueReader;

/**
 * Resolves Android String resources to constants in the code.
 *
 * @author Tim Lange
 */
public class StringResourcesResolver implements ICodeOptimizer {
    private static final String CONTEXT_CLASS = "android.content.Context";
    private static final String GET_STRING_SUBSIG = "java.lang.String getString(int)";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String fileName = null;
    private int runtime = -1;
    private int replaced = -1;

    public int getRuntime() {
        return runtime;
    }

    public int getReplacedStatementCount() {
        return replaced;
    }

    @Override
    public void initialize(InfoflowConfiguration config) {
        if (config instanceof InfoflowAndroidConfiguration)
            this.fileName = ((InfoflowAndroidConfiguration) config).getAnalysisFileConfig().getTargetAPKFile();
    }

    @Override
    public void run(InfoflowManager manager, Collection<SootMethod> excluded, ISourceSinkManager sourcesSinks,
                    ITaintPropagationWrapper taintWrapper) {
        // This optimization enables more constant propagation. It doesn't make sense to perform it without
        // constant propagation enabled.
        if (manager.getConfig().getCodeEliminationMode() == InfoflowConfiguration.CodeEliminationMode.NoCodeElimination)
            return;

        // Only android apps contain resources
        if (fileName == null)
            return;

        long beforeOptimization = System.nanoTime();

        ARSCFileParser parser;
        try {
            parser = ARSCFileParser.getInstance(new File(fileName));
        } catch (IOException e) {
            logger.error("Could not parse the ARSC file! Aborting string resource resolving...", e);
            return;
        }
        assert parser != null;

        SootClass contextClass = Scene.v().getSootClassUnsafe(CONTEXT_CLASS);
        if (contextClass == null) {
            logger.error("Could not load class " + CONTEXT_CLASS + ". Aborting string resource resolving...");
            return;
        }

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
                if (!manager.getHierarchy().isSubclass(callee.getDeclaringClass(), contextClass)
                        || !subSig.equals(GET_STRING_SUBSIG))
                    continue;

                // Extract the resource id
                Value arg0 = stmt.getInvokeExpr().getArg(0);
                // We expect an integer constant here
                if (!(arg0 instanceof IntConstant))
                    continue;
                int resourceId = ((IntConstant) arg0).value;

                // Get the string for the given resource id
                ARSCFileParser.AbstractResource res = parser.findResource(resourceId);
                if (!(res instanceof ARSCFileParser.StringResource))
                    continue;
                String str = ((ARSCFileParser.StringResource) res).getValue();

                // Construct a new constant assignment
                AssignStmt constantAssign = Jimple.v().newAssignStmt(((AssignStmt) stmt).getLeftOp(), StringConstant.v(str));
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
