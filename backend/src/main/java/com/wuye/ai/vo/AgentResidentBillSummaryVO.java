package com.wuye.ai.vo;

import com.wuye.bill.vo.BillListItemVO;
import com.wuye.room.vo.RoomVO;

import java.math.BigDecimal;
import java.util.List;

public class AgentResidentBillSummaryVO {

    private Long accountId;
    private String realName;
    private Integer roomCount;
    private Integer activeRoomCount;
    private Integer issuedBillCount;
    private Integer unpaidBillCount;
    private BigDecimal unpaidAmountTotal;
    private List<RoomVO> rooms;
    private List<BillListItemVO> recentBills;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Integer getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(Integer roomCount) {
        this.roomCount = roomCount;
    }

    public Integer getActiveRoomCount() {
        return activeRoomCount;
    }

    public void setActiveRoomCount(Integer activeRoomCount) {
        this.activeRoomCount = activeRoomCount;
    }

    public Integer getIssuedBillCount() {
        return issuedBillCount;
    }

    public void setIssuedBillCount(Integer issuedBillCount) {
        this.issuedBillCount = issuedBillCount;
    }

    public Integer getUnpaidBillCount() {
        return unpaidBillCount;
    }

    public void setUnpaidBillCount(Integer unpaidBillCount) {
        this.unpaidBillCount = unpaidBillCount;
    }

    public BigDecimal getUnpaidAmountTotal() {
        return unpaidAmountTotal;
    }

    public void setUnpaidAmountTotal(BigDecimal unpaidAmountTotal) {
        this.unpaidAmountTotal = unpaidAmountTotal;
    }

    public List<RoomVO> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomVO> rooms) {
        this.rooms = rooms;
    }

    public List<BillListItemVO> getRecentBills() {
        return recentBills;
    }

    public void setRecentBills(List<BillListItemVO> recentBills) {
        this.recentBills = recentBills;
    }
}
