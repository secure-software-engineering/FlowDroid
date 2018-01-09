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
package soot.jimple.infoflow.test.junit;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.IInfoflow;

/**
 * contain test cases for taint propagation in Maps.
 */
public class MapTests extends JUnitTests {

    @Test(timeout=300000)
    public void mapPos0Test(){
    	IInfoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.MapTestCode: void writeReadPos0Test()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);		
    }
    
    @Test(timeout=300000)
    public void mapPos1Test(){
    	IInfoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.MapTestCode: void writeReadPos1Test()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
    }

    @Test(timeout=300000)
    public void concreteMapPos0Test(){
    	IInfoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.MapTestCode: void concreteWriteReadPos0Test()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
    @Test(timeout=300000)
    public void concreteLinkedMapPos0Test(){
    	IInfoflow infoflow = initInfoflow();
    	infoflow.getConfig().setFlowSensitiveAliasing(false);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.MapTestCode: void concreteLinkedWriteReadPos0Test()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }

    @Test(timeout=300000)
    public void concreteMapPos1Test(){
    	IInfoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.MapTestCode: void concreteWriteReadPos1Test()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
    @Test(timeout=300000)
    public void concreteMapTest2(){
    	IInfoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.MapTestCode: void concreteWriteRead2Test()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
    	infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		
		// We only publish a constant string, though the key in the map is
		// sensitive - but never gets sent out
		negativeCheckInfoflow(infoflow);
    }
    
    @Test(timeout=300000)
    public void mapIteratorTest(){
    	IInfoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.MapTestCode: void iteratorTest()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);		
    }
    
    @Test(timeout=300000)
    public void mapEntryTest(){
    	IInfoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.MapTestCode: void entryTest()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
    }
    
    @Test(timeout=300000)
    public void concreteTableTest(){
    	IInfoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.MapTestCode: void concreteWriteReadTableTest()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
    @Test(timeout=300000)
    @Ignore // does not work anymore since JRE 1.7.0_45
    public void concreteNegativeTest(){
    	IInfoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.MapTestCode: void concreteWriteReadNegativeTest()>");
		infoflow.getConfig().setFlowSensitiveAliasing(false);
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
    }
    
    @Test(timeout=300000)
    public void loopCallTest() {
    	IInfoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.MapTestCode: void loopCallTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }

}
