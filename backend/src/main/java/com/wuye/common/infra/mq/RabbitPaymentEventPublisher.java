package com.wuye.common.infra.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.payment.event.PaymentSuccessEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitPaymentEventPublisher implements PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitPaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String exchange;
    private final String routingKey;

    public RabbitPaymentEventPublisher(RabbitTemplate rabbitTemplate,
                                       ObjectMapper objectMapper,
                                       String exchange,
                                       String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize payment success event", ex);
        } catch (RuntimeException ex) {
            log.warn("rabbitmq publish degraded for payment success event {}", event.getPayOrderNo(), ex);
        }
    }
}
