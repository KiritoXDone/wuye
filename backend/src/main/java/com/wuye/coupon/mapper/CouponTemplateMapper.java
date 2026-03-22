package com.wuye.coupon.mapper;

import com.wuye.coupon.entity.CouponTemplate;
import com.wuye.coupon.vo.AdminCouponSummaryVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CouponTemplateMapper {
    @Insert("""
            INSERT INTO coupon_template(template_code, type, fee_type, name, goods_name, goods_spec, fulfillment_type, redeem_instruction, discount_mode, value_amount, threshold_amount,
                                        valid_from, valid_to, stackable, status)
            VALUES(#{templateCode}, #{type}, #{feeType}, #{name}, #{goodsName}, #{goodsSpec}, #{fulfillmentType}, #{redeemInstruction}, #{discountMode}, #{valueAmount}, #{thresholdAmount},
                   #{validFrom}, #{validTo}, #{stackable}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CouponTemplate couponTemplate);

    @Select("""
            SELECT id, template_code, type, fee_type, name, goods_name, goods_spec, fulfillment_type, redeem_instruction, discount_mode, value_amount, threshold_amount,
                   valid_from, valid_to, stackable, status
            FROM coupon_template
            WHERE status = 1
            ORDER BY id DESC
            """)
    List<CouponTemplate> listActive();

    @Select("""
            SELECT ct.id,
                   ct.template_code,
                   ct.type,
                   ct.fee_type,
                   ct.name,
                   ct.discount_mode,
                   ct.goods_name,
                   ct.goods_spec,
                   ct.fulfillment_type,
                   ct.redeem_instruction,
                   ct.value_amount,
                   ct.threshold_amount,
                   ct.valid_from,
                   ct.valid_to,
                   ct.status,
                   cir.id AS rule_id,
                   cir.rule_name,
                   cir.trigger_type,
                   cir.min_pay_amount,
                   cir.reward_count,
                   cir.status AS rule_status,
                   COALESCE(ci_stats.issued_count, 0) AS issued_count
            FROM coupon_template ct
            LEFT JOIN coupon_issue_rule cir ON cir.template_id = ct.id AND cir.status = 1
            LEFT JOIN (
                SELECT template_id, COUNT(1) AS issued_count
                FROM coupon_instance
                GROUP BY template_id
            ) ci_stats ON ci_stats.template_id = ct.id
            ORDER BY ct.id DESC
            """)
    List<AdminCouponSummaryVO> listAdminSummaries();

    @Select("""
            SELECT id, template_code, type, fee_type, name, goods_name, goods_spec, fulfillment_type, redeem_instruction, discount_mode, value_amount, threshold_amount,
                   valid_from, valid_to, stackable, status
            FROM coupon_template
            WHERE template_code = #{templateCode}
            """)
    CouponTemplate findByTemplateCode(@Param("templateCode") String templateCode);

    @Select("""
            SELECT id, template_code, type, fee_type, name, goods_name, goods_spec, fulfillment_type, redeem_instruction, discount_mode, value_amount, threshold_amount,
                   valid_from, valid_to, stackable, status
            FROM coupon_template
            WHERE id = #{id}
            """)
    CouponTemplate findById(@Param("id") Long id);

    @Update("""
            UPDATE coupon_template
            SET status = 0
            WHERE id = #{id}
              AND status = 1
            """)
    int deleteById(@Param("id") Long id);

    @Update("""
            UPDATE coupon_template
            SET fee_type = #{feeType},
                name = #{name},
                goods_name = #{goodsName},
                goods_spec = #{goodsSpec},
                fulfillment_type = #{fulfillmentType},
                redeem_instruction = #{redeemInstruction},
                discount_mode = #{discountMode},
                value_amount = #{valueAmount},
                threshold_amount = #{thresholdAmount},
                valid_from = #{validFrom},
                valid_to = #{validTo},
                status = #{status},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int update(CouponTemplate couponTemplate);
}
