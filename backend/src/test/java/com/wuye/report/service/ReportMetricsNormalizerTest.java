package com.wuye.report.service;

import com.wuye.report.vo.AdminMonthlyReportVO;
import com.wuye.report.vo.AgentMonthlyReportVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReportMetricsNormalizerTest {

    private final ReportMetricsNormalizer reportMetricsNormalizer = new ReportMetricsNormalizer();

    @Test
    void shouldFillMissingAdminMetricsWithZeros() {
        AdminMonthlyReportVO metrics = new AdminMonthlyReportVO();

        AdminMonthlyReportVO normalized = reportMetricsNormalizer.normalize(metrics);

        assertThat(normalized.getPaidCount()).isEqualTo(0L);
        assertThat(normalized.getTotalCount()).isEqualTo(0L);
        assertThat(normalized.getPaidAmount()).isEqualByComparingTo("0.00");
        assertThat(normalized.getDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(normalized.getUnpaidAmount()).isEqualByComparingTo("0.00");
        assertThat(normalized.getPayRate()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCalculateAgentPayRateAndNormalizeScale() {
        AgentMonthlyReportVO metrics = new AgentMonthlyReportVO();
        metrics.setPaidCount(1L);
        metrics.setTotalCount(3L);
        metrics.setPaidAmount(new BigDecimal("10"));
        metrics.setDiscountAmount(new BigDecimal("2.5"));
        metrics.setUnpaidAmount(new BigDecimal("5.678"));

        AgentMonthlyReportVO normalized = reportMetricsNormalizer.normalize(metrics);

        assertThat(normalized.getPaidAmount()).isEqualByComparingTo("10.00");
        assertThat(normalized.getDiscountAmount()).isEqualByComparingTo("2.50");
        assertThat(normalized.getUnpaidAmount()).isEqualByComparingTo("5.68");
        assertThat(normalized.getPayRate()).isEqualByComparingTo("0.33");
    }
}
