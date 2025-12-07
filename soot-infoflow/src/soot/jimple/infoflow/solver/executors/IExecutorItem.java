package soot.jimple.infoflow.solver.executors;

/**
 * An runnable item to be placed in an executor.
 * It gets informed about the executor that is used.
 * @author Marc Miltenberger
 */
public interface IExecutorItem extends Runnable {
	public void setExecutor(InterruptableExecutor executor);
}
