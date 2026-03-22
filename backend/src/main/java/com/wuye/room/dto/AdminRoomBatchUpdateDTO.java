package com.wuye.room.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class AdminRoomBatchUpdateDTO {

    @NotNull
    @Min(1)
    private Long communityId;

    private List<Long> selectionRoomIds;
    private Boolean applyToFiltered = Boolean.FALSE;
    private String buildingNo;
    private String unitNo;
    private String roomNoKeyword;
    private String roomSuffix;
    private Long roomTypeId;

    @Min(1)
    private Long targetRoomTypeId;

    @DecimalMin(value = "0.01")
    private BigDecimal targetAreaM2;

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public List<Long> getSelectionRoomIds() {
        return selectionRoomIds;
    }

    public void setSelectionRoomIds(List<Long> selectionRoomIds) {
        this.selectionRoomIds = selectionRoomIds;
    }

    public Boolean getApplyToFiltered() {
        return applyToFiltered;
    }

    public void setApplyToFiltered(Boolean applyToFiltered) {
        this.applyToFiltered = applyToFiltered;
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

    public String getRoomNoKeyword() {
        return roomNoKeyword;
    }

    public void setRoomNoKeyword(String roomNoKeyword) {
        this.roomNoKeyword = roomNoKeyword;
    }

    public String getRoomSuffix() {
        return roomSuffix;
    }

    public void setRoomSuffix(String roomSuffix) {
        this.roomSuffix = roomSuffix;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public Long getTargetRoomTypeId() {
        return targetRoomTypeId;
    }

    public void setTargetRoomTypeId(Long targetRoomTypeId) {
        this.targetRoomTypeId = targetRoomTypeId;
    }

    public BigDecimal getTargetAreaM2() {
        return targetAreaM2;
    }

    public void setTargetAreaM2(BigDecimal targetAreaM2) {
        this.targetAreaM2 = targetAreaM2;
    }
}
