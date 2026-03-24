package com.wuye.report.service;

import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.report.mapper.ReportMapper;
import com.wuye.report.vo.AdminMonthlyReportVO;
import org.springframework.stereotype.Service;

@Service
public class AdminMonthlyReportService {

    private final ReportMapper reportMapper;
    private final AccessGuard accessGuard;
    private final ReportMetricsNormalizer reportMetricsNormalizer;

    public AdminMonthlyReportService(ReportMapper reportMapper,
                                     AccessGuard accessGuard,
                                     ReportMetricsNormalizer reportMetricsNormalizer) {
        this.reportMapper = reportMapper;
        this.accessGuard = accessGuard;
        this.reportMetricsNormalizer = reportMetricsNormalizer;
    }

    public AdminMonthlyReportVO propertyYearly(LoginUser loginUser, Integer periodYear) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "FINANCE");
        return reportMetricsNormalizer.normalize(reportMapper.propertyYearly(periodYear));
    }

    public AdminMonthlyReportVO waterMonthly(LoginUser loginUser, Integer periodYear, Integer periodMonth) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "FINANCE");
        return reportMetricsNormalizer.normalize(reportMapper.waterMonthly(periodYear, periodMonth));
    }

    public AdminMonthlyReportVO monthly(LoginUser loginUser, Integer periodYear, Integer periodMonth) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "FINANCE");
        return reportMetricsNormalizer.normalize(reportMapper.monthly(periodYear, periodMonth));
    }
}
