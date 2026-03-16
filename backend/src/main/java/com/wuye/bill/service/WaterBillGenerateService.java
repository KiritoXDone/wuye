package com.wuye.bill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.bill.dto.WaterBillGenerateDTO;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.entity.BillLine;
import com.wuye.bill.entity.FeeRule;
import com.wuye.bill.entity.WaterMeterReading;
import com.wuye.bill.mapper.BillLineMapper;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.bill.mapper.WaterReadingMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.common.util.MoneyUtils;
import com.wuye.common.util.NoGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class WaterBillGenerateService {

    private final WaterReadingMapper waterReadingMapper;
    private final FeeRuleService feeRuleService;
    private final BillMapper billMapper;
    private final BillLineMapper billLineMapper;
    private final AccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public WaterBillGenerateService(WaterReadingMapper waterReadingMapper,
                                    FeeRuleService feeRuleService,
                                    BillMapper billMapper,
                                    BillLineMapper billLineMapper,
                                    AccessGuard accessGuard,
                                    ObjectMapper objectMapper) {
        this.waterReadingMapper = waterReadingMapper;
        this.feeRuleService = feeRuleService;
        this.billMapper = billMapper;
        this.billLineMapper = billLineMapper;
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
            Bill existed = billMapper.findByUniqueKey(reading.getRoomId(), "WATER", dto.getYear(), dto.getMonth());
            if (existed != null) {
                if ("SKIP".equalsIgnoreCase(dto.getOverwriteStrategy())) {
                    continue;
                }
                throw new BusinessException("CONFLICT", "存在重复水费账单: roomId=" + reading.getRoomId(), HttpStatus.CONFLICT);
            }
            BigDecimal amountDue = MoneyUtils.scaleMoney(reading.getUsageAmount().multiply(feeRule.getUnitPrice()));
            Bill bill = new Bill();
            bill.setBillNo(NoGenerator.billNo());
            bill.setRoomId(reading.getRoomId());
            bill.setFeeType("WATER");
            bill.setPeriodYear(dto.getYear());
            bill.setPeriodMonth(dto.getMonth());
            bill.setAmountDue(amountDue);
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
            line.setItemName(dto.getYear() + "-" + String.format("%02d", dto.getMonth()) + " 水费");
            line.setUnitPrice(feeRule.getUnitPrice());
            line.setQuantity(reading.getUsageAmount());
            line.setLineAmount(amountDue);
            line.setExtJson(writeJson(new LinkedHashMap<>() {{
                put("prevReading", reading.getPrevReading());
                put("currReading", reading.getCurrReading());
                put("usage", reading.getUsageAmount());
                put("meterId", reading.getMeterId());
            }}));
            billLineMapper.insert(line);
            generated++;
        }
        return generated;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize json", ex);
        }
    }
}
