package com.github.eclipseace.jtoolset.redis_benchmark;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;

import com.github.eclipseace.jtoolset.redis_benchmark.MultiThreadTest.RoutineContext;

import redis.clients.jedis.Jedis;

public class MGETRoutine extends AbstractSharedJedisRoutine {
	private static final byte[] KEY_PREFIX = "presstest-value:".getBytes(UTF_8);
	private static final byte[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
			.getBytes(UTF_8);

	private final byte[] payload;
	private final byte[][] keys;

	public MGETRoutine(URI jedisUri, int payloadSize, int keyCount) {
		super(jedisUri);

		this.payload = new byte[payloadSize];
		for (int i = 0; i < payloadSize; i++) {
			this.payload[i] = CHARS[(int) (Math.random() * CHARS.length)];
		}

		try (Jedis jedis = new Jedis(jedisUri)) {
			keys = new byte[keyCount][];
			for (int i = 0; i < keyCount; i++) {
				keys[i] = concatKeyBytes(KEY_PREFIX, String.format("%011d", i).getBytes(UTF_8));
				jedis.set(keys[i], payload);
			}
		}
	}

	@Override
	public void execute(RoutineContext context) {
		Jedis jedis = getJedis();
		context.run(() -> {
			jedis.mget(keys);
		});
	}

	private byte[] concatKeyBytes(byte[] prefix, byte[] key) {
		return (new String(prefix, UTF_8) + new String(key, UTF_8)).getBytes(UTF_8);
	}
}