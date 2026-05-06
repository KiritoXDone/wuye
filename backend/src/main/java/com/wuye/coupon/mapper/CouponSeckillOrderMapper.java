package com.wuye.coupon.mapper;

import com.wuye.coupon.entity.CouponSeckillOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponSeckillOrderMapper {

    @Insert("""
            INSERT INTO coupon_seckill_order(order_no, campaign_id, account_id, coupon_instance_id, status, request_id, fail_reason)
            VALUES(#{orderNo}, #{campaignId}, #{accountId}, #{couponInstanceId}, #{status}, #{requestId}, #{failReason})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CouponSeckillOrder order);

    @Select("""
            SELECT id, order_no, campaign_id, account_id, coupon_instance_id, status, request_id, fail_reason, created_at
            FROM coupon_seckill_order
            WHERE campaign_id = #{campaignId}
              AND account_id = #{accountId}
              AND request_id = #{requestId}
            LIMIT 1
            """)
    CouponSeckillOrder findByRequest(@Param("campaignId") Long campaignId,
                                     @Param("accountId") Long accountId,
                                     @Param("requestId") String requestId);

    @Select("""
            SELECT id, order_no, campaign_id, account_id, coupon_instance_id, status, request_id, fail_reason, created_at
            FROM coupon_seckill_order
            WHERE order_no = #{orderNo}
            """)
    CouponSeckillOrder findByOrderNo(@Param("orderNo") String orderNo);

    @Select("""
            SELECT COUNT(1)
            FROM coupon_seckill_order
            WHERE campaign_id = #{campaignId}
              AND account_id = #{accountId}
              AND status IN ('PENDING', 'SUCCESS')
            """)
    int countActiveByAccount(@Param("campaignId") Long campaignId, @Param("accountId") Long accountId);

    @Update("""
            UPDATE coupon_seckill_order
            SET status = 'SUCCESS',
                coupon_instance_id = #{couponInstanceId},
                updated_at = CURRENT_TIMESTAMP
            WHERE order_no = #{orderNo}
              AND status = 'PENDING'
            """)
    int markSuccess(@Param("orderNo") String orderNo, @Param("couponInstanceId") Long couponInstanceId);

    @Update("""
            UPDATE coupon_seckill_order
            SET status = 'FAILED',
                fail_reason = #{failReason},
                updated_at = CURRENT_TIMESTAMP
            WHERE order_no = #{orderNo}
              AND status = 'PENDING'
            """)
    int markFailed(@Param("orderNo") String orderNo, @Param("failReason") String failReason);
}
