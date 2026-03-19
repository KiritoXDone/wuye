package com.wuye.bill.service;

import com.wuye.audit.service.AuditLogService;
import com.wuye.bill.dto.FeeRuleCreateDTO;
import com.wuye.bill.dto.FeeRuleWaterTierDTO;
import com.wuye.bill.entity.FeeRule;
import com.wuye.bill.entity.FeeRuleWaterTier;
import com.wuye.bill.mapper.FeeRuleMapper;
import com.wuye.bill.mapper.FeeRuleWaterTierMapper;
import com.wuye.bill.vo.FeeRuleVO;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FeeRuleService {

    private static final Set<String> PROPERTY_CYCLE_TYPES = Set.of("MONTH", "YEAR");
    private static final Set<String> WATER_CYCLE_TYPES = Set.of("MONTH");

    private final FeeRuleMapper feeRuleMapper;
    private final FeeRuleWaterTierMapper feeRuleWaterTierMapper;
    private final AccessGuard accessGuard;
    private final AuditLogService auditLogService;

    public FeeRuleService(FeeRuleMapper feeRuleMapper,
                          FeeRuleWaterTierMapper feeRuleWaterTierMapper,
                          AccessGuard accessGuard,
                          AuditLogService auditLogService) {
        this.feeRuleMapper = feeRuleMapper;
        this.feeRuleWaterTierMapper = feeRuleWaterTierMapper;
        this.accessGuard = accessGuard;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public FeeRuleVO create(LoginUser loginUser, FeeRuleCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        String feeType = normalizeFeeType(dto.getFeeType());
        String cycleType = normalizeCycleType(dto.getCycleType());
        FeeRule feeRule = new FeeRule();
        feeRule.setCommunityId(dto.getCommunityId());
        feeRule.setFeeType(feeType);
        feeRule.setRuleName(dto.getEffectiveFrom() + feeType + "规则");
        feeRule.setUnitPrice(dto.getUnitPrice());
        feeRule.setCycleType(cycleType);
        feeRule.setPricingMode(resolvePricingMode(dto));
        feeRule.setEffectiveFrom(dto.getEffectiveFrom());
        feeRule.setEffectiveTo(dto.getEffectiveTo());
        feeRule.setStatus(1);
        feeRule.setRemark(dto.getRemark());
        feeRule.setAbnormalAbsThreshold(dto.getAbnormalAbsThreshold());
        feeRule.setAbnormalMultiplierThreshold(dto.getAbnormalMultiplierThreshold());
        validateRuleSemantics(feeRule, dto);
        feeRuleMapper.insert(feeRule);
        insertWaterTiers(feeRule.getId(), dto.getWaterTiers());
        auditLogService.record(loginUser, "BILL", String.valueOf(feeRule.getId()), "CREATE", buildAuditDetail(feeRule, dto));
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
                    vo.setPricingMode(feeRule.getPricingMode());
                    vo.setEffectiveFrom(feeRule.getEffectiveFrom());
                    vo.setEffectiveTo(feeRule.getEffectiveTo());
                    vo.setRemark(feeRule.getRemark());
                    vo.setAbnormalAbsThreshold(feeRule.getAbnormalAbsThreshold());
                    vo.setAbnormalMultiplierThreshold(feeRule.getAbnormalMultiplierThreshold());
                    vo.setWaterTiers(List.of());
                    return vo;
                });
    }

    public List<FeeRuleVO> list(LoginUser loginUser, Long communityId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        List<FeeRuleVO> rules = feeRuleMapper.listByCommunity(communityId);
        rules.forEach(rule -> rule.setWaterTiers(feeRuleWaterTierMapper.listVOByFeeRuleId(rule.getId())));
        return rules;
    }

    public FeeRule requireActiveRule(Long communityId, String feeType, LocalDate targetDate) {
        FeeRule feeRule = feeRuleMapper.findActiveRule(communityId, feeType, targetDate);
        if (feeRule != null) {
            feeRule.setWaterTiers(feeRuleWaterTierMapper.listByFeeRuleId(feeRule.getId()));
        }
        return feeRule;
    }

    private String resolvePricingMode(FeeRuleCreateDTO dto) {
        if (dto.getPricingMode() != null && !dto.getPricingMode().isBlank()) {
            return dto.getPricingMode().trim().toUpperCase();
        }
        return dto.getWaterTiers() == null || dto.getWaterTiers().isEmpty() ? "FLAT" : "TIERED";
    }

    private void validateRuleSemantics(FeeRule feeRule, FeeRuleCreateDTO dto) {
        if ("PROPERTY".equals(feeRule.getFeeType())) {
            if (!PROPERTY_CYCLE_TYPES.contains(feeRule.getCycleType())) {
                throw new BusinessException("INVALID_ARGUMENT", "物业费周期仅支持月或年", HttpStatus.BAD_REQUEST);
            }
            if (!"FLAT".equals(feeRule.getPricingMode())) {
                throw new BusinessException("INVALID_ARGUMENT", "物业费仅支持固定单价", HttpStatus.BAD_REQUEST);
            }
            if (dto.getWaterTiers() != null && !dto.getWaterTiers().isEmpty()) {
                throw new BusinessException("INVALID_ARGUMENT", "物业费不支持阶梯配置", HttpStatus.BAD_REQUEST);
            }
            if (dto.getAbnormalAbsThreshold() != null || dto.getAbnormalMultiplierThreshold() != null) {
                throw new BusinessException("INVALID_ARGUMENT", "物业费不支持异常阈值", HttpStatus.BAD_REQUEST);
            }
            return;
        }

        if ("WATER".equals(feeRule.getFeeType())) {
            if (!WATER_CYCLE_TYPES.contains(feeRule.getCycleType())) {
                throw new BusinessException("INVALID_ARGUMENT", "水费周期仅支持月", HttpStatus.BAD_REQUEST);
            }
            return;
        }

        throw new BusinessException("INVALID_ARGUMENT", "暂不支持的费用类型", HttpStatus.BAD_REQUEST);
    }

    private String normalizeFeeType(String feeType) {
        return feeType == null ? null : feeType.trim().toUpperCase();
    }

    private String normalizeCycleType(String cycleType) {
        return cycleType == null ? null : cycleType.trim().toUpperCase();
    }

    private void insertWaterTiers(Long feeRuleId, List<FeeRuleWaterTierDTO> waterTiers) {
        if (waterTiers == null || waterTiers.isEmpty()) {
            return;
        }
        for (int i = 0; i < waterTiers.size(); i++) {
            FeeRuleWaterTierDTO tierDTO = waterTiers.get(i);
            if (tierDTO.getStartUsage() == null || tierDTO.getUnitPrice() == null) {
                throw new BusinessException("INVALID_ARGUMENT", "分段水价配置不完整", HttpStatus.BAD_REQUEST);
            }
            FeeRuleWaterTier tier = new FeeRuleWaterTier();
            tier.setFeeRuleId(feeRuleId);
            tier.setTierOrder(i + 1);
            tier.setStartUsage(tierDTO.getStartUsage());
            tier.setEndUsage(tierDTO.getEndUsage());
            tier.setUnitPrice(tierDTO.getUnitPrice());
            feeRuleWaterTierMapper.insert(tier);
        }
    }

    private Map<String, Object> buildAuditDetail(FeeRule feeRule, FeeRuleCreateDTO dto) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("feeRuleId", feeRule.getId());
        detail.put("communityId", feeRule.getCommunityId());
        detail.put("feeType", feeRule.getFeeType());
        detail.put("ruleName", feeRule.getRuleName());
        detail.put("unitPrice", feeRule.getUnitPrice());
        detail.put("cycleType", feeRule.getCycleType());
        detail.put("pricingMode", feeRule.getPricingMode());
        detail.put("effectiveFrom", feeRule.getEffectiveFrom());
        detail.put("effectiveTo", feeRule.getEffectiveTo());
        detail.put("remark", feeRule.getRemark());
        detail.put("abnormalAbsThreshold", feeRule.getAbnormalAbsThreshold());
        detail.put("abnormalMultiplierThreshold", feeRule.getAbnormalMultiplierThreshold());
        detail.put("waterTiers", dto.getWaterTiers());
        return detail;
    }
}
