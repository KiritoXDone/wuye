package com.wuye.common.infra.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.coupon.event.CouponSeckillOrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitCouponSeckillEventPublisher implements CouponSeckillEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitCouponSeckillEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String exchange;
    private final String routingKey;

    public RabbitCouponSeckillEventPublisher(RabbitTemplate rabbitTemplate,
                                             ObjectMapper objectMapper,
                                             String exchange,
                                             String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publishSeckillOrder(CouponSeckillOrderEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize coupon seckill event", ex);
        } catch (RuntimeException ex) {
            log.warn("rabbitmq publish degraded for coupon seckill order {}", event.getOrderNo(), ex);
            throw ex;
        }
    }
}
