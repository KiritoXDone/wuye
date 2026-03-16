package com.wuye.report.service;

import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.report.mapper.ReportMapper;
import com.wuye.report.vo.AdminMonthlyReportVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AdminMonthlyReportService {

    private final ReportMapper reportMapper;
    private final AccessGuard accessGuard;

    public AdminMonthlyReportService(ReportMapper reportMapper, AccessGuard accessGuard) {
        this.reportMapper = reportMapper;
        this.accessGuard = accessGuard;
    }

    public AdminMonthlyReportVO monthly(LoginUser loginUser, Integer periodYear, Integer periodMonth) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "FINANCE");
        AdminMonthlyReportVO vo = reportMapper.monthly(periodYear, periodMonth);
        if (vo.getPaidCount() == null) {
            vo.setPaidCount(0L);
        }
        if (vo.getTotalCount() == null) {
            vo.setTotalCount(0L);
        }
        if (vo.getPaidAmount() == null) {
            vo.setPaidAmount(BigDecimal.ZERO.setScale(2));
        }
        if (vo.getDiscountAmount() == null) {
            vo.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        }
        if (vo.getUnpaidAmount() == null) {
            vo.setUnpaidAmount(BigDecimal.ZERO.setScale(2));
        }
        if (vo.getTotalCount() == 0) {
            vo.setPayRate(BigDecimal.ZERO.setScale(2));
        } else {
            vo.setPayRate(BigDecimal.valueOf(vo.getPaidCount())
                    .divide(BigDecimal.valueOf(vo.getTotalCount()), 2, RoundingMode.HALF_UP));
        }
        return vo;
    }
}
