package com.wuye.bill.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class HouseholdPaymentOverviewVO {

    private Long roomId;
    private Long communityId;
    private String communityName;
    private String buildingNo;
    private String unitNo;
    private String roomNo;
    private String roomLabel;
    private String roomTypeName;
    private BigDecimal areaM2;
    private Long propertyBillId;
    private String propertyBillNo;
    private BigDecimal propertyAmountDue;
    private BigDecimal propertyAmountPaid;
    private String propertyStatus;
    private LocalDate propertyDueDate;
    private LocalDateTime propertyPaidAt;
    private Long waterBillId;
    private String waterBillNo;
    private BigDecimal waterAmountDue;
    private BigDecimal waterAmountPaid;
    private String waterStatus;
    private LocalDate waterDueDate;
    private LocalDateTime waterPaidAt;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getCommunityName() {
        return communityName;
    }

    public void setCommunityName(String communityName) {
        this.communityName = communityName;
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

    public String getRoomLabel() {
        return roomLabel;
    }

    public void setRoomLabel(String roomLabel) {
        this.roomLabel = roomLabel;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public BigDecimal getAreaM2() {
        return areaM2;
    }

    public void setAreaM2(BigDecimal areaM2) {
        this.areaM2 = areaM2;
    }

    public Long getPropertyBillId() {
        return propertyBillId;
    }

    public void setPropertyBillId(Long propertyBillId) {
        this.propertyBillId = propertyBillId;
    }

    public String getPropertyBillNo() {
        return propertyBillNo;
    }

    public void setPropertyBillNo(String propertyBillNo) {
        this.propertyBillNo = propertyBillNo;
    }

    public BigDecimal getPropertyAmountDue() {
        return propertyAmountDue;
    }

    public void setPropertyAmountDue(BigDecimal propertyAmountDue) {
        this.propertyAmountDue = propertyAmountDue;
    }

    public BigDecimal getPropertyAmountPaid() {
        return propertyAmountPaid;
    }

    public void setPropertyAmountPaid(BigDecimal propertyAmountPaid) {
        this.propertyAmountPaid = propertyAmountPaid;
    }

    public String getPropertyStatus() {
        return propertyStatus;
    }

    public void setPropertyStatus(String propertyStatus) {
        this.propertyStatus = propertyStatus;
    }

    public LocalDate getPropertyDueDate() {
        return propertyDueDate;
    }

    public void setPropertyDueDate(LocalDate propertyDueDate) {
        this.propertyDueDate = propertyDueDate;
    }

    public LocalDateTime getPropertyPaidAt() {
        return propertyPaidAt;
    }

    public void setPropertyPaidAt(LocalDateTime propertyPaidAt) {
        this.propertyPaidAt = propertyPaidAt;
    }

    public Long getWaterBillId() {
        return waterBillId;
    }

    public void setWaterBillId(Long waterBillId) {
        this.waterBillId = waterBillId;
    }

    public String getWaterBillNo() {
        return waterBillNo;
    }

    public void setWaterBillNo(String waterBillNo) {
        this.waterBillNo = waterBillNo;
    }

    public BigDecimal getWaterAmountDue() {
        return waterAmountDue;
    }

    public void setWaterAmountDue(BigDecimal waterAmountDue) {
        this.waterAmountDue = waterAmountDue;
    }

    public BigDecimal getWaterAmountPaid() {
        return waterAmountPaid;
    }

    public void setWaterAmountPaid(BigDecimal waterAmountPaid) {
        this.waterAmountPaid = waterAmountPaid;
    }

    public String getWaterStatus() {
        return waterStatus;
    }

    public void setWaterStatus(String waterStatus) {
        this.waterStatus = waterStatus;
    }

    public LocalDate getWaterDueDate() {
        return waterDueDate;
    }

    public void setWaterDueDate(LocalDate waterDueDate) {
        this.waterDueDate = waterDueDate;
    }

    public LocalDateTime getWaterPaidAt() {
        return waterPaidAt;
    }

    public void setWaterPaidAt(LocalDateTime waterPaidAt) {
        this.waterPaidAt = waterPaidAt;
    }
}
