package com.wuye.common.infra.redis;

public class NoopSeckillStockLock implements SeckillStockLock {
    @Override
    public boolean tryLock(String key) {
        return true;
    }

    @Override
    public void unlock(String key) {
    }
}
