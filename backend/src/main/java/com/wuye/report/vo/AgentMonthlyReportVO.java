package com.wuye.report.vo;

import java.math.BigDecimal;

public class AgentMonthlyReportVO {

    private Long groupId;
    private String groupName;
    private Long orgUnitId;
    private String orgUnitName;
    private String tenantCode;
    private String period;
    private Long paidCount;
    private Long totalCount;
    private BigDecimal payRate;
    private BigDecimal paidAmount;
    private BigDecimal discountAmount;
    private BigDecimal unpaidAmount;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Long getOrgUnitId() { return orgUnitId; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public String getOrgUnitName() { return orgUnitName; }
    public void setOrgUnitName(String orgUnitName) { this.orgUnitName = orgUnitName; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Long getPaidCount() { return paidCount; }
    public void setPaidCount(Long paidCount) { this.paidCount = paidCount; }
    public Long getTotalCount() { return totalCount; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    public BigDecimal getPayRate() { return payRate; }
    public void setPayRate(BigDecimal payRate) { this.payRate = payRate; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getUnpaidAmount() { return unpaidAmount; }
    public void setUnpaidAmount(BigDecimal unpaidAmount) { this.unpaidAmount = unpaidAmount; }
}
