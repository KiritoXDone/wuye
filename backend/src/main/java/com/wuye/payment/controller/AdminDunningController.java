package com.wuye.payment.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.payment.dto.DunningTriggerDTO;
import com.wuye.payment.service.DunningService;
import com.wuye.payment.vo.DunningLogVO;
import com.wuye.payment.vo.DunningTaskVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dunning")
public class AdminDunningController {

    private final DunningService dunningService;

    public AdminDunningController(DunningService dunningService) {
        this.dunningService = dunningService;
    }

    @PostMapping("/trigger")
    public ApiResponse<List<DunningTaskVO>> trigger(@CurrentUser LoginUser loginUser,
                                                    @RequestBody(required = false) DunningTriggerDTO dto) {
        return ApiResponse.success(dunningService.trigger(loginUser, dto == null ? new DunningTriggerDTO() : dto));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<DunningTaskVO>> tasks(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(dunningService.listTasks(loginUser));
    }

    @GetMapping("/bills/{billId}/logs")
    public ApiResponse<List<DunningLogVO>> logs(@CurrentUser LoginUser loginUser, @PathVariable Long billId) {
        return ApiResponse.success(dunningService.listLogs(loginUser, billId));
    }
}
