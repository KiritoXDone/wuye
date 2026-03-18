package com.wuye.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.common.infra.mq.NoopPaymentEventPublisher;
import com.wuye.common.infra.mq.PaymentEventPublisher;
import com.wuye.common.infra.mq.RabbitPaymentEventPublisher;
import com.wuye.common.infra.redis.NoopRedisCallbackLock;
import com.wuye.common.infra.redis.RedisCallbackLock;
import com.wuye.common.infra.redis.StringRedisCallbackLock;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
public class InfraRuntimeConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.infra.redis", name = "enabled", havingValue = "true")
    public RedisCallbackLock redisCallbackLock(StringRedisTemplate stringRedisTemplate,
                                               AppInfraProperties appInfraProperties) {
        return new StringRedisCallbackLock(
                stringRedisTemplate,
                appInfraProperties.getRedis().getKeyPrefix(),
                Duration.ofSeconds(appInfraProperties.getRedis().getCallbackLockSeconds())
        );
    }

    @Bean
    @ConditionalOnMissingBean(RedisCallbackLock.class)
    public RedisCallbackLock noopRedisCallbackLock() {
        return new NoopRedisCallbackLock();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.infra.rabbit", name = "enabled", havingValue = "true")
    public TopicExchange paymentEventExchange(AppInfraProperties appInfraProperties) {
        return new TopicExchange(appInfraProperties.getRabbit().getPaymentExchange(), true, false);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.infra.rabbit", name = "enabled", havingValue = "true")
    public Queue paymentSuccessQueue(AppInfraProperties appInfraProperties) {
        return new Queue(appInfraProperties.getRabbit().getPaymentSuccessQueue(), true);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.infra.rabbit", name = "enabled", havingValue = "true")
    public Queue paymentSuccessDlq(AppInfraProperties appInfraProperties) {
        return new Queue(appInfraProperties.getRabbit().getPaymentSuccessDlq(), true);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.infra.rabbit", name = "enabled", havingValue = "true")
    public Binding paymentSuccessBinding(Queue paymentSuccessQueue,
                                         TopicExchange paymentEventExchange,
                                         AppInfraProperties appInfraProperties) {
        return BindingBuilder.bind(paymentSuccessQueue)
                .to(paymentEventExchange)
                .with(appInfraProperties.getRabbit().getPaymentSuccessRoutingKey());
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.infra.rabbit", name = "enabled", havingValue = "true")
    public PaymentEventPublisher paymentEventPublisher(RabbitTemplate rabbitTemplate,
                                                       ObjectMapper objectMapper,
                                                       AppInfraProperties appInfraProperties) {
        return new RabbitPaymentEventPublisher(
                rabbitTemplate,
                objectMapper,
                appInfraProperties.getRabbit().getPaymentExchange(),
                appInfraProperties.getRabbit().getPaymentSuccessRoutingKey()
        );
    }

    @Bean
    @ConditionalOnMissingBean(PaymentEventPublisher.class)
    public PaymentEventPublisher noopPaymentEventPublisher() {
        return new NoopPaymentEventPublisher();
    }
}
