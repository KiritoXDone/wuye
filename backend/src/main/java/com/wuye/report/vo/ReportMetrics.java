package com.wuye.report.vo;

import java.math.BigDecimal;

public interface ReportMetrics {

    Long getPaidCount();

    void setPaidCount(Long paidCount);

    Long getTotalCount();

    void setTotalCount(Long totalCount);

    BigDecimal getPayRate();

    void setPayRate(BigDecimal payRate);

    BigDecimal getPaidAmount();

    void setPaidAmount(BigDecimal paidAmount);

    BigDecimal getDiscountAmount();

    void setDiscountAmount(BigDecimal discountAmount);

    BigDecimal getUnpaidAmount();

    void setUnpaidAmount(BigDecimal unpaidAmount);
}
