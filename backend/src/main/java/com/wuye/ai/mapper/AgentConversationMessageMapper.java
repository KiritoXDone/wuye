package com.wuye.ai.mapper;

import com.wuye.ai.entity.AgentConversationMessageEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentConversationMessageMapper {

    @Insert("""
            INSERT INTO ai_agent_conversation_message(message_id, session_id, seq_no, role, mode, content, action, command_id, risk_level, confirmation_required, payload_json)
            VALUES(#{messageId}, #{sessionId}, #{seqNo}, #{role}, #{mode}, #{content}, #{action}, #{commandId}, #{riskLevel}, #{confirmationRequired}, #{payloadJson})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentConversationMessageEntity entity);

    @Select("""
            SELECT id, message_id, session_id, seq_no, role, mode, content, action, command_id, risk_level, confirmation_required, payload_json, created_at
            FROM ai_agent_conversation_message
            WHERE session_id = #{sessionId}
            ORDER BY seq_no ASC, id ASC
            """)
    List<AgentConversationMessageEntity> listBySessionId(@Param("sessionId") String sessionId);

    @Select("""
            SELECT COALESCE(MAX(seq_no), 0)
            FROM ai_agent_conversation_message
            WHERE session_id = #{sessionId}
            """)
    Integer maxSeqNoBySessionId(@Param("sessionId") String sessionId);
}
