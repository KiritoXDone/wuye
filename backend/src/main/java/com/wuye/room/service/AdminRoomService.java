package com.wuye.room.service;

import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.room.dto.AdminRoomUpdateDTO;
import com.wuye.room.entity.Room;
import com.wuye.room.entity.RoomType;
import com.wuye.room.mapper.RoomMapper;
import com.wuye.room.mapper.RoomTypeMapper;
import com.wuye.room.vo.AdminRoomVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminRoomService {

    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final AccessGuard accessGuard;

    public AdminRoomService(RoomMapper roomMapper, RoomTypeMapper roomTypeMapper, AccessGuard accessGuard) {
        this.roomMapper = roomMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.accessGuard = accessGuard;
    }

    public List<AdminRoomVO> list(LoginUser loginUser, Long communityId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return roomMapper.listActiveByCommunity(communityId).stream().map(this::toAdminVO).collect(Collectors.toList());
    }

    @Transactional
    public AdminRoomVO update(LoginUser loginUser, Long roomId, AdminRoomUpdateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Room room = roomMapper.findById(roomId);
        if (room == null) {
            throw new BusinessException("NOT_FOUND", "房间不存在", HttpStatus.NOT_FOUND);
        }
        RoomType roomType = null;
        if (dto.getRoomTypeId() != null) {
            roomType = roomTypeMapper.findById(dto.getRoomTypeId());
            if (roomType == null) {
                throw new BusinessException("NOT_FOUND", "户型不存在", HttpStatus.NOT_FOUND);
            }
        }
        room.setRoomTypeId(dto.getRoomTypeId());
        room.setAreaM2(dto.getAreaM2());
        roomMapper.updateAdminRoom(room);
        return toAdminVO(roomMapper.findById(roomId));
    }

    private AdminRoomVO toAdminVO(Room room) {
        AdminRoomVO vo = new AdminRoomVO();
        vo.setId(room.getId());
        vo.setCommunityId(room.getCommunityId());
        vo.setBuildingNo(room.getBuildingNo());
        vo.setUnitNo(room.getUnitNo());
        vo.setRoomNo(room.getRoomNo());
        vo.setRoomTypeId(room.getRoomTypeId());
        vo.setAreaM2(room.getAreaM2());
        vo.setStatus(room.getStatus());
        if (room.getRoomTypeId() != null) {
            RoomType roomType = roomTypeMapper.findById(room.getRoomTypeId());
            vo.setRoomTypeName(roomType == null ? null : roomType.getTypeName());
        }
        return vo;
    }
}
