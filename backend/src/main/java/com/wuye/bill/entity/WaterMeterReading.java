package com.wuye.bill.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WaterMeterReading {

    private Long id;
    private Long roomId;
    private Long meterId;
    private Integer periodYear;
    private Integer periodMonth;
    private BigDecimal prevReading;
    private BigDecimal currReading;
    private BigDecimal usageAmount;
    private Long readByAdminId;
    private LocalDateTime readAt;
    private String photoUrl;
    private String remark;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getMeterId() {
        return meterId;
    }

    public void setMeterId(Long meterId) {
        this.meterId = meterId;
    }

    public Integer getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(Integer periodYear) {
        this.periodYear = periodYear;
    }

    public Integer getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(Integer periodMonth) {
        this.periodMonth = periodMonth;
    }

    public BigDecimal getPrevReading() {
        return prevReading;
    }

    public void setPrevReading(BigDecimal prevReading) {
        this.prevReading = prevReading;
    }

    public BigDecimal getCurrReading() {
        return currReading;
    }

    public void setCurrReading(BigDecimal currReading) {
        this.currReading = currReading;
    }

    public BigDecimal getUsageAmount() {
        return usageAmount;
    }

    public void setUsageAmount(BigDecimal usageAmount) {
        this.usageAmount = usageAmount;
    }

    public Long getReadByAdminId() {
        return readByAdminId;
    }

    public void setReadByAdminId(Long readByAdminId) {
        this.readByAdminId = readByAdminId;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
