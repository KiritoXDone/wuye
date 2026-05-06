package com.wuye.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.ai.service.AgentConversationCacheService;
import com.wuye.ai.service.NoopAgentConversationCacheService;
import com.wuye.common.infra.mq.CouponSeckillEventPublisher;
import com.wuye.common.infra.mq.NoopCouponSeckillEventPublisher;
import com.wuye.common.infra.mq.NoopPaymentEventPublisher;
import com.wuye.common.infra.mq.PaymentEventPublisher;
import com.wuye.common.infra.mq.RabbitCouponSeckillEventPublisher;
import com.wuye.common.infra.mq.RabbitPaymentEventPublisher;
import com.wuye.common.infra.redis.NoopRedisCallbackLock;
import com.wuye.common.infra.redis.NoopSeckillStockLock;
import com.wuye.common.infra.redis.RedissonSeckillStockLock;
import com.wuye.common.infra.redis.RedisCallbackLock;
import com.wuye.common.infra.redis.SeckillStockLock;
import com.wuye.common.infra.redis.StringRedisCallbackLock;
import org.redisson.api.RedissonClient;
import org.redisson.Redisson;
import org.redisson.config.Config;
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
    @ConditionalOnProperty(prefix = "app.infra.redis", name = "enabled", havingValue = "true")
    public RedissonClient redissonClient(org.springframework.boot.autoconfigure.data.redis.RedisProperties redisProperties) {
        Config config = new Config();
        String address = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase())
                .setPassword(redisProperties.getPassword() == null || redisProperties.getPassword().isBlank() ? null : redisProperties.getPassword());
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.infra.redis", name = "enabled", havingValue = "true")
    public SeckillStockLock redissonSeckillStockLock(RedissonClient redissonClient,
                                                     AppInfraProperties appInfraProperties) {
        return new RedissonSeckillStockLock(
                redissonClient,
                appInfraProperties.getRedis().getKeyPrefix(),
                Duration.ofSeconds(appInfraProperties.getRedis().getSeckillLockSeconds())
        );
    }

    @Bean
    @ConditionalOnMissingBean(SeckillStockLock.class)
    public SeckillStockLock noopSeckillStockLock() {
        return new NoopSeckillStockLock();
    }

    @Bean
    @ConditionalOnMissingBean(AgentConversationCacheService.class)
    public AgentConversationCacheService noopAgentConversationCacheService() {
        return new NoopAgentConversationCacheService();
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
    public TopicExchange couponEventExchange(AppInfraProperties appInfraProperties) {
        return new TopicExchange(appInfraProperties.getRabbit().getCouponExchange(), true, false);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.infra.rabbit", name = "enabled", havingValue = "true")
    public Queue couponSeckillQueue(AppInfraProperties appInfraProperties) {
        return new Queue(appInfraProperties.getRabbit().getCouponSeckillQueue(), true);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.infra.rabbit", name = "enabled", havingValue = "true")
    public Queue couponSeckillDlq(AppInfraProperties appInfraProperties) {
        return new Queue(appInfraProperties.getRabbit().getCouponSeckillDlq(), true);
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
    public Binding couponSeckillBinding(Queue couponSeckillQueue,
                                        TopicExchange couponEventExchange,
                                        AppInfraProperties appInfraProperties) {
        return BindingBuilder.bind(couponSeckillQueue)
                .to(couponEventExchange)
                .with(appInfraProperties.getRabbit().getCouponSeckillRoutingKey());
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

    @Bean
    @ConditionalOnProperty(prefix = "app.infra.rabbit", name = "enabled", havingValue = "true")
    public CouponSeckillEventPublisher couponSeckillEventPublisher(RabbitTemplate rabbitTemplate,
                                                                   ObjectMapper objectMapper,
                                                                   AppInfraProperties appInfraProperties) {
        return new RabbitCouponSeckillEventPublisher(
                rabbitTemplate,
                objectMapper,
                appInfraProperties.getRabbit().getCouponExchange(),
                appInfraProperties.getRabbit().getCouponSeckillRoutingKey()
        );
    }

    @Bean
    @ConditionalOnMissingBean(CouponSeckillEventPublisher.class)
    public CouponSeckillEventPublisher noopCouponSeckillEventPublisher() {
        return new NoopCouponSeckillEventPublisher();
    }
}
