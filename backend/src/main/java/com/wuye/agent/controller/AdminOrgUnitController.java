package com.wuye.agent.controller;

import com.wuye.agent.service.OrgUnitService;
import com.wuye.agent.vo.OrgUnitVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/org-units")
public class AdminOrgUnitController {

    private final OrgUnitService orgUnitService;

    public AdminOrgUnitController(OrgUnitService orgUnitService) {
        this.orgUnitService = orgUnitService;
    }

    @GetMapping
    public ApiResponse<List<OrgUnitVO>> list(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(orgUnitService.listAll(loginUser));
    }
}
