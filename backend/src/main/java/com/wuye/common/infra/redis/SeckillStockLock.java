package com.wuye.common.infra.redis;

public interface SeckillStockLock {
    boolean tryLock(String key);
    void unlock(String key);
}
