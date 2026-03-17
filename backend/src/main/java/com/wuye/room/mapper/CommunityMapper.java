package com.wuye.room.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommunityMapper {

    @Select("""
            SELECT id
            FROM community
            WHERE community_code = #{communityCode}
              AND status = 1
            """)
    Long findIdByCommunityCode(@Param("communityCode") String communityCode);
}
