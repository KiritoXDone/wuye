package com.wuye.room.dto;

public class AdminRoomListQuery {

    private Long communityId;
    private String buildingNo;
    private String unitNo;
    private String roomNoKeyword;
    private String roomNo;
    private String roomSuffix;
    private Long roomTypeId;

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

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
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
}
