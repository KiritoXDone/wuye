package com.wuye.ai.service;

import com.wuye.ai.vo.AgentConversationListItemVO;
import com.wuye.ai.vo.AgentConversationVO;
import com.wuye.common.api.PageResponse;

import java.util.Map;
import java.util.Optional;

public class NoopAgentConversationCacheService extends AgentConversationCacheService {

    public NoopAgentConversationCacheService() {
        super(null, null);
    }

    @Override
    public Optional<AgentConversationVO> getConversationDetail(String sessionId) {
        return Optional.empty();
    }

    @Override
    public void cacheConversationDetail(String sessionId, AgentConversationVO conversation) {
    }

    @Override
    public void evictConversationDetail(String sessionId) {
    }

    @Override
    public Optional<PageResponse<AgentConversationListItemVO>> getConversationList(Long operatorId, int pageNo, int pageSize) {
        return Optional.empty();
    }

    @Override
    public void cacheConversationList(Long operatorId, int pageNo, int pageSize, PageResponse<AgentConversationListItemVO> page) {
    }

    @Override
    public void evictConversationList(Long operatorId, int pageNo, int pageSize) {
    }

    @Override
    public void saveCommandConfirmation(String commandId, Map<String, Object> payload) {
    }

    @Override
    public Optional<Map<String, Object>> getCommandConfirmation(String commandId) {
        return Optional.empty();
    }

    @Override
    public void deleteCommandConfirmation(String commandId) {
    }
}
