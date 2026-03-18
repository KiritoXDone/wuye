package com.wuye.common.infra.redis;

public interface RedisCallbackLock {

    boolean acquire(String key);

    void release(String key);
}
