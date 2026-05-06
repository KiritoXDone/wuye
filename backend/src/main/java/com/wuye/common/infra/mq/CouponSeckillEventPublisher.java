package com.wuye.common.infra.mq;

import com.wuye.coupon.event.CouponSeckillOrderEvent;

public interface CouponSeckillEventPublisher {
    void publishSeckillOrder(CouponSeckillOrderEvent event);
}
