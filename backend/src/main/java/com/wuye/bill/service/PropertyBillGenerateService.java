package com.wuye.bill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.bill.dto.PropertyBillGenerateDTO;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.entity.BillLine;
import com.wuye.bill.entity.FeeRule;
import com.wuye.bill.mapper.BillLineMapper;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.common.util.MoneyUtils;
import com.wuye.common.util.NoGenerator;
import com.wuye.room.entity.Room;
import com.wuye.room.mapper.RoomMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class PropertyBillGenerateService {

    private final RoomMapper roomMapper;
    private final FeeRuleService feeRuleService;
    private final BillMapper billMapper;
    private final BillLineMapper billLineMapper;
    private final AccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public PropertyBillGenerateService(RoomMapper roomMapper,
                                       FeeRuleService feeRuleService,
                                       BillMapper billMapper,
                                       BillLineMapper billLineMapper,
                                       AccessGuard accessGuard,
                                       ObjectMapper objectMapper) {
        this.roomMapper = roomMapper;
        this.feeRuleService = feeRuleService;
        this.billMapper = billMapper;
        this.billLineMapper = billLineMapper;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int generate(LoginUser loginUser, PropertyBillGenerateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        LocalDate targetDate = LocalDate.of(dto.getYear(), dto.getMonth(), 1);
        FeeRule feeRule = feeRuleService.requireActiveRule(dto.getCommunityId(), "PROPERTY", targetDate);
        if (feeRule == null) {
            throw new BusinessException("NOT_FOUND", "未找到生效中的物业费规则", HttpStatus.NOT_FOUND);
        }
        List<Room> rooms = roomMapper.listActiveByCommunity(dto.getCommunityId());
        int generated = 0;
        for (Room room : rooms) {
            Bill existed = billMapper.findByUniqueKey(room.getId(), "PROPERTY", dto.getYear(), dto.getMonth());
            if (existed != null) {
                if ("SKIP".equalsIgnoreCase(dto.getOverwriteStrategy())) {
                    continue;
                }
                throw new BusinessException("CONFLICT", "存在重复账单: roomId=" + room.getId(), HttpStatus.CONFLICT);
            }
            BigDecimal amountDue = MoneyUtils.scaleMoney(room.getAreaM2().multiply(feeRule.getUnitPrice()));
            Bill bill = new Bill();
            bill.setBillNo(NoGenerator.billNo());
            bill.setRoomId(room.getId());
            bill.setFeeType("PROPERTY");
            bill.setPeriodYear(dto.getYear());
            bill.setPeriodMonth(dto.getMonth());
            bill.setAmountDue(amountDue);
            bill.setDiscountAmountTotal(BigDecimal.ZERO.setScale(2));
            bill.setAmountPaid(BigDecimal.ZERO.setScale(2));
            bill.setDueDate(targetDate.with(TemporalAdjusters.lastDayOfMonth()));
            bill.setStatus("ISSUED");
            bill.setSourceType("GENERATED");
            bill.setRemark("物业费自动开单");
            billMapper.insert(bill);

            BillLine billLine = new BillLine();
            billLine.setBillId(bill.getId());
            billLine.setLineNo(1);
            billLine.setLineType("PROPERTY");
            billLine.setItemName(dto.getYear() + "-" + String.format("%02d", dto.getMonth()) + " 物业费");
            billLine.setUnitPrice(feeRule.getUnitPrice());
            billLine.setQuantity(MoneyUtils.scaleQuantity(room.getAreaM2()));
            billLine.setLineAmount(amountDue);
            billLine.setExtJson(writeJson(new LinkedHashMap<>() {{
                put("areaM2", room.getAreaM2());
                put("formula", "area * unitPrice");
            }}));
            billLineMapper.insert(billLine);
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
