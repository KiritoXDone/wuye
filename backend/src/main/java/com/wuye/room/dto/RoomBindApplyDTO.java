package com.wuye.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RoomBindApplyDTO {

    @NotNull(message = "communityId 不能为空")
    private Long communityId;
    @NotBlank(message = "buildingNo 不能为空")
    private String buildingNo;
    @NotBlank(message = "unitNo 不能为空")
    private String unitNo;
    @NotBlank(message = "roomNo 不能为空")
    private String roomNo;
    @NotBlank(message = "relationType 不能为空")
    private String relationType;
    private String applyRemark;

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

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public String getApplyRemark() {
        return applyRemark;
    }

    public void setApplyRemark(String applyRemark) {
        this.applyRemark = applyRemark;
    }
}
