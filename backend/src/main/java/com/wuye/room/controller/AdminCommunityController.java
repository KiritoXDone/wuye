package com.wuye.room.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.room.dto.CommunityUpsertDTO;
import com.wuye.room.service.AdminCommunityService;
import com.wuye.room.vo.AdminCommunityVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/communities")
public class AdminCommunityController {

    private final AdminCommunityService adminCommunityService;

    public AdminCommunityController(AdminCommunityService adminCommunityService) {
        this.adminCommunityService = adminCommunityService;
    }

    @GetMapping
    public ApiResponse<List<AdminCommunityVO>> list(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(adminCommunityService.list(loginUser));
    }

    @PostMapping
    public ApiResponse<AdminCommunityVO> create(@CurrentUser LoginUser loginUser,
                                                @Valid @RequestBody CommunityUpsertDTO dto) {
        return ApiResponse.success(adminCommunityService.create(loginUser, dto));
    }

    @PutMapping("/{communityId}")
    public ApiResponse<AdminCommunityVO> update(@CurrentUser LoginUser loginUser,
                                                @PathVariable Long communityId,
                                                @Valid @RequestBody CommunityUpsertDTO dto) {
        return ApiResponse.success(adminCommunityService.update(loginUser, communityId, dto));
    }

    @DeleteMapping("/{communityId}")
    public ApiResponse<AdminCommunityVO> disable(@CurrentUser LoginUser loginUser,
                                                 @PathVariable Long communityId) {
        return ApiResponse.success(adminCommunityService.disable(loginUser, communityId));
    }
}
