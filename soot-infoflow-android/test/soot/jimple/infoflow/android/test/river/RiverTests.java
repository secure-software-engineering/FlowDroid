package soot.jimple.infoflow.android.test.river;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.FastHierarchy;
import soot.Scene;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SimpleSourceSinkManager;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.toolkits.scalar.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

public class RiverTests {
    protected static String appPath, libPath;

    protected static List<String> sources;
    protected static final String localSource = "<soot.jimple.infoflow.android.test.river.RiverTestCode: java.lang.String source()>";

    protected static List<String> primarySinks;
    protected static final String osWrite = "<java.io.OutputStream: void write(byte[])>";

    private static void appendWithSeparator(StringBuilder sb, File f) throws IOException {
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

    @BeforeClass
    public static void setUp() throws IOException {
        File testSrc = new File("./build" + File.separator + "testclasses");
        if (!testSrc.exists()) {
            fail("Test aborted - none of the test sources are available");
        }
        appPath = testSrc.toString();

        StringBuilder libPathBuilder = new StringBuilder();
        appendWithSeparator(libPathBuilder,
                new File(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar"));
        appendWithSeparator(libPathBuilder, new File("/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar"));
        libPath = libPathBuilder.toString();

        sources = new ArrayList<String>();
        sources.add(localSource);

        primarySinks = new ArrayList<String>();
        primarySinks.add(osWrite);
    }

    protected IInfoflow initInfoflow() {
        AbstractInfoflow result = new Infoflow("", false, new DefaultBiDiICFGFactory());
        result.setThrowExceptions(true);
        EasyTaintWrapper easyWrapper;
        try {
            easyWrapper = EasyTaintWrapper.getDefault();
            result.setTaintWrapper(easyWrapper);
        } catch (IOException e) {
            System.err.println("Could not initialized Taintwrapper:");
            e.printStackTrace();
        }
        result.getConfig().setAdditionalFlowsEnabled(true);
        result.getConfig().getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Fast);

        return result;
    }

    protected void checkInfoflow(IInfoflow infoflow, int resultCount) {
        if (infoflow.isResultAvailable()) {
            InfoflowResults map = infoflow.getResults();
            assertEquals(resultCount, map.size());
            assertTrue(primarySinks.stream().anyMatch(map::containsSinkMethod));
            assertTrue(primarySinks.stream()
                    .flatMap(sink -> sources.stream().map(source -> new Pair<>(sink, source)))
                    .anyMatch(p -> map.isPathBetweenMethods(p.getO1(), p.getO2())));
        } else {
            fail("result is not available");
        }
    }

    protected void negativeCheckInfoflow(IInfoflow infoflow) {
        if (infoflow.isResultAvailable()) {
            InfoflowResults map = infoflow.getResults();
            assertEquals(0, map.size());
            assertTrue(primarySinks.stream().noneMatch(map::containsSinkMethod));
        }
    }

    // Test condition met
    @Test(timeout = 300000)
    public void riverTest1() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.android.test.river.RiverTestCode: void riverTest1()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("testAPKs/SourceSinkDefinitions/RiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }

    // Test condition not met
    @Test(timeout = 300000)
    public void riverTest2() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.android.test.river.RiverTestCode: void riverTest2()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("testAPKs/SourceSinkDefinitions/RiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.negativeCheckInfoflow(infoflow);
    }

    // Test that we accept all conditional sinks if additional flows are disabled
    @Test(timeout = 300000)
    public void riverTest2b() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        infoflow.getConfig().setAdditionalFlowsEnabled(false);
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.android.test.river.RiverTestCode: void riverTest2()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("testAPKs/SourceSinkDefinitions/RiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }

    // Test condition met and the conditional sink is in a superclass
    @Test(timeout = 300000)
    public void riverTest3() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.android.test.river.RiverTestCode: void riverTest1()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("testAPKs/SourceSinkDefinitions/RiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }

    // Test condition not met and the conditional sink is in a superclass
    @Test(timeout = 300000)
    public void riverTest4() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.android.test.river.RiverTestCode: void riverTest2()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("testAPKs/SourceSinkDefinitions/RiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.negativeCheckInfoflow(infoflow);
    }

}
