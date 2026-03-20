package com.wuye.auth.controller;

import com.wuye.auth.dto.AccountStatusUpdateDTO;
import com.wuye.auth.dto.AdminAccountCreateDTO;
import com.wuye.auth.dto.AdminPasswordResetDTO;
import com.wuye.auth.service.AdminAccountService;
import com.wuye.auth.vo.AdminAccountVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @GetMapping
    public ApiResponse<List<AdminAccountVO>> list(@CurrentUser LoginUser loginUser,
                                                  @RequestParam(required = false) String accountType) {
        if (accountType != null && !"ADMIN".equals(accountType)) {
            return ApiResponse.success(List.of());
        }
        return ApiResponse.success(adminAccountService.listAdmins(loginUser));
    }

    @PostMapping("/admins")
    public ApiResponse<AdminAccountVO> createAdmin(@CurrentUser LoginUser loginUser,
                                                   @Valid @RequestBody AdminAccountCreateDTO dto) {
        return ApiResponse.success(adminAccountService.createAdmin(loginUser, dto));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@CurrentUser LoginUser loginUser,
                                          @PathVariable Long id,
                                          @Valid @RequestBody AccountStatusUpdateDTO dto) {
        adminAccountService.updateStatus(loginUser, id, dto);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@CurrentUser LoginUser loginUser,
                                           @PathVariable Long id,
                                           @Valid @RequestBody AdminPasswordResetDTO dto) {
        adminAccountService.resetPassword(loginUser, id, dto);
        return ApiResponse.success();
    }
}
