package com.wuye.coupon.service;

import com.wuye.audit.service.AuditLogService;
import com.wuye.auth.entity.Account;
import com.wuye.auth.mapper.AccountMapper;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.common.util.MoneyUtils;
import com.wuye.coupon.dto.AdminCouponInstanceQuery;
import com.wuye.coupon.dto.AdminCouponManualIssueDTO;
import com.wuye.coupon.dto.AdminCouponUpsertDTO;
import com.wuye.coupon.dto.AdminVoucherExchangeStatusDTO;
import com.wuye.coupon.dto.CouponRuleCreateDTO;
import com.wuye.coupon.dto.CouponTemplateCreateDTO;
import com.wuye.coupon.dto.CouponValidateDTO;
import com.wuye.coupon.dto.VoucherExchangeDTO;
import com.wuye.coupon.entity.CouponExchangeRecord;
import com.wuye.coupon.entity.CouponInstance;
import com.wuye.coupon.entity.CouponIssueRule;
import com.wuye.coupon.entity.CouponTemplate;
import com.wuye.coupon.mapper.CouponExchangeRecordMapper;
import com.wuye.coupon.mapper.CouponInstanceMapper;
import com.wuye.coupon.mapper.CouponIssueRuleMapper;
import com.wuye.coupon.mapper.CouponRedemptionMapper;
import com.wuye.coupon.mapper.CouponTemplateMapper;
import com.wuye.coupon.vo.AdminCouponInstanceVO;
import com.wuye.coupon.vo.AdminCouponManualIssueResultVO;
import com.wuye.coupon.vo.AdminCouponSummaryVO;
import com.wuye.coupon.vo.AdminVoucherExchangeVO;
import com.wuye.coupon.vo.AvailableCouponVO;
import com.wuye.coupon.vo.CouponValidateVO;
import com.wuye.coupon.vo.ResidentVoucherVO;
import org.springframework.dao.DuplicateKeyException;
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
    private final CouponExchangeRecordMapper couponExchangeRecordMapper;
    private final CouponInstanceMapper couponInstanceMapper;
    private final CouponRedemptionMapper couponRedemptionMapper;
    private final BillMapper billMapper;
    private final AccountMapper accountMapper;
    private final AccessGuard accessGuard;
    private final AuditLogService auditLogService;

    public CouponService(CouponTemplateMapper couponTemplateMapper,
                         CouponIssueRuleMapper couponIssueRuleMapper,
                         CouponExchangeRecordMapper couponExchangeRecordMapper,
                         CouponInstanceMapper couponInstanceMapper,
                         CouponRedemptionMapper couponRedemptionMapper,
                         BillMapper billMapper,
                         AccountMapper accountMapper,
                         AccessGuard accessGuard,
                         AuditLogService auditLogService) {
        this.couponTemplateMapper = couponTemplateMapper;
        this.couponIssueRuleMapper = couponIssueRuleMapper;
        this.couponExchangeRecordMapper = couponExchangeRecordMapper;
        this.couponInstanceMapper = couponInstanceMapper;
        this.couponRedemptionMapper = couponRedemptionMapper;
        this.billMapper = billMapper;
        this.accountMapper = accountMapper;
        this.accessGuard = accessGuard;
        this.auditLogService = auditLogService;
    }

    public List<AvailableCouponVO> listMyCoupons(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        return couponInstanceMapper.listAvailableByAccountAndBill(loginUser.accountId(), null, null);
    }

    public List<ResidentVoucherVO> listMyVouchers(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        return couponExchangeRecordMapper.listResidentVouchers(loginUser.accountId());
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
        entity.setGoodsName(null);
        entity.setGoodsSpec(null);
        entity.setFulfillmentType(null);
        entity.setRedeemInstruction(null);
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

    @Transactional
    public AdminCouponSummaryVO saveCoupon(LoginUser loginUser, AdminCouponUpsertDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        CouponTemplate template = dto.getId() == null ? null : couponTemplateMapper.findById(dto.getId());
        if (dto.getId() != null && template == null) {
            throw new BusinessException("NOT_FOUND", "优惠券不存在", HttpStatus.NOT_FOUND);
        }
        if (template == null) {
            template = new CouponTemplate();
            template.setTemplateCode(dto.getTemplateCode());
            template.setType(dto.getType());
            template.setStackable(0);
        }
        template.setFeeType(dto.getFeeType());
        template.setName(dto.getName());
        template.setGoodsName(dto.getType().equals("VOUCHER") ? dto.getName() : null);
        template.setGoodsSpec("线下自提兑换");
        template.setFulfillmentType(dto.getType().equals("VOUCHER") ? "PICKUP" : null);
        template.setRedeemInstruction(dto.getType().equals("VOUCHER") ? "请前往物业服务中心线下自提" : null);
        template.setDiscountMode(dto.getDiscountMode());
        template.setValueAmount(MoneyUtils.scaleMoney(dto.getValueAmount()));
        template.setThresholdAmount(MoneyUtils.scaleMoney(dto.getThresholdAmount()));
        template.setValidFrom(dto.getValidFrom());
        template.setValidTo(dto.getValidTo());
        template.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (template.getId() == null) {
            couponTemplateMapper.insert(template);
        } else {
            couponTemplateMapper.update(template);
        }
        CouponIssueRule existedRule = couponIssueRuleMapper.findByTemplateId(template.getId());
        if (dto.getTriggerType() == null || dto.getTriggerType().isBlank()) {
            if (existedRule != null && existedRule.getStatus() != null && existedRule.getStatus() == 1) {
                couponIssueRuleMapper.deleteById(existedRule.getId());
            }
        } else {
            validateRuleTemplate(template, dto.getTriggerType());
            CouponIssueRule rule = existedRule == null ? new CouponIssueRule() : existedRule;
            rule.setRuleName(dto.getName() + "-" + dto.getTriggerType());
            rule.setTriggerType(dto.getTriggerType());
            rule.setFeeType(resolveFeeTypeByTriggerType(dto.getTriggerType(), dto.getFeeType()));
            rule.setTemplateId(template.getId());
            rule.setMinPayAmount(MoneyUtils.scaleMoney(dto.getMinPayAmount() == null ? BigDecimal.ZERO : dto.getMinPayAmount()));
            rule.setRewardCount(dto.getRewardCount() == null ? 1 : dto.getRewardCount());
            rule.setStatus(dto.getRuleStatus() == null ? 1 : dto.getRuleStatus());
            if (rule.getId() == null) {
                couponIssueRuleMapper.insert(rule);
            } else {
                couponIssueRuleMapper.update(rule);
            }
        }
        Long templateId = template.getId();
        auditLogService.record(loginUser, "COUPON", String.valueOf(templateId), dto.getId() == null ? "CREATE" : "UPDATE", buildTemplateAuditDetail(template));
        return couponTemplateMapper.listAdminSummaries().stream().filter(item -> item.getId().equals(templateId)).findFirst().orElseThrow();
    }

    public List<CouponTemplate> listTemplates(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return couponTemplateMapper.listActive();
    }

    public List<AdminCouponSummaryVO> listAdminCoupons(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return couponTemplateMapper.listAdminSummaries();
    }

    public List<AdminCouponInstanceVO> listAdminInstances(LoginUser loginUser, AdminCouponInstanceQuery query) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return couponInstanceMapper.listAdminInstances(query == null ? new AdminCouponInstanceQuery() : query);
    }

    public List<AdminVoucherExchangeVO> listVoucherExchanges(LoginUser loginUser, Long templateId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return couponExchangeRecordMapper.listByTemplateId(templateId);
    }

    @Transactional
    public CouponIssueRule createRule(LoginUser loginUser, CouponRuleCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        CouponTemplate template = couponTemplateMapper.findByTemplateCode(dto.getTemplateCode());
        if (template == null) {
            throw new BusinessException("NOT_FOUND", "券模板不存在", HttpStatus.NOT_FOUND);
        }
        validateRuleTemplate(template, dto.getTriggerType());
        CouponIssueRule rule = new CouponIssueRule();
        rule.setRuleName(dto.getName());
        rule.setTriggerType(dto.getTriggerType());
        rule.setFeeType(resolveFeeTypeByTriggerType(dto.getTriggerType(), dto.getFeeType()));
        rule.setTemplateId(template.getId());
        rule.setMinPayAmount(MoneyUtils.scaleMoney(dto.getMinPayAmount() == null ? BigDecimal.ZERO : dto.getMinPayAmount()));
        rule.setRewardCount(dto.getRewardCount());
        rule.setStatus(1);
        couponIssueRuleMapper.insert(rule);
        return rule;
    }

    public List<CouponIssueRule> listRules(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return couponIssueRuleMapper.listActive();
    }

    @Transactional
    public void deleteRule(LoginUser loginUser, Long ruleId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        CouponIssueRule rule = couponIssueRuleMapper.findById(ruleId);
        if (rule == null) {
            throw new BusinessException("NOT_FOUND", "发券规则不存在", HttpStatus.NOT_FOUND);
        }
        int affected = couponIssueRuleMapper.deleteById(ruleId);
        if (affected == 0) {
            throw new BusinessException("CONFLICT", "发券规则已停用", HttpStatus.CONFLICT);
        }
        auditLogService.record(loginUser, "COUPON", String.valueOf(ruleId), "DISABLE", Map.of(
                "ruleId", ruleId,
                "templateId", rule.getTemplateId(),
                "ruleName", rule.getRuleName(),
                "status", 0
        ));
    }

    @Transactional
    public void deleteTemplate(LoginUser loginUser, Long templateId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        CouponTemplate template = couponTemplateMapper.findById(templateId);
        if (template == null) {
            throw new BusinessException("NOT_FOUND", "券模板不存在", HttpStatus.NOT_FOUND);
        }
        if (couponIssueRuleMapper.countByTemplateId(templateId) > 0) {
            throw new BusinessException("CONFLICT", "券模板已被发券规则引用，暂不可删除", HttpStatus.CONFLICT);
        }
        int affected = couponTemplateMapper.deleteById(templateId);
        if (affected == 0) {
            throw new BusinessException("CONFLICT", "券模板已停用", HttpStatus.CONFLICT);
        }
        auditLogService.record(loginUser, "COUPON", String.valueOf(templateId), "DISABLE", Map.of(
                "templateId", templateId,
                "templateCode", template.getTemplateCode(),
                "templateName", template.getName(),
                "status", 0
        ));
    }

    @Transactional
    public ResidentVoucherVO exchangeVoucher(LoginUser loginUser, Long couponInstanceId, VoucherExchangeDTO dto) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        CouponInstance couponInstance = requireCoupon(couponInstanceId);
        if (!loginUser.accountId().equals(couponInstance.getOwnerAccountId())) {
            throw new BusinessException("FORBIDDEN", "券不属于当前账号", HttpStatus.FORBIDDEN);
        }
        if (!"NEW".equals(couponInstance.getStatus())) {
            throw new BusinessException("CONFLICT", "当前奖励券不可兑换", HttpStatus.CONFLICT);
        }
        if (couponExchangeRecordMapper.findByCouponInstanceId(couponInstanceId) != null) {
            throw new BusinessException("CONFLICT", "该奖励券已兑换", HttpStatus.CONFLICT);
        }
        CouponTemplate template = couponTemplateMapper.findById(couponInstance.getTemplateId());
        if (template == null || template.getStatus() == null || template.getStatus() != 1) {
            throw new BusinessException("CONFLICT", "奖励券模板不可用", HttpStatus.CONFLICT);
        }
        if (!"VOUCHER".equals(template.getType())) {
            throw new BusinessException("CONFLICT", "当前券不支持兑换", HttpStatus.CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now();
        if (couponInstance.getExpiresAt() == null || couponInstance.getExpiresAt().isBefore(now)) {
            throw new BusinessException("CONFLICT", "奖励券已过期", HttpStatus.CONFLICT);
        }
        CouponExchangeRecord record = new CouponExchangeRecord();
        record.setCouponInstanceId(couponInstanceId);
        record.setTemplateId(template.getId());
        record.setOwnerAccountId(loginUser.accountId());
        record.setGoodsName(template.getGoodsName() == null ? template.getName() : template.getGoodsName());
        record.setGoodsSpec(template.getGoodsSpec());
        record.setExchangeStatus("PENDING");
        record.setPickupSite(template.getRedeemInstruction());
        record.setRemark(dto == null ? null : dto.getRemark());
        couponExchangeRecordMapper.insert(record);
        couponInstanceMapper.updateStatus(couponInstanceId, "USED");
        auditLogService.record(loginUser, "COUPON", String.valueOf(couponInstanceId), "CREATE", Map.of(
                "couponInstanceId", couponInstanceId,
                "templateId", template.getId(),
                "goodsName", record.getGoodsName(),
                "exchangeStatus", "PENDING"
        ));
        return couponExchangeRecordMapper.listResidentVouchers(loginUser.accountId()).stream()
                .filter(item -> item.getCouponInstanceId().equals(couponInstanceId))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public void updateVoucherExchangeStatus(LoginUser loginUser, Long exchangeId, AdminVoucherExchangeStatusDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        String exchangeStatus = dto.getExchangeStatus();
        if (!"FULFILLED".equals(exchangeStatus) && !"CANCELLED".equals(exchangeStatus)) {
            throw new BusinessException("INVALID_ARGUMENT", "exchangeStatus 仅支持 FULFILLED 或 CANCELLED", HttpStatus.BAD_REQUEST);
        }
        int affected = couponExchangeRecordMapper.updateStatus(exchangeId, exchangeStatus, dto.getRemark());
        if (affected == 0) {
            throw new BusinessException("NOT_FOUND", "兑换记录不存在", HttpStatus.NOT_FOUND);
        }
        auditLogService.record(loginUser, "COUPON", String.valueOf(exchangeId), "UPDATE", Map.of(
                "exchangeId", exchangeId,
                "exchangeStatus", exchangeStatus,
                "remark", dto.getRemark()
        ));
    }

    @Transactional
    public AdminCouponManualIssueResultVO manualIssue(LoginUser loginUser, AdminCouponManualIssueDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        CouponTemplate template = couponTemplateMapper.findById(dto.getTemplateId());
        if (template == null) {
            throw new BusinessException("NOT_FOUND", "券模板不存在", HttpStatus.NOT_FOUND);
        }
        if (template.getStatus() == null || template.getStatus() != 1) {
            throw new BusinessException("CONFLICT", "券模板已停用", HttpStatus.CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getValidFrom() != null && now.isBefore(template.getValidFrom())) {
            throw new BusinessException("CONFLICT", "券模板尚未生效", HttpStatus.CONFLICT);
        }
        if (template.getValidTo() != null && now.isAfter(template.getValidTo())) {
            throw new BusinessException("CONFLICT", "券模板已过期", HttpStatus.CONFLICT);
        }
        if (!"PAYMENT".equals(template.getType()) && !"VOUCHER".equals(template.getType())) {
            throw new BusinessException("CONFLICT", "当前券模板类型不支持发放", HttpStatus.CONFLICT);
        }
        Account account = requireAccount(dto.getOwnerAccountId());
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new BusinessException("CONFLICT", "目标账号已停用", HttpStatus.CONFLICT);
        }
        java.util.ArrayList<Long> couponInstanceIds = new java.util.ArrayList<>();
        String normalizedRemark = dto.getRemark() == null ? "" : dto.getRemark().trim();
        for (int i = 0; i < dto.getIssueCount(); i++) {
            CouponInstance instance = new CouponInstance();
            instance.setTemplateId(template.getId());
            instance.setOwnerAccountId(account.getId());
            instance.setOwnerGroupId(null);
            instance.setSourceType("MANUAL");
            instance.setSourceRefNo("MANUAL-" + loginUser.accountId() + "-" + System.currentTimeMillis() + "-" + i);
            instance.setStatus("NEW");
            instance.setIssuedAt(now);
            instance.setExpiresAt(template.getValidTo());
            couponInstanceMapper.insert(instance);
            couponInstanceIds.add(instance.getId());
        }
        auditLogService.record(loginUser, "COUPON", String.valueOf(template.getId()), "CREATE", Map.of(
                "templateId", template.getId(),
                "templateCode", template.getTemplateCode(),
                "ownerAccountId", account.getId(),
                "issueCount", dto.getIssueCount(),
                "remark", normalizedRemark,
                "sourceType", "MANUAL",
                "couponInstanceIds", couponInstanceIds
        ));
        AdminCouponManualIssueResultVO vo = new AdminCouponManualIssueResultVO();
        vo.setTemplateId(template.getId());
        vo.setOwnerAccountId(account.getId());
        vo.setIssueCount(dto.getIssueCount());
        vo.setCouponInstanceIds(couponInstanceIds);
        return vo;
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
        String triggerType = resolvePaymentTriggerType(bill.getFeeType());
        List<CouponIssueRule> rules = couponIssueRuleMapper.listMatchedRules(triggerType, bill.getAmountPaid());
        return issueByRules(rules, ownerAccountId, payOrderNo, "PAYMENT_REWARD");
    }

    @Transactional
    public int issueLoginCoupons(Account account) {
        if (account == null || !"RESIDENT".equals(account.getAccountType())) {
            return 0;
        }
        String sourceRefNo = account.getId() + "-" + java.time.LocalDate.now();
        List<CouponIssueRule> rules = couponIssueRuleMapper.listMatchedRules("LOGIN", null);
        int issued = issueByRules(rules, account.getId(), sourceRefNo, "LOGIN_REWARD");
        if (issued > 0) {
            auditLogService.record(new LoginUser(account.getId(), account.getAccountType(), account.getAccountType(), account.getRealName(), java.util.List.of("RESIDENT"), "SELF", java.util.List.of()), "COUPON", String.valueOf(account.getId()), "CREATE", Map.of(
                    "sourceType", "LOGIN_REWARD",
                    "ownerAccountId", account.getId(),
                    "issuedCount", issued,
                    "sourceRefNo", sourceRefNo
            ));
        }
        return issued;
    }

    public int countRewardIssuedByPayOrderNo(String payOrderNo) {
        return couponInstanceMapper.countRewardIssuedByPayOrderNo(payOrderNo);
    }

    private int issueByRules(List<CouponIssueRule> rules, Long ownerAccountId, String sourceRefNo, String sourceType) {
        int issued = 0;
        for (CouponIssueRule rule : rules) {
            CouponTemplate template = couponTemplateMapper.findById(rule.getTemplateId());
            if (template == null || template.getStatus() == null || template.getStatus() != 1) {
                continue;
            }
            LocalDateTime now = LocalDateTime.now();
            if (template.getValidFrom() != null && now.isBefore(template.getValidFrom())) {
                continue;
            }
            if (template.getValidTo() != null && now.isAfter(template.getValidTo())) {
                continue;
            }
            for (int i = 0; i < rule.getRewardCount(); i++) {
                CouponInstance existed = couponInstanceMapper.findRewardInstance(sourceType, sourceRefNo + "-" + i, ownerAccountId, template.getId());
                if (existed != null) {
                    continue;
                }
                CouponInstance instance = new CouponInstance();
                instance.setTemplateId(template.getId());
                instance.setOwnerAccountId(ownerAccountId);
                instance.setOwnerGroupId(null);
                instance.setSourceType(sourceType);
                instance.setSourceRefNo(sourceRefNo + "-" + i);
                instance.setStatus("NEW");
                instance.setIssuedAt(now);
                instance.setExpiresAt(template.getValidTo());
                try {
                    couponInstanceMapper.insert(instance);
                    issued++;
                } catch (DuplicateKeyException ex) {
                    // 并发回调下依赖数据库唯一约束兜底，重复奖励券直接忽略。
                }
            }
        }
        return issued;
    }

    private void validateRuleTemplate(CouponTemplate template, String triggerType) {
        if (template.getStatus() == null || template.getStatus() != 1) {
            throw new BusinessException("CONFLICT", "券模板已停用", HttpStatus.CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getValidTo() != null && now.isAfter(template.getValidTo())) {
            throw new BusinessException("CONFLICT", "券模板已过期", HttpStatus.CONFLICT);
        }
        if ("LOGIN".equals(triggerType) && !"VOUCHER".equals(template.getType())) {
            throw new BusinessException("CONFLICT", "登录赠送仅支持奖励券模板", HttpStatus.CONFLICT);
        }
        if (("PROPERTY_PAYMENT".equals(triggerType) || "WATER_PAYMENT".equals(triggerType)) && !"VOUCHER".equals(template.getType())) {
            throw new BusinessException("CONFLICT", "支付后发放仅支持奖励券模板", HttpStatus.CONFLICT);
        }
    }

    private String resolveFeeTypeByTriggerType(String triggerType, String feeType) {
        if ("PROPERTY_PAYMENT".equals(triggerType)) {
            return "PROPERTY";
        }
        if ("WATER_PAYMENT".equals(triggerType)) {
            return "WATER";
        }
        return feeType;
    }

    private String resolvePaymentTriggerType(String feeType) {
        if ("PROPERTY".equals(feeType)) {
            return "PROPERTY_PAYMENT";
        }
        if ("WATER".equals(feeType)) {
            return "WATER_PAYMENT";
        }
        return feeType;
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

    private Account requireAccount(Long accountId) {
        Account account = accountMapper.findById(accountId);
        if (account == null) {
            throw new BusinessException("NOT_FOUND", "目标账号不存在", HttpStatus.NOT_FOUND);
        }
        return account;
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
