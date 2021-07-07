package soot.jimple.infoflow.util;

/**
 * This class can be used to create threads.
 * Own implementations may provide their own factory if they wish.
 */
public class ThreadUtils {
	/**
	 * Can be used to create threads
	 */
	public static interface IThreadFactory {
		/**
		 * Adds a thread which is generic, i.e. not necessary tied to a specific soot instance.
		 * @param r the runnable
		 * @param name the name of the thread
		 * @param daemon whether the thread is a daemon
		 * @return the thread
		 */
		public Thread createGenericThread(Runnable r, String name, boolean daemon);
	}

	public static IThreadFactory threadFactory = new IThreadFactory() {

		@Override
		public Thread createGenericThread(Runnable r, String name, boolean daemon) {
			Thread thr = new Thread(r, name);
			thr.setDaemon(daemon);
			return thr;
		}

	};

	/**
	 * Adds a thread which is generic, i.e. not necessary tied to a specific soot instance.
	 * @param r the runnable
	 * @param name the name of the thread
	 * @param daemon whether the thread is a daemon
	 * @return the thread
	 */
	public static Thread createGenericThread(Runnable r, String name, boolean daemon) {
		return threadFactory.createGenericThread(r, name, daemon);
	}
}
