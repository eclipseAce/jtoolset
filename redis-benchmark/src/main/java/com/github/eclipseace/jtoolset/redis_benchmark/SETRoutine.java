package com.github.eclipseace.jtoolset.redis_benchmark;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import com.github.eclipseace.jtoolset.redis_benchmark.MultiThreadTest.RoutineContext;

import redis.clients.jedis.Jedis;

public class SETRoutine extends AbstractSharedJedisRoutine {
	private static final byte[] KEY_PREFIX = "presstest-value:".getBytes(UTF_8);
	private static final byte[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
			.getBytes(UTF_8);

	private final byte[] payload;
	private final byte[][] hashKeys;

	public SETRoutine(URI jedisUri, int payloadSize, int keyCount) {
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
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		try {
			s.write(KEY_PREFIX);
			s.write(hashKeys[context.getLoopIndex() % hashKeys.length]);
		} catch (IOException e) {
			// imposible
		}
		byte[] key = s.toByteArray();
		context.run(() -> {
			jedis.set(key, payload);
		});
	}
}