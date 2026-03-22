package com.wuye.room.mapper;

import com.wuye.room.dto.AdminRoomListQuery;
import com.wuye.room.entity.Room;
import com.wuye.room.vo.RoomVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Set;

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
                   c.name AS community_name,
                   r.building_no,
                   r.unit_no,
                   r.room_no,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   r.room_type_id,
                   r.area_m2,
                   ar.status AS binding_status
            FROM account_room ar
            JOIN room r ON r.id = ar.room_id
            JOIN community c ON c.id = r.community_id
            WHERE ar.account_id = #{accountId}
            ORDER BY r.id
            """)
    List<RoomVO> listByAccountId(@Param("accountId") Long accountId);

    @Select("""
            SELECT r.id AS room_id,
                   r.community_id,
                   c.name AS community_name,
                   r.building_no,
                   r.unit_no,
                   r.room_no,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   r.room_type_id,
                   r.area_m2,
                   ar.status AS binding_status
            FROM account_room ar
            JOIN room r ON r.id = ar.room_id
            JOIN community c ON c.id = r.community_id
            WHERE ar.account_id = #{accountId}
              AND r.id = #{roomId}
            """)
    RoomVO findOwnedRoom(@Param("accountId") Long accountId, @Param("roomId") Long roomId);

    @Select("""
            <script>
            SELECT id, community_id, building_no, unit_no, room_no, room_type_id, area_m2, status
            FROM room
            WHERE community_id = #{communityId}
              AND status = 1
              <if test='buildingNo != null and buildingNo != ""'>
                AND building_no = #{buildingNo}
              </if>
              <if test='unitNo != null and unitNo != ""'>
                AND unit_no = #{unitNo}
              </if>
              <if test='roomNo != null and roomNo != ""'>
                AND room_no = #{roomNo}
              </if>
              <if test='roomNoKeyword != null and roomNoKeyword != ""'>
                AND room_no LIKE CONCAT('%', #{roomNoKeyword}, '%')
              </if>
              <if test='roomSuffix != null and roomSuffix != ""'>
                AND room_no LIKE CONCAT('%', #{roomSuffix})
              </if>
              <if test='roomTypeId != null'>
                AND room_type_id = #{roomTypeId}
              </if>
            ORDER BY building_no ASC, unit_no ASC, room_no ASC, id ASC
            </script>
            """)
    List<Room> listByAdminQuery(AdminRoomListQuery query);

    @Select("""
            <script>
            SELECT id, community_id, building_no, unit_no, room_no, room_type_id, area_m2, status
            FROM room
            WHERE community_id = #{communityId}
              AND status = 1
              AND id IN
              <foreach collection='roomIds' item='roomId' open='(' separator=',' close=')'>
                #{roomId}
              </foreach>
            ORDER BY building_no ASC, unit_no ASC, room_no ASC, id ASC
            </script>
            """)
    List<Room> listByIds(@Param("communityId") Long communityId, @Param("roomIds") Set<Long> roomIds);

    @Select("""
            SELECT id, community_id, building_no, unit_no, room_no, room_type_id, area_m2, status
            FROM room
            WHERE community_id = #{communityId}
              AND status = 1
            ORDER BY id
            """)
    List<Room> listActiveByCommunity(@Param("communityId") Long communityId);

    @Insert("""
            INSERT INTO room(community_id, building_no, unit_no, room_no, room_type_id, area_m2, status)
            VALUES(#{communityId}, #{buildingNo}, #{unitNo}, #{roomNo}, #{roomTypeId}, #{areaM2}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Room room);

    @Update("""
            UPDATE room
            SET room_type_id = #{roomTypeId},
                area_m2 = #{areaM2},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateAdminRoom(Room room);

    @Update("""
            UPDATE room
            SET status = 0,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{roomId}
              AND status = 1
            """)
    int deleteById(@Param("roomId") Long roomId);

    @Select("""
            SELECT COUNT(1)
            FROM water_meter
            WHERE room_id = #{roomId}
            """)
    long countWaterMeters(@Param("roomId") Long roomId);

    @Select("""
            SELECT DISTINCT building_no
            FROM room
            WHERE community_id = #{communityId}
              AND status = 1
            ORDER BY building_no ASC
            """)
    List<String> listBuildingsByCommunity(@Param("communityId") Long communityId);

    @Select("""
            SELECT DISTINCT unit_no
            FROM room
            WHERE community_id = #{communityId}
              AND building_no = #{buildingNo}
              AND status = 1
            ORDER BY unit_no ASC
            """)
    List<String> listUnitsByCommunityAndBuilding(@Param("communityId") Long communityId,
                                                 @Param("buildingNo") String buildingNo);

    @Select("""
            SELECT id AS room_id,
                   room_no,
                   CONCAT(building_no, '-', unit_no, '-', room_no) AS room_label,
                   area_m2
            FROM room
            WHERE community_id = #{communityId}
              AND building_no = #{buildingNo}
              AND unit_no = #{unitNo}
              AND status = 1
            ORDER BY room_no ASC, id ASC
            """)
    List<com.wuye.room.vo.ResidentRoomOptionVO> listRoomOptions(@Param("communityId") Long communityId,
                                                                @Param("buildingNo") String buildingNo,
                                                                @Param("unitNo") String unitNo);
}
