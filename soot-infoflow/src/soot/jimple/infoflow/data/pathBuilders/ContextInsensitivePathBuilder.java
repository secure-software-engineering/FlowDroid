package soot.jimple.infoflow.data.pathBuilders;

import java.util.HashSet;
import java.util.Set;

import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.collect.ConcurrentIdentityHashMultiMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.SourceContext;
import soot.jimple.infoflow.data.SourceContextAndPath;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

/**
 * Class for reconstructing abstraction paths from sinks to source. This builder
 * is context-insensitive which makes it faster, but also less precise than
 * {@link ContextSensitivePathBuilder}.
 * 
 * @author Steven Arzt
 */
public class ContextInsensitivePathBuilder extends ConcurrentAbstractionPathBuilder {

	private ConcurrentIdentityHashMultiMap<Abstraction, SourceContextAndPath> pathCache = new ConcurrentIdentityHashMultiMap<>();

	/**
	 * Creates a new instance of the {@link ContextSensitivePathBuilder} class
	 * 
	 * @param manager
	 *            The data flow manager that gives access to the icfg and other
	 *            objects
	 * @param executor
	 *            The executor in which to run the path reconstruction tasks
	 */
	public ContextInsensitivePathBuilder(InfoflowManager manager, InterruptableExecutor executor) {
		super(manager, executor);
	}

	/**
	 * Task for tracking back the path from sink to source.
	 * 
	 * @author Steven Arzt
	 */
	private class SourceFindingTask implements Runnable {
		private final Abstraction abstraction;

		public SourceFindingTask(Abstraction abstraction) {
			this.abstraction = abstraction;
		}

		@Override
		public void run() {
			final Set<SourceContextAndPath> paths = pathCache.get(abstraction);
			final Abstraction pred = abstraction.getPredecessor();

			if (pred != null) {
				for (SourceContextAndPath scap : paths) {
					// Process the predecessor
					if (processPredecessor(scap, pred))
						// Schedule the predecessor
						scheduleDependentTask(new SourceFindingTask(pred));

					// Process the predecessor's neighbors
					if (pred.getNeighbors() != null)
						for (Abstraction neighbor : pred.getNeighbors())
							if (processPredecessor(scap, neighbor))
								// Schedule the predecessor
								scheduleDependentTask(new SourceFindingTask(neighbor));
				}
			}
		}

		private boolean processPredecessor(SourceContextAndPath scap, Abstraction pred) {
			// Put the current statement on the list
			SourceContextAndPath extendedScap = scap.extendPath(pred, pathConfig);
			if (extendedScap == null)
				return false;

			// Add the new path
			checkForSource(pred, extendedScap);

			final int maxPaths = pathConfig.getMaxPathsPerAbstraction();
			if (maxPaths > 0) {
				Set<SourceContextAndPath> existingPaths = pathCache.get(pred);
				if (existingPaths != null && existingPaths.size() > maxPaths)
					return false;
			}
			return pathCache.put(pred, extendedScap);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((abstraction == null) ? 0 : abstraction.hashCode());
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
			SourceFindingTask other = (SourceFindingTask) obj;
			if (abstraction != other.abstraction)
				return false;
			return true;
		}

	}

	/**
	 * Checks whether the given abstraction is a source. If so, a result entry is
	 * created.
	 * 
	 * @param abs
	 *            The abstraction to check
	 * @param scap
	 *            The path leading up to the current abstraction
	 * @return True if the current abstraction is a source, otherwise false
	 */
	private boolean checkForSource(Abstraction abs, SourceContextAndPath scap) {
		if (abs.getPredecessor() != null)
			return false;

		// If we have no predecessors, this must be a source
		assert abs.getSourceContext() != null;
		assert abs.getNeighbors() == null;

		// Register the source that we have found
		SourceContext sourceContext = abs.getSourceContext();
		results.addResult(scap.getDefinition(), scap.getAccessPath(), scap.getStmt(), sourceContext.getDefinition(),
				sourceContext.getAccessPath(), sourceContext.getStmt(), sourceContext.getUserData(),
				scap.getAbstractionPath());
		return true;
	}

	@Override
	protected Runnable getTaintPathTask(final AbstractionAtSink abs) {
		SourceContextAndPath scap = new SourceContextAndPath(abs.getSinkDefinition(),
				abs.getAbstraction().getAccessPath(), abs.getSinkStmt());
		scap = scap.extendPath(abs.getAbstraction(), pathConfig);
		if (pathCache.put(abs.getAbstraction(), scap))
			if (!checkForSource(abs.getAbstraction(), scap))
				return new SourceFindingTask(abs.getAbstraction());
		return null;
	}

	@Override
	protected boolean triggerComputationForNeighbors() {
		return true;
	}

	@Override
	public void runIncrementalPathCompuation() {
		Set<AbstractionAtSink> incrementalAbs = new HashSet<>();
		for (Abstraction abs : pathCache.keySet())
			for (SourceContextAndPath scap : pathCache.get(abs)) {
				if (abs.getNeighbors() != null && abs.getNeighbors().size() != scap.getNeighborCounter()) {
					// This is a path for which we have to process the new
					// neighbors
					scap.setNeighborCounter(abs.getNeighbors().size());

					for (Abstraction neighbor : abs.getNeighbors())
						incrementalAbs.add(new AbstractionAtSink(scap.getDefinition(), neighbor, scap.getStmt()));
				}
			}

		if (!incrementalAbs.isEmpty())
			this.computeTaintPaths(incrementalAbs);
	}

}
