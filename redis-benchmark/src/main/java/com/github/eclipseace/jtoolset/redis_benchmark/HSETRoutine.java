package com.github.eclipseace.jtoolset.redis_benchmark;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;

import com.github.eclipseace.jtoolset.redis_benchmark.MultiThreadTest.RoutineContext;

import redis.clients.jedis.Jedis;

public class HSETRoutine extends AbstractSharedJedisRoutine {
	private static final byte[] KEY = "presstest-hash".getBytes(UTF_8);
	private static final byte[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
			.getBytes(UTF_8);

	private final byte[] payload;
	private final byte[][] hashKeys;

	public HSETRoutine(URI jedisUri, int payloadSize, int keyCount) {
		super(jedisUri);

		this.payload = new byte[payloadSize];
		for (int i = 0; i < payloadSize; i++) {
			this.payload[i] = CHARS[(int) (Math.random() * CHARS.length)];
		}

		hashKeys = new byte[keyCount][];
		for (int i = 0; i < keyCount; i++) {
			hashKeys[i] = String.format("%011d", i).getBytes(UTF_8);
		}
	}

	@Override
	public void execute(RoutineContext context) {
		Jedis jedis = getJedis();
		byte[] hashKey = hashKeys[context.getLoopIndex() % hashKeys.length];
		context.run(() -> {
			jedis.hset(KEY, hashKey, payload);
		});
	}
}