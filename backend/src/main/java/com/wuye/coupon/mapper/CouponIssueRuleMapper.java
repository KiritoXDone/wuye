package com.wuye.coupon.mapper;

import com.wuye.coupon.entity.CouponIssueRule;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CouponIssueRuleMapper {
    @Insert("""
            INSERT INTO coupon_issue_rule(rule_name, fee_type, template_id, min_pay_amount, reward_count, status)
            VALUES(#{ruleName}, #{feeType}, #{templateId}, #{minPayAmount}, #{rewardCount}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CouponIssueRule couponIssueRule);

    @Select("""
            SELECT id, rule_name, fee_type, template_id, min_pay_amount, reward_count, status
            FROM coupon_issue_rule
            WHERE status = 1
            ORDER BY id DESC
            """)
    List<CouponIssueRule> listActive();

    @Select("""
            SELECT id, rule_name, fee_type, template_id, min_pay_amount, reward_count, status
            FROM coupon_issue_rule
            WHERE status = 1
              AND fee_type = #{feeType}
              AND min_pay_amount <= #{payAmount}
            ORDER BY id ASC
            """)
    List<CouponIssueRule> listMatchedRules(@Param("feeType") String feeType,
                                           @Param("payAmount") java.math.BigDecimal payAmount);
}
