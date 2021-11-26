package soot.jimple.infoflow.data.pathBuilders;

import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import soot.jimple.infoflow.InfoflowConfiguration.PathBuildingAlgorithm;
import soot.jimple.infoflow.InfoflowConfiguration.PathConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;

/**
 * Default factory class for abstraction path builders
 * 
 * @author Steven Arzt
 */
public class DefaultPathBuilderFactory implements IPathBuilderFactory {

	private final PathConfiguration pathConfiguration;

	/**
	 * Repeatable context-senisitive path builder
	 * 
	 * @author Steven Arzt
	 *
	 */
	private static class RepeatableContextSensitivePathBuilder extends ContextSensitivePathBuilder {

		public RepeatableContextSensitivePathBuilder(InfoflowManager manager) {
			super(manager);
		}

		@Override
		protected void onTaintPathsComputed() {
			// Do not terminate the executor
		}

	}

	/**
	 * Variant of the {@link BatchPathBuilder} that can shut down the inner builder
	 * 
	 * @author Steven Arzt
	 *
	 */
	private static class ShutdownBatchPathBuilder extends BatchPathBuilder {

		public ShutdownBatchPathBuilder(InfoflowManager manager, RepeatableContextSensitivePathBuilder innerBuilder) {
			super(manager, innerBuilder);
		}

		@Override
		public void computeTaintPaths(Set<AbstractionAtSink> res) {
			try {
				super.computeTaintPaths(res);
			} finally {
				((RepeatableContextSensitivePathBuilder) innerBuilder).shutdown();
			}
		}

	}

	/**
	 * Creates a new instance of the {@link DefaultPathBuilderFactory} class
	 * 
	 * @param config The configuration for reconstructing data flow paths
	 */
	public DefaultPathBuilderFactory(PathConfiguration config) {
		this.pathConfiguration = config;
	}

	/**
	 * Creates a new executor object for spawning worker threads
	 * 
	 * @param maxThreadNum The number of threads to use
	 * @return The generated executor
	 */
	private InterruptableExecutor createExecutor(int maxThreadNum) {
		int numThreads = Runtime.getRuntime().availableProcessors();
		InterruptableExecutor executor = new InterruptableExecutor(
				maxThreadNum == -1 ? numThreads : Math.min(maxThreadNum, numThreads), Integer.MAX_VALUE, 30,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		executor.setThreadFactory(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread thr = new Thread(r, "Path reconstruction");
				return thr;
			}
		});
		return executor;
	}

	@Override
	public IAbstractionPathBuilder createPathBuilder(InfoflowManager manager, int maxThreadNum) {
		return createPathBuilder(manager, createExecutor(maxThreadNum));
	}

	@Override
	public IAbstractionPathBuilder createPathBuilder(InfoflowManager manager, InterruptableExecutor executor) {
		switch (pathConfiguration.getPathBuildingAlgorithm()) {
		case Recursive:
			return new BatchPathBuilder(manager, new RecursivePathBuilder(manager, executor));
		case ContextSensitive:
			return new ShutdownBatchPathBuilder(manager, new RepeatableContextSensitivePathBuilder(manager));
		case ContextInsensitive:
			return new BatchPathBuilder(manager, new ContextInsensitivePathBuilder(manager, executor));
		case ContextInsensitiveSourceFinder:
			return new BatchPathBuilder(manager, new ContextInsensitiveSourceFinder(manager, executor));
		case None:
			return new EmptyPathBuilder();
		}
		throw new RuntimeException("Unsupported path building algorithm");
	}

	@Override
	public boolean supportsPathReconstruction() {
		switch (pathConfiguration.getPathBuildingAlgorithm()) {
		case Recursive:
		case ContextSensitive:
		case ContextInsensitive:
			return true;
		case ContextInsensitiveSourceFinder:
		case None:
			return false;
		}
		throw new RuntimeException("Unsupported path building algorithm");
	}

	@Override
	public boolean isContextSensitive() {
		return pathConfiguration.getPathBuildingAlgorithm() == PathBuildingAlgorithm.ContextSensitive;
	}

}
