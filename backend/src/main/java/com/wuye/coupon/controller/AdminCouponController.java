package com.wuye.coupon.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.coupon.dto.CouponRuleCreateDTO;
import com.wuye.coupon.dto.CouponTemplateCreateDTO;
import com.wuye.coupon.entity.CouponIssueRule;
import com.wuye.coupon.entity.CouponTemplate;
import com.wuye.coupon.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/coupon-templates")
    public ApiResponse<CouponTemplate> createTemplate(@CurrentUser LoginUser loginUser,
                                                      @Valid @RequestBody CouponTemplateCreateDTO dto) {
        return ApiResponse.success(couponService.createTemplate(loginUser, dto));
    }

    @GetMapping("/coupon-templates")
    public ApiResponse<List<CouponTemplate>> listTemplates(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(couponService.listTemplates(loginUser));
    }

    @PostMapping("/coupon-rules")
    public ApiResponse<CouponIssueRule> createRule(@CurrentUser LoginUser loginUser,
                                                   @Valid @RequestBody CouponRuleCreateDTO dto) {
        return ApiResponse.success(couponService.createRule(loginUser, dto));
    }

    @GetMapping("/coupon-rules")
    public ApiResponse<List<CouponIssueRule>> listRules(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(couponService.listRules(loginUser));
    }
}
