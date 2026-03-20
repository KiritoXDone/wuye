package com.wuye.bill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.bill.dto.WaterBillGenerateDTO;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.entity.BillLine;
import com.wuye.bill.entity.FeeRule;
import com.wuye.bill.entity.FeeRuleWaterTier;
import com.wuye.bill.entity.WaterMeterReading;
import com.wuye.bill.mapper.BillLineMapper;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.bill.mapper.WaterReadingMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.common.util.MoneyUtils;
import com.wuye.common.util.NoGenerator;
import com.wuye.room.mapper.GroupRoomMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class WaterBillGenerateService {

    private final WaterReadingMapper waterReadingMapper;
    private final FeeRuleService feeRuleService;
    private final BillMapper billMapper;
    private final BillLineMapper billLineMapper;
    private final GroupRoomMapper groupRoomMapper;
    private final AccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public WaterBillGenerateService(WaterReadingMapper waterReadingMapper,
                                    FeeRuleService feeRuleService,
                                    BillMapper billMapper,
                                    BillLineMapper billLineMapper,
                                    GroupRoomMapper groupRoomMapper,
                                    AccessGuard accessGuard,
                                    ObjectMapper objectMapper) {
        this.waterReadingMapper = waterReadingMapper;
        this.feeRuleService = feeRuleService;
        this.billMapper = billMapper;
        this.billLineMapper = billLineMapper;
        this.groupRoomMapper = groupRoomMapper;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int generate(LoginUser loginUser, WaterBillGenerateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        LocalDate targetDate = LocalDate.of(dto.getYear(), dto.getMonth(), 1);
        FeeRule feeRule = feeRuleService.requireActiveRule(dto.getCommunityId(), "WATER", targetDate);
        if (feeRule == null) {
            throw new BusinessException("NOT_FOUND", "未找到生效中的水费规则", HttpStatus.NOT_FOUND);
        }
        List<WaterMeterReading> readings = waterReadingMapper.listByCommunityAndPeriod(dto.getCommunityId(), dto.getYear(), dto.getMonth());
        int generated = 0;
        for (WaterMeterReading reading : readings) {
            if (generateForReading(reading, feeRule, dto.getOverwriteStrategy()) != null) {
                generated++;
            }
        }
        return generated;
    }

    @Transactional
    public Bill generateForReading(WaterMeterReading reading, FeeRule feeRule, String overwriteStrategy) {
        Bill existed = billMapper.findByUniqueKey(reading.getRoomId(), "WATER", reading.getPeriodYear(), reading.getPeriodMonth());
        if (existed != null) {
            if ("SKIP".equalsIgnoreCase(overwriteStrategy)) {
                return null;
            }
            throw new BusinessException("CONFLICT", "存在重复水费账单: roomId=" + reading.getRoomId(), HttpStatus.CONFLICT);
        }
        LocalDate targetDate = LocalDate.of(reading.getPeriodYear(), reading.getPeriodMonth(), 1);
        WaterChargeResult chargeResult = calculateCharge(feeRule, reading.getUsageAmount());
        Bill bill = new Bill();
        bill.setBillNo(NoGenerator.billNo());
        bill.setRoomId(reading.getRoomId());
        bill.setGroupId(groupRoomMapper.findPrimaryGroupIdByRoomId(reading.getRoomId()));
        bill.setFeeType("WATER");
        bill.setCycleType("MONTH");
        bill.setPeriodYear(reading.getPeriodYear());
        bill.setPeriodMonth(reading.getPeriodMonth());
        bill.setServicePeriodStart(targetDate);
        bill.setServicePeriodEnd(targetDate.with(TemporalAdjusters.lastDayOfMonth()));
        bill.setAmountDue(chargeResult.amountDue());
        bill.setDiscountAmountTotal(BigDecimal.ZERO.setScale(2));
        bill.setAmountPaid(BigDecimal.ZERO.setScale(2));
        bill.setDueDate(targetDate.with(TemporalAdjusters.lastDayOfMonth()));
        bill.setStatus("ISSUED");
        bill.setSourceType("GENERATED");
        bill.setRemark("水费自动开单");
        billMapper.insert(bill);

        BillLine line = new BillLine();
        line.setBillId(bill.getId());
        line.setLineNo(1);
        line.setLineType("WATER");
        line.setItemName(reading.getPeriodYear() + "-" + String.format("%02d", reading.getPeriodMonth()) + " 水费");
        line.setUnitPrice(chargeResult.displayUnitPrice());
        line.setQuantity(reading.getUsageAmount());
        line.setLineAmount(chargeResult.amountDue());
        line.setExtJson(writeJson(new LinkedHashMap<>() {{
            put("prevReading", reading.getPrevReading());
            put("currReading", reading.getCurrReading());
            put("usage", reading.getUsageAmount());
            put("meterId", reading.getMeterId());
            put("pricingMode", feeRule.getPricingMode());
            put("servicePeriodStart", bill.getServicePeriodStart());
            put("servicePeriodEnd", bill.getServicePeriodEnd());
            put("tierBreakdown", chargeResult.tierBreakdown());
        }}));
        billLineMapper.insert(line);
        return bill;
    }

    private WaterChargeResult calculateCharge(FeeRule feeRule, BigDecimal usageAmount) {
        if (!"TIERED".equalsIgnoreCase(feeRule.getPricingMode())
                || feeRule.getWaterTiers() == null
                || feeRule.getWaterTiers().isEmpty()) {
            BigDecimal amountDue = MoneyUtils.scaleMoney(usageAmount.multiply(feeRule.getUnitPrice()));
            return new WaterChargeResult(amountDue, feeRule.getUnitPrice(), List.of());
        }
        BigDecimal remaining = usageAmount;
        BigDecimal total = BigDecimal.ZERO;
        List<LinkedHashMap<String, Object>> breakdown = new ArrayList<>();
        for (FeeRuleWaterTier tier : feeRule.getWaterTiers()) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal start = tier.getStartUsage();
            BigDecimal end = tier.getEndUsage();
            BigDecimal capacity = end == null ? remaining : end.subtract(start);
            if (capacity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal tierUsage = remaining.min(capacity);
            BigDecimal tierAmount = MoneyUtils.scaleMoney(tierUsage.multiply(tier.getUnitPrice()));
            total = total.add(tierAmount);
            LinkedHashMap<String, Object> tierItem = new LinkedHashMap<>();
            tierItem.put("startUsage", start);
            tierItem.put("endUsage", end);
            tierItem.put("unitPrice", tier.getUnitPrice());
            tierItem.put("usage", tierUsage);
            tierItem.put("amount", tierAmount);
            breakdown.add(tierItem);
            remaining = remaining.subtract(tierUsage);
        }
        return new WaterChargeResult(MoneyUtils.scaleMoney(total), feeRule.getUnitPrice(), breakdown);
    }

    private record WaterChargeResult(BigDecimal amountDue,
                                     BigDecimal displayUnitPrice,
                                     List<LinkedHashMap<String, Object>> tierBreakdown) {
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize json", ex);
        }
    }
}
