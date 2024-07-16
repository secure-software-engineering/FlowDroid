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
package soot.jimple.infoflow.data;

import java.util.Collection;

import soot.jimple.Stmt;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

public class AbstractionAtSink {

	private final Collection<ISourceSinkDefinition> sinkDefinitions;
	private final Abstraction abstraction;
	private final Stmt sinkStmt;

	/**
	 * Creates a new instance of the {@link AbstractionAtSink} class
	 * 
	 * @param sinkDefinitions The original definition of the sink that has been
	 *                        reached
	 * @param abstraction     The abstraction with which the sink has been reached
	 * @param sinkStmt        The statement that triggered the sink
	 */
	public AbstractionAtSink(Collection<ISourceSinkDefinition> sinkDefinitions, Abstraction abstraction,
			Stmt sinkStmt) {
		this.sinkDefinitions = sinkDefinitions;
		this.abstraction = cleanse(abstraction);
		this.sinkStmt = sinkStmt;
	}

	/**
	 * Cleanses the given abstraction from all information that is irrelevant for a
	 * result
	 * 
	 * @param abs The abstraction to cleanse
	 * @return The original abstraction if there was no need for cleansing, or a
	 *         reduced abstraction otherwise
	 */
	private static Abstraction cleanse(Abstraction abs) {
		if (abs.getTurnUnit() == null)
			return abs;

		return abs.deriveNewAbstractionWithTurnUnit(null);
	}

	/**
	 * Gets the original definition of the sink that has been reached
	 * 
	 * @return The original definition of the sink that has been reached
	 */
	public Collection<ISourceSinkDefinition> getSinkDefinitions() {
		return sinkDefinitions;
	}

	/**
	 * Gets the abstraction with which the sink has been reached
	 * 
	 * @return The abstraction with which the sink has been reached
	 */
	public Abstraction getAbstraction() {
		return this.abstraction;
	}

	/**
	 * Gets the statement that triggered the sink
	 * 
	 * @return The statement that triggered the sink
	 */
	public Stmt getSinkStmt() {
		return this.sinkStmt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((abstraction == null) ? 0 : abstraction.hashCode());
		result = prime * result + ((sinkDefinitions == null) ? 0 : sinkDefinitions.hashCode());
		result = prime * result + ((sinkStmt == null) ? 0 : sinkStmt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractionAtSink other = (AbstractionAtSink) obj;
		if (abstraction == null) {
			if (other.abstraction != null)
				return false;
		} else if (!abstraction.equals(other.abstraction))
			return false;
		if (sinkDefinitions == null) {
			if (other.sinkDefinitions != null)
				return false;
		} else if (!sinkDefinitions.equals(other.sinkDefinitions))
			return false;
		if (sinkStmt == null) {
			if (other.sinkStmt != null)
				return false;
		} else if (!sinkStmt.equals(other.sinkStmt))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return abstraction + " at " + sinkStmt;
	}

}
