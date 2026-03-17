package com.wuye.agent.mapper;

import com.wuye.agent.entity.UserGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserGroupMapper {

    @Select("""
            SELECT id, group_code, name, scope_type, community_id, status
            FROM user_group
            WHERE id = #{id}
            """)
    UserGroup findById(@Param("id") Long id);

    @Select("""
            SELECT id, group_code, name, scope_type, community_id, status
            FROM user_group
            WHERE group_code = #{groupCode}
            """)
    UserGroup findByGroupCode(@Param("groupCode") String groupCode);
}
