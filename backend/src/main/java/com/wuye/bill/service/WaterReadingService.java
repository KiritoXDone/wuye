package com.wuye.bill.service;

import com.wuye.bill.dto.WaterMeterCreateDTO;
import com.wuye.bill.dto.WaterReadingCreateDTO;
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

@Service
public class WaterReadingService {

    private final WaterMeterMapper waterMeterMapper;
    private final WaterReadingMapper waterReadingMapper;
    private final RoomMapper roomMapper;
    private final AccessGuard accessGuard;

    public WaterReadingService(WaterMeterMapper waterMeterMapper,
                               WaterReadingMapper waterReadingMapper,
                               RoomMapper roomMapper,
                               AccessGuard accessGuard) {
        this.waterMeterMapper = waterMeterMapper;
        this.waterReadingMapper = waterReadingMapper;
        this.roomMapper = roomMapper;
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
    public WaterMeterReading createReading(LoginUser loginUser, WaterReadingCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        if (dto.getCurrReading().compareTo(dto.getPrevReading()) < 0) {
            throw new BusinessException("INVALID_ARGUMENT", "currReading 不能小于 prevReading", HttpStatus.BAD_REQUEST);
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
        return reading;
    }
}
