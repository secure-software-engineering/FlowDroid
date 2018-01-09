package soot.jimple.infoflow.android.iccta;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.tagkit.Tag;
import soot.util.Chain;

public class JimpleIndexNumberTransformer implements PreAnalysisHandler {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void onBeforeCallgraphConstruction() {
		Chain<SootClass> sootClasses = Scene.v().getClasses();

		for (Iterator<SootClass> iter = sootClasses.iterator(); iter.hasNext();) {
			SootClass sc = iter.next();

			// Putting all the code in a try-catch.
			// Just trying the best to put the index number to
			// "JimpleIndexNumberTag" of Stmt.
			try {
				List<SootMethod> sms = sc.getMethods();

				for (SootMethod sm : sms) {
					Body b = sm.retrieveActiveBody();

					PatchingChain<Unit> units = b.getUnits();

					int indexNumber = 0;

					for (Iterator<Unit> iterU = units.snapshotIterator(); iterU.hasNext();) {
						Stmt stmt = (Stmt) iterU.next();

						Tag t = new JimpleIndexNumberTag(indexNumber++);
						stmt.addTag(t);
					}
				}
			} catch (Exception ex) {
				logger.error("Exception in " + sc.getName(), ex);
			}
		}
	}

	@Override
	public void onAfterCallgraphConstruction() {
		//
	}
}
