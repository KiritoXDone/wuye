package com.wuye.agent.service;

import com.wuye.agent.dto.AgentGroupAssignDTO;
import com.wuye.agent.entity.AgentGroup;
import com.wuye.agent.entity.OrgUnit;
import com.wuye.agent.entity.AgentProfile;
import com.wuye.agent.entity.UserGroup;
import com.wuye.agent.mapper.AgentGroupMapper;
import com.wuye.agent.mapper.OrgUnitMapper;
import com.wuye.agent.mapper.AgentProfileMapper;
import com.wuye.agent.mapper.UserGroupMapper;
import com.wuye.agent.vo.AgentGroupVO;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgentAuthorizationService {

    private final AgentProfileMapper agentProfileMapper;
    private final AgentGroupMapper agentGroupMapper;
    private final OrgUnitMapper orgUnitMapper;
    private final UserGroupMapper userGroupMapper;
    private final AccessGuard accessGuard;

    public AgentAuthorizationService(AgentProfileMapper agentProfileMapper,
                                     AgentGroupMapper agentGroupMapper,
                                     OrgUnitMapper orgUnitMapper,
                                     UserGroupMapper userGroupMapper,
                                     AccessGuard accessGuard) {
        this.agentProfileMapper = agentProfileMapper;
        this.agentGroupMapper = agentGroupMapper;
        this.orgUnitMapper = orgUnitMapper;
        this.userGroupMapper = userGroupMapper;
        this.accessGuard = accessGuard;
    }

    public List<Long> loadAuthorizedGroupIds(Long accountId) {
        return agentGroupMapper.listAuthorizedGroupIdsByAccountId(accountId);
    }

    public List<AgentGroupVO> listMyGroups(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "AGENT");
        return agentGroupMapper.listAuthorizedGroupsByAccountId(loginUser.accountId());
    }

    @Transactional
    public AgentGroupVO assign(LoginUser loginUser, AgentGroupAssignDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        AgentProfile agentProfile = agentProfileMapper.findByAgentCode(dto.getAgentCode());
        if (agentProfile == null || agentProfile.getStatus() == null || agentProfile.getStatus() != 1) {
            throw new BusinessException("NOT_FOUND", "Agent 不存在或已停用", HttpStatus.NOT_FOUND);
        }
        UserGroup userGroup = userGroupMapper.findByGroupCode(dto.getGroupCode());
        if (userGroup == null || userGroup.getStatus() == null || userGroup.getStatus() != 1) {
            throw new BusinessException("NOT_FOUND", "用户组不存在或已停用", HttpStatus.NOT_FOUND);
        }
        AgentGroup agentGroup = new AgentGroup();
        agentGroup.setAgentId(agentProfile.getId());
        agentGroup.setGroupId(userGroup.getId());
        agentGroup.setPermission(dto.getPermission());
        agentGroup.setStatus(dto.getStatus());
        Long existedId = agentGroupMapper.findId(agentProfile.getId(), userGroup.getId());
        if (existedId == null) {
            agentGroupMapper.insert(agentGroup);
        } else {
            agentGroup.setId(existedId);
            agentGroupMapper.update(agentGroup);
        }
        return buildVO(userGroup, dto.getPermission());
    }

    public List<AgentGroupVO> listAll(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return agentGroupMapper.listAllAssignments();
    }

    public void requireAuthorizedGroup(LoginUser loginUser, Long groupId) {
        accessGuard.requireRole(loginUser, "AGENT");
        if (loginUser.groupIds() == null || !loginUser.groupIds().contains(groupId)) {
            throw new BusinessException("FORBIDDEN", "数据范围不足", HttpStatus.FORBIDDEN);
        }
    }

    private AgentGroupVO buildVO(UserGroup userGroup, String permission) {
        AgentGroupVO vo = new AgentGroupVO();
        vo.setGroupId(userGroup.getId());
        vo.setGroupCode(userGroup.getGroupCode());
        vo.setGroupName(userGroup.getName());
        vo.setPermission(permission);
        vo.setOrgUnitId(userGroup.getOrgUnitId());
        if (userGroup.getOrgUnitId() != null) {
            OrgUnit orgUnit = orgUnitMapper.findById(userGroup.getOrgUnitId());
            if (orgUnit != null) {
                vo.setOrgUnitName(orgUnit.getName());
                vo.setTenantCode(orgUnit.getTenantCode());
            }
        }
        return vo;
    }
}
