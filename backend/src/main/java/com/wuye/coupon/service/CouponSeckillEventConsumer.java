package com.wuye.coupon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.coupon.event.CouponSeckillOrderEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.infra.rabbit", name = "enabled", havingValue = "true")
public class CouponSeckillEventConsumer {

    private final ObjectMapper objectMapper;
    private final CouponSeckillOrderProcessor processor;

    public CouponSeckillEventConsumer(ObjectMapper objectMapper, CouponSeckillOrderProcessor processor) {
        this.objectMapper = objectMapper;
        this.processor = processor;
    }

    @RabbitListener(queues = "${app.infra.rabbit.coupon-seckill-queue}")
    public void onSeckillOrder(String payload) throws JsonProcessingException {
        processor.process(objectMapper.readValue(payload, CouponSeckillOrderEvent.class));
    }
}
