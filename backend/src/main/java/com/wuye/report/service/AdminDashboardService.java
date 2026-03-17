package com.wuye.report.service;

import com.wuye.common.security.LoginUser;
import com.wuye.report.vo.AdminDashboardSummaryVO;
import com.wuye.report.vo.AdminMonthlyReportVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AdminDashboardService {

    private final AdminMonthlyReportService adminMonthlyReportService;

    public AdminDashboardService(AdminMonthlyReportService adminMonthlyReportService) {
        this.adminMonthlyReportService = adminMonthlyReportService;
    }

    public AdminDashboardSummaryVO summary(LoginUser loginUser, Integer periodYear, Integer periodMonth) {
        LocalDate now = LocalDate.now();
        int year = periodYear == null ? now.getYear() : periodYear;
        int month = periodMonth == null ? now.getMonthValue() : periodMonth;
        AdminMonthlyReportVO monthly = adminMonthlyReportService.monthly(loginUser, year, month);
        AdminDashboardSummaryVO summary = new AdminDashboardSummaryVO();
        summary.setPeriodYear(monthly.getPeriodYear());
        summary.setPeriodMonth(monthly.getPeriodMonth());
        summary.setPaidCount(monthly.getPaidCount());
        summary.setTotalCount(monthly.getTotalCount());
        summary.setPayRate(monthly.getPayRate());
        summary.setPaidAmount(monthly.getPaidAmount());
        summary.setDiscountAmount(monthly.getDiscountAmount());
        summary.setUnpaidAmount(monthly.getUnpaidAmount());
        return summary;
    }
}
