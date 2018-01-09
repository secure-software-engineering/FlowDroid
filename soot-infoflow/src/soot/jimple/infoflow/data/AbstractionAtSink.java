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

import soot.jimple.Stmt;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;

public class AbstractionAtSink {

	private final SourceSinkDefinition sinkDefinition;
	private final Abstraction abstraction;
	private final Stmt sinkStmt;

	/**
	 * Creates a new instance of the {@link AbstractionAtSink} class
	 * 
	 * @param sinkDefinition
	 *            The original definition of the sink that has been reached
	 * @param abstraction
	 *            The abstraction with which the sink has been reached
	 * @param sinkStmt
	 *            The statement that triggered the sink
	 */
	public AbstractionAtSink(SourceSinkDefinition sinkDefinition, Abstraction abstraction, Stmt sinkStmt) {
		this.sinkDefinition = sinkDefinition;
		this.abstraction = abstraction;
		this.sinkStmt = sinkStmt;
	}

	/**
	 * Gets the original definition of the sink that has been reached
	 * 
	 * @return The original definition of the sink that has been reached
	 */
	public SourceSinkDefinition getSinkDefinition() {
		return sinkDefinition;
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
		result = prime * result + ((sinkDefinition == null) ? 0 : sinkDefinition.hashCode());
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
		if (sinkDefinition == null) {
			if (other.sinkDefinition != null)
				return false;
		} else if (!sinkDefinition.equals(other.sinkDefinition))
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
