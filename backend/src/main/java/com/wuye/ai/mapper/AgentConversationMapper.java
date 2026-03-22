package com.wuye.ai.mapper;

import com.wuye.ai.entity.AgentConversationEntity;
import com.wuye.ai.vo.AgentConversationListItemVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AgentConversationMapper {

    @Insert("""
            INSERT INTO ai_agent_conversation(session_id, operator_id, title, context_json, last_message_preview, message_count, status)
            VALUES(#{sessionId}, #{operatorId}, #{title}, #{contextJson}, #{lastMessagePreview}, #{messageCount}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentConversationEntity entity);

    @Select("""
            SELECT id, session_id, operator_id, title, context_json, last_message_preview, message_count, status, created_at, updated_at
            FROM ai_agent_conversation
            WHERE session_id = #{sessionId}
            """)
    AgentConversationEntity findBySessionId(@Param("sessionId") String sessionId);

    @Update("""
            UPDATE ai_agent_conversation
            SET title = #{title},
                context_json = #{contextJson},
                last_message_preview = #{lastMessagePreview},
                message_count = #{messageCount},
                status = #{status},
                updated_at = CURRENT_TIMESTAMP
            WHERE session_id = #{sessionId}
            """)
    int updateConversation(AgentConversationEntity entity);

    @Select("""
            SELECT session_id, title, last_message_preview, message_count, created_at, updated_at
            FROM ai_agent_conversation
            WHERE operator_id = #{operatorId}
            ORDER BY updated_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<AgentConversationListItemVO> listPage(@Param("operatorId") Long operatorId,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    @Select("""
            SELECT COUNT(1)
            FROM ai_agent_conversation
            WHERE operator_id = #{operatorId}
            """)
    long countByOperatorId(@Param("operatorId") Long operatorId);
}
