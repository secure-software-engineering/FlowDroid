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
package soot.jimple.infoflow.ipc;

import heros.InterproceduralCFG;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

/**
 * Abstracts from the very generic statement-based IPCManager so that users
 * can conveniently work on the called methods instead of having to analyze the
 * call statement every time
 * 
 * @author Steven Arzt
 *
 */
public abstract class MethodBasedIPCManager implements IIPCManager {

    public abstract boolean isIPCMethod(SootMethod method);
    
    @Override
    public boolean isIPC(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg) {
        assert sCallSite != null;
        return sCallSite.containsInvokeExpr()
                && isIPCMethod(sCallSite.getInvokeExpr().getMethod());
    }

}
