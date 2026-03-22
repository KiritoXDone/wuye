package com.wuye.coupon.mapper;

import com.wuye.coupon.dto.AdminCouponInstanceQuery;
import com.wuye.coupon.entity.CouponInstance;
import com.wuye.coupon.vo.AdminCouponInstanceVO;
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

    @Select("""
            SELECT COUNT(1)
            FROM coupon_instance
            WHERE template_id = #{templateId}
              AND status IN ('NEW', 'LOCKED')
            """)
    int countByTemplateId(@Param("templateId") Long templateId);

    @Select("""
            <script>
            SELECT ci.id AS coupon_instance_id,
                   ct.id AS template_id,
                   ct.template_code,
                   ct.name AS template_name,
                   ct.type AS template_type,
                   ci.owner_account_id,
                   COALESCE(a.real_name, a.username, a.account_no) AS owner_account_name,
                   ci.source_type,
                   ci.source_ref_no,
                   ci.status,
                   ci.issued_at,
                   ci.expires_at
            FROM coupon_instance ci
            JOIN coupon_template ct ON ct.id = ci.template_id
            LEFT JOIN account a ON a.id = ci.owner_account_id
            <where>
                <if test='query.templateId != null'>
                    AND ct.id = #{query.templateId}
                </if>
                <if test='query.templateKeyword != null and query.templateKeyword != ""'>
                    AND (ct.template_code LIKE CONCAT('%', #{query.templateKeyword}, '%')
                      OR ct.name LIKE CONCAT('%', #{query.templateKeyword}, '%'))
                </if>
                <if test='query.status != null and query.status != ""'>
                    AND ci.status = #{query.status}
                </if>
                <if test='query.sourceType != null and query.sourceType != ""'>
                    AND ci.source_type = #{query.sourceType}
                </if>
                <if test='query.ownerAccountId != null'>
                    AND ci.owner_account_id = #{query.ownerAccountId}
                </if>
            </where>
            ORDER BY ci.issued_at DESC, ci.id DESC
            </script>
            """)
    List<AdminCouponInstanceVO> listAdminInstances(@Param("query") AdminCouponInstanceQuery query);
}
