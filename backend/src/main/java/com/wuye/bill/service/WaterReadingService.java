package com.wuye.bill.service;

import com.wuye.bill.dto.WaterMeterCreateDTO;
import com.wuye.bill.dto.WaterReadingCreateDTO;
import com.wuye.bill.entity.FeeRule;
import com.wuye.bill.entity.WaterMeter;
import com.wuye.bill.entity.WaterMeterReading;
import com.wuye.bill.mapper.WaterMeterMapper;
import com.wuye.bill.mapper.WaterReadingMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.common.util.MoneyUtils;
import com.wuye.room.entity.Room;
import com.wuye.room.mapper.RoomMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WaterReadingService {

    private final WaterMeterMapper waterMeterMapper;
    private final WaterReadingMapper waterReadingMapper;
    private final RoomMapper roomMapper;
    private final FeeRuleService feeRuleService;
    private final WaterBillGenerateService waterBillGenerateService;
    private final WaterUsageAlertService waterUsageAlertService;
    private final AccessGuard accessGuard;

    public WaterReadingService(WaterMeterMapper waterMeterMapper,
                               WaterReadingMapper waterReadingMapper,
                               RoomMapper roomMapper,
                               FeeRuleService feeRuleService,
                               WaterBillGenerateService waterBillGenerateService,
                               WaterUsageAlertService waterUsageAlertService,
                               AccessGuard accessGuard) {
        this.waterMeterMapper = waterMeterMapper;
        this.waterReadingMapper = waterReadingMapper;
        this.roomMapper = roomMapper;
        this.feeRuleService = feeRuleService;
        this.waterBillGenerateService = waterBillGenerateService;
        this.waterUsageAlertService = waterUsageAlertService;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public WaterMeter createOrUpdateMeter(LoginUser loginUser, WaterMeterCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Room room = roomMapper.findById(dto.getRoomId());
        if (room == null) {
            throw new BusinessException("NOT_FOUND", "房间不存在", HttpStatus.NOT_FOUND);
        }
        WaterMeter existed = waterMeterMapper.findByRoomId(dto.getRoomId());
        if (existed == null) {
            WaterMeter waterMeter = new WaterMeter();
            waterMeter.setRoomId(dto.getRoomId());
            waterMeter.setMeterNo(dto.getMeterNo());
            waterMeter.setInstallAt(dto.getInstallAt());
            waterMeter.setStatus(1);
            waterMeterMapper.insert(waterMeter);
            return waterMeter;
        }
        existed.setMeterNo(dto.getMeterNo());
        existed.setInstallAt(dto.getInstallAt());
        existed.setStatus(1);
        waterMeterMapper.update(existed);
        return existed;
    }

    @Transactional
    public Map<String, Object> createReading(LoginUser loginUser, WaterReadingCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        if (dto.getCurrReading().compareTo(dto.getPrevReading()) < 0) {
            throw new BusinessException("INVALID_ARGUMENT", "currReading 不能小于 prevReading", HttpStatus.BAD_REQUEST);
        }
        Room room = roomMapper.findById(dto.getRoomId());
        if (room == null) {
            throw new BusinessException("NOT_FOUND", "房间不存在", HttpStatus.NOT_FOUND);
        }
        WaterMeter meter = waterMeterMapper.findByRoomId(dto.getRoomId());
        if (meter == null) {
            throw new BusinessException("NOT_FOUND", "请先配置水表", HttpStatus.NOT_FOUND);
        }
        WaterMeterReading existed = waterReadingMapper.findByRoomAndPeriod(dto.getRoomId(), dto.getYear(), dto.getMonth());
        if (existed != null) {
            throw new BusinessException("CONFLICT", "同一房间同账期抄表已存在", HttpStatus.CONFLICT);
        }
        BigDecimal usageAmount = MoneyUtils.scaleQuantity(dto.getCurrReading().subtract(dto.getPrevReading()));
        WaterMeterReading reading = new WaterMeterReading();
        reading.setRoomId(dto.getRoomId());
        reading.setMeterId(meter.getId());
        reading.setPeriodYear(dto.getYear());
        reading.setPeriodMonth(dto.getMonth());
        reading.setPrevReading(MoneyUtils.scaleQuantity(dto.getPrevReading()));
        reading.setCurrReading(MoneyUtils.scaleQuantity(dto.getCurrReading()));
        reading.setUsageAmount(usageAmount);
        reading.setReadByAdminId(loginUser.accountId());
        reading.setReadAt(dto.getReadAt());
        reading.setPhotoUrl(dto.getPhotoUrl());
        reading.setRemark(dto.getRemark());
        reading.setStatus("NORMAL");
        waterReadingMapper.insert(reading);
        FeeRule feeRule = feeRuleService.requireActiveRule(room.getCommunityId(), "WATER", dto.getReadAt().toLocalDate());
        reading.setStatus(waterUsageAlertService.evaluateAndPersist(reading, feeRule));
        waterReadingMapper.updateStatus(reading.getId(), reading.getStatus());
        var generatedBill = waterBillGenerateService.generateForReading(reading, feeRule, "ERROR");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", reading.getId());
        result.put("roomId", reading.getRoomId());
        result.put("meterId", reading.getMeterId());
        result.put("periodYear", reading.getPeriodYear());
        result.put("periodMonth", reading.getPeriodMonth());
        result.put("prevReading", reading.getPrevReading());
        result.put("currReading", reading.getCurrReading());
        result.put("usageAmount", reading.getUsageAmount());
        result.put("readByAdminId", reading.getReadByAdminId());
        result.put("readAt", reading.getReadAt());
        result.put("photoUrl", reading.getPhotoUrl());
        result.put("remark", reading.getRemark());
        result.put("status", reading.getStatus());
        result.put("reading", reading);
        result.put("billId", generatedBill.getId());
        result.put("billNo", generatedBill.getBillNo());
        result.put("generatedBill", generatedBill);
        return result;
    }
}
