package com.wuye.bill.service;

import com.wuye.bill.entity.FeeRule;
import com.wuye.bill.entity.WaterMeterReading;
import com.wuye.bill.entity.WaterUsageAlert;
import com.wuye.bill.mapper.WaterReadingMapper;
import com.wuye.bill.mapper.WaterUsageAlertMapper;
import com.wuye.bill.vo.WaterUsageAlertVO;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class WaterUsageAlertService {

    private final WaterUsageAlertMapper waterUsageAlertMapper;
    private final WaterReadingMapper waterReadingMapper;
    private final AccessGuard accessGuard;

    public WaterUsageAlertService(WaterUsageAlertMapper waterUsageAlertMapper,
                                  WaterReadingMapper waterReadingMapper,
                                  AccessGuard accessGuard) {
        this.waterUsageAlertMapper = waterUsageAlertMapper;
        this.waterReadingMapper = waterReadingMapper;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public String evaluateAndPersist(WaterMeterReading reading, FeeRule feeRule) {
        List<WaterUsageAlert> alerts = buildAlerts(reading, feeRule);
        if (alerts.isEmpty()) {
            return "NORMAL";
        }
        alerts.forEach(waterUsageAlertMapper::insert);
        return "ABNORMAL";
    }

    public List<WaterUsageAlertVO> listAdminAlerts(LoginUser loginUser, Integer periodYear, Integer periodMonth) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "FINANCE");
        return waterUsageAlertMapper.listAdminAlerts(periodYear, periodMonth);
    }

    private List<WaterUsageAlert> buildAlerts(WaterMeterReading reading, FeeRule feeRule) {
        List<WaterUsageAlert> alerts = new ArrayList<>();
        if (feeRule == null) {
            return alerts;
        }
        if (feeRule.getAbnormalAbsThreshold() != null
                && reading.getUsageAmount().compareTo(feeRule.getAbnormalAbsThreshold()) > 0) {
            alerts.add(buildAlert(reading, "ABS_THRESHOLD", "本期用水量超过绝对阈值", feeRule.getAbnormalAbsThreshold(), reading.getUsageAmount()));
        }
        if (feeRule.getAbnormalMultiplierThreshold() != null) {
            WaterMeterReading previous = waterReadingMapper.findPreviousReading(reading.getRoomId(), reading.getPeriodYear(), reading.getPeriodMonth());
            if (previous != null && previous.getUsageAmount() != null && previous.getUsageAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal threshold = previous.getUsageAmount().multiply(feeRule.getAbnormalMultiplierThreshold());
                if (reading.getUsageAmount().compareTo(threshold) > 0) {
                    alerts.add(buildAlert(reading, "MULTIPLIER_THRESHOLD", "本期用水量较上期异常放大", threshold, reading.getUsageAmount()));
                }
            }
        }
        return alerts;
    }

    private WaterUsageAlert buildAlert(WaterMeterReading reading,
                                       String alertCode,
                                       String alertMessage,
                                       BigDecimal thresholdValue,
                                       BigDecimal actualValue) {
        WaterUsageAlert alert = new WaterUsageAlert();
        alert.setReadingId(reading.getId());
        alert.setRoomId(reading.getRoomId());
        alert.setAlertCode(alertCode);
        alert.setAlertMessage(alertMessage);
        alert.setThresholdValue(thresholdValue);
        alert.setActualValue(actualValue);
        alert.setStatus("OPEN");
        return alert;
    }
}
