package com.wuye.room.service;

import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.room.dto.RoomBindApplyDTO;
import com.wuye.room.entity.AccountRoom;
import com.wuye.room.entity.Room;
import com.wuye.room.mapper.AccountRoomMapper;
import com.wuye.room.mapper.RoomMapper;
import com.wuye.room.vo.RoomVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoomBindingService {

    private final RoomMapper roomMapper;
    private final AccountRoomMapper accountRoomMapper;
    private final AccessGuard accessGuard;

    public RoomBindingService(RoomMapper roomMapper,
                              AccountRoomMapper accountRoomMapper,
                              AccessGuard accessGuard) {
        this.roomMapper = roomMapper;
        this.accountRoomMapper = accountRoomMapper;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public RoomVO applyBinding(LoginUser loginUser, RoomBindApplyDTO dto) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        Room room = roomMapper.findByLocation(dto.getCommunityId(), dto.getBuildingNo(), dto.getUnitNo(), dto.getRoomNo());
        if (room == null) {
            throw new BusinessException("NOT_FOUND", "房间不存在", HttpStatus.NOT_FOUND);
        }
        AccountRoom existed = accountRoomMapper.findByAccountAndRoom(loginUser.accountId(), room.getId());
        if (existed == null) {
            AccountRoom accountRoom = new AccountRoom();
            accountRoom.setAccountId(loginUser.accountId());
            accountRoom.setRoomId(room.getId());
            accountRoom.setRelationType(dto.getRelationType());
            accountRoom.setStatus("PENDING");
            accountRoom.setBindSource("SELF");
            accountRoom.setRemark(dto.getApplyRemark());
            accountRoomMapper.insert(accountRoom);
        } else {
            existed.setRelationType(dto.getRelationType());
            existed.setStatus("PENDING");
            existed.setBindSource("SELF");
            existed.setConfirmedAt(null);
            existed.setRemark(dto.getApplyRemark());
            accountRoomMapper.update(existed);
        }
        return requireOwnedRoom(loginUser, room.getId(), false);
    }

    @Transactional
    public RoomVO confirmBinding(LoginUser loginUser, Long roomId) {
        AccountRoom binding = accountRoomMapper.findByAccountAndRoom(loginUser.accountId(), roomId);
        if (binding == null && loginUser.hasRole("ADMIN")) {
            binding = accountRoomMapper.findLatestByRoomAndStatus(roomId, "PENDING");
        }
        if (binding == null && !loginUser.hasRole("ADMIN")) {
            throw new BusinessException("NOT_FOUND", "绑定关系不存在", HttpStatus.NOT_FOUND);
        }
        if (binding == null) {
            throw new BusinessException("NOT_FOUND", "绑定关系不存在", HttpStatus.NOT_FOUND);
        }
        Long targetAccountId = loginUser.hasRole("ADMIN") ? binding.getAccountId() : loginUser.accountId();
        accountRoomMapper.updateStatus(targetAccountId, roomId, "ACTIVE", LocalDateTime.now());
        return requireOwnedRoom(new LoginUser(targetAccountId, "RESIDENT", "USER", loginUser.realName(), List.of("USER"), "SELF", List.of()), roomId, false);
    }

    @Transactional
    public void unbind(LoginUser loginUser, Long roomId) {
        AccountRoom binding = accountRoomMapper.findByAccountAndRoom(loginUser.accountId(), roomId);
        if (binding == null && loginUser.hasRole("ADMIN")) {
            binding = accountRoomMapper.findLatestByRoomAndStatus(roomId, "ACTIVE");
        }
        if (binding == null && !loginUser.hasRole("ADMIN")) {
            throw new BusinessException("NOT_FOUND", "绑定关系不存在", HttpStatus.NOT_FOUND);
        }
        if (binding == null) {
            throw new BusinessException("NOT_FOUND", "绑定关系不存在", HttpStatus.NOT_FOUND);
        }
        Long targetAccountId = loginUser.hasRole("ADMIN") ? binding.getAccountId() : loginUser.accountId();
        accountRoomMapper.updateStatus(targetAccountId, roomId, "INACTIVE", null);
    }

    public List<RoomVO> myRooms(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        return roomMapper.listByAccountId(loginUser.accountId());
    }

    public RoomVO myRoom(LoginUser loginUser, Long roomId) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        return requireOwnedRoom(loginUser, roomId, true);
    }

    public boolean hasActiveBinding(Long accountId, Long roomId) {
        return accountRoomMapper.countActiveBinding(accountId, roomId) > 0;
    }

    private RoomVO requireOwnedRoom(LoginUser loginUser, Long roomId, boolean activeOnly) {
        RoomVO roomVO = roomMapper.findOwnedRoom(loginUser.accountId(), roomId);
        if (roomVO == null) {
            throw new BusinessException("FORBIDDEN", "无权访问该房间", HttpStatus.FORBIDDEN);
        }
        if (activeOnly && !"ACTIVE".equals(roomVO.getBindingStatus())) {
            throw new BusinessException("FORBIDDEN", "房间绑定未激活", HttpStatus.FORBIDDEN);
        }
        return roomVO;
    }
}
