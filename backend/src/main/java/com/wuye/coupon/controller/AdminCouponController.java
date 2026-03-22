package com.wuye.coupon.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.coupon.dto.AdminCouponInstanceQuery;
import com.wuye.coupon.dto.AdminCouponManualIssueDTO;
import com.wuye.coupon.dto.AdminCouponUpsertDTO;
import com.wuye.coupon.dto.AdminVoucherExchangeStatusDTO;
import com.wuye.coupon.dto.CouponRuleCreateDTO;
import com.wuye.coupon.dto.CouponTemplateCreateDTO;
import com.wuye.coupon.entity.CouponIssueRule;
import com.wuye.coupon.entity.CouponTemplate;
import com.wuye.coupon.vo.AdminCouponInstanceVO;
import com.wuye.coupon.vo.AdminCouponManualIssueResultVO;
import com.wuye.coupon.vo.AdminCouponSummaryVO;
import com.wuye.coupon.vo.AdminVoucherExchangeVO;
import com.wuye.coupon.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/coupons")
    public ApiResponse<List<AdminCouponSummaryVO>> listCoupons(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(couponService.listAdminCoupons(loginUser));
    }

    @PostMapping("/coupons")
    public ApiResponse<AdminCouponSummaryVO> saveCoupon(@CurrentUser LoginUser loginUser,
                                                        @Valid @RequestBody AdminCouponUpsertDTO dto) {
        return ApiResponse.success(couponService.saveCoupon(loginUser, dto));
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

    @GetMapping("/coupon-instances")
    public ApiResponse<List<AdminCouponInstanceVO>> listInstances(@CurrentUser LoginUser loginUser,
                                                                  AdminCouponInstanceQuery query) {
        return ApiResponse.success(couponService.listAdminInstances(loginUser, query));
    }

    @GetMapping("/coupons/{templateId}/instances")
    public ApiResponse<List<AdminCouponInstanceVO>> listCouponInstances(@CurrentUser LoginUser loginUser,
                                                                        @PathVariable Long templateId) {
        AdminCouponInstanceQuery query = new AdminCouponInstanceQuery();
        query.setTemplateId(templateId);
        return ApiResponse.success(couponService.listAdminInstances(loginUser, query));
    }

    @GetMapping("/coupons/{templateId}/exchanges")
    public ApiResponse<List<AdminVoucherExchangeVO>> listCouponExchanges(@CurrentUser LoginUser loginUser,
                                                                         @PathVariable Long templateId) {
        return ApiResponse.success(couponService.listVoucherExchanges(loginUser, templateId));
    }

    @PostMapping("/voucher-exchanges/{exchangeId}/status")
    public ApiResponse<Void> updateVoucherExchangeStatus(@CurrentUser LoginUser loginUser,
                                                         @PathVariable Long exchangeId,
                                                         @Valid @RequestBody AdminVoucherExchangeStatusDTO dto) {
        couponService.updateVoucherExchangeStatus(loginUser, exchangeId, dto);
        return ApiResponse.success();
    }

    @PostMapping("/coupon-instances/manual-issue")
    public ApiResponse<AdminCouponManualIssueResultVO> manualIssue(@CurrentUser LoginUser loginUser,
                                                                   @Valid @RequestBody AdminCouponManualIssueDTO dto) {
        return ApiResponse.success(couponService.manualIssue(loginUser, dto));
    }

    @DeleteMapping("/coupon-templates/{templateId}")
    public ApiResponse<Void> deleteTemplate(@CurrentUser LoginUser loginUser, @PathVariable Long templateId) {
        couponService.deleteTemplate(loginUser, templateId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/coupon-rules/{ruleId}")
    public ApiResponse<Void> deleteRule(@CurrentUser LoginUser loginUser, @PathVariable Long ruleId) {
        couponService.deleteRule(loginUser, ruleId);
        return ApiResponse.success(null);
    }
}
