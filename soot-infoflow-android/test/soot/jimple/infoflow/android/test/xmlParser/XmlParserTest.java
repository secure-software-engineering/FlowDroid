package soot.jimple.infoflow.android.test.xmlParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;

/**
 * Testing the new xml-parser with the new xml format
 * 
 * @author Jannik Juergens
 *
 */
public class XmlParserTest {

	/**
	 * Compares the new and the old Parser for different xml files
	 * 
	 * @param xmlFile    in new format
	 * @param oldXmlFile
	 * @throws IOException
	 */
	private void compareParserResults(String xmlFile, String oldXmlFile) throws IOException {
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
		String xmlFile = "testXmlParser/empty.xml";
		compareParserResults(xmlFile, xmlFile);
	}

	/**
	 * Test with a complete xml file
	 * 
	 * @throws IOException
	 */
	@Test
	public void completeXmlTest() throws IOException {
		String xmlFile = "testXmlParser/complete.xml";
		String oldXmlFile = "testXmlParser/completeOld.txt";
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * Test with a empty txt file
	 * 
	 * @throws IOException
	 */
	@Test(expected = IOException.class)
	public void emptyTxtTest() throws IOException {
		String xmlFile = "testXmlParser/empty.txt";
		compareParserResults(xmlFile, xmlFile);
	}

	/**
	 * Test with a complete txt file
	 * 
	 * @throws IOException
	 */
	@Test
	public void completeTxtTest() throws IOException {
		String xmlFile = "testXmlParser/complete.txt";
		String oldXmlFile = "testXmlParser/completeOld.txt";
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * Test with a incomplete but valid xml file
	 * 
	 * @throws IOException
	 */
	@Test
	public void missingPartsXmlTest() throws IOException {
		String xmlFile = "testXmlParser/missingParts.xml";
		String oldXmlFile = "testXmlParser/missingPartsOld.txt";
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * Test with a incomplete but valid xml file
	 * 
	 * @throws IOException
	 */
	@Test(expected = IOException.class)
	public void notValidXmlTest() throws IOException {
		String xmlFile = "testXmlParser/notValid.xml";
		String oldXmlFile = "testXmlParser/completeOld.txt";
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * manual verification of the parser result
	 * 
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	@Test
	public void verifyParserResultTest() throws IOException, XmlPullParserException {
		// parsing data from xml file
		String xmlFile = "testXmlParser/complete.xml";
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
}
