package soot.jimple.infoflow.android.test.xmlParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.android.test.BaseJUnitTests;
import soot.jimple.infoflow.river.AdditionalFlowCondition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkCondition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;

/**
 * Testing the new xml-parser with the new xml format
 * 
 * @author Jannik Juergens
 *
 */
public class XmlParserTest extends BaseJUnitTests {

	/**
	 * Compares the new and the old Parser for different xml files
	 * 
	 * @param xmlFile    in new format
	 * @param oldXmlFile
	 * @throws IOException
	 */
	private void compareParserResults(File xmlFile, File oldXmlFile) throws IOException {
		XMLSourceSinkParser newParser = XMLSourceSinkParser.fromFile(xmlFile);
		PermissionMethodParser oldParser = PermissionMethodParser.fromFile(oldXmlFile);

		// The old format can't specify access paths, so we need to fix the data
		// objects for not comparing apples and oranges
		Set<ISourceSinkDefinition> cleanedSources = new HashSet<>();
		Set<ISourceSinkDefinition> cleanedSinks = new HashSet<>();
		for (ISourceSinkDefinition def : newParser.getSources()) {
			MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) def;
			cleanedSources.add(new MethodSourceSinkDefinition(methodDef.getMethod()));
		}
		for (ISourceSinkDefinition def : newParser.getSinks()) {
			MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) def;
			cleanedSinks.add(new MethodSourceSinkDefinition(methodDef.getMethod()));
		}

		if (newParser != null && oldParser != null) {
			Assert.assertEquals(oldParser.getSources(), cleanedSources);
			Assert.assertEquals(oldParser.getSinks(), cleanedSinks);
		} else
			Assert.fail();
	}

	/**
	 * Test with a empty xml file
	 * 
	 * @throws IOException
	 */
	@Test(expected = IOException.class)
	public void emptyXmlTest() throws IOException {
		File xmlFile = new File(getInfoflowAndroidRoot(), "testXmlParser/empty.xml");
		compareParserResults(xmlFile, xmlFile);
	}

	/**
	 * Test with a complete xml file
	 * 
	 * @throws IOException
	 */
	@Test
	public void completeXmlTest() throws IOException {
		File rootDir = getInfoflowAndroidRoot();
		File xmlFile = new File(rootDir, "testXmlParser/complete.xml");
		File oldXmlFile = new File(rootDir, "testXmlParser/completeOld.txt");
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * Test with a empty txt file
	 * 
	 * @throws IOException
	 */
	@Test(expected = IOException.class)
	public void emptyTxtTest() throws IOException {
		File xmlFile = new File(getInfoflowAndroidRoot(), "testXmlParser/empty.txt");
		compareParserResults(xmlFile, xmlFile);
	}

	/**
	 * Test with a complete txt file
	 * 
	 * @throws IOException
	 */
	@Test
	public void completeTxtTest() throws IOException {
		File rootDir = getInfoflowAndroidRoot();
		File xmlFile = new File(rootDir, "testXmlParser/complete.txt");
		File oldXmlFile = new File(rootDir, "testXmlParser/completeOld.txt");
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * Test with a incomplete but valid xml file
	 * 
	 * @throws IOException
	 */
	@Test
	public void missingPartsXmlTest() throws IOException {
		File rootDir = getInfoflowAndroidRoot();
		File xmlFile = new File(rootDir, "testXmlParser/missingParts.xml");
		File oldXmlFile = new File(rootDir, "testXmlParser/missingPartsOld.txt");
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * Test with a incomplete but valid xml file
	 * 
	 * @throws IOException
	 */
	@Test(expected = IOException.class)
	public void notValidXmlTest() throws IOException {
		File rootDir = getInfoflowAndroidRoot();
		File xmlFile = new File(rootDir, "testXmlParser/notValid.xml");
		File oldXmlFile = new File(rootDir, "testXmlParser/completeOld.txt");
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * manual verification of the parser result
	 * 
	 * @throws IOException
	 */
	@Test
	public void verifyParserResultTest() throws IOException {
		// parsing data from xml file
		File xmlFile = new File(getInfoflowAndroidRoot(), "testXmlParser/complete.xml");
		XMLSourceSinkParser newParser = XMLSourceSinkParser.fromFile(xmlFile);
		Set<? extends ISourceSinkDefinition> sourceListParser = newParser.getSources();
		Set<? extends ISourceSinkDefinition> sinkListParser = newParser.getSinks();

		// create two methods with reference data
		String methodName = "sourceTest";
		String returnType = "java.lang.String";
		String className = "com.example.androidtest.Sources";
		List<String> methodParameters = new ArrayList<String>();
		methodParameters.add("com.example.androidtest.MyTestObject");
		methodParameters.add("int");
		AndroidMethod am1 = new AndroidMethod(methodName, methodParameters, returnType, className);
		am1.setSourceSinkType(SourceSinkType.Both);

		methodParameters = new ArrayList<String>();
		methodParameters.add("double");
		methodParameters.add("double");
		AndroidMethod am2 = new AndroidMethod("sinkTest", methodParameters, "void", "com.example.androidtest.Sinks");
		am2.setSourceSinkType(SourceSinkType.Sink);

		// Check the loaded access paths (sources)
		Assert.assertEquals(1, sourceListParser.size());
		MethodSourceSinkDefinition loadedSource = (MethodSourceSinkDefinition) sourceListParser.iterator().next();
		Assert.assertEquals(am1, loadedSource.getMethod());
		Assert.assertEquals(0, loadedSource.getBaseObjectCount());
		Assert.assertEquals(2, loadedSource.getParameterCount());
		Assert.assertEquals(1, loadedSource.getReturnValueCount());

		// Check the loaded access paths (sinks)
		Assert.assertEquals(2, sinkListParser.size());
		for (ISourceSinkDefinition def : sinkListParser) {
			MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) def;
			Assert.assertTrue(methodDef.getMethod().equals(am1) || methodDef.getMethod().equals(am2));
			if (methodDef.getMethod().equals(am1)) {
				Assert.assertEquals(1, methodDef.getBaseObjectCount());
				Assert.assertEquals(1, methodDef.getParameterCount());
			} else if (methodDef.getMethod().equals(am2)) {
				Assert.assertEquals(1, methodDef.getParameterCount());
			} else
				Assert.fail("should never happen");
		}
	}

	/**
	 * Test that additional flows are parsed correctly.
	 *
	 * @throws IOException
	 */
	@Test
	public void additionalFlowsXMLTest() throws IOException {
		File xmlFile = new File(getInfoflowAndroidRoot(), "testXmlParser/additionalFlows.xml");
		XMLSourceSinkParser parser = XMLSourceSinkParser.fromFile(xmlFile);
		Set<ISourceSinkDefinition> sinkSet = parser.getSinks();

		final String stringWriteSig = "<java.io.PrintWriter: void write(java.lang.String)>";
		final String stringOffsetWriteSig = "<java.io.PrintWriter: void write(java.lang.String,int,int)>";
		final String intWriteSig = "<java.io.PrintWriter: void write(int)>";
		final String byteWriteSig = "<java.io.PrintWriter: void write(byte[])>";

		final String openConSig = "<java.net.URL: java.net.URLConnection openConnection()>";
		final String servletSig = "<javax.servlet.ServletResponse: java.io.PrintWriter getWriter()>";
		final String httpSrvSig = "<javax.servlet.http.HttpServletResponse: java.io.PrintWriter getWriter()>";

		final String byteArrayClass = "java.io.ByteArrayOutputStream";

		Assert.assertEquals(4, sinkSet.size());
		boolean foundStrSig = false, foundStrOffsetSig = false, foundIntSig = false, foundByteSig = false;
		for (ISourceSinkDefinition sink : sinkSet) {
			Assert.assertTrue(sink instanceof MethodSourceSinkDefinition);
			MethodSourceSinkDefinition methodSink = (MethodSourceSinkDefinition) sink;
			String methodSig = methodSink.getMethod().getSignature();

			Set<SourceSinkCondition> conds = sink.getConditions();
			switch (methodSig) {
			case stringWriteSig: {
				Assert.assertEquals(1, conds.size());
				AdditionalFlowCondition cond = (AdditionalFlowCondition) conds.stream().findAny().get();
				Set<String> mRefs = cond.getSignaturesOnPath();
				Assert.assertEquals(1, mRefs.size());
				Assert.assertTrue(mRefs.contains(openConSig));
				Assert.assertEquals(0, cond.getClassNamesOnPath().size());
				Assert.assertEquals(0, cond.getExcludedClassNames().size());
				foundStrSig = true;
				break;
			}
			case stringOffsetWriteSig: {
				Assert.assertEquals(1, conds.size());
				AdditionalFlowCondition cond = (AdditionalFlowCondition) conds.stream().findAny().get();
				Assert.assertEquals(0, cond.getSignaturesOnPath().size());
				Set<String> cRefs = cond.getClassNamesOnPath();
				Assert.assertEquals(1, cRefs.size());
				Assert.assertTrue(cRefs.contains("java.lang.String"));
				Assert.assertEquals(0, cond.getExcludedClassNames().size());
				foundStrOffsetSig = true;
				break;
			}
			case intWriteSig: {
				Assert.assertEquals(1, conds.size());
				AdditionalFlowCondition cond = (AdditionalFlowCondition) conds.stream().findAny().get();
				Set<String> mRefs = cond.getSignaturesOnPath();
				Assert.assertEquals(2, mRefs.size());
				Assert.assertTrue(mRefs.contains(servletSig));
				Assert.assertTrue(mRefs.contains(httpSrvSig));
				Assert.assertEquals(0, cond.getClassNamesOnPath().size());
				Assert.assertEquals(0, cond.getExcludedClassNames().size());
				foundIntSig = true;
				break;
			}
			case byteWriteSig: {
				Assert.assertEquals(2, conds.size());
				boolean foundServlet = false, foundHttpServlet = false;
				for (SourceSinkCondition cond : conds) {
					Set<String> mRefs = ((AdditionalFlowCondition) cond).getSignaturesOnPath();
					Assert.assertEquals(1, mRefs.size());
					Assert.assertEquals(0, ((AdditionalFlowCondition) cond).getClassNamesOnPath().size());

					if (mRefs.contains(servletSig)) {
						foundServlet = true;
						Assert.assertEquals(2, ((AdditionalFlowCondition) cond).getExcludedClassNames().size());
						Assert.assertTrue(((AdditionalFlowCondition) cond).getExcludedClassNames().stream()
								.allMatch(cn -> cn.equals(byteArrayClass) || cn.equals("java.lang.Object")));
					} else if (mRefs.contains(httpSrvSig)) {
						foundHttpServlet = true;
						Assert.assertEquals(1, ((AdditionalFlowCondition) cond).getExcludedClassNames().size());
						Assert.assertTrue(((AdditionalFlowCondition) cond).getExcludedClassNames().stream()
								.allMatch(cn -> cn.equals(byteArrayClass) || cn.equals("java.lang.String")));
					} else {
						Assert.fail();
					}
				}
				Assert.assertTrue(foundServlet && foundHttpServlet);
				foundByteSig = true;
				break;
			}
			default:
				Assert.fail();
			}
		}
		Assert.assertTrue(foundStrSig && foundStrOffsetSig && foundIntSig && foundByteSig);
	}
}
