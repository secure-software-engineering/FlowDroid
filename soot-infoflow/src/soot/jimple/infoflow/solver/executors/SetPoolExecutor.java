package soot.jimple.infoflow.solver.executors;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Executor class that ensures that no two equal tasks are in the queue at the
 * same time
 * 
 * @author Steven Arzt
 *
 */
public class SetPoolExecutor extends InterruptableExecutor {
	
	protected Set<Runnable> waiting = Collections.newSetFromMap(new ConcurrentHashMap<Runnable, Boolean>());
	
	public SetPoolExecutor(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}
	
	@Override
	public void execute(Runnable command) {
		// Make sure that we don't schedule a task for execution that is already
		// in the queue
		if (waiting.add(command))
			super.execute(command);
	}
	
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		waiting.remove(r);
		super.afterExecute(r, t);
	}
	
	@Override
	public void interrupt() {
		super.interrupt();
		this.waiting.clear();
	}
	
	@Override
	public void shutdown() {
		super.shutdown();
		this.waiting.clear();
	}
	
	@Override
	public List<Runnable> shutdownNow() {
		List<Runnable> tasks = super.shutdownNow();
		this.waiting.clear();
		return tasks;
	}

}
