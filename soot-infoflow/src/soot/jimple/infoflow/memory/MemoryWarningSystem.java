package soot.jimple.infoflow.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.infoflow.util.ThreadUtils;

/**
 * Notification system that triggers a callback when the JVM is about to run out
 * of memory. Inspired by code from
 * http://www.javaspecialists.eu/archive/Issue092.html.
 * 
 * Be careful, because memory allocation is usually fast. There might not be a
 * lot of time left to perform corrective measures if the warning threshold is
 * reached.
 * 
 * @author Steven Arzt, Marc Miltenberger
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

	private final static MemoryPoolMXBean tenuredGenPool = findTenuredGenPool();

	private final Set<OnMemoryThresholdReached> listeners = new HashSet<>();
	private final static NotificationListener memoryListener;
	private boolean isClosed = false;

	private long threshold;

	private static Thread thrLowMemoryWarningThread;

	private static TreeSet<MemoryWarningSystem> warningSystems = new TreeSet<>(new Comparator<MemoryWarningSystem>() {

		@Override
		public int compare(MemoryWarningSystem o1, MemoryWarningSystem o2) {
			return Long.compare(o1.threshold, o2.threshold);
		}
	});

	static {
		memoryListener = new NotificationListener() {

			@Override
			public void handleNotification(Notification notification, Object handback) {
				if (notification.getType().equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
					triggerNotification();
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

	private static long triggerNotification() {
		long maxMemory = tenuredGenPool.getUsage().getMax();
		long usedMemory = tenuredGenPool.getUsage().getUsed();

		Runtime runtime = Runtime.getRuntime();
		long usedMem = runtime.totalMemory() - runtime.freeMemory();

		synchronized (warningSystems) {
			Iterator<MemoryWarningSystem> it = warningSystems.iterator();
			while (it.hasNext()) {
				MemoryWarningSystem ws = it.next();
				if (ws.threshold <= usedMemory) {
					logger.info("Triggering memory warning at " + (usedMem / 1000 / 1000) + " MB ("
							+ (maxMemory / 1000 / 1000) + " MB max, " + (usedMemory / 1000 / 1000)
							+ " in watched memory pool)...");
					for (OnMemoryThresholdReached listener : ws.listeners)
						listener.onThresholdReached(usedMemory, maxMemory);
					it.remove();
				} else {
					// At least one warning systme has a higher threshold
					tenuredGenPool.setUsageThreshold(ws.threshold);
					return ws.threshold;
				}
			}
			return -1;
		}
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
	public static MemoryPoolMXBean findTenuredGenPool() {
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
	public void setWarningThreshold(double percentage) {
		if (percentage <= 0.0 || percentage > 1.0) {
			throw new IllegalArgumentException("Percentage not in range");
		}
		long maxMemory = tenuredGenPool.getUsage().getMax();
		long warningThreshold = (long) (maxMemory * percentage);
		synchronized (warningSystems) {
			// If element has changed, it needs to removed first
			warningSystems.remove(this);
			this.threshold = warningThreshold;
			logger.info(MessageFormat.format("Registered a memory warning system for {0} MiB",
					(threshold / 1024D / 1024D)));
			warningSystems.add(this);
			MemoryUsage usage = tenuredGenPool.getUsage();
			long threshold = warningSystems.iterator().next().threshold;
			boolean useOwnImplementation = !tenuredGenPool.isUsageThresholdSupported();
			if (!useOwnImplementation && usage != null && usage.getUsed() > threshold) {
				tenuredGenPool.setUsageThreshold(threshold);
			} else {
				// when the usage is already above the threshold, we use our own implementation,
				// since the jvm implementation does not seem to get called in that case.
				useOwnImplementation = true;
			}
			if (useOwnImplementation) {
				// No JVM support is available, use our own implementation
				if (thrLowMemoryWarningThread == null) {
					thrLowMemoryWarningThread = ThreadUtils.createGenericThread(new Runnable() {

						@Override
						public void run() {
							while (true) {
								MemoryWarningSystem l;
								synchronized (warningSystems) {
									if (warningSystems.isEmpty()) {
										thrLowMemoryWarningThread = null;
										return;
									}
									l = warningSystems.iterator().next();
								}
								long nextThreshold = l.threshold;
								MemoryUsage usage = tenuredGenPool.getUsage();
								if (usage == null) {
									logger.warn(MessageFormat.format("Memory usage of {0} could not be estimated",
											tenuredGenPool.getName()));
									return;
								} else {
									long used = usage.getUsed();
									if (used >= l.threshold) {
										nextThreshold = triggerNotification();
										if (nextThreshold == -1) {
											synchronized (warningSystems) {
												if (warningSystems.isEmpty()) {
													thrLowMemoryWarningThread = null;
													return;
												}
											}
										}
									}
								}
								long used = usage.getUsed();
								// Depending on how far we are from the next threshold, we can rest longer
								// or shorter
								long missing = nextThreshold - used;
								if (missing <= 0)
									continue;
								try {
									long wait = (long) ((missing / (double) tenuredGenPool.getUsage().getMax()) * 500);
									Thread.sleep(wait);
								} catch (InterruptedException e) {
								}
							}
						}

					}, "Low memory monitor", true);
					thrLowMemoryWarningThread.setPriority(Thread.MIN_PRIORITY);
					thrLowMemoryWarningThread.start();
				}
			}
		}
	}

	/**
	 * Closes this warning system instance. It will no longer notify any listeners
	 * of memory shortages.
	 */
	public void close() {
		// Avoid duplicate attempts to unregister the listener
		if (isClosed)
			return;
		logger.info("Shutting down the memory warning system...");
		synchronized (warningSystems) {
			warningSystems.remove(this);
		}

		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		try {
			((NotificationEmitter) memoryBean).removeNotificationListener(memoryListener);
		} catch (ListenerNotFoundException e) {
			// Doesn't matter, we wanted to get rid of it anyway
		}
		isClosed = true;
	}

}
