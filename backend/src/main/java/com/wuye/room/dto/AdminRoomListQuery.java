package com.wuye.room.dto;

public class AdminRoomListQuery {

    private Long communityId;
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
