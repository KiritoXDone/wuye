package com.wuye.importexport.dto;

public class BillExportCreateDTO {
    private Integer periodYear;
    private Integer periodMonth;
    private String feeType;
    private String status;

    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
    public String getFeeType() { return feeType; }
    public void setFeeType(String feeType) { this.feeType = feeType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
