package com.wuye.agent.service;

import com.wuye.agent.mapper.OrgUnitMapper;
import com.wuye.agent.vo.OrgUnitVO;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrgUnitService {

    private final OrgUnitMapper orgUnitMapper;
    private final AccessGuard accessGuard;

    public OrgUnitService(OrgUnitMapper orgUnitMapper, AccessGuard accessGuard) {
        this.orgUnitMapper = orgUnitMapper;
        this.accessGuard = accessGuard;
    }

    public List<OrgUnitVO> listAll(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return orgUnitMapper.listAll();
    }
}
