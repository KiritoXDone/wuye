package com.wuye.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.audit.service.AuditLogService;
import com.wuye.payment.event.PaymentSuccessEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.infra.rabbit", name = "enabled", havingValue = "true")
public class PaymentSuccessEventConsumer {

    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    public PaymentSuccessEventConsumer(ObjectMapper objectMapper,
                                       AuditLogService auditLogService) {
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
    }

    @RabbitListener(queues = "${app.infra.rabbit.payment-success-queue}")
    public void onPaymentSuccess(String payload) throws JsonProcessingException {
        PaymentSuccessEvent event = objectMapper.readValue(payload, PaymentSuccessEvent.class);
        auditLogService.record(
                null,
                "PAYMENT_EVENT",
                event.getPayOrderNo(),
                "PAYMENT_SUCCESS_CONSUMED",
                Map.of(
                        "payOrderNo", event.getPayOrderNo(),
                        "billId", event.getBillId(),
                        "accountId", event.getAccountId(),
                        "channel", event.getChannel(),
                        "payAmount", event.getPayAmount(),
                        "paidAt", event.getPaidAt()
                )
        );
    }
}
