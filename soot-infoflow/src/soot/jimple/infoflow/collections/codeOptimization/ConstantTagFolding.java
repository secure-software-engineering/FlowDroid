package soot.jimple.infoflow.collections.codeOptimization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.codeOptimization.ICodeOptimizer;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.tagkit.ConstantValueTag;
import soot.tagkit.Tag;

import java.util.Collection;
import java.util.Iterator;

/**
 * Folds in the constant of a {@link ConstantValueTag} into the code
 *
 * @author Tim Lange
 */
public class ConstantTagFolding implements ICodeOptimizer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

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
    }

    private Constant getConstant(FieldRef f) {
        SootField field = f.getField();
        for (Tag t : field.getTags()) {
            if (t instanceof ConstantValueTag)
                return ((ConstantValueTag) t).getConstant();
        }
        return null;
    }

    @Override
    public void run(InfoflowManager manager, Collection<SootMethod> entryPoints, ISourceSinkManager sourcesSinks,
                    ITaintPropagationWrapper taintWrapper) {
        // This optimization enables more constant propagation. It doesn't make sense to perform it without
        // constant propagation enabled.
        if (manager.getConfig().getCodeEliminationMode() == InfoflowConfiguration.CodeEliminationMode.NoCodeElimination)
            return;

        long beforeOptimization = System.nanoTime();

        // First pass: collect all constant fields
        ReplacementCandidates rcs = new ReplacementCandidates();
        ReachableMethods rm = Scene.v().getReachableMethods();
        for (Iterator<MethodOrMethodContext> iter = rm.listener(); iter.hasNext(); ) {
            SootMethod sm = iter.next().method();
            if (sm == null || !sm.hasActiveBody() || entryPoints.contains(sm))
                continue;

            for (Unit u : sm.retrieveActiveBody().getUnits()) {
                if (!(u instanceof AssignStmt))
                    continue;

                AssignStmt stmt = (AssignStmt) u;
                Value rightOp = stmt.getRightOp();
                if (!(rightOp instanceof FieldRef))
                    continue;

                Constant c = getConstant((FieldRef) rightOp);
                if (c == null)
                    continue;

                AssignStmt replacement = Jimple.v().newAssignStmt(stmt.getLeftOp(), c);
                rcs.add(sm, stmt, replacement);
            }
        }

        // Second pass: replace all fields with their constant values
        rcs.replace(manager.getICFG());

        replaced = rcs.size();
        runtime = (int) Math.round((System.nanoTime() - beforeOptimization) / 1E9);
        logger.info(String.format("Resolved %d constant fields in %d seconds.", replaced, runtime));
    }
}
