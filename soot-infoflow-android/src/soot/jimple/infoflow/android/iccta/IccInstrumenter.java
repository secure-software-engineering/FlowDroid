package soot.jimple.infoflow.android.iccta;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootClass;
import soot.jimple.infoflow.entryPointCreators.android.components.ComponentEntryPointCollection;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;

public class IccInstrumenter implements PreAnalysisHandler {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final String iccModel;
	private final SootClass dummyMainClass;
	private final ComponentEntryPointCollection componentToEntryPoint;

	public IccInstrumenter(String iccModel, SootClass dummyMainClass,
			ComponentEntryPointCollection componentToEntryPoint) {
		this.iccModel = iccModel;
		this.dummyMainClass = dummyMainClass;
		this.componentToEntryPoint = componentToEntryPoint;
	}

	@Override
	public void onBeforeCallgraphConstruction() {
		logger.info("[IccTA] Launching IccTA Transformer...");

		logger.info("[IccTA] Loading the ICC Model...");
		Ic3Provider provider = new Ic3Provider(iccModel);
		List<IccLink> iccLinks = provider.getIccLinks();
		logger.info("[IccTA] ...End Loading the ICC Model");

		logger.info("[IccTA] Lauching ICC Redirection Creation...");
		IccRedirectionCreator redirectionCreator = new IccRedirectionCreator(dummyMainClass, componentToEntryPoint);
		for (IccLink link : iccLinks) {
			if (link.fromU == null) {
				continue;
			}
			redirectionCreator.redirectToDestination(link);
		}
		logger.info("[IccTA] ...End ICC Redirection Creation");
	}

	@Override
	public void onAfterCallgraphConstruction() {
		//
	}
}
