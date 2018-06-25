package com.github.eclipseace.jtoolset.redis_benchmark;

import java.net.URI;

import redis.clients.jedis.Jedis;

public abstract class AbstractSharedJedisRoutine implements MultiThreadTest.Routine {
	private final ThreadLocal<Jedis> jedisHolder = new ThreadLocal<>();

	private final URI jedisUri;

	public AbstractSharedJedisRoutine(URI jedisUri) {
		this.jedisUri = jedisUri;
	}

	protected Jedis getJedis() {
		return jedisHolder.get();
	}

	@Override
	public void beforeLoop() {
		jedisHolder.set(new Jedis(jedisUri));
	}

	@Override
	public void afterLoop() {
		Jedis jedis = jedisHolder.get();
		jedisHolder.remove();
		jedis.close();
	}
}