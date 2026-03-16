package com.wuye.bill.entity;

import java.time.LocalDate;

public class WaterMeter {

    private Long id;
    private Long roomId;
    private String meterNo;
    private LocalDate installAt;
    private Integer status;

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

    public String getMeterNo() {
        return meterNo;
    }

    public void setMeterNo(String meterNo) {
        this.meterNo = meterNo;
    }

    public LocalDate getInstallAt() {
        return installAt;
    }

    public void setInstallAt(LocalDate installAt) {
        this.installAt = installAt;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
