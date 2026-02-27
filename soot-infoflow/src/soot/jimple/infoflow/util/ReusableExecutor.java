package soot.jimple.infoflow.util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class ReusableExecutor implements ExecutorService {
	private ExecutorService executor;

	private final AtomicInteger counter = new AtomicInteger(0);
	private final Object obj = new Object();;

	public ReusableExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public void waitUntilFinished() throws InterruptedException {
		while (counter.get() > 0) {
			synchronized (obj) {
				obj.wait();
			}
		}
	}

	@Override
	public void execute(Runnable command) {
		counter.getAndIncrement();
		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					command.run();
				} finally {
					int c = counter.decrementAndGet();
					if (c == 0) {
						synchronized (obj) {
							obj.notify();
						}
					}
				}
			}

		});
	}

	public boolean awaitTermination(long arg0, TimeUnit arg1) throws InterruptedException {
		return executor.awaitTermination(arg0, arg1);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> arg0, long arg1, TimeUnit arg2)
			throws InterruptedException {
		return executor.invokeAll(arg0, arg1, arg2);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> arg0) throws InterruptedException {
		return executor.invokeAll(arg0);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> arg0, long arg1, TimeUnit arg2)
			throws InterruptedException, ExecutionException, TimeoutException {
		return executor.invokeAny(arg0, arg1, arg2);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> arg0) throws InterruptedException, ExecutionException {
		return executor.invokeAny(arg0);
	}

	public boolean isShutdown() {
		return executor.isShutdown();
	}

	public boolean isTerminated() {
		return executor.isTerminated();
	}

	public void shutdown() {
		executor.shutdown();
	}

	public List<Runnable> shutdownNow() {
		return executor.shutdownNow();
	}

	public <T> Future<T> submit(Callable<T> arg0) {
		return executor.submit(arg0);
	}

	public <T> Future<T> submit(Runnable arg0, T arg1) {
		return executor.submit(arg0, arg1);
	}

	public Future<?> submit(Runnable arg0) {
		return executor.submit(arg0);
	}

}