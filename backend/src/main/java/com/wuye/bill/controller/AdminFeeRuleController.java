package com.wuye.bill.controller;

import com.wuye.bill.dto.FeeRuleCreateDTO;
import com.wuye.bill.service.FeeRuleService;
import com.wuye.bill.vo.FeeRuleVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/fee-rules")
public class AdminFeeRuleController {

    private final FeeRuleService feeRuleService;

    public AdminFeeRuleController(FeeRuleService feeRuleService) {
        this.feeRuleService = feeRuleService;
    }

    @PostMapping
    public ApiResponse<FeeRuleVO> create(@CurrentUser LoginUser loginUser, @Valid @RequestBody FeeRuleCreateDTO dto) {
        return ApiResponse.success(feeRuleService.create(loginUser, dto));
    }

    @GetMapping
    public ApiResponse<List<FeeRuleVO>> list(@CurrentUser LoginUser loginUser, @RequestParam Long communityId) {
        return ApiResponse.success(feeRuleService.list(loginUser, communityId));
    }

    @DeleteMapping("/{ruleId}")
    public ApiResponse<Void> delete(@CurrentUser LoginUser loginUser, @PathVariable Long ruleId) {
        feeRuleService.delete(loginUser, ruleId);
        return ApiResponse.success(null);
    }
}
