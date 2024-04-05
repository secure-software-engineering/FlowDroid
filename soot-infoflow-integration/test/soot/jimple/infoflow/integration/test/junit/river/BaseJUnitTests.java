package soot.jimple.infoflow.integration.test.junit.river;

import org.junit.Assert;
import org.junit.Before;
import soot.SootMethod;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.BaseSourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Abstract test class used for all river tests
 *
 * @author Tim Lange
 */
public abstract class BaseJUnitTests {
    static class SimpleSourceSinkManager extends BaseSourceSinkManager {
        public SimpleSourceSinkManager(Collection<? extends ISourceSinkDefinition> sources,
                                       Collection<? extends ISourceSinkDefinition> sinks,
                                       InfoflowConfiguration config) {
            super(sources, sinks, config);
        }

        @Override
        protected boolean isEntryPointMethod(SootMethod method) {
            return false;
        }
    }

    protected static String appPath, libPath;

    static void appendWithSeparator(StringBuilder sb, File f) throws IOException {
        if (f.exists()) {
            if (sb.length() > 0)
                sb.append(System.getProperty("path.separator"));
            sb.append(f.getCanonicalPath());
        }
    }

    @Before
    public void resetSootAndStream() throws IOException {
        soot.G.reset();
        System.gc();

    }

    public static void commonSetup() throws IOException {
        File testSrc = new File("build" + File.separator + "testclasses");
        if (!testSrc.exists()) {
            Assert.fail("Test aborted - none of the test sources are available");
        }
        appPath = testSrc.toString();

        StringBuilder libPathBuilder = new StringBuilder();
        String javaHomeStr = System.getProperty("java.home");
        boolean found = false;
        if (!javaHomeStr.isEmpty()) {
            // Find the Java 8 rt.jar even when the JVM is of a higher version
            File parentDir = new File(javaHomeStr).getParentFile();
            File[] files = parentDir.listFiles((dir, name) -> name.contains("java-1.8.0-") || name.contains("java-8-"));
            if (files != null) {
                for (File java8Path : files) {
                    File rtjar = new File(java8Path, "jre" + File.separator + "lib" + File.separator + "rt.jar");
                    if (rtjar.exists()) {
                        appendWithSeparator(libPathBuilder, rtjar);
                        found = true;
                        break;
                    }
                }
            }
        }
        if (!found) {
            // Try the default path on ubuntu
            appendWithSeparator(libPathBuilder, new File("/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar"));
            // Try the default path on fedora
            appendWithSeparator(libPathBuilder, new File("/usr/lib/jvm/java-1.8.0/jre/lib/rt.jar"));
        }
        libPath = libPathBuilder.toString();
        if (libPath.isEmpty())
            throw new RuntimeException("Could not find rt.jar!");
    }

    protected abstract ITaintPropagationWrapper getTaintWrapper();

    protected abstract void setConfiguration(InfoflowConfiguration config);

    protected IInfoflow initInfoflow() {
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
