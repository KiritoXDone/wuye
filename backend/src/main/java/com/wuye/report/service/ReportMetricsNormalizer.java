package com.wuye.report.service;

import com.wuye.common.util.MoneyUtils;
import com.wuye.report.vo.ReportMetrics;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ReportMetricsNormalizer {

    public <T extends ReportMetrics> T normalize(T metrics) {
        metrics.setPaidCount(defaultCount(metrics.getPaidCount()));
        metrics.setTotalCount(defaultCount(metrics.getTotalCount()));
        metrics.setPaidAmount(defaultAmount(metrics.getPaidAmount()));
        metrics.setDiscountAmount(defaultAmount(metrics.getDiscountAmount()));
        metrics.setUnpaidAmount(defaultAmount(metrics.getUnpaidAmount()));
        metrics.setPayRate(calculatePayRate(metrics.getPaidCount(), metrics.getTotalCount()));
        return metrics;
    }

    private Long defaultCount(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? MoneyUtils.scaleMoney(BigDecimal.ZERO) : MoneyUtils.scaleMoney(value);
    }

    private BigDecimal calculatePayRate(Long paidCount, Long totalCount) {
        if (totalCount == null || totalCount == 0L) {
            return MoneyUtils.scaleMoney(BigDecimal.ZERO);
        }
        return BigDecimal.valueOf(paidCount == null ? 0L : paidCount)
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
    }
}
