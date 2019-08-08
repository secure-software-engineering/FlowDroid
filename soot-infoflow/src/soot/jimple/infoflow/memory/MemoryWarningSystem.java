package soot.jimple.infoflow.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notification system that triggers a callback when the JVM is about to run out
 * of memory. Inspired by code from
 * http://www.javaspecialists.eu/archive/Issue092.html.
 * 
 * Be careful, because memory allocation is usually fast. There might not be a
 * lot of time left to perform corrective measures if the warning threshold is
 * reached.
 * 
 * @author Steven Arzt
 *
 */
public class MemoryWarningSystem {

	private static final Logger logger = LoggerFactory.getLogger(MemoryWarningSystem.class);

	/**
	 * Interface that is invoked when a certain memory threshold has been reached.
	 * 
	 * @author Steven Arzt
	 *
	 */
	public interface OnMemoryThresholdReached {

		/**
		 * Method that is called when a certain memory threshold has been reached.
		 * 
		 * @param usedMemory The amount of memory currently in use
		 * @param maxMemory  The maximum amount of allocated memory
		 */
		public void onThresholdReached(long usedMemory, long maxMemory);

	}

	private static final MemoryPoolMXBean tenuredGenPool = findTenuredGenPool();

	private final Set<OnMemoryThresholdReached> listeners = new HashSet<>();
	private final NotificationListener memoryListener;
	private boolean isClosed = false;

	/**
	 * Creates a new instance of the {@link MemoryWarningSystem} class
	 */
	public MemoryWarningSystem() {
		this.memoryListener = new NotificationListener() {

			@Override
			public void handleNotification(Notification notification, Object handback) {
				if (notification.getType().equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
					long maxMemory = tenuredGenPool.getUsage().getMax();
					long usedMemory = tenuredGenPool.getUsage().getUsed();

					Runtime runtime = Runtime.getRuntime();
					long usedMem = runtime.totalMemory() - runtime.freeMemory();

					logger.info("Triggering memory warning at " + (usedMem / 1000 / 1000) + " MB ("
							+ (usedMem / 1000 / 1000) + " MB in tenured gen)...");
					for (OnMemoryThresholdReached listener : listeners)
						listener.onThresholdReached(usedMemory, maxMemory);
				}
			}
		};

		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		((NotificationEmitter) memoryBean).addNotificationListener(memoryListener, new NotificationFilter() {

			private static final long serialVersionUID = -3755179266517545663L;

			@Override
			public boolean isNotificationEnabled(Notification notification) {
				return notification.getType().equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED);
			}

		}, null);
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	/**
	 * Adds a listener that is called when the memory usage threshold defined using
	 * setWarningThreshold() has been reached
	 * 
	 * @param listener The listener to add
	 */
	public void addListener(OnMemoryThresholdReached listener) {
		this.listeners.add(listener);
	}

	/**
	 * Tenured Space Pool can be determined by it being of type HEAP and by it being
	 * possible to set the usage threshold.
	 */
	private static MemoryPoolMXBean findTenuredGenPool() {
		List<MemoryPoolMXBean> usablePools = new ArrayList<>();
		for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
			// Can we put a threshold on this pool?
			if (pool.getType() == MemoryType.HEAP && pool.isUsageThresholdSupported()) {
				usablePools.add(pool);

				// We use the tenured generation as an indicator if we can
				if (pool.getName().equals("Tenured Gen"))
					return pool;
			}
		}

		// Apparently, we did not find the tenured generation. Take some other pool
		// instead.
		if (!usablePools.isEmpty())
			return usablePools.get(0);

		// Without any usable pool at all, we're out of luck.
		throw new AssertionError("Could not find tenured space");
	}

	/**
	 * Sets the one single global warning threshold for memory usage. If at least
	 * the given fraction of the overall tenured pool is in use, the registered
	 * hanlders will be invoked.
	 * 
	 * @param percentage
	 */
	public static void setWarningThreshold(double percentage) {
		if (percentage <= 0.0 || percentage > 1.0) {
			throw new IllegalArgumentException("Percentage not in range");
		}
		long maxMemory = tenuredGenPool.getUsage().getMax();
		long warningThreshold = (long) (maxMemory * percentage);
		tenuredGenPool.setUsageThreshold(warningThreshold);
	}

	/**
	 * Closes this warning system instance. It will no longer notify any listeners
	 * of memory shortages.
	 */
	public void close() {
		// Avoid duplicate attempts to unregister the listener
		if (isClosed)
			return;
		isClosed = true;
		logger.info("Shutting down the memory warning system...");

		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		try {
			((NotificationEmitter) memoryBean).removeNotificationListener(memoryListener);
		} catch (ListenerNotFoundException e) {
			// Doesn't matter, we wanted to get rid of it anyway
		}
	}

}
