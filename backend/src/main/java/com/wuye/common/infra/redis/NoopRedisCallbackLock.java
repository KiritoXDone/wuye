package com.wuye.common.infra.redis;

public class NoopRedisCallbackLock implements RedisCallbackLock {

    @Override
    public boolean acquire(String key) {
        return true;
    }

    @Override
    public void release(String key) {
    }
}
