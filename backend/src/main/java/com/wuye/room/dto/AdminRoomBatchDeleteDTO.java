package com.wuye.room.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AdminRoomBatchDeleteDTO {

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
    private Integer status;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
