package com.wuye.room.vo;

public class AdminCommunityVO {

    private Long id;
    private String communityCode;
    private String name;
    private Integer status;
    private Long roomTypeCount;
    private Long roomCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCommunityCode() {
        return communityCode;
    }

    public void setCommunityCode(String communityCode) {
        this.communityCode = communityCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getRoomTypeCount() {
        return roomTypeCount;
    }

    public void setRoomTypeCount(Long roomTypeCount) {
        this.roomTypeCount = roomTypeCount;
    }

    public Long getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(Long roomCount) {
        this.roomCount = roomCount;
    }
}
