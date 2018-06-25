package com.github.eclipseace.jtoolset.redis_benchmark;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.google.common.collect.Lists;

public class Main {
	private static int DISPLAY_WIDTH = 100;
	
	public static void main(String[] args) throws Exception {
		Options opts = new Options()
				.addRequiredOption("u", "uri", true, "redis connection uri")
				.addRequiredOption("y", "type", true, "test type")
				.addOption("t", "threads", true, "total threads, default 100")
				.addOption("h", "thread-interval", true, "thread startup interval, in millis, default 50")
				.addOption("l", "loops", true, "total loops per thread, default 10")
				.addOption("o", "loop-interval", true, "interval per loop, in millis, default 10")
				.addOption("p", "payload", true, "payload size, in bytes, default 1024");
		CommandLine cl = new DefaultParser().parse(opts, args);

		URI uri = URI.create(cl.getOptionValue("u"));
		int threads = Integer.parseInt(cl.getOptionValue("t", "100"));
		int threadInterval = Integer.parseInt(cl.getOptionValue("h", "50"));
		int loops = Integer.parseInt(cl.getOptionValue("l", "10"));
		int loopInterval = Integer.parseInt(cl.getOptionValue("o", "10"));
		int size = Integer.parseInt(cl.getOptionValue("p", "1024"));
		String type = cl.getOptionValue("y");

		System.out.println(String.join("\r\n", Arrays.asList(
				"Target Redis     :" + uri.toString(),
				"Threads          :" + threads,
				"Thread interval  :" + threadInterval,
				"Loops            :" + loops,
				"Loops interval   :" + loopInterval,
				"Payload size     :" + size,
				"Test type        :" + type)));
		for (int n = 0; n < DISPLAY_WIDTH; n++) {
			System.out.print('=');
		}
		System.out.println();

		MultiThreadTest mtt = new MultiThreadTest("test-", threads, threadInterval, loops, loopInterval);

		Thread tui = new Thread(() -> {
			while (true) {
				mtt.compute(cc -> {
					System.out.print('\r');
					for (int n = 0; n < DISPLAY_WIDTH; n++) {
						System.out.print(' ');
					}
					System.out.print('\r');

					List<Object> events = Lists.newArrayList();
					cc.getPendingEvents().drainTo(events);
					if (!events.isEmpty()) {
						for (Object event : events) {
							if (event instanceof Throwable) {
								System.out.println(((Throwable) event).getMessage());
							} else {
								System.out.println(event.toString());
							}
						}
					}

					DescriptiveStatistics ds = cc.getStatistics();
					Object[][] model = {
							{ "avg", "%.2f", ds.getSum() / ds.getN() },
							{ "min", "%.0f", ds.getMin() },
							{ "max", "%.0f", ds.getMax() },
							{ "50%%", "%.1f", ds.getPercentile(50) },
							{ "95%%", "%.1f", ds.getPercentile(95) },
							{ "99%%", "%.1f", ds.getPercentile(99) },
							{ "smps", "%d", ds.getN() },
							{ "threads", "%d", cc.getActiveThreads() },
							{ "errors", "%d", cc.getTotalErrors() },
					};
					System.out.print(String.format(
							Stream.of(model).map(m -> m[0] + "=" + m[1]).collect(Collectors.joining(", ")),
							Stream.of(model).map(m -> m[2]).collect(Collectors.toList()).toArray()));

					return null;
				});

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					return;
				}
			}
		});
		tui.start();

		MultiThreadTest.Routine routine;
		if ("hset".equals(type)) {
			routine = new HSETRoutine(uri, size, 10000);
		} else if (type.startsWith("hmget:")) {
			int keyCount = Integer.parseInt(type.substring("hmget:".length()));
			routine = new HMGETRoutine(uri, size, keyCount);
		} else if (type.startsWith("mget:")) {
			int keyCount = Integer.parseInt(type.substring("mget:".length()));
			routine = new MGETRoutine(uri, size, keyCount);
		} else {
			routine = new SETRoutine(uri, size, 10000);
		}
		mtt.start(routine);
	}
}
