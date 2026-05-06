package com.wuye.coupon.service;

import com.wuye.audit.service.AuditLogService;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.infra.mq.CouponSeckillEventPublisher;
import com.wuye.common.infra.redis.SeckillStockLock;
import com.wuye.common.config.AppInfraProperties;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.coupon.dto.AdminCouponSeckillCampaignCreateDTO;
import com.wuye.coupon.dto.CouponSeckillRequestDTO;
import com.wuye.coupon.entity.CouponInstance;
import com.wuye.coupon.entity.CouponSeckillCampaign;
import com.wuye.coupon.entity.CouponSeckillOrder;
import com.wuye.coupon.entity.CouponTemplate;
import com.wuye.coupon.event.CouponSeckillOrderEvent;
import com.wuye.coupon.mapper.CouponInstanceMapper;
import com.wuye.coupon.mapper.CouponSeckillCampaignMapper;
import com.wuye.coupon.mapper.CouponSeckillOrderMapper;
import com.wuye.coupon.mapper.CouponTemplateMapper;
import com.wuye.coupon.vo.CouponSeckillOrderVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class CouponSeckillService implements CouponSeckillOrderProcessor {

    private final CouponSeckillCampaignMapper campaignMapper;
    private final CouponSeckillOrderMapper orderMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponInstanceMapper couponInstanceMapper;
    private final CouponSeckillEventPublisher eventPublisher;
    private final SeckillStockLock seckillStockLock;
    private final AccessGuard accessGuard;
    private final AuditLogService auditLogService;
    private final AppInfraProperties appInfraProperties;

    public CouponSeckillService(CouponSeckillCampaignMapper campaignMapper,
                                CouponSeckillOrderMapper orderMapper,
                                CouponTemplateMapper couponTemplateMapper,
                                CouponInstanceMapper couponInstanceMapper,
                                CouponSeckillEventPublisher eventPublisher,
                                SeckillStockLock seckillStockLock,
                                AccessGuard accessGuard,
                                AuditLogService auditLogService,
                                AppInfraProperties appInfraProperties) {
        this.campaignMapper = campaignMapper;
        this.orderMapper = orderMapper;
        this.couponTemplateMapper = couponTemplateMapper;
        this.couponInstanceMapper = couponInstanceMapper;
        this.eventPublisher = eventPublisher;
        this.seckillStockLock = seckillStockLock;
        this.accessGuard = accessGuard;
        this.auditLogService = auditLogService;
        this.appInfraProperties = appInfraProperties;
    }

    @Transactional
    public CouponSeckillCampaign createCampaign(LoginUser loginUser, AdminCouponSeckillCampaignCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        CouponTemplate template = couponTemplateMapper.findById(dto.getTemplateId());
        if (template == null || template.getStatus() == null || template.getStatus() != 1) {
            throw new BusinessException("NOT_FOUND", "券模板不存在或已停用", HttpStatus.NOT_FOUND);
        }
        if (dto.getStartAt() != null && dto.getEndAt() != null && !dto.getStartAt().isBefore(dto.getEndAt())) {
            throw new BusinessException("BAD_REQUEST", "活动开始时间必须早于结束时间", HttpStatus.BAD_REQUEST);
        }
        CouponSeckillCampaign campaign = new CouponSeckillCampaign();
        campaign.setCampaignCode(dto.getCampaignCode() == null || dto.getCampaignCode().isBlank()
                ? "SECKILL-" + UUID.randomUUID()
                : dto.getCampaignCode());
        campaign.setTemplateId(dto.getTemplateId());
        campaign.setTitle(dto.getTitle());
        campaign.setTotalStock(dto.getTotalStock());
        campaign.setAvailableStock(dto.getTotalStock());
        campaign.setPerUserLimit(dto.getPerUserLimit() == null ? 1 : dto.getPerUserLimit());
        campaign.setStartAt(dto.getStartAt());
        campaign.setEndAt(dto.getEndAt());
        campaign.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        campaignMapper.insert(campaign);
        auditLogService.record(loginUser, "COUPON_SECKILL", String.valueOf(campaign.getId()), "CREATE", Map.of(
                "campaignId", campaign.getId(),
                "templateId", campaign.getTemplateId(),
                "totalStock", campaign.getTotalStock(),
                "perUserLimit", campaign.getPerUserLimit()
        ));
        return campaign;
    }

    @Transactional
    public CouponSeckillOrderVO submit(LoginUser loginUser, Long campaignId, CouponSeckillRequestDTO dto) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        CouponSeckillCampaign campaign = requireOpenCampaign(campaignId);
        CouponSeckillOrder existed = orderMapper.findByRequest(campaignId, loginUser.accountId(), dto.getRequestId());
        if (existed != null) {
            return toVo(existed, "重复请求已拦截，返回已有抢购订单。");
        }
        if (orderMapper.countActiveByAccount(campaignId, loginUser.accountId()) >= campaign.getPerUserLimit()) {
            throw new BusinessException("CONFLICT", "已达到该活动的每人限购数量", HttpStatus.CONFLICT);
        }

        CouponSeckillOrder order = new CouponSeckillOrder();
        order.setOrderNo("CSO-" + UUID.randomUUID());
        order.setCampaignId(campaignId);
        order.setAccountId(loginUser.accountId());
        order.setStatus("PENDING");
        order.setRequestId(dto.getRequestId());
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException ex) {
            CouponSeckillOrder duplicated = orderMapper.findByRequest(campaignId, loginUser.accountId(), dto.getRequestId());
            if (duplicated != null) {
                return toVo(duplicated, "重复请求已拦截，返回已有抢购订单。");
            }
            throw ex;
        }

        CouponSeckillOrderEvent event = new CouponSeckillOrderEvent();
        event.setOrderNo(order.getOrderNo());
        event.setCampaignId(campaignId);
        event.setAccountId(loginUser.accountId());
        event.setRequestId(dto.getRequestId());
        event.setRequestedAt(LocalDateTime.now());
        eventPublisher.publishSeckillOrder(event);
        if (!appInfraProperties.getRabbit().isEnabled()) {
            process(event);
        }
        auditLogService.record(loginUser, "COUPON_SECKILL", String.valueOf(campaignId), "CREATE", Map.of(
                "campaignId", campaignId,
                "orderNo", order.getOrderNo(),
                "requestId", dto.getRequestId(),
                "status", "PENDING"
        ));
        CouponSeckillOrder latest = orderMapper.findByOrderNo(order.getOrderNo());
        CouponSeckillOrder returned = latest == null ? order : latest;
        return toVo(returned, "FAILED".equals(returned.getStatus()) ? returned.getFailReason() : "抢购请求已受理。");
    }

    public CouponSeckillOrderVO getOrder(LoginUser loginUser, String orderNo) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        CouponSeckillOrder order = orderMapper.findByOrderNo(orderNo);
        if (order == null || !loginUser.accountId().equals(order.getAccountId())) {
            throw new BusinessException("NOT_FOUND", "抢购订单不存在", HttpStatus.NOT_FOUND);
        }
        return toVo(order, null);
    }

    @Override
    @Transactional
    public void process(CouponSeckillOrderEvent event) {
        String lockKey = "coupon:seckill:" + event.getCampaignId();
        if (!seckillStockLock.tryLock(lockKey)) {
            markFailed(event.getOrderNo(), "系统繁忙，请稍后重试");
            return;
        }
        try {
            CouponSeckillOrder order = orderMapper.findByOrderNo(event.getOrderNo());
            if (order == null || !"PENDING".equals(order.getStatus())) {
                return;
            }
            CouponSeckillCampaign campaign = campaignMapper.findByIdForUpdate(event.getCampaignId());
            if (!isCampaignOpen(campaign)) {
                markFailed(order.getOrderNo(), "活动未开始或已结束");
                return;
            }
            if (orderMapper.countActiveByAccount(campaign.getId(), order.getAccountId()) > campaign.getPerUserLimit()) {
                markFailed(order.getOrderNo(), "超过每人限购数量");
                return;
            }
            CouponTemplate template = couponTemplateMapper.findById(campaign.getTemplateId());
            if (template == null || template.getStatus() == null || template.getStatus() != 1) {
                markFailed(order.getOrderNo(), "券模板不可用");
                return;
            }
            if (campaignMapper.decrementStock(campaign.getId()) == 0) {
                markFailed(order.getOrderNo(), "库存不足");
                return;
            }
            CouponInstance instance = new CouponInstance();
            instance.setTemplateId(template.getId());
            instance.setOwnerAccountId(order.getAccountId());
            instance.setOwnerGroupId(null);
            instance.setSourceType("SECKILL");
            instance.setSourceRefNo(order.getOrderNo());
            instance.setStatus("NEW");
            instance.setIssuedAt(LocalDateTime.now());
            instance.setExpiresAt(template.getValidTo());
            couponInstanceMapper.insert(instance);
            orderMapper.markSuccess(order.getOrderNo(), instance.getId());
        } finally {
            seckillStockLock.unlock(lockKey);
        }
    }

    private CouponSeckillCampaign requireOpenCampaign(Long campaignId) {
        CouponSeckillCampaign campaign = campaignMapper.findById(campaignId);
        if (!isCampaignOpen(campaign)) {
            throw new BusinessException("CONFLICT", "抢购活动未开始或已结束", HttpStatus.CONFLICT);
        }
        return campaign;
    }

    private boolean isCampaignOpen(CouponSeckillCampaign campaign) {
        if (campaign == null || campaign.getStatus() == null || campaign.getStatus() != 1) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(campaign.getStartAt()) && !now.isAfter(campaign.getEndAt());
    }

    private void markFailed(String orderNo, String reason) {
        orderMapper.markFailed(orderNo, reason);
    }

    private CouponSeckillOrderVO toVo(CouponSeckillOrder order, String message) {
        CouponSeckillOrderVO vo = new CouponSeckillOrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setCampaignId(order.getCampaignId());
        vo.setCouponInstanceId(order.getCouponInstanceId());
        vo.setStatus(order.getStatus());
        vo.setMessage(message == null ? order.getFailReason() : message);
        return vo;
    }
}
