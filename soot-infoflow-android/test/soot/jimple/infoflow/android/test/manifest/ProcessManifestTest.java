package soot.jimple.infoflow.android.test.manifest;

import java.io.IOException;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.android.manifest.ProcessManifest;

public class ProcessManifestTest 
{
	@Test
	public void testGetVersionCode()
	{
		ProcessManifest manifest = null;
		
		try 
		{
			manifest = new ProcessManifest("testAPKs/enriched1.apk");
		} 
		catch (IOException | XmlPullParserException e) 
		{
			e.printStackTrace();
		}
		
		boolean throwsException = false;
		
		try
		{
			manifest.getVersionCode();
			manifest.getMinSdkVersion();
			manifest.targetSdkVersion();
		}
		catch (Exception ex)
		{
			throwsException = true;
		}
		
		org.junit.Assert.assertFalse(throwsException);
	}
	
	@Test
	public void testSdkVersion()
	{
		ProcessManifest manifest = null;
		
		try 
		{
			manifest = new ProcessManifest("testAPKs/enriched1.apk");
		} 
		catch (IOException | XmlPullParserException e) 
		{
			e.printStackTrace();
		}
		
		boolean throwsException = false;
		
		try
		{
			manifest.getMinSdkVersion();
			manifest.targetSdkVersion();
		}
		catch (Exception ex)
		{
			throwsException = true;
		}
		
		org.junit.Assert.assertFalse(throwsException);
	}
}
