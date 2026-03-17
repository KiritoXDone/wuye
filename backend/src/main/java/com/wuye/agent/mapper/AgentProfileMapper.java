package com.wuye.agent.mapper;

import com.wuye.agent.entity.AgentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgentProfileMapper {

    @Select("""
            SELECT id, account_id, agent_code, org_name, org_unit_id, status
            FROM agent_profile
            WHERE account_id = #{accountId}
              AND status = 1
            """)
    AgentProfile findByAccountId(@Param("accountId") Long accountId);

    @Select("""
            SELECT id, account_id, agent_code, org_name, org_unit_id, status
            FROM agent_profile
            WHERE agent_code = #{agentCode}
            """)
    AgentProfile findByAgentCode(@Param("agentCode") String agentCode);
}
