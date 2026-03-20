package com.wuye.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.audit.entity.AuditLog;
import com.wuye.audit.mapper.AuditLogMapper;
import com.wuye.audit.service.AuditLogService;
import com.wuye.audit.vo.AuditLogVO;
import com.wuye.common.security.AccessGuard;
import com.wuye.payment.event.PaymentSuccessEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentSuccessEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldRecordAuditLogAfterConsumingPaymentSuccessEvent() throws Exception {
        InMemoryAuditLogMapper auditLogMapper = new InMemoryAuditLogMapper();
        AuditLogService auditLogService = new AuditLogService(auditLogMapper, new AccessGuard(), objectMapper);
        PaymentSuccessEventConsumer paymentSuccessEventConsumer = new PaymentSuccessEventConsumer(objectMapper, auditLogService);

        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setPayOrderNo("P202603170001");
        event.setBillId(10001L);
        event.setAccountId(20002L);
        event.setChannel("WECHAT");
        event.setPayAmount(new BigDecimal("88.50"));
        event.setPaidAt(LocalDateTime.of(2026, 3, 17, 22, 0, 0));
        event.setAnnualPayment(true);
        event.setCoveredBillCount(1);

        paymentSuccessEventConsumer.onPaymentSuccess(objectMapper.writeValueAsString(event));

        AuditLog recorded = auditLogMapper.lastInserted;
        assertThat(recorded).isNotNull();
        assertThat(recorded.getBizType()).isEqualTo("PAYMENT_EVENT");
        assertThat(recorded.getBizId()).isEqualTo("P202603170001");
        assertThat(recorded.getAction()).isEqualTo("PAYMENT_SUCCESS_CONSUMED");
        assertThat(recorded.getOperatorId()).isNull();
        assertThat(recorded.getDetailJson()).contains("\"payOrderNo\":\"P202603170001\"");
        assertThat(recorded.getDetailJson()).contains("\"billId\":10001");
        assertThat(recorded.getDetailJson()).contains("\"accountId\":20002");
        assertThat(recorded.getDetailJson()).contains("\"channel\":\"WECHAT\"");
        assertThat(recorded.getDetailJson()).contains("\"payAmount\":88.50");
        assertThat(recorded.getDetailJson()).contains("\"annualPayment\":true");
        assertThat(recorded.getDetailJson()).contains("\"coveredBillCount\":1");
    }

    private static class InMemoryAuditLogMapper implements AuditLogMapper {
        private AuditLog lastInserted;

        @Override
        public int insert(AuditLog auditLog) {
            this.lastInserted = auditLog;
            return 1;
        }

        @Override
        public List<AuditLogVO> listPage(String bizType,
                                         String bizId,
                                         Long operatorId,
                                         LocalDateTime createdAtStart,
                                         LocalDateTime createdAtEnd,
                                         int offset,
                                         int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long count(String bizType,
                          String bizId,
                          Long operatorId,
                          LocalDateTime createdAtStart,
                          LocalDateTime createdAtEnd) {
            throw new UnsupportedOperationException();
        }
    }
}
