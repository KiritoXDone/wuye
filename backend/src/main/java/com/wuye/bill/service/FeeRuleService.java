package com.wuye.bill.service;

import com.wuye.bill.dto.FeeRuleCreateDTO;
import com.wuye.bill.entity.FeeRule;
import com.wuye.bill.mapper.FeeRuleMapper;
import com.wuye.bill.vo.FeeRuleVO;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FeeRuleService {

    private final FeeRuleMapper feeRuleMapper;
    private final AccessGuard accessGuard;

    public FeeRuleService(FeeRuleMapper feeRuleMapper, AccessGuard accessGuard) {
        this.feeRuleMapper = feeRuleMapper;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public FeeRuleVO create(LoginUser loginUser, FeeRuleCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        FeeRule feeRule = new FeeRule();
        feeRule.setCommunityId(dto.getCommunityId());
        feeRule.setFeeType(dto.getFeeType());
        feeRule.setRuleName(dto.getEffectiveFrom() + dto.getFeeType() + "规则");
        feeRule.setUnitPrice(dto.getUnitPrice());
        feeRule.setCycleType(dto.getCycleType());
        feeRule.setEffectiveFrom(dto.getEffectiveFrom());
        feeRule.setEffectiveTo(dto.getEffectiveTo());
        feeRule.setStatus(1);
        feeRule.setRemark(dto.getRemark());
        feeRuleMapper.insert(feeRule);
        return list(loginUser, dto.getCommunityId()).stream()
                .filter(item -> item.getId().equals(feeRule.getId()))
                .findFirst()
                .orElseGet(() -> {
                    FeeRuleVO vo = new FeeRuleVO();
                    vo.setId(feeRule.getId());
                    vo.setCommunityId(feeRule.getCommunityId());
                    vo.setFeeType(feeRule.getFeeType());
                    vo.setRuleName(feeRule.getRuleName());
                    vo.setUnitPrice(feeRule.getUnitPrice());
                    vo.setCycleType(feeRule.getCycleType());
                    vo.setEffectiveFrom(feeRule.getEffectiveFrom());
                    vo.setEffectiveTo(feeRule.getEffectiveTo());
                    vo.setRemark(feeRule.getRemark());
                    return vo;
                });
    }

    public List<FeeRuleVO> list(LoginUser loginUser, Long communityId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return feeRuleMapper.listByCommunity(communityId);
    }

    public FeeRule requireActiveRule(Long communityId, String feeType, LocalDate targetDate) {
        return feeRuleMapper.findActiveRule(communityId, feeType, targetDate);
    }
}
