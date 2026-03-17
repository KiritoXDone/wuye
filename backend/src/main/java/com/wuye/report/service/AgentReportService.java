package com.wuye.report.service;

import com.wuye.agent.mapper.UserGroupMapper;
import com.wuye.agent.service.AgentAuthorizationService;
import com.wuye.agent.entity.UserGroup;
import com.wuye.common.security.LoginUser;
import com.wuye.report.mapper.ReportMapper;
import com.wuye.report.vo.AgentMonthlyReportVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AgentReportService {

    private final AgentAuthorizationService agentAuthorizationService;
    private final UserGroupMapper userGroupMapper;
    private final ReportMapper reportMapper;

    public AgentReportService(AgentAuthorizationService agentAuthorizationService,
                              UserGroupMapper userGroupMapper,
                              ReportMapper reportMapper) {
        this.agentAuthorizationService = agentAuthorizationService;
        this.userGroupMapper = userGroupMapper;
        this.reportMapper = reportMapper;
    }

    public AgentMonthlyReportVO monthly(LoginUser loginUser, Long groupId, Integer periodYear, Integer periodMonth) {
        agentAuthorizationService.requireAuthorizedGroup(loginUser, groupId);
        UserGroup userGroup = userGroupMapper.findById(groupId);
        AgentMonthlyReportVO vo = reportMapper.agentMonthly(groupId, periodYear, periodMonth);
        vo.setGroupId(groupId);
        vo.setGroupName(userGroup == null ? "" : userGroup.getName());
        vo.setPeriod(periodYear + "-" + String.format("%02d", periodMonth));
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
