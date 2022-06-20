package soot.jimple.infoflow.android.iccta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.Local;
import soot.LocalGenerator;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.RefType;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.entryPointCreators.components.ComponentEntryPointCollection;
import soot.jimple.infoflow.entryPointCreators.SimulatedCodeElementTag;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.util.Chain;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

public class IccInstrumenter implements PreAnalysisHandler {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final String iccModel;
	protected final SootClass dummyMainClass;
	protected final ComponentEntryPointCollection componentToEntryPoint;

	protected IccRedirectionCreator redirectionCreator = null;

	protected final SootMethod smMessengerSend;
	protected final SootMethod smSendMessenge;
	protected final Set<SootMethod> processedMethods = new HashSet<>();
	protected final MultiMap<Body, Unit> instrumentedUnits = new HashMultiMap<>();

	public IccInstrumenter(String iccModel, SootClass dummyMainClass,
			ComponentEntryPointCollection componentToEntryPoint) {
		this.iccModel = iccModel;
		this.dummyMainClass = dummyMainClass;
		this.componentToEntryPoint = componentToEntryPoint;

		// Fetch some Soot methods
		smMessengerSend = Scene.v().grabMethod("<android.os.Messenger: void send(android.os.Message)>");
		smSendMessenge = Scene.v().grabMethod("<android.os.Handler: boolean sendMessage(android.os.Message)>");
	}

	@Override
	public void onBeforeCallgraphConstruction() {
		logger.info("[IccTA] Launching IccTA Transformer...");

		logger.info("[IccTA] Loading the ICC Model...");
		Ic3Provider provider = new Ic3Provider(iccModel);
		List<IccLink> iccLinks = provider.getIccLinks();
		logger.info("[IccTA] ...End Loading the ICC Model");

		// Create the redirection creator
		if (redirectionCreator == null)
			redirectionCreator = new IccRedirectionCreator(dummyMainClass, componentToEntryPoint);
		else
			redirectionCreator.undoInstrumentation();

		logger.info("[IccTA] Lauching ICC Redirection Creation...");
		for (IccLink link : iccLinks) {
			if (link.fromU == null) {
				continue;
			}
			redirectionCreator.redirectToDestination(link);
		}

		// Remove any potential leftovers from the last last instrumentation
		undoInstrumentation();

		// Instrument the messenger class
		instrumentMessenger();

		// Remove data that is no longer needed
		processedMethods.clear();

		logger.info("[IccTA] ...End ICC Redirection Creation");
	}

	/**
	 * Removes all units generated through instrumentation
	 */
	protected void undoInstrumentation() {
		for (Body body : instrumentedUnits.keySet()) {
			for (Unit u : instrumentedUnits.get(body)) {
				body.getUnits().remove(u);
			}
		}
		instrumentedUnits.clear();
	}

	protected void instrumentMessenger() {
		logger.info("Launching Messenger Transformer...");

		Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();
		Map<Value,String> handlerClass= new HashMap<>();
		Map<String,String> handlerInner= new HashMap<>();

		for (Iterator<SootClass> iter = applicationClasses.snapshotIterator(); iter.hasNext();) {
			SootClass sootClass = iter.next();

			// We copy the list of methods to emulate a snapshot iterator which
			// doesn't exist for methods in Soot
			List<SootMethod> methodCopyList = new ArrayList<>(sootClass.getMethods());
			for (SootMethod sootMethod : methodCopyList) {
				if (sootMethod.isConcrete()) {
					final Body body = sootMethod.retrieveActiveBody();				

					// Mark the method as processed
					if (!processedMethods.add(sootMethod))
						continue;

					for (Iterator<Unit> unitIter = body.getUnits().snapshotIterator(); unitIter.hasNext();) {
						Unit unit = unitIter.next();
						Stmt stmt = (Stmt) unit;
						//+++ collect handler fields signature	
						if (stmt.containsFieldRef())
							if(stmt.getFieldRef().getType() instanceof RefType)
								if(((RefType)stmt.getFieldRef()).getSootClass().getName().contains("android.os.Handler")) {
									if(stmt.getUseBoxes().size() > 1)
										handlerInner.putIfAbsent(stmt.getFieldRef().getField().getSignature(),stmt.getUseBoxes().get(1).getValue().getType().toString());
									// use value as locator
									handlerClass.put(stmt.getDefBoxes().get(0).getValue(),stmt.getFieldRef().getField().getSignature());
									}													
					}

				}

			}

								
		}
		//instrument the outerclass
		for(Value v : handlerClass.keySet())
			if(v!=null)
				//generateSendMessage(v.getClass(),handlerClass,handlerInner);
				System.out.println("##" + v + v.getType());

	}

	public void generateSendMessage(String sc,Map<String,String> handlerClass,Map<String,String> handlerInner) {
		
		if(sc == null || handlerClass.isEmpty() || handlerInner.isEmpty())
			return;
		SootClass sootClass = Scene.v().getSootClass(sc);
		List<SootMethod> methodCopyList = new ArrayList<>(sootClass.getMethods());
		for (SootMethod sootMethod : methodCopyList) {
			if (sootMethod.isConcrete()) {
				final Body body = sootMethod.retrieveActiveBody();
				final LocalGenerator lg = Scene.v().createLocalGenerator(body);
				
				for (Iterator<Unit> unitIter = body.getUnits().snapshotIterator(); unitIter.hasNext();) {
					Unit unit = unitIter.next();
					Stmt stmt = (Stmt) unit;
					if (stmt.containsInvokeExpr()) {
						SootMethod callee = stmt.getInvokeExpr().getMethod();
						
						// For sendMessage(), we directly call the respective handler.handleMessage()
						if (callee == smMessengerSend || callee == smSendMessenge) {
							//collect the value for sendMessage()
							String hc = handlerClass.get(sootClass.getName() + ":" + stmt.getInvokeExpr().getUseBoxes().get(1).getValue().toString());
							//System.out.println("--1: "+hc);
							Set<SootClass> handlers = MessageHandler.v().getAllHandlers();								
							for (SootClass handler : handlers) {
								// matching the handler and its signature	
								if(hc != null && handlerInner.get(hc) == handler.getName()) {
									Local handlerLocal = lg.generateLocal(handler.getType());													
									SootMethod hmMethod = handler.getMethod("void handleMessage(android.os.Message)");
									Unit callHMU = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(
											handlerLocal, hmMethod.makeRef(), stmt.getInvokeExpr().getArg(0)));
									callHMU.addTag(SimulatedCodeElementTag.TAG);
									body.getUnits().insertAfter(callHMU, stmt);
									instrumentedUnits.put(body, callHMU);
									break;
								}			
							}
						}
					}	
												
					
				}

			}
		}
		
	}

	@Override
	public void onAfterCallgraphConstruction() {
		//
	}
}
