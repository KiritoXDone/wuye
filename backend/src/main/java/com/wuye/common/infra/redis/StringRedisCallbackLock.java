package com.wuye.common.infra.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class StringRedisCallbackLock implements RedisCallbackLock {

    private static final Logger log = LoggerFactory.getLogger(StringRedisCallbackLock.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final String keyPrefix;
    private final Duration ttl;

    public StringRedisCallbackLock(StringRedisTemplate stringRedisTemplate, String keyPrefix, Duration ttl) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.keyPrefix = keyPrefix;
        this.ttl = ttl;
    }

    @Override
    public boolean acquire(String key) {
        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(keyPrefix + key, "1", ttl);
            return Boolean.TRUE.equals(acquired);
        } catch (RuntimeException ex) {
            log.warn("redis callback lock degraded for key {}", key, ex);
            return true;
        }
    }

    @Override
    public void release(String key) {
        try {
            stringRedisTemplate.delete(keyPrefix + key);
        } catch (RuntimeException ex) {
            log.warn("redis callback unlock degraded for key {}", key, ex);
        }
    }
}
