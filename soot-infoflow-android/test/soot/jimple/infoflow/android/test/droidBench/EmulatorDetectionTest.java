/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.android.test.droidBench;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.results.InfoflowResults;

public class EmulatorDetectionTest extends JUnitTests {
	
	@Test(timeout=300000)
	public void runTestBattery1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/Battery1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestBluetooth1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/Bluetooth1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestBuild1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/Build1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestContacts1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/Contacts1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestContentProvider1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/ContentProvider1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestDeviceId1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/DeviceId1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestFile1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/File1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestIMEI1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/IMEI1.apk", true);
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestIP1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/IP1.apk", true);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestPI1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/PI1.apk", true);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestPlayStore1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/PlayStore1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestPlayStore2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/PlayStore2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestSensors1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/Sensors1.apk", true);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestSubscriberId1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/SubscriberId1.apk", true);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestVoiceMail1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/VoiceMail1.apk", true);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
	
}
