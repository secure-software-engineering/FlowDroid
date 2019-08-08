package soot.jimple.infoflow.threading;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.solver.executors.InterruptableExecutor;
import soot.jimple.infoflow.solver.executors.SetPoolExecutor;

/**
 * Default implementation of a factory for thread pool executors
 * 
 * @author Steven Arzt
 *
 */
public class DefaultExecutorFactory implements IExecutorFactory {

	public DefaultExecutorFactory() {
		//
	}

	@Override
	public InterruptableExecutor createExecutor(int numThreads, boolean allowSetSemantics,
			InfoflowConfiguration config) {
		if (allowSetSemantics) {
			return new SetPoolExecutor(
					config.getMaxThreadNum() == -1 ? numThreads : Math.min(config.getMaxThreadNum(), numThreads),
					Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		} else {
			return new InterruptableExecutor(
					config.getMaxThreadNum() == -1 ? numThreads : Math.min(config.getMaxThreadNum(), numThreads),
					Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		}
	}

}
