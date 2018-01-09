package soot.jimple.infoflow.android.test.xmlParser;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import soot.jimple.infoflow.android.data.AndroidMethod;

/**
 * Testclass for the static Methode AndroidMethod.createfromSignature
 * 
 * @author Joern Tillmanns 
 */
public class SigToAndroidMethodTest {

	@Test
	public void signaturTest() {
		String methodName = "sourceTest";
		String returnType = "void";
		String className = "com.example.androidtest.Sources";
		List<String> methodParameters = new ArrayList<String>();
		methodParameters.add("com.example.androidtest.MyTestObject");
		methodParameters.add("int");
		AndroidMethod am1 = new AndroidMethod(methodName, methodParameters, returnType, className);
		String sig = am1.getSignature();
		AndroidMethod am2 = AndroidMethod.createFromSignature(sig);
		Assert.assertEquals(am1, am2);
	}

	/**
	 * Testing AndroidMethod.createfromSignature for signatures without
	 * surrounding <>
	 */
	@Test
	public void signatureTest2() {
		String methodName = "sourceTest";
		String returnType = "void";
		String className = "com.example.androidtest.Sources";
		List<String> methodParameters = new ArrayList<String>();
		methodParameters.add("com.example.androidtest.MyTestObject");
		methodParameters.add("int");
		AndroidMethod am1 = new AndroidMethod(methodName, methodParameters, returnType, className);
		String sig = am1.getSignature();
		sig = sig.substring(1, sig.length() - 1);
		AndroidMethod am2 = AndroidMethod.createFromSignature(sig);

		Assert.assertEquals(am1, am2);
	}

	/**
	 * Testing AndroidMethod.createfromSignature if parameters are switched
	 */
	@Test
	public void switchedParameterTest() {
		String methodName = "poll";
		String returnType = "java.lang.Object";
		String className = "java.util.concurrent.LinkedBlockingQueue";
		List<String> methodParameters = new ArrayList<String>();
		methodParameters.add("java.util.concurrent.TimeUnit");
		methodParameters.add("long");
		AndroidMethod am1 = new AndroidMethod(methodName, methodParameters, returnType, className);
		String sig = "&lt;java.util.concurrent.LinkedBlockingQueue: java.lang.Object poll(long,java.util.concurrent.TimeUnit)&gt";
		AndroidMethod am2 = AndroidMethod.createFromSignature(sig);

		Assert.assertNotEquals(am1, am2);
	}

}
