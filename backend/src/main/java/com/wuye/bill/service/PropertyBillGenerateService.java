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
import com.wuye.room.entity.RoomType;
import com.wuye.room.mapper.GroupRoomMapper;
import com.wuye.room.mapper.RoomMapper;
import com.wuye.room.mapper.RoomTypeMapper;
import com.wuye.room.vo.RoomPrimaryGroupVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        if (rooms.isEmpty()) {
            return 0;
        }

        List<Long> roomIds = rooms.stream().map(Room::getId).toList();
        Set<Long> roomTypeIds = extractRoomTypeIds(rooms);
        Map<Long, RoomType> roomTypeMap = roomTypeIds.isEmpty()
                ? Map.of()
                : roomTypeMapper.listByIds(roomTypeIds).stream()
                        .collect(Collectors.toMap(RoomType::getId, roomType -> roomType));
        Map<Long, Long> primaryGroupMap = groupRoomMapper.listPrimaryGroupByRoomIds(roomIds).stream()
                .collect(Collectors.toMap(RoomPrimaryGroupVO::getRoomId, RoomPrimaryGroupVO::getGroupId));
        Map<Long, Bill> activeBillMap = toRoomBillMap(billMapper.listActiveByRoomIdsAndPeriod(roomIds, "PROPERTY", dto.getYear(), null));
        Map<Long, Bill> anyBillMap = toRoomBillMap(billMapper.listByRoomIdsAndPeriod(roomIds, "PROPERTY", dto.getYear(), null));

        int generated = 0;
        for (Room room : rooms) {
            Bill existed = activeBillMap.get(room.getId());
            if (existed != null) {
                if ("SKIP".equalsIgnoreCase(dto.getOverwriteStrategy())) {
                    continue;
                }
                throw new BusinessException("CONFLICT", "存在重复年度物业费账单: roomId=" + room.getId(), HttpStatus.CONFLICT);
            }

            BigDecimal areaM2 = room.getAreaM2();
            RoomType roomType = room.getRoomTypeId() == null ? null : roomTypeMap.get(room.getRoomTypeId());
            BigDecimal amountDue = MoneyUtils.scaleMoney(areaM2.multiply(billingSemantics.unitPrice()));
            Bill bill = anyBillMap.get(room.getId());
            if (bill == null) {
                bill = new Bill();
                bill.setBillNo(NoGenerator.billNo());
                bill.setRoomId(room.getId());
                bill.setGroupId(primaryGroupMap.get(room.getId()));
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
                anyBillMap.put(room.getId(), bill);
            } else if ("CANCELLED".equals(bill.getStatus())) {
                bill.setBillNo(NoGenerator.billNo());
                bill.setGroupId(primaryGroupMap.get(room.getId()));
                bill.setCycleType("YEAR");
                bill.setPeriodMonth(null);
                bill.setServicePeriodStart(billingSemantics.servicePeriodStart());
                bill.setServicePeriodEnd(billingSemantics.servicePeriodEnd());
                bill.setAmountDue(amountDue);
                bill.setDiscountAmountTotal(BigDecimal.ZERO.setScale(2));
                bill.setAmountPaid(BigDecimal.ZERO.setScale(2));
                bill.setDueDate(billingSemantics.dueDate());
                bill.setStatus("ISSUED");
                bill.setPaidAt(null);
                bill.setCancelledAt(null);
                bill.setSourceType("GENERATED");
                bill.setRemark(billingSemantics.remark());
                if (billMapper.reissueCancelled(bill) == 0) {
                    throw new BusinessException("CONFLICT", "账单状态已变化，无法重新开单: roomId=" + room.getId(), HttpStatus.CONFLICT);
                }
                billLineMapper.deleteByBillId(bill.getId());
            } else {
                throw new BusinessException("CONFLICT", "存在重复年度物业费账单: roomId=" + room.getId(), HttpStatus.CONFLICT);
            }

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

    private Set<Long> extractRoomTypeIds(List<Room> rooms) {
        return rooms.stream()
                .map(Room::getRoomTypeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Long, Bill> toRoomBillMap(List<Bill> bills) {
        Map<Long, Bill> result = new HashMap<>();
        for (Bill bill : bills) {
            result.putIfAbsent(bill.getRoomId(), bill);
        }
        return result;
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
