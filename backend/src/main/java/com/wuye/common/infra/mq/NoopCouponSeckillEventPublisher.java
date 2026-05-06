package com.wuye.common.infra.mq;

import com.wuye.coupon.event.CouponSeckillOrderEvent;

public class NoopCouponSeckillEventPublisher implements CouponSeckillEventPublisher {
    @Override
    public void publishSeckillOrder(CouponSeckillOrderEvent event) {
    }
}
