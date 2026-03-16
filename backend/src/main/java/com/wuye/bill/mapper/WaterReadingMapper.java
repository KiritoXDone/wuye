package com.wuye.bill.mapper;

import com.wuye.bill.entity.WaterMeterReading;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
            SELECT wr.id, wr.room_id, wr.meter_id, wr.period_year, wr.period_month, wr.prev_reading, wr.curr_reading, wr.usage_amount,
                   wr.read_by_admin_id, wr.read_at, wr.photo_url, wr.remark, wr.status
            FROM water_meter_reading wr
            JOIN room r ON r.id = wr.room_id
            WHERE r.community_id = #{communityId}
              AND wr.period_year = #{year}
              AND wr.period_month = #{month}
              AND wr.status = 'NORMAL'
            ORDER BY wr.room_id
            """)
    List<WaterMeterReading> listByCommunityAndPeriod(@Param("communityId") Long communityId,
                                                     @Param("year") Integer year,
                                                     @Param("month") Integer month);
}
