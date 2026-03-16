package com.wuye.room.mapper;

import com.wuye.room.entity.AccountRoom;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface AccountRoomMapper {

    @Select("""
            SELECT id, account_id, room_id, relation_type, status, bind_source, confirmed_at, remark
            FROM account_room
            WHERE account_id = #{accountId} AND room_id = #{roomId}
            """)
    AccountRoom findByAccountAndRoom(@Param("accountId") Long accountId, @Param("roomId") Long roomId);

    @Select("""
            SELECT id, account_id, room_id, relation_type, status, bind_source, confirmed_at, remark
            FROM account_room
            WHERE room_id = #{roomId} AND status = #{status}
            ORDER BY id DESC
            LIMIT 1
            """)
    AccountRoom findLatestByRoomAndStatus(@Param("roomId") Long roomId, @Param("status") String status);

    @Insert("""
            INSERT INTO account_room(account_id, room_id, relation_type, status, bind_source, confirmed_at, remark)
            VALUES(#{accountId}, #{roomId}, #{relationType}, #{status}, #{bindSource}, #{confirmedAt}, #{remark})
            """)
    int insert(AccountRoom accountRoom);

    @Update("""
            UPDATE account_room
            SET relation_type = #{relationType},
                status = #{status},
                bind_source = #{bindSource},
                confirmed_at = #{confirmedAt},
                remark = #{remark},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int update(AccountRoom accountRoom);

    @Select("""
            SELECT COUNT(1)
            FROM account_room
            WHERE account_id = #{accountId} AND room_id = #{roomId} AND status = 'ACTIVE'
            """)
    long countActiveBinding(@Param("accountId") Long accountId, @Param("roomId") Long roomId);

    @Update("""
            UPDATE account_room
            SET status = #{status},
                confirmed_at = #{confirmedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE account_id = #{accountId} AND room_id = #{roomId}
            """)
    int updateStatus(@Param("accountId") Long accountId,
                     @Param("roomId") Long roomId,
                     @Param("status") String status,
                     @Param("confirmedAt") LocalDateTime confirmedAt);
}
