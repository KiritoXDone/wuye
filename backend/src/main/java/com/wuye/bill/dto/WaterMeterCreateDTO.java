package com.wuye.bill.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class WaterMeterCreateDTO {

    @NotNull(message = "roomId 不能为空")
    private Long roomId;
    private String meterNo;
    private LocalDate installAt;

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
}
