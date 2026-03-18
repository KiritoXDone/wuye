package com.wuye.common.infra.mq;

import com.wuye.payment.event.PaymentSuccessEvent;

public interface PaymentEventPublisher {

    void publishPaymentSuccess(PaymentSuccessEvent event);
}
