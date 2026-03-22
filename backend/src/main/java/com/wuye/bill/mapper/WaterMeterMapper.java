package com.wuye.bill.mapper;

import com.wuye.bill.entity.WaterMeter;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WaterMeterMapper {

    @Select("""
            SELECT id, room_id, meter_no, install_at, status
            FROM water_meter
            WHERE room_id = #{roomId}
            """)
    WaterMeter findByRoomId(@Param("roomId") Long roomId);

    @Insert("""
            INSERT INTO water_meter(room_id, meter_no, install_at, status)
            VALUES(#{roomId}, #{meterNo}, #{installAt}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WaterMeter waterMeter);

    @Update("""
            UPDATE water_meter
            SET meter_no = #{meterNo}, install_at = #{installAt}, status = #{status}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int update(WaterMeter waterMeter);

    @Delete("""
            DELETE FROM water_meter
            WHERE room_id = #{roomId}
            """)
    int deleteByRoomId(@Param("roomId") Long roomId);
}
