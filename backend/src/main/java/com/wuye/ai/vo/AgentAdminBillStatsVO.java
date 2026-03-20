package com.wuye.ai.vo;

import com.wuye.report.vo.AdminDashboardSummaryVO;
import com.wuye.report.vo.AdminMonthlyReportVO;

public class AgentAdminBillStatsVO {

    private Integer periodYear;
    private Integer periodMonth;
    private AdminDashboardSummaryVO summary;
    private AdminMonthlyReportVO propertyYearly;
    private AdminMonthlyReportVO waterMonthly;

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

    public AdminDashboardSummaryVO getSummary() {
        return summary;
    }

    public void setSummary(AdminDashboardSummaryVO summary) {
        this.summary = summary;
    }

    public AdminMonthlyReportVO getPropertyYearly() {
        return propertyYearly;
    }

    public void setPropertyYearly(AdminMonthlyReportVO propertyYearly) {
        this.propertyYearly = propertyYearly;
    }

    public AdminMonthlyReportVO getWaterMonthly() {
        return waterMonthly;
    }

    public void setWaterMonthly(AdminMonthlyReportVO waterMonthly) {
        this.waterMonthly = waterMonthly;
    }
}
