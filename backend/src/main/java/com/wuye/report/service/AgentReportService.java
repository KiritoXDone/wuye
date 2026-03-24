package com.wuye.report.service;

import com.wuye.agent.mapper.UserGroupMapper;
import com.wuye.agent.service.AgentAuthorizationService;
import com.wuye.agent.entity.UserGroup;
import com.wuye.common.security.LoginUser;
import com.wuye.report.mapper.ReportMapper;
import com.wuye.report.vo.AgentMonthlyReportVO;
import org.springframework.stereotype.Service;

@Service
public class AgentReportService {

    private final AgentAuthorizationService agentAuthorizationService;
    private final UserGroupMapper userGroupMapper;
    private final ReportMapper reportMapper;
    private final ReportMetricsNormalizer reportMetricsNormalizer;

    public AgentReportService(AgentAuthorizationService agentAuthorizationService,
                              UserGroupMapper userGroupMapper,
                              ReportMapper reportMapper,
                              ReportMetricsNormalizer reportMetricsNormalizer) {
        this.agentAuthorizationService = agentAuthorizationService;
        this.userGroupMapper = userGroupMapper;
        this.reportMapper = reportMapper;
        this.reportMetricsNormalizer = reportMetricsNormalizer;
    }

    public AgentMonthlyReportVO monthly(LoginUser loginUser, Long groupId, Integer periodYear, Integer periodMonth) {
        agentAuthorizationService.requireAuthorizedGroup(loginUser, groupId);
        UserGroup userGroup = userGroupMapper.findById(groupId);
        AgentMonthlyReportVO vo = reportMapper.agentMonthly(groupId, periodYear, periodMonth);
        vo.setGroupId(groupId);
        vo.setGroupName(userGroup == null ? "" : userGroup.getName());
        vo.setOrgUnitId(userGroup == null ? null : userGroup.getOrgUnitId());
        if (userGroup != null && userGroup.getOrgUnitId() != null) {
            var authorizedGroups = agentAuthorizationService.listMyGroups(loginUser);
            authorizedGroups.stream()
                    .filter(group -> groupId.equals(group.getGroupId()))
                    .findFirst()
                    .ifPresent(group -> {
                        vo.setOrgUnitName(group.getOrgUnitName());
                        vo.setTenantCode(group.getTenantCode());
                    });
        }
        vo.setPeriod(periodYear + "-" + String.format("%02d", periodMonth));
        return reportMetricsNormalizer.normalize(vo);
    }
}
