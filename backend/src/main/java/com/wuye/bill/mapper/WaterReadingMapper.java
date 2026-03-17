package com.wuye.bill.mapper;

import com.wuye.bill.entity.WaterMeterReading;
import com.wuye.bill.vo.AdminWaterReadingVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WaterReadingMapper {

    @Insert("""
            INSERT INTO water_meter_reading(room_id, meter_id, period_year, period_month, prev_reading, curr_reading, usage_amount,
                                           read_by_admin_id, read_at, photo_url, remark, status)
            VALUES(#{roomId}, #{meterId}, #{periodYear}, #{periodMonth}, #{prevReading}, #{currReading}, #{usageAmount},
                   #{readByAdminId}, #{readAt}, #{photoUrl}, #{remark}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WaterMeterReading reading);

    @Select("""
            SELECT id, room_id, meter_id, period_year, period_month, prev_reading, curr_reading, usage_amount,
                   read_by_admin_id, read_at, photo_url, remark, status
            FROM water_meter_reading
            WHERE room_id = #{roomId} AND period_year = #{year} AND period_month = #{month}
            """)
    WaterMeterReading findByRoomAndPeriod(@Param("roomId") Long roomId,
                                          @Param("year") Integer year,
                                          @Param("month") Integer month);

    @Select("""
            SELECT id, room_id, meter_id, period_year, period_month, prev_reading, curr_reading, usage_amount,
                   read_by_admin_id, read_at, photo_url, remark, status
            FROM water_meter_reading
            WHERE room_id = #{roomId}
              AND (period_year < #{year} OR (period_year = #{year} AND period_month < #{month}))
            ORDER BY period_year DESC, period_month DESC
            LIMIT 1
            """)
    WaterMeterReading findPreviousReading(@Param("roomId") Long roomId,
                                          @Param("year") Integer year,
                                          @Param("month") Integer month);

    @Select("""
            SELECT wr.id, wr.room_id, wr.meter_id, wr.period_year, wr.period_month, wr.prev_reading, wr.curr_reading, wr.usage_amount,
                   wr.read_by_admin_id, wr.read_at, wr.photo_url, wr.remark, wr.status
            FROM water_meter_reading wr
            JOIN room r ON r.id = wr.room_id
            WHERE r.community_id = #{communityId}
              AND wr.period_year = #{year}
              AND wr.period_month = #{month}
              AND wr.status IN ('NORMAL', 'ABNORMAL')
            ORDER BY wr.room_id
            """)
    List<WaterMeterReading> listByCommunityAndPeriod(@Param("communityId") Long communityId,
                                                     @Param("year") Integer year,
                                                     @Param("month") Integer month);

    @Select("""
            SELECT wr.id,
                   wr.room_id,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   wr.period_year,
                   wr.period_month,
                   wr.prev_reading,
                   wr.curr_reading,
                   wr.usage_amount,
                   wr.read_at,
                   wr.status
            FROM water_meter_reading wr
            JOIN room r ON r.id = wr.room_id
            WHERE (#{periodYear} IS NULL OR wr.period_year = #{periodYear})
              AND (#{periodMonth} IS NULL OR wr.period_month = #{periodMonth})
            ORDER BY wr.period_year DESC, wr.period_month DESC, wr.room_id ASC
            """)
    List<AdminWaterReadingVO> listAdminReadings(@Param("periodYear") Integer periodYear,
                                                @Param("periodMonth") Integer periodMonth);

    @Update("""
            UPDATE water_meter_reading
            SET status = #{status},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
