#-------------------------------------------------------------------------------
# Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the GNU Lesser Public License v2.1
# which accompanies this distribution, and is available at
# http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
# 
# Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
# Bodden, and others.
#-------------------------------------------------------------------------------
This document describes the assumptions we made for our analysis:

Aliasing3 -> javadoc and getVulnerability claim that one vulnerability exist but as String is an immutable object there is no dataflow.
Hence we assume there is no vulnerability.

We omitted all reflection, sanitizer and predicates tests because they are out of scope.
We also omitted the testcase 'StrongUpdates4' because it involves threading issues.


Datastructures1:
'private String str;
    	private String tag = "abc";
    	
    	public String getData(){return this.str;}
    	public String getTag(){return this.str;}
'
We assume that the getTag() method should return the variable tag (otherwise two vulnerabilities should be found) so we fixed this in the code.


Datastructures4 -> javadoc and getVulnerability claim that one vulnerability exist but there is only one call to a sink and next to it there is a java comment stating 'OK'.
Manual inspection shows that there is no leak. In fact the description supports this finding: "simple nexted data (false positive)" 

Basic14, Basic41 and Basic42 required a code change in the BasicTestCase class (dummy implementation of getServletConfig() method)
