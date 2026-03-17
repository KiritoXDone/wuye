package com.wuye.bill.mapper;

import com.wuye.bill.entity.FeeRuleWaterTier;
import com.wuye.bill.vo.FeeRuleWaterTierVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FeeRuleWaterTierMapper {

    @Insert("""
            INSERT INTO fee_rule_water_tier(fee_rule_id, tier_order, start_usage, end_usage, unit_price)
            VALUES(#{feeRuleId}, #{tierOrder}, #{startUsage}, #{endUsage}, #{unitPrice})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FeeRuleWaterTier tier);

    @Select("""
            SELECT id, fee_rule_id, tier_order, start_usage, end_usage, unit_price
            FROM fee_rule_water_tier
            WHERE fee_rule_id = #{feeRuleId}
            ORDER BY tier_order ASC
            """)
    List<FeeRuleWaterTier> listByFeeRuleId(@Param("feeRuleId") Long feeRuleId);

    @Select("""
            SELECT tier_order, start_usage, end_usage, unit_price
            FROM fee_rule_water_tier
            WHERE fee_rule_id = #{feeRuleId}
            ORDER BY tier_order ASC
            """)
    List<FeeRuleWaterTierVO> listVOByFeeRuleId(@Param("feeRuleId") Long feeRuleId);
}
