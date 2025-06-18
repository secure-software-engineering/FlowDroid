package soot.jimple.infoflow.memory;

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.text.MessageFormat;
import java.util.TreeSet;

import org.slf4j.Logger;

/**
 * We made this class separate to ensure that there is no direct connection to a concrete instance 
 * of a MemoryWarningSystem, since this runner runs independently from all instances.
 */
final class MemoryWarningThreadRunnable implements Runnable {

	private MemoryPoolMXBean tenuredGenPool;
	private TreeSet<MemoryWarningSystem> warningSystems;
	private Logger logger;

	public MemoryWarningThreadRunnable(MemoryPoolMXBean tenuredgenpool, TreeSet<MemoryWarningSystem> warningSystems,
			Logger logger) {
		this.tenuredGenPool = tenuredgenpool;
		this.warningSystems = warningSystems;
		this.logger = logger;
	}

	@Override
	public void run() {
		try {
			while (true) {
				MemoryWarningSystem l;
				synchronized (warningSystems) {
					if (warningSystems.isEmpty()) {
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
						nextThreshold = MemoryWarningSystem.triggerNotification();
						if (nextThreshold == -1) {
							synchronized (warningSystems) {
								if (warningSystems.isEmpty()) {
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
		} finally {
			MemoryWarningSystem.thrLowMemoryWarningThread = null;
		}
	}

}
