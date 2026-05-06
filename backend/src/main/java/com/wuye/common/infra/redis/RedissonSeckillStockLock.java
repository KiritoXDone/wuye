package com.wuye.common.infra.redis;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class RedissonSeckillStockLock implements SeckillStockLock {

    private static final Logger log = LoggerFactory.getLogger(RedissonSeckillStockLock.class);

    private final RedissonClient redissonClient;
    private final String keyPrefix;
    private final Duration leaseTime;

    public RedissonSeckillStockLock(RedissonClient redissonClient, String keyPrefix, Duration leaseTime) {
        this.redissonClient = redissonClient;
        this.keyPrefix = keyPrefix;
        this.leaseTime = leaseTime;
    }

    @Override
    public boolean tryLock(String key) {
        try {
            RLock lock = redissonClient.getLock(keyPrefix + key);
            return lock.tryLock(0, leaseTime.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (RuntimeException ex) {
            log.warn("redisson seckill lock degraded for key {}", key, ex);
            return true;
        }
    }

    @Override
    public void unlock(String key) {
        try {
            RLock lock = redissonClient.getLock(keyPrefix + key);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (RuntimeException ex) {
            log.warn("redisson seckill unlock degraded for key {}", key, ex);
        }
    }
}
