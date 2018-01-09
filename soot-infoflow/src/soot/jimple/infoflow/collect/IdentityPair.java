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
package soot.jimple.infoflow.collect;

import heros.solver.Pair;

/**
 * Pair class that uses object identities for equals() and hashCode() 
 * 
 * @author Steven Arzt
 *
 * @param <K> Type of the first element in the pair
 * @param <V> Type of the second element in the pair
 */
public class IdentityPair<K, V> extends Pair<K, V> {
	
	protected int o1Hash;
	protected int o2Hash;
	
	public IdentityPair(K o1, V o2) {
		super(o1, o2);
		o1Hash = System.identityHashCode(o1);
		o2Hash = System.identityHashCode(o2);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof IdentityPair))
			return false;
		@SuppressWarnings("rawtypes")
		IdentityPair pair = (IdentityPair) other;
		return (this.o1 == pair.o1 && this.o2 == pair.o2);
	}
	
	@Override
	public int hashCode() {
		return 31 * o1Hash + 7 * o2Hash;
	}

	@Override
	public void setO1(K o1) {
		super.setO1(o1);
		o1Hash = System.identityHashCode(o1);
	}

	@Override
	public void setO2(V o2) {
		super.setO2(o2);
		o2Hash = System.identityHashCode(o2);
	}

}
