package com.wuye.room.mapper;

import com.wuye.room.entity.Room;
import com.wuye.room.vo.RoomVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RoomMapper {

    @Select("""
            SELECT id, community_id, building_no, unit_no, room_no, room_type_id, area_m2, status
            FROM room
            WHERE community_id = #{communityId}
              AND building_no = #{buildingNo}
              AND unit_no = #{unitNo}
              AND room_no = #{roomNo}
              AND status = 1
            """)
    Room findByLocation(@Param("communityId") Long communityId,
                        @Param("buildingNo") String buildingNo,
                        @Param("unitNo") String unitNo,
                        @Param("roomNo") String roomNo);

    @Select("""
            SELECT id, community_id, building_no, unit_no, room_no, room_type_id, area_m2, status
            FROM room
            WHERE id = #{id}
            """)
    Room findById(@Param("id") Long id);

    @Select("""
            SELECT r.id AS room_id,
                   r.community_id,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   r.room_type_id,
                   r.area_m2,
                   ar.status AS binding_status,
                   ar.relation_type
            FROM account_room ar
            JOIN room r ON r.id = ar.room_id
            WHERE ar.account_id = #{accountId}
            ORDER BY r.id
            """)
    List<RoomVO> listByAccountId(@Param("accountId") Long accountId);

    @Select("""
            SELECT r.id AS room_id,
                   r.community_id,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   r.room_type_id,
                   r.area_m2,
                   ar.status AS binding_status,
                   ar.relation_type
            FROM account_room ar
            JOIN room r ON r.id = ar.room_id
            WHERE ar.account_id = #{accountId}
              AND r.id = #{roomId}
            """)
    RoomVO findOwnedRoom(@Param("accountId") Long accountId, @Param("roomId") Long roomId);

    @Select("""
            SELECT id, community_id, building_no, unit_no, room_no, room_type_id, area_m2, status
            FROM room
            WHERE community_id = #{communityId}
              AND status = 1
            ORDER BY id
            """)
    List<Room> listActiveByCommunity(@Param("communityId") Long communityId);

    @Update("""
            UPDATE room
            SET room_type_id = #{roomTypeId},
                area_m2 = #{areaM2},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateAdminRoom(Room room);
}
