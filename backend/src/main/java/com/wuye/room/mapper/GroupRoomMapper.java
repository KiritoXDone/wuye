package com.wuye.room.mapper;

import com.wuye.room.vo.RoomPrimaryGroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface GroupRoomMapper {

    @Select("""
            SELECT MIN(group_id)
            FROM group_room
            WHERE room_id = #{roomId}
            """)
    Long findPrimaryGroupIdByRoomId(@Param("roomId") Long roomId);

    @Select("""
            <script>
            SELECT room_id AS roomId, MIN(group_id) AS groupId
            FROM group_room
            WHERE room_id IN
            <foreach collection="roomIds" item="roomId" open="(" separator="," close=")">
                #{roomId}
            </foreach>
            GROUP BY room_id
            </script>
            """)
    List<RoomPrimaryGroupVO> listPrimaryGroupByRoomIds(@Param("roomIds") Collection<Long> roomIds);
}
