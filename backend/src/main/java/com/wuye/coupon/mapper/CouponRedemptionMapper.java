package com.wuye.coupon.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface CouponRedemptionMapper {
    @Insert("""
            INSERT INTO coupon_redemption(coupon_instance_id, redeem_type, pay_order_no, redeem_target, redeemed_at, operator_id)
            VALUES(#{couponInstanceId}, #{redeemType}, #{payOrderNo}, #{redeemTarget}, #{redeemedAt}, #{operatorId})
            """)
    int insert(@Param("couponInstanceId") Long couponInstanceId,
               @Param("redeemType") String redeemType,
               @Param("payOrderNo") String payOrderNo,
               @Param("redeemTarget") String redeemTarget,
               @Param("redeemedAt") LocalDateTime redeemedAt,
               @Param("operatorId") Long operatorId);
}
