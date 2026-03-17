package com.wuye.coupon.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.coupon.dto.CouponValidateDTO;
import com.wuye.coupon.service.CouponService;
import com.wuye.coupon.vo.AvailableCouponVO;
import com.wuye.coupon.vo.CouponValidateVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/me/coupons")
    public ApiResponse<List<AvailableCouponVO>> myCoupons(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(couponService.listMyCoupons(loginUser));
    }

    @PostMapping("/coupons/validate")
    public ApiResponse<CouponValidateVO> validate(@CurrentUser LoginUser loginUser,
                                                  @Valid @RequestBody CouponValidateDTO dto) {
        return ApiResponse.success(couponService.validate(loginUser, dto));
    }
}
