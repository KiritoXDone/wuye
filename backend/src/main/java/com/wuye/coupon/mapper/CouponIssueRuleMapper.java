package com.wuye.coupon.mapper;

import com.wuye.coupon.entity.CouponIssueRule;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CouponIssueRuleMapper {
    @Insert("""
            INSERT INTO coupon_issue_rule(rule_name, trigger_type, fee_type, template_id, min_pay_amount, reward_count, status)
            VALUES(#{ruleName}, #{triggerType}, #{feeType}, #{templateId}, #{minPayAmount}, #{rewardCount}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CouponIssueRule couponIssueRule);

    @Select("""
            SELECT id, rule_name, trigger_type, fee_type, template_id, min_pay_amount, reward_count, status
            FROM coupon_issue_rule
            WHERE status = 1
            ORDER BY id DESC
            """)
    List<CouponIssueRule> listActive();

    @Select("""
            SELECT id, rule_name, trigger_type, fee_type, template_id, min_pay_amount, reward_count, status
            FROM coupon_issue_rule
            WHERE status = 1
              AND trigger_type = #{triggerType}
              AND (#{payAmount} IS NULL OR min_pay_amount <= #{payAmount})
            ORDER BY id ASC
            """)
    List<CouponIssueRule> listMatchedRules(@Param("triggerType") String triggerType,
                                           @Param("payAmount") java.math.BigDecimal payAmount);

    @Select("""
            SELECT id, rule_name, trigger_type, fee_type, template_id, min_pay_amount, reward_count, status
            FROM coupon_issue_rule
            WHERE id = #{id}
            """)
    CouponIssueRule findById(@Param("id") Long id);

    @Update("""
            UPDATE coupon_issue_rule
            SET status = 0
            WHERE id = #{id}
              AND status = 1
            """)
    int deleteById(@Param("id") Long id);

    @Select("""
            SELECT COUNT(1)
            FROM coupon_issue_rule
            WHERE template_id = #{templateId}
              AND status = 1
            """)
    int countByTemplateId(@Param("templateId") Long templateId);

    @Select("""
            SELECT id, rule_name, trigger_type, fee_type, template_id, min_pay_amount, reward_count, status
            FROM coupon_issue_rule
            WHERE template_id = #{templateId}
            LIMIT 1
            """)
    CouponIssueRule findByTemplateId(@Param("templateId") Long templateId);

    @Update("""
            UPDATE coupon_issue_rule
            SET rule_name = #{ruleName},
                trigger_type = #{triggerType},
                fee_type = #{feeType},
                min_pay_amount = #{minPayAmount},
                reward_count = #{rewardCount},
                status = #{status}
            WHERE id = #{id}
            """)
    int update(CouponIssueRule couponIssueRule);
}
