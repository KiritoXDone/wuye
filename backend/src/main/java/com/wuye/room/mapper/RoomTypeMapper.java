package com.wuye.room.mapper;

import com.wuye.room.entity.RoomType;
import com.wuye.room.vo.RoomTypeVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RoomTypeMapper {

    @Insert("""
            INSERT INTO room_type(community_id, type_code, type_name, area_m2, status)
            VALUES(#{communityId}, #{typeCode}, #{typeName}, #{areaM2}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RoomType roomType);

    @Update("""
            UPDATE room_type
            SET type_code = #{typeCode},
                type_name = #{typeName},
                area_m2 = #{areaM2},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int update(RoomType roomType);

    @Update("""
            UPDATE room_type
            SET status = 0,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{roomTypeId}
              AND status = 1
            """)
    int deleteById(@Param("roomTypeId") Long roomTypeId);

    @Select("""
            SELECT id, community_id, type_code, type_name, area_m2, status
            FROM room_type
            WHERE id = #{id}
            """)
    RoomType findById(@Param("id") Long id);

    @Select("""
            SELECT id, community_id, type_code, type_name, area_m2, status
            FROM room_type
            WHERE community_id = #{communityId}
              AND status = 1
            ORDER BY id ASC
            """)
    List<RoomTypeVO> listByCommunityId(@Param("communityId") Long communityId);
}
