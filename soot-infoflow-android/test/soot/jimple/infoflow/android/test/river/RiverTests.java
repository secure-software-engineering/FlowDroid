package soot.jimple.infoflow.android.test.river;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.Local;
import soot.Scene;
import soot.jimple.Stmt;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.river.AdditionalFlowInfoSpecification;
import soot.jimple.infoflow.river.IConditionalFlowManager;
import soot.jimple.infoflow.river.IUsageContextProvider;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SimpleSourceSinkManager;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.util.DebugFlowFunctionTaintPropagationHandler;
import soot.toolkits.scalar.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

public class RiverTests {
    protected static String appPath, libPath;

    protected static List<String> sources;
    protected static final String localSource = "<soot.jimple.infoflow.android.test.river.RiverTestCode: java.lang.String source()>";

    protected static List<String> primarySinks;
    protected static final String osWrite = "<java.io.OutputStream: void write(byte[])>";
    protected static final String writerWrite = "<java.io.Writer: void write(java.lang.String)>";
    protected static final String sendToUrl = "<soot.jimple.infoflow.android.test.river.RiverTestCode: void sendToUrl(java.net.URL,java.lang.String)>";
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
        primarySinks.add(writerWrite);
        primarySinks.add(sendToUrl);
    }

    protected IInfoflow initInfoflow() {
        AbstractInfoflow result = new Infoflow("", false, new DefaultBiDiICFGFactory());
        result.setThrowExceptions(true);
        EasyTaintWrapper easyWrapper;
        try {
            easyWrapper = EasyTaintWrapper.getDefault();
            // Add methods used in the test cases
            easyWrapper.addMethodForWrapping("java.io.BufferedOutputStream", "void <init>(java.io.OutputStream)");
            easyWrapper.addMethodForWrapping("java.io.BufferedWriter", "void <init>(java.io.Writer)");
            easyWrapper.addMethodForWrapping("java.io.OutputStreamWriter", "void <init>(java.io.OutputStream)");
            easyWrapper.addMethodForWrapping("java.io.Writer", "void write(java.lang.String)");
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

    // Test condition met
    @Test(timeout = 300000)
    public void riverTest5() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        infoflow.setTaintPropagationHandler(new DebugFlowFunctionTaintPropagationHandler());
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.android.test.river.RiverTestCode: void riverTest5()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("testAPKs/SourceSinkDefinitions/RiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 1);
    }

    // Test condition not met
    @Test(timeout = 300000)
    public void riverTest6() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.android.test.river.RiverTestCode: void riverTest6()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("testAPKs/SourceSinkDefinitions/RiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.negativeCheckInfoflow(infoflow);
    }

    // Example from the paper
    @Test(timeout = 300000)
    public void riverTest7() throws IOException {
        IInfoflow infoflow = this.initInfoflow();
        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.android.test.river.RiverTestCode: void riverTest7()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("testAPKs/SourceSinkDefinitions/RiverSourcesAndSinks.xml");
        ISourceSinkManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());
        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), ssm);
        this.checkInfoflow(infoflow, 2);
    }

    // Test Usage Contexts
    @Test(timeout = 300000)
    public void riverTest8() throws IOException {
        IInfoflow infoflow = this.initInfoflow();

        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.android.test.river.RiverTestCode: void riverTest8()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("testAPKs/SourceSinkDefinitions/RiverSourcesAndSinks.xml");
        IConditionalFlowManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());

        infoflow.setPreProcessors(Collections.singleton(new PreAnalysisHandler() {
            @Override
            public void onBeforeCallgraphConstruction() {

            }

            @Override
            public void onAfterCallgraphConstruction() {
                ssm.registerSecondarySource(Scene.v().grabMethod("<java.net.URL: void <init>(java.lang.String)>"));
            }
        }));

        infoflow.setUsageContextProvider(new IUsageContextProvider() {
            @Override
            public Set<AdditionalFlowInfoSpecification> needsAdditionalInformation(Stmt stmt) {
                if (stmt.containsInvokeExpr()) {
                    String sig = stmt.getInvokeExpr().getMethod().getSignature();
                    if (sig.equals(sendToUrl)) {
                        Local local = (Local) stmt.getInvokeExpr().getArg(0);
                        return Collections.singleton(new AdditionalFlowInfoSpecification(local, stmt));
                    }
                }
                return Collections.emptySet();
            }

            @Override
            public boolean isStatementWithAdditionalInformation(Stmt stmt) {
                return false;
            }
        });

        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), (ISourceSinkManager) ssm);
        this.checkInfoflow(infoflow, 1);

        // Check that the usage context was found
        Assert.assertTrue(infoflow.getResults().getAdditionalResultSet().stream().anyMatch(dfResult ->
                dfResult.getSink().getStmt().toString().contains("sendToUrl")
                && dfResult.getSource().getStmt().toString().contains("java.net.URL: void <init>")));
    }

    @Test(timeout = 300000)
    public void riverTest8b() throws IOException {
        IInfoflow infoflow = this.initInfoflow();

        List<String> epoints = new ArrayList();
        epoints.add("<soot.jimple.infoflow.android.test.river.RiverTestCode: void riverTest8()>");
        XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile("testAPKs/SourceSinkDefinitions/RiverSourcesAndSinks.xml");
        IConditionalFlowManager ssm = new SimpleSourceSinkManager(parser.getSources(), parser.getSinks(), infoflow.getConfig());

        infoflow.setUsageContextProvider(new IUsageContextProvider() {
            @Override
            public Set<AdditionalFlowInfoSpecification> needsAdditionalInformation(Stmt stmt) {
                if (stmt.containsInvokeExpr()) {
                    String sig = stmt.getInvokeExpr().getMethod().getSignature();
                    if (sig.equals(sendToUrl)) {
                        Local local = (Local) stmt.getInvokeExpr().getArg(0);
                        return Collections.singleton(new AdditionalFlowInfoSpecification(local, stmt));
                    }
                }
                return Collections.emptySet();
            }

            @Override
            public boolean isStatementWithAdditionalInformation(Stmt stmt) {
                if (!stmt.containsInvokeExpr())
                    return false;

                return stmt.getInvokeExpr().getMethod().getSignature().contains("java.net.URL: void <init>");
            }
        });

        infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), (ISourceSinkManager) ssm);
        this.checkInfoflow(infoflow, 1);

        // Check that the usage context was found
        Assert.assertTrue(infoflow.getResults().getAdditionalResultSet().stream().anyMatch(dfResult ->
                dfResult.getSink().getStmt().toString().contains("sendToUrl")
                        && dfResult.getSource().getStmt().toString().contains("java.net.URL: void <init>")));
    }

}
