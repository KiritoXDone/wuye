package com.wuye.room.mapper;

import com.wuye.room.entity.Community;
import com.wuye.room.vo.AdminCommunityVO;
import com.wuye.room.vo.ResidentCommunityOptionVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommunityMapper {

    @Select("""
            SELECT id
            FROM community
            WHERE community_code = #{communityCode}
            """)
    Long findIdByCommunityCode(@Param("communityCode") String communityCode);

    @Select("""
            SELECT c.id,
                   c.community_code,
                   c.name,
                   c.status,
                   COALESCE(rt.room_type_count, 0) AS room_type_count,
                   COALESCE(r.room_count, 0) AS room_count
            FROM community c
            LEFT JOIN (
                SELECT community_id, COUNT(1) AS room_type_count
                FROM room_type
                WHERE status = 1
                GROUP BY community_id
            ) rt ON rt.community_id = c.id
            LEFT JOIN (
                SELECT community_id, COUNT(1) AS room_count
                FROM room
                WHERE status = 1
                GROUP BY community_id
            ) r ON r.community_id = c.id
            ORDER BY c.id ASC
            """)
    List<AdminCommunityVO> listAdminCommunities();

    @Select("""
            SELECT id, community_code, name, status
            FROM community
            WHERE id = #{id}
            """)
    Community findById(@Param("id") Long id);

    @Select("""
            SELECT id, community_code, name, status
            FROM community
            WHERE community_code = #{communityCode}
              AND id <> #{excludeId}
            LIMIT 1
            """)
    Community findByCodeExcludingId(@Param("communityCode") String communityCode, @Param("excludeId") Long excludeId);

    @Insert("""
            INSERT INTO community(community_code, name)
            VALUES(#{communityCode}, #{name})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Community community);

    @Update("""
            UPDATE community
            SET community_code = #{communityCode},
                name = #{name},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int update(Community community);

    @Update("""
            UPDATE community
            SET status = 0,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{communityId}
              AND status = 1
            """)
    int deleteById(@Param("communityId") Long communityId);

    @Select("""
            SELECT COUNT(1)
            FROM room_type
            WHERE community_id = #{communityId}
            """)
    long countRoomTypes(@Param("communityId") Long communityId);

    @Select("""
            SELECT COUNT(1)
            FROM room
            WHERE community_id = #{communityId}
            """)
    long countRooms(@Param("communityId") Long communityId);

    @Select("""
            SELECT id AS community_id,
                   name AS community_name
            FROM community
            WHERE status = 1
            ORDER BY id ASC
            """)
    List<ResidentCommunityOptionVO> listActiveCommunityOptions();
}
