package com.github.eclipseace.jtoolset.redis_benchmark;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class MultiThreadTest {
	private final String threadNamePrefix;
	private final int threadCount;
	private final long threadInterval;
	private final int loopCount;
	private final long loopInterval;

	public MultiThreadTest(
			String threadNamePrefix,
			int threadCount,
			long threadInterval,
			int loopCount,
			long loopInterval) {
		this.threadNamePrefix = threadNamePrefix;
		this.threadCount = threadCount;
		this.threadInterval = threadInterval;
		this.loopCount = loopCount;
		this.loopInterval = loopInterval;
	}

	private int totalErrors;
	private AtomicBoolean bootstraping = new AtomicBoolean(false);
	private DescriptiveStatistics statistics = new DescriptiveStatistics(DescriptiveStatistics.INFINITE_WINDOW);
	private BlockingQueue<Object> pendingEvents = new LinkedBlockingDeque<>();
	private ConcurrentMap<String, Thread> threads = new ConcurrentHashMap<>();
	private Routine routine;

	public void start(Routine routine) {
		if (!threads.isEmpty() || !bootstraping.compareAndSet(false, true)) {
			throw new IllegalStateException("Previous test is still running");
		}
		this.routine = routine;
		Thread bootstrapThread = new Thread(() -> {
			int fmtDigits = (int) Math.floor(Math.log10(threadCount) + 1);
			for (int t = 0; t < threadCount; t++) {
				String threadName = String.format("%s%0" + fmtDigits + "d", threadNamePrefix, t);
				Thread thread = new Thread(new RoutineWrapper(t), threadName);
				synchronized (this) {
					threads.put(threadName, thread);
					//pendingEvents.add("Thread [" + threadName + "] created");
				}
				thread.start();

				try {
					Thread.sleep(threadInterval);
				} catch (InterruptedException e) {
					pendingEvents.add("Bootstrap thread interruped, exit");
					bootstraping.set(false);
					return;
				}
			}
			pendingEvents.add("Bootstrap done");
			bootstraping.set(false);
		}, threadNamePrefix + "bootstrap");
		bootstrapThread.start();
	}

	public void join() throws InterruptedException {
		while (!threads.isEmpty()) {
			Thread.sleep(0);
		}
	}

	public <T> T compute(Function<ComputeContext, T> fn) {
		synchronized (this) {
			return fn.apply(new ComputeContext(this));
		}
	}

	public static interface Routine {
		void execute(RoutineContext context);

		void beforeLoop();

		void afterLoop();
	}

	public class RoutineContext {
		private final int threadIndex;
		private final int loopIndex;
		private long tooks;

		private RoutineContext(int threadIndex, int loopIndex) {
			this.threadIndex = threadIndex;
			this.loopIndex = loopIndex;
		}

		public int getThreadIndex() {
			return threadIndex;
		}

		public int getLoopIndex() {
			return loopIndex;
		}

		public void run(Runnable runnable) {
			Throwable error = null;
			try {
				long ts = System.currentTimeMillis();
				runnable.run();
				tooks = System.currentTimeMillis() - ts;
			} catch (Throwable t) {
				error = t;
			}
			synchronized (this) {
				if (error == null) {
					statistics.addValue(tooks);
				} else {
					pendingEvents.add(error);
					totalErrors++;
				}
			}
		}
	}

	public static class ComputeContext {
		private final DescriptiveStatistics statistics;
		private final BlockingQueue<Object> pendingEvents;
		private final int totalErrors;
		private final int activeThreads;

		private ComputeContext(MultiThreadTest mtt) {
			this.statistics = mtt.statistics;
			this.pendingEvents = mtt.pendingEvents;
			this.totalErrors = mtt.totalErrors;
			this.activeThreads = mtt.threads.size();
		}

		public DescriptiveStatistics getStatistics() {
			return statistics;
		}

		public BlockingQueue<Object> getPendingEvents() {
			return pendingEvents;
		}

		public int getTotalErrors() {
			return totalErrors;
		}

		public int getActiveThreads() {
			return activeThreads;
		}
	}

	private class RoutineWrapper implements Runnable {
		private final int threadIndex;

		private RoutineWrapper(int threadIndex) {
			this.threadIndex = threadIndex;
		}

		@Override
		public void run() {
			boolean interrupted = false;
			RuntimeException exception = null;
			try {
				routine.beforeLoop();
				for (int l = 0; l < loopCount; l++) {
					routine.execute(new RoutineContext(threadIndex, l));
					try {
						Thread.sleep(loopInterval);
					} catch (InterruptedException e) {
						interrupted = true;
						break;
					}
				}
				routine.afterLoop();
			} catch (RuntimeException e) {
				exception = e;
			}
			synchronized (this) {
				if (exception != null) {
					pendingEvents.add(exception);
				} else if (interrupted) {
					pendingEvents.add("Thread [" + Thread.currentThread().getName() + "] interruped, exit");
				} else {
					pendingEvents.add("Thread [" + Thread.currentThread().getName() + "] finished");
				}
				threads.remove(Thread.currentThread().getName());
			}
		}
	}
}
