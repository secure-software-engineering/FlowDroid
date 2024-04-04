package soot.jimple.infoflow.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import heros.solver.Pair;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.PathConfiguration;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.util.extensiblelist.ExtensibleList;

/**
 * Extension of {@link SourceContext} that also allows a paths from the source
 * to the current statement to be stored
 * 
 * @author Steven Arzt
 */
public class SourceContextAndPath extends SourceContext implements Cloneable {

	protected ExtensibleList<Abstraction> path = null;
	protected ExtensibleList<Stmt> callStack = null;
	protected int neighborCounter = 0;
	protected InfoflowConfiguration config;

	private int hashCode = 0;

	public SourceContextAndPath(InfoflowConfiguration config, Collection<ISourceSinkDefinition> definitions,
			AccessPath value, Stmt stmt) {
		this(config, definitions, value, stmt, null);
	}

	public SourceContextAndPath(InfoflowConfiguration config, Collection<ISourceSinkDefinition> definitions,
			AccessPath value, Stmt stmt, Object userData) {
		super(definitions, value, stmt, userData);
		this.config = config;
	}

	public List<Stmt> getPath() {
		if (path == null)
			return Collections.<Stmt>emptyList();
		List<Stmt> stmtPath = new ArrayList<>(this.path.size());
		Iterator<Abstraction> it = path.reverseIterator();
		while (it.hasNext()) {
			Abstraction abs = it.next();
			if (abs.getCurrentStmt() != null) {
				stmtPath.add(abs.getCurrentStmt());
			}
		}
		return stmtPath;
	}

	public Abstraction getFirstAbstractionSlow() {
		return path.getFirstSlow();
	}

	public List<Abstraction> getAbstractionPath() {
		if (path == null)
			return null;

		List<Abstraction> reversePath = new ArrayList<>(path.size());
		Iterator<Abstraction> it = path.reverseIterator();
		while (it.hasNext()) {
			reversePath.add(it.next());
		}
		return reversePath;
	}

	/**
	 * Return the last abstraction on the taint propagation path
	 *
	 * @return the last abstraction
	 */
	public Abstraction getLastAbstraction() {
		return path.getLast();
	}

	private int getCallStackSize() {
		if (isCallStackEmpty())
			return 0;
		return callStack.size();
	}

	/**
	 * Extends the taint propagation path of THIS with the additional abstractions from OTHER
	 *
	 * @param other longer taint propagation path
	 * @return The new taint propagation path
	 */
	public SourceContextAndPath extendPath(SourceContextAndPath other) {
		// Bail out if the cached path is shorter than the current one
		if (this.path == null || other.path == null || other.path.size() <= this.path.size())
			return null;

		Stack<Abstraction> pathStack = new Stack<>();
		Abstraction lastAbs = this.getLastAbstraction();
		boolean foundCommonAbs = false;

		// Collect all additional abstractions on the cached path
		Iterator<Abstraction> pathIt = other.path.reverseIterator();
		while (pathIt.hasNext()) {
			Abstraction next = pathIt.next();
			if (next == lastAbs || (next.neighbors != null && next.neighbors.contains(lastAbs))) {
				foundCommonAbs = true;
				break;
			}
			pathStack.push(next);
		}

		// If the paths do not have a common abstraction, there's probably something wrong...
		if (!foundCommonAbs)
			return null;

		// Append the additional abstractions to the new taint propagation path
		SourceContextAndPath extendedScap = clone();
		while (!pathStack.isEmpty())
			extendedScap.path.add(pathStack.pop());

		int newCallStackCapacity = other.getCallStackSize() - this.getCallStackSize();
		// Sanity Check: The callStack of other should always be larger than the one of this
		if (newCallStackCapacity < 0)
			return null;
		if (newCallStackCapacity > 0) {
			Stack<Stmt> callStackBuf = new Stack<>();
			Stmt topStmt = this.callStack == null ? null : this.callStack.getLast();

			// Collect all additional statements on the call stack...
			Iterator<Stmt> callStackIt = other.callStack.reverseIterator();
			while (callStackIt.hasNext()) {
				Stmt next = callStackIt.next();
				if (next == topStmt)
					break;
				callStackBuf.push(next);
			}

			if (callStackBuf.size() > 0) {
				if (extendedScap.callStack == null)
					extendedScap.callStack = new ExtensibleList<>();
				// ...and append them.
				while (!callStackBuf.isEmpty())
					extendedScap.callStack.add(callStackBuf.pop());
			}
		}

		return extendedScap;
	}

	/**
	 * Extends the taint propagation path with the given abstraction
	 * 
	 * @param abs The abstraction to put on the taint propagation path
	 * @return The new taint propagation path If this path would contain a loop,
	 *         null is returned instead of the looping path.
	 */
	public SourceContextAndPath extendPath(Abstraction abs) {
		return extendPath(abs, null);
	}

