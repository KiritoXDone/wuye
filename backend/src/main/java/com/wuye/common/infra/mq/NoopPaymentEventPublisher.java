package com.wuye.common.infra.mq;

import com.wuye.payment.event.PaymentSuccessEvent;
public class NoopPaymentEventPublisher implements PaymentEventPublisher {

    @Override
    public void publishPaymentSuccess(PaymentSuccessEvent event) {
    }
}
