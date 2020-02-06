package soot.jimple.infoflow.test.junit;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.infoflow.memory.MemoryWarningSystem;
import soot.jimple.infoflow.memory.MemoryWarningSystem.OnMemoryThresholdReached;

public class MemoryWatcherTest {
	private static final Logger logger = LoggerFactory.getLogger(MemoryWatcherTest.class);

	private static final int MEMORY_STEP_SMALL = 1024 * 2;
	private static final int MEMORY_STEP_BIG = 1024 * 1024 * 100;
	List<byte[]> memoryLeak = new LinkedList<>();

	double[] thresholds = new double[] { 0.1, 0.3, 0.5, 0.8 };

	boolean[] wsReached = new boolean[thresholds.length];

	String fail;

	@Test
	public void runMemoryWatcherTest() {
		try {
			MemoryWarningSystem[] ws = new MemoryWarningSystem[thresholds.length];
			for (int i = 0; i < thresholds.length; i++) {
				final int current = i;
				ws[i] = new MemoryWarningSystem();
				ws[i].setWarningThreshold(thresholds[i]);
				ws[i].addListener(new OnMemoryThresholdReached() {

					@Override
					public void onThresholdReached(long usedMemory, long maxMemory) {
						logger.info("Threshold reached: " + thresholds[current] + " with " + usedMemory);
						wsReached[current] = true;
						for (int i = current + 1; i < wsReached.length; i++) {
							if (wsReached[i])
								fail = "Threshold for " + i + " was reached before " + current + ", although threshold "
										+ thresholds[current] + " < " + thresholds[i];
						}
					}
				});
			}
			while (!allHandlersCalled()) {
				leakMemory();
			}
		} finally {
			memoryLeak.clear();
			if (fail != null)
				Assert.fail(fail);
		}
	}

	private void leakMemory() {
		for (long i = 0; i < MEMORY_STEP_BIG; i += MEMORY_STEP_SMALL)
			memoryLeak.add(new byte[MEMORY_STEP_SMALL]);
		long used = MemoryWarningSystem.findTenuredGenPool().getUsage().getUsed();
		logger.info("Leaking " + (memoryLeak.size() * (long) MEMORY_STEP_SMALL / 1024D / 1024D)
				+ " MiB, in tenured gen pool " + (used / 1024D / 1024D) + " MiB");

	}

	private boolean allHandlersCalled() {
		for (boolean b : wsReached)
			if (!b)
				return false;
		return true;
	}
}