	/**
	 * Extends the taint propagation path with the given abstraction
	 * 
	 * @param abs    The abstraction to put on the taint propagation path
	 * @param config The configuration for constructing taint propagation paths
	 * @return The new taint propagation path. If this path would contain a loop,
	 *         null is returned instead of the looping path.
	 */
	public SourceContextAndPath extendPath(Abstraction abs, InfoflowConfiguration config) {
		if (abs == null)
			return this;

		// If we have no data at all, there is nothing we can do here
		if (abs.getCurrentStmt() == null && abs.getCorrespondingCallSite() == null)
			return this;

		final PathConfiguration pathConfig = config == null ? null : config.getPathConfiguration();

		// If we don't track paths and have nothing to put on the stack, there
		// is no need to create a new object
		final boolean trackPath = pathConfig == null ? true : pathConfig.getPathReconstructionMode().reconstructPaths();
		if (abs.getCorrespondingCallSite() == null && !trackPath)
			return this;

		// Do not add the very same abstraction over and over again.
		if (this.path != null) {
			Iterator<Abstraction> it = path.reverseIterator();
			while (it.hasNext()) {
				Abstraction a = it.next();
				if (a == abs)
					return null;
			}
		}

		SourceContextAndPath scap = null;
		if (trackPath && abs.getCurrentStmt() != null) {
			if (this.path != null) {
				// We cannot leave the same method at two different sites
				Abstraction topAbs = path.getLast();
				if (topAbs.equals(abs) && topAbs.getCorrespondingCallSite() != null
						&& topAbs.getCorrespondingCallSite() == abs.getCorrespondingCallSite()
						&& topAbs.getCurrentStmt() != abs.getCurrentStmt())
					return null;
			}

			scap = clone();

			// Extend the propagation path
			if (scap.path == null)
				scap.path = new ExtensibleList<Abstraction>();
			scap.path.add(abs);

			if (pathConfig != null && pathConfig.getMaxPathLength() > 0
					&& scap.path.size() > pathConfig.getMaxPathLength()) {
				return null;
			}
		}

		// Extend the call stack
		if (abs.getCorrespondingCallSite() != null && abs.getCorrespondingCallSite() != abs.getCurrentStmt()) {
			if (scap == null)
				scap = this.clone();
			if (scap.callStack == null)
				scap.callStack = new ExtensibleList<Stmt>();
			else if (pathConfig != null && pathConfig.getMaxCallStackSize() > 0
					&& scap.callStack.size() >= pathConfig.getMaxCallStackSize())
				return null;
			scap.callStack.add(abs.getCorrespondingCallSite());
		}

		this.neighborCounter = abs.getNeighbors() == null ? 0 : abs.getNeighbors().size();
		return scap == null ? this : scap;
	}

	/**
	 * Pops the top item off the call stack.
	 * 
	 * @return The new {@link SourceContextAndPath} object as the first element of
	 *         the pair and the call stack item that was popped off as the second
	 *         element. If there is no call stack, null is returned.
	 */
	public Pair<SourceContextAndPath, Stmt> popTopCallStackItem() {
		if (callStack == null || callStack.isEmpty())
			return null;

		SourceContextAndPath scap = clone();
		Stmt lastStmt = null;
		Object c = scap.callStack.removeLast();
		if (c instanceof ExtensibleList) {
			lastStmt = scap.callStack.getLast();
			scap.callStack = (ExtensibleList<Stmt>) c;
		} else
			lastStmt = (Stmt) c;

		if (scap.callStack.isEmpty())
			scap.callStack = null;
		return new Pair<>(scap, lastStmt);
	}

	/**
	 * Gets whether the current call stack is empty, i.e., the path is in the method
	 * from which it originated
	 * 
	 * @return True if the call stack is empty, otherwise false
	 */
	public boolean isCallStackEmpty() {
		return this.callStack == null || this.callStack.isEmpty();
	}

	public void setNeighborCounter(int counter) {
		this.neighborCounter = counter;
	}

	public int getNeighborCounter() {
		return this.neighborCounter;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other == null || !(other instanceof SourceContextAndPath))
			return false;
		SourceContextAndPath scap = (SourceContextAndPath) other;

		if (this.hashCode != 0 && scap.hashCode != 0 && this.hashCode != scap.hashCode)
			return false;

		boolean mergeDifferentPaths = !config.getPathAgnosticResults() && path != null && scap.path != null;
		if (mergeDifferentPaths) {
			if (path.size() != scap.path.size()) {
				// Quick check: they cannot be equal
				return false;
			}
		}

		if (this.callStack == null || this.callStack.isEmpty()) {
			if (scap.callStack != null && !scap.callStack.isEmpty())
				return false;
		} else {
			if (scap.callStack == null || scap.callStack.isEmpty())
				return false;

			if (callStack.size() != scap.callStack.size() || !this.callStack.equals(scap.callStack))
				return false;
		}

		if (mergeDifferentPaths) {
			if (!this.path.equals(scap.path))
				return false;
		}

		return super.equals(other);
	}

	@Override
	public int hashCode() {
		if (hashCode != 0)
			return hashCode;

		final int prime = 31;
		int result = super.hashCode();
		if (!config.getPathAgnosticResults())
			result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((callStack == null) ? 0 : callStack.hashCode());
		this.hashCode = result;
		return hashCode;
	}

	@Override
	public SourceContextAndPath clone() {
		final SourceContextAndPath scap = new SourceContextAndPath(config, definitions, accessPath, stmt, userData);
		if (path != null)
			scap.path = new ExtensibleList<Abstraction>(this.path);
		if (callStack != null)
			scap.callStack = new ExtensibleList<Stmt>(callStack);
		return scap;
	}

	@Override
	public String toString() {
		return super.toString() + "\n\ton Path: " + getAbstractionPath();
	}
}
