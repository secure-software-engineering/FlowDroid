package soot.jimple.infoflow.integration.test.junit.river;

import java.io.IOException;
import java.util.Collection;

import org.junit.Before;

import soot.SootMethod;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.BaseSourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.test.junit.JUnitTests;

/**
 * Abstract test class used for all river tests
 *
 * @author Tim Lange
 */
public abstract class BaseJUnitTests extends JUnitTests {
	static class SimpleSourceSinkManager extends BaseSourceSinkManager {
		public SimpleSourceSinkManager(Collection<? extends ISourceSinkDefinition> sources,
				Collection<? extends ISourceSinkDefinition> sinks, InfoflowConfiguration config) {
			super(sources, sinks, config);
		}

		@Override
		protected boolean isEntryPointMethod(SootMethod method) {
			return false;
		}
	}

	@Before
	public void resetSootAndStream() throws IOException {
		soot.G.reset();
		System.gc();

	}

	protected abstract ITaintPropagationWrapper getTaintWrapper();

	protected abstract void setConfiguration(InfoflowConfiguration config);

	@Override
	protected AbstractInfoflow createInfoflowInstance() {
		AbstractInfoflow result = new Infoflow("", false, new DefaultBiDiICFGFactory());
		result.setThrowExceptions(true);
		result.setTaintWrapper(getTaintWrapper());
		setConfiguration(result.getConfig());
		return result;
	}

	protected SetupApplication initApplication(String fileName) {
		String androidJars = System.getenv("ANDROID_JARS");
		if (androidJars == null)
			androidJars = System.getProperty("ANDROID_JARS");
		if (androidJars == null)
			throw new RuntimeException("Android JAR dir not set");
		System.out.println("Loading Android.jar files from " + androidJars);

		SetupApplication setupApplication = new SetupApplication(androidJars, fileName);

		setupApplication.getConfig().setMergeDexFiles(true);
		setupApplication.setTaintWrapper(getTaintWrapper());
		setConfiguration(setupApplication.getConfig());

		return setupApplication;
	}
}
