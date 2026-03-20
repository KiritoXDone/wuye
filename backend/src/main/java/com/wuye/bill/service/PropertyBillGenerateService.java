package com.wuye.bill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.bill.dto.PropertyBillGenerateDTO;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.entity.BillLine;
import com.wuye.bill.entity.FeeRule;
import com.wuye.bill.mapper.BillLineMapper;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.room.mapper.GroupRoomMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.common.util.MoneyUtils;
import com.wuye.common.util.NoGenerator;
import com.wuye.room.entity.Room;
import com.wuye.room.entity.RoomType;
import com.wuye.room.mapper.RoomMapper;
import com.wuye.room.mapper.RoomTypeMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class PropertyBillGenerateService {

    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final FeeRuleService feeRuleService;
    private final BillMapper billMapper;
    private final BillLineMapper billLineMapper;
    private final GroupRoomMapper groupRoomMapper;
    private final AccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public PropertyBillGenerateService(RoomMapper roomMapper,
                                       RoomTypeMapper roomTypeMapper,
                                       FeeRuleService feeRuleService,
                                       BillMapper billMapper,
                                       BillLineMapper billLineMapper,
                                       GroupRoomMapper groupRoomMapper,
                                       AccessGuard accessGuard,
                                       ObjectMapper objectMapper) {
        this.roomMapper = roomMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.feeRuleService = feeRuleService;
        this.billMapper = billMapper;
        this.billLineMapper = billLineMapper;
        this.groupRoomMapper = groupRoomMapper;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int generate(LoginUser loginUser, PropertyBillGenerateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        LocalDate targetDate = LocalDate.of(dto.getYear(), 12, 31);
        FeeRule feeRule = feeRuleService.requireActiveRule(dto.getCommunityId(), "PROPERTY", targetDate);
        if (feeRule == null) {
            throw new BusinessException("NOT_FOUND", "未找到生效中的物业费规则", HttpStatus.NOT_FOUND);
        }
        BillingSemantics billingSemantics = resolveBillingSemantics(feeRule, dto.getYear());
        List<Room> rooms = roomMapper.listActiveByCommunity(dto.getCommunityId());
        int generated = 0;
        for (Room room : rooms) {
            Bill existed = billMapper.findByUniqueKey(room.getId(), "PROPERTY", dto.getYear(), null);
            if (existed != null) {
                if ("SKIP".equalsIgnoreCase(dto.getOverwriteStrategy())) {
                    continue;
                }
                throw new BusinessException("CONFLICT", "存在重复年度物业费账单: roomId=" + room.getId(), HttpStatus.CONFLICT);
            }
            BigDecimal areaM2 = room.getAreaM2();
            RoomType roomType = room.getRoomTypeId() == null ? null : roomTypeMapper.findById(room.getRoomTypeId());
            BigDecimal amountDue = MoneyUtils.scaleMoney(areaM2.multiply(billingSemantics.unitPrice()));
            Bill bill = new Bill();
            bill.setBillNo(NoGenerator.billNo());
            bill.setRoomId(room.getId());
            bill.setGroupId(groupRoomMapper.findPrimaryGroupIdByRoomId(room.getId()));
            bill.setFeeType("PROPERTY");
            bill.setCycleType("YEAR");
            bill.setPeriodYear(dto.getYear());
            bill.setPeriodMonth(null);
            bill.setServicePeriodStart(billingSemantics.servicePeriodStart());
            bill.setServicePeriodEnd(billingSemantics.servicePeriodEnd());
            bill.setAmountDue(amountDue);
            bill.setDiscountAmountTotal(BigDecimal.ZERO.setScale(2));
            bill.setAmountPaid(BigDecimal.ZERO.setScale(2));
            bill.setDueDate(billingSemantics.dueDate());
            bill.setStatus("ISSUED");
            bill.setSourceType("GENERATED");
            bill.setRemark(billingSemantics.remark());
            billMapper.insert(bill);

            BillLine billLine = new BillLine();
            billLine.setBillId(bill.getId());
            billLine.setLineNo(1);
            billLine.setLineType("PROPERTY");
            billLine.setItemName(dto.getYear() + " 年度物业费");
            billLine.setUnitPrice(billingSemantics.unitPrice());
            billLine.setQuantity(MoneyUtils.scaleQuantity(areaM2));
            billLine.setLineAmount(amountDue);
            billLine.setExtJson(writeJson(new LinkedHashMap<>() {{
                put("areaM2", areaM2);
                put("roomTypeId", room.getRoomTypeId());
                put("roomTypeName", roomType == null ? null : roomType.getTypeName());
                put("areaM2Source", roomType == null ? "ROOM" : "ROOM_TYPE");
                put("cycleType", billingSemantics.cycleType());
                put("servicePeriodStart", billingSemantics.servicePeriodStart());
                put("servicePeriodEnd", billingSemantics.servicePeriodEnd());
                put("unitPrice", billingSemantics.unitPrice());
                put("formula", billingSemantics.formula());
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

    private BillingSemantics resolveBillingSemantics(FeeRule feeRule, Integer year) {
        String cycleType = feeRule.getCycleType() == null ? "YEAR" : feeRule.getCycleType().trim().toUpperCase();
        if (!"YEAR".equals(cycleType)) {
            throw new BusinessException("INVALID_ARGUMENT", "物业费规则必须为按年周期", HttpStatus.BAD_REQUEST);
        }
        LocalDate servicePeriodStart = LocalDate.of(year, 1, 1);
        LocalDate servicePeriodEnd = LocalDate.of(year, 12, 31);
        return new BillingSemantics(
                cycleType,
                feeRule.getUnitPrice(),
                servicePeriodStart,
                servicePeriodEnd,
                servicePeriodEnd,
                "area * annualUnitPrice",
                "年度物业费自动开单"
        );
    }

    private record BillingSemantics(String cycleType,
                                    BigDecimal unitPrice,
                                    LocalDate servicePeriodStart,
                                    LocalDate servicePeriodEnd,
                                    LocalDate dueDate,
                                    String formula,
                                    String remark) {
    }
}
