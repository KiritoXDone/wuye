package com.wuye.coupon.mapper;

import com.wuye.coupon.entity.CouponInstance;
import com.wuye.coupon.vo.AvailableCouponVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CouponInstanceMapper {
    @Insert("""
            INSERT INTO coupon_instance(template_id, owner_account_id, owner_group_id, source_type, source_ref_no, status, issued_at, expires_at)
            VALUES(#{templateId}, #{ownerAccountId}, #{ownerGroupId}, #{sourceType}, #{sourceRefNo}, #{status}, #{issuedAt}, #{expiresAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CouponInstance couponInstance);

    @Select("""
            SELECT ci.id AS coupon_instance_id,
                   ct.template_code,
                   ct.name,
                   ct.value_amount AS discount_amount,
                   ci.expires_at
            FROM coupon_instance ci
            JOIN coupon_template ct ON ct.id = ci.template_id
            WHERE ci.owner_account_id = #{accountId}
              AND ci.status = 'NEW'
              AND ct.type = 'PAYMENT'
              AND (#{feeType} IS NULL OR #{feeType} = '' OR ct.fee_type IS NULL OR ct.fee_type = #{feeType})
              AND ci.expires_at >= CURRENT_TIMESTAMP
              AND (#{amountDue} IS NULL OR ct.threshold_amount <= #{amountDue})
            ORDER BY ci.expires_at ASC, ci.id ASC
            """)
    List<AvailableCouponVO> listAvailableByAccountAndBill(@Param("accountId") Long accountId,
                                                          @Param("feeType") String feeType,
                                                          @Param("amountDue") java.math.BigDecimal amountDue);

    @Select("""
            SELECT id, template_id, owner_account_id, owner_group_id, source_type, source_ref_no, status, issued_at, expires_at
            FROM coupon_instance
            WHERE id = #{id}
            """)
    CouponInstance findById(@Param("id") Long id);

    @Select("""
            SELECT id, template_id, owner_account_id, owner_group_id, source_type, source_ref_no, status, issued_at, expires_at
            FROM coupon_instance
            WHERE source_type = #{sourceType}
              AND source_ref_no = #{sourceRefNo}
              AND owner_account_id = #{ownerAccountId}
              AND template_id = #{templateId}
            LIMIT 1
            """)
    CouponInstance findRewardInstance(@Param("sourceType") String sourceType,
                                      @Param("sourceRefNo") String sourceRefNo,
                                      @Param("ownerAccountId") Long ownerAccountId,
                                      @Param("templateId") Long templateId);

    @Update("""
            UPDATE coupon_instance
            SET status = #{status},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("""
            SELECT COUNT(1)
            FROM coupon_instance
            WHERE source_type = 'PAYMENT_REWARD'
              AND source_ref_no LIKE CONCAT(#{payOrderNo}, '-%')
            """)
    int countRewardIssuedByPayOrderNo(@Param("payOrderNo") String payOrderNo);
}
