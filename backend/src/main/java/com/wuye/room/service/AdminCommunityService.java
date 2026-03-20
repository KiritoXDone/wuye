package com.wuye.room.service;

import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.room.dto.CommunityUpsertDTO;
import com.wuye.room.entity.Community;
import com.wuye.room.mapper.CommunityMapper;
import com.wuye.room.vo.AdminCommunityVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminCommunityService {

    private final CommunityMapper communityMapper;
    private final AccessGuard accessGuard;

    public AdminCommunityService(CommunityMapper communityMapper, AccessGuard accessGuard) {
        this.communityMapper = communityMapper;
        this.accessGuard = accessGuard;
    }

    public List<AdminCommunityVO> list(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return communityMapper.listAdminCommunities();
    }

    @Transactional
    public AdminCommunityVO create(LoginUser loginUser, CommunityUpsertDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        ensureCodeUnique(dto.getCommunityCode(), null);
        Community community = new Community();
        community.setCommunityCode(dto.getCommunityCode().trim());
        community.setName(dto.getName().trim());
        community.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        communityMapper.insert(community);
        return requireOne(community.getId());
    }

    @Transactional
    public AdminCommunityVO update(LoginUser loginUser, Long communityId, CommunityUpsertDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Community existed = requireEntity(communityId);
        ensureCodeUnique(dto.getCommunityCode(), communityId);
        existed.setCommunityCode(dto.getCommunityCode().trim());
        existed.setName(dto.getName().trim());
        existed.setStatus(dto.getStatus() == null ? existed.getStatus() : dto.getStatus());
        communityMapper.update(existed);
        return requireOne(communityId);
    }

    @Transactional
    public AdminCommunityVO disable(LoginUser loginUser, Long communityId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Community existed = requireEntity(communityId);
        existed.setStatus(0);
        communityMapper.update(existed);
        return requireOne(communityId);
    }

    private void ensureCodeUnique(String communityCode, Long excludeId) {
        Community duplicated = communityMapper.findByCodeExcludingId(communityCode.trim(), excludeId == null ? -1L : excludeId);
        if (duplicated != null) {
            throw new BusinessException("COMMUNITY_CODE_DUPLICATED", "小区编码已存在", HttpStatus.BAD_REQUEST);
        }
    }

    private Community requireEntity(Long communityId) {
        Community community = communityMapper.findById(communityId);
        if (community == null) {
            throw new BusinessException("NOT_FOUND", "小区不存在", HttpStatus.NOT_FOUND);
        }
        return community;
    }

    private AdminCommunityVO requireOne(Long communityId) {
        return listInternal().stream()
                .filter(item -> item.getId().equals(communityId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "小区不存在", HttpStatus.NOT_FOUND));
    }

    private List<AdminCommunityVO> listInternal() {
        return communityMapper.listAdminCommunities();
    }
}
