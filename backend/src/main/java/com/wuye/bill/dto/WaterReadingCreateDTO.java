package com.wuye.bill.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WaterReadingCreateDTO {

    @NotNull(message = "roomId 不能为空")
    private Long roomId;
    @NotNull(message = "year 不能为空")
    private Integer year;
    @NotNull(message = "month 不能为空")
    @Min(value = 1, message = "month 必须在 1 到 12 之间")
    @Max(value = 12, message = "month 必须在 1 到 12 之间")
    private Integer month;
    @NotNull(message = "prevReading 不能为空")
    @DecimalMin(value = "0.000", message = "prevReading 不能小于 0")
    private BigDecimal prevReading;
    @NotNull(message = "currReading 不能为空")
    @DecimalMin(value = "0.000", message = "currReading 不能小于 0")
    private BigDecimal currReading;
    @NotNull(message = "readAt 不能为空")
    private LocalDateTime readAt;
    private String photoUrl;
    private String remark;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
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
}
