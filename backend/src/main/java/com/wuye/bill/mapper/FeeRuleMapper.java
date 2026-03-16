package com.wuye.bill.mapper;

import com.wuye.bill.entity.FeeRule;
import com.wuye.bill.vo.FeeRuleVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FeeRuleMapper {

    @Insert("""
            INSERT INTO fee_rule(community_id, fee_type, rule_name, unit_price, cycle_type, effective_from, effective_to, status, remark)
            VALUES(#{communityId}, #{feeType}, #{ruleName}, #{unitPrice}, #{cycleType}, #{effectiveFrom}, #{effectiveTo}, #{status}, #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FeeRule feeRule);

    @Select("""
            SELECT id, community_id, fee_type, rule_name, unit_price, cycle_type, effective_from, effective_to, remark
            FROM fee_rule
            WHERE community_id = #{communityId}
            ORDER BY id DESC
            """)
    List<FeeRuleVO> listByCommunity(@Param("communityId") Long communityId);

    @Select("""
            SELECT id, community_id, fee_type, rule_name, unit_price, cycle_type, effective_from, effective_to, status, remark
            FROM fee_rule
            WHERE community_id = #{communityId}
              AND fee_type = #{feeType}
              AND status = 1
              AND effective_from <= #{targetDate}
              AND (effective_to IS NULL OR effective_to >= #{targetDate})
            ORDER BY effective_from DESC, id DESC
            LIMIT 1
            """)
    FeeRule findActiveRule(@Param("communityId") Long communityId,
                           @Param("feeType") String feeType,
                           @Param("targetDate") LocalDate targetDate);
}
