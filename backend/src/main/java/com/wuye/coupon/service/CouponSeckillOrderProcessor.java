package com.wuye.coupon.service;

import com.wuye.coupon.event.CouponSeckillOrderEvent;

public interface CouponSeckillOrderProcessor {
    void process(CouponSeckillOrderEvent event);
}
