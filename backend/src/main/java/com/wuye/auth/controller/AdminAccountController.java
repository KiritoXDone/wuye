package com.wuye.auth.controller;

import com.wuye.auth.dto.AccountStatusUpdateDTO;
import com.wuye.auth.dto.AdminAccountCreateDTO;
import com.wuye.auth.dto.AdminPasswordResetDTO;
import com.wuye.auth.service.AdminAccountService;
import com.wuye.auth.vo.AdminAccountVO;
import com.wuye.room.vo.RoomVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
        return ApiResponse.success(adminAccountService.listAccounts(loginUser, accountType));
    }

    @PostMapping("/admins")
    public ApiResponse<AdminAccountVO> createAdmin(@CurrentUser LoginUser loginUser,
                                                   @Valid @RequestBody AdminAccountCreateDTO dto) {
        return ApiResponse.success(adminAccountService.createAdmin(loginUser, dto));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@CurrentUser LoginUser loginUser,
                                           @PathVariable Long id,
                                           @Valid @RequestBody AdminPasswordResetDTO dto) {
        adminAccountService.resetPassword(loginUser, id, dto);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@CurrentUser LoginUser loginUser,
                                          @PathVariable Long id,
                                          @Valid @RequestBody AccountStatusUpdateDTO dto) {
        adminAccountService.updateStatus(loginUser, id, dto);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser LoginUser loginUser,
                                    @PathVariable Long id) {
        adminAccountService.deleteAccount(loginUser, id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/rooms")
    public ApiResponse<List<RoomVO>> rooms(@CurrentUser LoginUser loginUser,
                                           @PathVariable Long id) {
        return ApiResponse.success(adminAccountService.listAccountRooms(loginUser, id));
    }

    @PostMapping("/{id}/rooms/{roomId}/unbind")
    public ApiResponse<Void> unbindRoom(@CurrentUser LoginUser loginUser,
                                        @PathVariable Long id,
                                        @PathVariable Long roomId) {
        adminAccountService.unbindAccountRoom(loginUser, id, roomId);
        return ApiResponse.success();
    }
}
