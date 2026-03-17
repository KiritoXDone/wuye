package com.wuye.coupon.service;

import com.wuye.audit.service.AuditLogService;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.common.util.MoneyUtils;
import com.wuye.coupon.dto.CouponRuleCreateDTO;
import com.wuye.coupon.dto.CouponTemplateCreateDTO;
import com.wuye.coupon.dto.CouponValidateDTO;
import com.wuye.coupon.entity.CouponInstance;
import com.wuye.coupon.entity.CouponIssueRule;
import com.wuye.coupon.entity.CouponTemplate;
import com.wuye.coupon.mapper.CouponInstanceMapper;
import com.wuye.coupon.mapper.CouponIssueRuleMapper;
import com.wuye.coupon.mapper.CouponRedemptionMapper;
import com.wuye.coupon.mapper.CouponTemplateMapper;
import com.wuye.coupon.vo.AvailableCouponVO;
import com.wuye.coupon.vo.CouponValidateVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CouponService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponIssueRuleMapper couponIssueRuleMapper;
    private final CouponInstanceMapper couponInstanceMapper;
    private final CouponRedemptionMapper couponRedemptionMapper;
    private final BillMapper billMapper;
    private final AccessGuard accessGuard;
    private final AuditLogService auditLogService;

    public CouponService(CouponTemplateMapper couponTemplateMapper,
                         CouponIssueRuleMapper couponIssueRuleMapper,
                         CouponInstanceMapper couponInstanceMapper,
                         CouponRedemptionMapper couponRedemptionMapper,
                         BillMapper billMapper,
                         AccessGuard accessGuard,
                         AuditLogService auditLogService) {
        this.couponTemplateMapper = couponTemplateMapper;
        this.couponIssueRuleMapper = couponIssueRuleMapper;
        this.couponInstanceMapper = couponInstanceMapper;
        this.couponRedemptionMapper = couponRedemptionMapper;
        this.billMapper = billMapper;
        this.accessGuard = accessGuard;
        this.auditLogService = auditLogService;
    }

    public List<AvailableCouponVO> listMyCoupons(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        return couponInstanceMapper.listAvailableByAccountAndBill(loginUser.accountId(), null, null);
    }

    public List<AvailableCouponVO> listBillCoupons(LoginUser loginUser, Bill bill) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        return couponInstanceMapper.listAvailableByAccountAndBill(loginUser.accountId(), bill.getFeeType(), bill.getAmountDue());
    }

    public CouponValidateVO validate(LoginUser loginUser, CouponValidateDTO dto) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        Bill bill = requireBill(dto.getBillId());
        CouponInstance couponInstance = requireCoupon(dto.getCouponInstanceId());
        BigDecimal discount = validateInternal(loginUser.accountId(), bill, couponInstance);
        CouponValidateVO vo = new CouponValidateVO();
        vo.setCouponInstanceId(couponInstance.getId());
        vo.setValid(true);
        vo.setDiscountAmount(discount);
        vo.setMessage("OK");
        return vo;
    }

    @Transactional
    public CouponTemplate createTemplate(LoginUser loginUser, CouponTemplateCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        CouponTemplate entity = new CouponTemplate();
        entity.setTemplateCode(dto.getTemplateCode());
        entity.setType(dto.getType());
        entity.setFeeType(dto.getFeeType());
        entity.setName(dto.getName());
        entity.setDiscountMode(dto.getDiscountMode());
        entity.setValueAmount(MoneyUtils.scaleMoney(dto.getValueAmount()));
        entity.setThresholdAmount(MoneyUtils.scaleMoney(dto.getThresholdAmount()));
        entity.setValidFrom(dto.getValidFrom());
        entity.setValidTo(dto.getValidTo());
        entity.setStackable(0);
        entity.setStatus(1);
        couponTemplateMapper.insert(entity);
        auditLogService.record(loginUser, "COUPON", entity.getTemplateCode(), "CREATE", buildTemplateAuditDetail(entity));
        return entity;
    }

    public List<CouponTemplate> listTemplates(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return couponTemplateMapper.listActive();
    }

    @Transactional
    public CouponIssueRule createRule(LoginUser loginUser, CouponRuleCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        CouponTemplate template = couponTemplateMapper.findByTemplateCode(dto.getTemplateCode());
        if (template == null) {
            throw new BusinessException("NOT_FOUND", "券模板不存在", HttpStatus.NOT_FOUND);
        }
        CouponIssueRule rule = new CouponIssueRule();
        rule.setRuleName(dto.getName());
        rule.setFeeType(dto.getFeeType());
        rule.setTemplateId(template.getId());
        rule.setMinPayAmount(MoneyUtils.scaleMoney(dto.getMinPayAmount()));
        rule.setRewardCount(dto.getRewardCount());
        rule.setStatus(1);
        couponIssueRuleMapper.insert(rule);
        return rule;
    }

    public List<CouponIssueRule> listRules(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return couponIssueRuleMapper.listActive();
    }

    public BigDecimal validateInternal(Long accountId, Bill bill, CouponInstance couponInstance) {
        if (couponInstance.getOwnerAccountId() == null || !couponInstance.getOwnerAccountId().equals(accountId)) {
            throw new BusinessException("FORBIDDEN", "券不属于当前账号", HttpStatus.FORBIDDEN);
        }
        if (!"NEW".equals(couponInstance.getStatus())) {
            throw new BusinessException("CONFLICT", "券当前不可用", HttpStatus.CONFLICT);
        }
        if (couponInstance.getExpiresAt() == null || couponInstance.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("CONFLICT", "券已过期", HttpStatus.CONFLICT);
        }
        CouponTemplate template = couponTemplateMapper.findById(couponInstance.getTemplateId());
        if (template == null || template.getStatus() == null || template.getStatus() != 1) {
            throw new BusinessException("CONFLICT", "券模板不可用", HttpStatus.CONFLICT);
        }
        if (!"PAYMENT".equals(template.getType())) {
            throw new BusinessException("CONFLICT", "当前券不支持支付抵扣", HttpStatus.CONFLICT);
        }
        if (template.getFeeType() != null && !template.getFeeType().isBlank() && !template.getFeeType().equals(bill.getFeeType())) {
            throw new BusinessException("CONFLICT", "券不适用于当前费种", HttpStatus.CONFLICT);
        }
        if (bill.getAmountDue().compareTo(template.getThresholdAmount()) < 0) {
            throw new BusinessException("CONFLICT", "未达到券使用门槛", HttpStatus.CONFLICT);
        }
        return MoneyUtils.scaleMoney(template.getValueAmount().min(bill.getAmountDue()));
    }

    @Transactional
    public BigDecimal lockCoupon(Long accountId, Bill bill, Long couponInstanceId) {
        if (couponInstanceId == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        CouponInstance couponInstance = requireCoupon(couponInstanceId);
        BigDecimal discountAmount = validateInternal(accountId, bill, couponInstance);
        couponInstanceMapper.updateStatus(couponInstanceId, "LOCKED");
        return discountAmount;
    }

    @Transactional
    public void rollbackLockedCoupon(Long couponInstanceId) {
        if (couponInstanceId != null) {
            couponInstanceMapper.updateStatus(couponInstanceId, "NEW");
        }
    }

    @Transactional
    public void markCouponUsed(Long couponInstanceId, String payOrderNo, Long accountId) {
        if (couponInstanceId == null) {
            return;
        }
        couponInstanceMapper.updateStatus(couponInstanceId, "USED");
        couponRedemptionMapper.insert(couponInstanceId, "PAYMENT", payOrderNo, payOrderNo, LocalDateTime.now(), accountId);
    }

    @Transactional
    public int issueRewardCoupons(Bill bill, Long ownerAccountId, String payOrderNo) {
        List<CouponIssueRule> rules = couponIssueRuleMapper.listMatchedRules(bill.getFeeType(), bill.getAmountPaid());
        int issued = 0;
        for (CouponIssueRule rule : rules) {
            CouponTemplate template = couponTemplateMapper.findById(rule.getTemplateId());
            if (template == null) {
                continue;
            }
            for (int i = 0; i < rule.getRewardCount(); i++) {
                CouponInstance existed = couponInstanceMapper.findRewardInstance("PAYMENT_REWARD", payOrderNo + "-" + i, ownerAccountId, template.getId());
                if (existed != null) {
                    continue;
                }
                CouponInstance instance = new CouponInstance();
                instance.setTemplateId(template.getId());
                instance.setOwnerAccountId(ownerAccountId);
                instance.setOwnerGroupId(null);
                instance.setSourceType("PAYMENT_REWARD");
                instance.setSourceRefNo(payOrderNo + "-" + i);
                instance.setStatus("NEW");
                instance.setIssuedAt(LocalDateTime.now());
                instance.setExpiresAt(template.getValidTo());
                couponInstanceMapper.insert(instance);
                issued++;
            }
        }
        return issued;
    }

    public int countRewardIssuedByPayOrderNo(String payOrderNo) {
        return couponInstanceMapper.countRewardIssuedByPayOrderNo(payOrderNo);
    }

    private Bill requireBill(Long billId) {
        Bill bill = billMapper.findById(billId);
        if (bill == null) {
            throw new BusinessException("NOT_FOUND", "账单不存在", HttpStatus.NOT_FOUND);
        }
        return bill;
    }

    private CouponInstance requireCoupon(Long couponInstanceId) {
        CouponInstance couponInstance = couponInstanceMapper.findById(couponInstanceId);
        if (couponInstance == null) {
            throw new BusinessException("NOT_FOUND", "券不存在", HttpStatus.NOT_FOUND);
        }
        return couponInstance;
    }

    private Map<String, Object> buildTemplateAuditDetail(CouponTemplate entity) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("templateId", entity.getId());
        detail.put("templateCode", entity.getTemplateCode());
        detail.put("type", entity.getType());
        detail.put("feeType", entity.getFeeType());
        detail.put("name", entity.getName());
        detail.put("discountMode", entity.getDiscountMode());
        detail.put("valueAmount", entity.getValueAmount());
        detail.put("thresholdAmount", entity.getThresholdAmount());
        detail.put("validFrom", entity.getValidFrom());
        detail.put("validTo", entity.getValidTo());
        detail.put("status", entity.getStatus());
        return detail;
    }
}
