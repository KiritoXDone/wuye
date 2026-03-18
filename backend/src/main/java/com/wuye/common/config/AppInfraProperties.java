package com.wuye.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.infra")
public class AppInfraProperties {

    private final Redis redis = new Redis();
    private final Rabbit rabbit = new Rabbit();

    public Redis getRedis() {
        return redis;
    }

    public Rabbit getRabbit() {
        return rabbit;
    }

    public static class Redis {
        private boolean enabled;
        private String keyPrefix = "wuye:";
        private long callbackLockSeconds = 300;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public long getCallbackLockSeconds() {
            return callbackLockSeconds;
        }

        public void setCallbackLockSeconds(long callbackLockSeconds) {
            this.callbackLockSeconds = callbackLockSeconds;
        }
    }

    public static class Rabbit {
        private boolean enabled;
        private String paymentExchange = "wuye.payment.events";
        private String paymentSuccessRoutingKey = "payment.success";
        private String paymentSuccessQueue = "wuye.payment.success";
        private String paymentSuccessDlq = "wuye.payment.success.dlq";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPaymentExchange() {
            return paymentExchange;
        }

        public void setPaymentExchange(String paymentExchange) {
            this.paymentExchange = paymentExchange;
        }

        public String getPaymentSuccessRoutingKey() {
            return paymentSuccessRoutingKey;
        }

        public void setPaymentSuccessRoutingKey(String paymentSuccessRoutingKey) {
            this.paymentSuccessRoutingKey = paymentSuccessRoutingKey;
        }

        public String getPaymentSuccessQueue() {
            return paymentSuccessQueue;
        }

        public void setPaymentSuccessQueue(String paymentSuccessQueue) {
            this.paymentSuccessQueue = paymentSuccessQueue;
        }

        public String getPaymentSuccessDlq() {
            return paymentSuccessDlq;
        }

        public void setPaymentSuccessDlq(String paymentSuccessDlq) {
            this.paymentSuccessDlq = paymentSuccessDlq;
        }
    }
}
