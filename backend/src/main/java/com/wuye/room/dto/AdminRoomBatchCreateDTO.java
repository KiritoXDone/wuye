package com.wuye.room.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class AdminRoomBatchCreateDTO {

    @NotNull
    @Min(1)
    private Long communityId;

    @NotBlank
    private String buildingNo;

    @NotBlank
    private String unitNo;

    @NotEmpty
    private List<String> roomNos;

    @Min(1)
    private Long roomTypeId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal areaM2;

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getBuildingNo() {
        return buildingNo;
    }

    public void setBuildingNo(String buildingNo) {
        this.buildingNo = buildingNo;
    }

    public String getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(String unitNo) {
        this.unitNo = unitNo;
    }

    public List<String> getRoomNos() {
        return roomNos;
    }

    public void setRoomNos(List<String> roomNos) {
        this.roomNos = roomNos;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public BigDecimal getAreaM2() {
        return areaM2;
    }

    public void setAreaM2(BigDecimal areaM2) {
        this.areaM2 = areaM2;
    }
}
