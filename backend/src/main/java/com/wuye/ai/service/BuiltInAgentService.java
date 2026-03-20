package com.wuye.ai.service;

import com.wuye.ai.vo.AgentAdminBillStatsVO;
import com.wuye.ai.vo.AgentResidentBillSummaryVO;
import com.wuye.bill.dto.BillListQuery;
import com.wuye.bill.service.BillQueryService;
import com.wuye.bill.vo.BillListItemVO;
import com.wuye.common.api.PageResponse;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.report.service.AdminDashboardService;
import com.wuye.report.service.AdminMonthlyReportService;
import com.wuye.report.vo.AdminDashboardSummaryVO;
import com.wuye.room.service.RoomBindingService;
import com.wuye.room.vo.RoomVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BuiltInAgentService {

    private final AccessGuard accessGuard;
    private final RoomBindingService roomBindingService;
    private final BillQueryService billQueryService;
    private final AdminDashboardService adminDashboardService;
    private final AdminMonthlyReportService adminMonthlyReportService;

    public BuiltInAgentService(AccessGuard accessGuard,
                               RoomBindingService roomBindingService,
                               BillQueryService billQueryService,
                               AdminDashboardService adminDashboardService,
                               AdminMonthlyReportService adminMonthlyReportService) {
        this.accessGuard = accessGuard;
        this.roomBindingService = roomBindingService;
        this.billQueryService = billQueryService;
        this.adminDashboardService = adminDashboardService;
        this.adminMonthlyReportService = adminMonthlyReportService;
    }

    public AgentResidentBillSummaryVO residentBillSummary(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        List<RoomVO> rooms = roomBindingService.myRooms(loginUser);
        BillListQuery query = new BillListQuery();
        query.setPageNo(1);
        query.setPageSize(10);
        PageResponse<BillListItemVO> page = billQueryService.listMyBills(loginUser, query);
        List<BillListItemVO> recentBills = page.list();
        int activeRoomCount = (int) rooms.stream().filter(room -> "ACTIVE".equals(room.getBindingStatus())).count();
        int unpaidBillCount = (int) recentBills.stream().filter(bill -> !"PAID".equals(bill.getStatus())).count();
        int issuedBillCount = recentBills.size();
        BigDecimal unpaidAmountTotal = recentBills.stream()
                .filter(bill -> !"PAID".equals(bill.getStatus()))
                .map(BillListItemVO::getAmountDue)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AgentResidentBillSummaryVO vo = new AgentResidentBillSummaryVO();
        vo.setAccountId(loginUser.accountId());
        vo.setRealName(loginUser.realName());
        vo.setRoomCount(rooms.size());
        vo.setActiveRoomCount(activeRoomCount);
        vo.setIssuedBillCount(issuedBillCount);
        vo.setUnpaidBillCount(unpaidBillCount);
        vo.setUnpaidAmountTotal(unpaidAmountTotal);
        vo.setRooms(rooms);
        vo.setRecentBills(recentBills);

        return vo;
    }

    public AgentAdminBillStatsVO adminBillStats(LoginUser loginUser, Integer periodYear, Integer periodMonth) {
        accessGuard.requireRole(loginUser, "ADMIN");
        AdminDashboardSummaryVO summary = adminDashboardService.summary(loginUser, periodYear, periodMonth);
        AgentAdminBillStatsVO vo = new AgentAdminBillStatsVO();
        vo.setPeriodYear(summary.getPeriodYear());
        vo.setPeriodMonth(summary.getPeriodMonth());
        vo.setSummary(summary);
        vo.setPropertyYearly(adminMonthlyReportService.propertyYearly(loginUser, periodYear == null ? summary.getPeriodYear() : periodYear));
        vo.setWaterMonthly(adminMonthlyReportService.waterMonthly(loginUser, summary.getPeriodYear(), summary.getPeriodMonth()));

        return vo;
    }
}
