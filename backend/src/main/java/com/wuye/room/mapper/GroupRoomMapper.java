package com.wuye.room.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GroupRoomMapper {

    @Select("""
            SELECT MIN(group_id)
            FROM group_room
            WHERE room_id = #{roomId}
            """)
    Long findPrimaryGroupIdByRoomId(@Param("roomId") Long roomId);
}
