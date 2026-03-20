package com.wuye.bill.vo;

import com.wuye.coupon.vo.AvailableCouponVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BillDetailVO {

    private Long billId;
    private String billNo;
    private Long roomId;
    private String roomLabel;
    private String roomTypeName;
    private String feeType;
    private String cycleType;
    private Integer periodYear;
    private Integer periodMonth;
    private LocalDate servicePeriodStart;
    private LocalDate servicePeriodEnd;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private String status;
    private LocalDate dueDate;
    private List<BillLineVO> billLines;
    private List<AvailableCouponVO> availableCoupons;

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
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

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    public String getCycleType() {
        return cycleType;
    }

    public void setCycleType(String cycleType) {
        this.cycleType = cycleType;
    }

    public Integer getPeriodYear() {
        return periodYear;
    }

    public void setPeriodYear(Integer periodYear) {
        this.periodYear = periodYear;
    }

    public Integer getPeriodMonth() {
        return periodMonth;
    }

    public void setPeriodMonth(Integer periodMonth) {
        this.periodMonth = periodMonth;
    }

    public LocalDate getServicePeriodStart() {
        return servicePeriodStart;
    }

    public void setServicePeriodStart(LocalDate servicePeriodStart) {
        this.servicePeriodStart = servicePeriodStart;
    }

    public LocalDate getServicePeriodEnd() {
        return servicePeriodEnd;
    }

    public void setServicePeriodEnd(LocalDate servicePeriodEnd) {
        this.servicePeriodEnd = servicePeriodEnd;
    }

    public BigDecimal getAmountDue() {
        return amountDue;
    }

    public void setAmountDue(BigDecimal amountDue) {
        this.amountDue = amountDue;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public List<BillLineVO> getBillLines() {
        return billLines;
    }

    public void setBillLines(List<BillLineVO> billLines) {
        this.billLines = billLines;
    }

    public List<AvailableCouponVO> getAvailableCoupons() {
        return availableCoupons;
    }

    public void setAvailableCoupons(List<AvailableCouponVO> availableCoupons) {
        this.availableCoupons = availableCoupons;
    }
}
