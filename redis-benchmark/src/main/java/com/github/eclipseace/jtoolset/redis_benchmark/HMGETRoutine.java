package com.github.eclipseace.jtoolset.redis_benchmark;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;

import com.github.eclipseace.jtoolset.redis_benchmark.MultiThreadTest.RoutineContext;

import redis.clients.jedis.Jedis;

public class HMGETRoutine extends AbstractSharedJedisRoutine {
	private static final byte[] KEY = "presstest-hash".getBytes(UTF_8);
	private static final byte[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
			.getBytes(UTF_8);

	private final byte[] payload;
	private final byte[][] hashKeys;

	public HMGETRoutine(URI jedisUri, int payloadSize, int keyCount) {
		super(jedisUri);

		this.payload = new byte[payloadSize];
		for (int i = 0; i < payloadSize; i++) {
			this.payload[i] = CHARS[(int) (Math.random() * CHARS.length)];
		}

		hashKeys = new byte[keyCount][];
		for (int i = 0; i < keyCount; i++) {
			hashKeys[i] = String.format("%011d", i).getBytes(UTF_8);
		}
		
		try (Jedis jedis = new Jedis(jedisUri)) {
			for (int i = 0; i < keyCount; i++) {
				jedis.hset(KEY, hashKeys[i % hashKeys.length], payload);
			}
		}
	}

	@Override
	public void execute(RoutineContext context) {
		Jedis jedis = getJedis();
		context.run(() -> {
			jedis.hmget(KEY, hashKeys);
		});
	}
}