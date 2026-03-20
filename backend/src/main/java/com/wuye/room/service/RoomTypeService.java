package com.wuye.room.service;

import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.room.dto.RoomTypeUpsertDTO;
import com.wuye.room.entity.RoomType;
import com.wuye.room.mapper.RoomTypeMapper;
import com.wuye.room.vo.RoomTypeVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomTypeService {

    private final RoomTypeMapper roomTypeMapper;
    private final AccessGuard accessGuard;

    public RoomTypeService(RoomTypeMapper roomTypeMapper, AccessGuard accessGuard) {
        this.roomTypeMapper = roomTypeMapper;
        this.accessGuard = accessGuard;
    }

    public List<RoomTypeVO> list(LoginUser loginUser, Long communityId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return roomTypeMapper.listByCommunityId(communityId);
    }

    @Transactional
    public RoomTypeVO create(LoginUser loginUser, RoomTypeUpsertDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        RoomType roomType = new RoomType();
        roomType.setCommunityId(dto.getCommunityId());
        roomType.setTypeCode(dto.getTypeCode().trim());
        roomType.setTypeName(dto.getTypeName().trim());
        roomType.setAreaM2(dto.getAreaM2());
        roomType.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        roomTypeMapper.insert(roomType);
        return requireOne(roomType.getId());
    }

    @Transactional
    public RoomTypeVO update(LoginUser loginUser, Long roomTypeId, RoomTypeUpsertDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        RoomType existed = roomTypeMapper.findById(roomTypeId);
        if (existed == null) {
            throw new BusinessException("NOT_FOUND", "户型不存在", HttpStatus.NOT_FOUND);
        }
        existed.setCommunityId(dto.getCommunityId());
        existed.setTypeCode(dto.getTypeCode().trim());
        existed.setTypeName(dto.getTypeName().trim());
        existed.setAreaM2(dto.getAreaM2());
        existed.setStatus(dto.getStatus() == null ? existed.getStatus() : dto.getStatus());
        roomTypeMapper.update(existed);
        return requireOne(roomTypeId);
    }

    private RoomTypeVO requireOne(Long roomTypeId) {
        RoomType roomType = roomTypeMapper.findById(roomTypeId);
        if (roomType == null) {
            throw new BusinessException("NOT_FOUND", "户型不存在", HttpStatus.NOT_FOUND);
        }
        RoomTypeVO vo = new RoomTypeVO();
        vo.setId(roomType.getId());
        vo.setCommunityId(roomType.getCommunityId());
        vo.setTypeCode(roomType.getTypeCode());
        vo.setTypeName(roomType.getTypeName());
        vo.setAreaM2(roomType.getAreaM2());
        vo.setStatus(roomType.getStatus());
        return vo;
    }
}
