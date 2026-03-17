package com.wuye.agent.mapper;

import com.wuye.agent.vo.AgentGroupVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AgentGroupMapper {

    @Select("""
            SELECT ag.id
            FROM agent_group ag
            WHERE ag.agent_id = #{agentId}
              AND ag.group_id = #{groupId}
            """)
    Long findId(@Param("agentId") Long agentId, @Param("groupId") Long groupId);

    @Insert("""
            INSERT INTO agent_group(agent_id, group_id, permission, status)
            VALUES(#{agentId}, #{groupId}, #{permission}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(com.wuye.agent.entity.AgentGroup agentGroup);

    @Update("""
            UPDATE agent_group
            SET permission = #{permission},
                status = #{status},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int update(com.wuye.agent.entity.AgentGroup agentGroup);

    @Select("""
            SELECT ug.id AS group_id,
                   ug.group_code,
                   ug.name AS group_name,
                   ag.permission
            FROM agent_group ag
            JOIN user_group ug ON ug.id = ag.group_id
            JOIN agent_profile ap ON ap.id = ag.agent_id
            WHERE ap.account_id = #{accountId}
              AND ap.status = 1
              AND ag.status = 1
              AND ug.status = 1
            ORDER BY ug.id
            """)
    List<AgentGroupVO> listAuthorizedGroupsByAccountId(@Param("accountId") Long accountId);

    @Select("""
            SELECT ug.id
            FROM agent_group ag
            JOIN user_group ug ON ug.id = ag.group_id
            JOIN agent_profile ap ON ap.id = ag.agent_id
            WHERE ap.account_id = #{accountId}
              AND ap.status = 1
              AND ag.status = 1
              AND ug.status = 1
            ORDER BY ug.id
            """)
    List<Long> listAuthorizedGroupIdsByAccountId(@Param("accountId") Long accountId);

    @Select("""
            SELECT ug.id AS group_id,
                   ug.group_code,
                   CONCAT(ug.name, ' / ', ap.agent_code) AS group_name,
                   ag.permission
            FROM agent_group ag
            JOIN user_group ug ON ug.id = ag.group_id
            JOIN agent_profile ap ON ap.id = ag.agent_id
            WHERE ag.status = 1
              AND ug.status = 1
              AND ap.status = 1
            ORDER BY ug.id, ap.id
            """)
    List<AgentGroupVO> listAllAssignments();
}
