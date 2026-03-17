package com.wuye.bill.mapper;

import com.wuye.bill.entity.WaterUsageAlert;
import com.wuye.bill.vo.WaterUsageAlertVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WaterUsageAlertMapper {

    @Insert("""
            INSERT INTO water_usage_alert(reading_id, room_id, alert_code, alert_message, threshold_value, actual_value, status)
            VALUES(#{readingId}, #{roomId}, #{alertCode}, #{alertMessage}, #{thresholdValue}, #{actualValue}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WaterUsageAlert alert);

    @Select("""
            SELECT wa.id,
                   wa.reading_id,
                   wa.room_id,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   wa.alert_code,
                   wa.alert_message,
                   wa.threshold_value,
                   wa.actual_value,
                   wa.status,
                   wa.created_at
            FROM water_usage_alert wa
            JOIN room r ON r.id = wa.room_id
            JOIN water_meter_reading wr ON wr.id = wa.reading_id
            WHERE (#{periodYear} IS NULL OR wr.period_year = #{periodYear})
              AND (#{periodMonth} IS NULL OR wr.period_month = #{periodMonth})
            ORDER BY wa.id ASC
            """)
    List<WaterUsageAlertVO> listAdminAlerts(@Param("periodYear") Integer periodYear,
                                            @Param("periodMonth") Integer periodMonth);
}
