package com.wuye.room.service;

import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.room.dto.RoomBindApplyDTO;
import com.wuye.room.entity.AccountRoom;
import com.wuye.room.entity.Room;
import com.wuye.room.mapper.AccountRoomMapper;
import com.wuye.room.mapper.CommunityMapper;
import com.wuye.room.mapper.RoomMapper;
import com.wuye.room.vo.ResidentBuildingOptionVO;
import com.wuye.room.vo.ResidentCommunityOptionVO;
import com.wuye.room.vo.ResidentRoomOptionVO;
import com.wuye.room.vo.ResidentUnitOptionVO;
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
    private final CommunityMapper communityMapper;
    private final AccessGuard accessGuard;

    public RoomBindingService(RoomMapper roomMapper,
                              AccountRoomMapper accountRoomMapper,
                              CommunityMapper communityMapper,
                              AccessGuard accessGuard) {
        this.roomMapper = roomMapper;
        this.accountRoomMapper = accountRoomMapper;
        this.communityMapper = communityMapper;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public RoomVO applyBinding(LoginUser loginUser, RoomBindApplyDTO dto) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        Room room = roomMapper.findById(dto.getRoomId());
        if (room == null || room.getStatus() == null || room.getStatus() != 1) {
            throw new BusinessException("NOT_FOUND", "房间不存在", HttpStatus.NOT_FOUND);
        }
        AccountRoom existed = accountRoomMapper.findByAccountAndRoom(loginUser.accountId(), room.getId());
        LocalDateTime now = LocalDateTime.now();
        if (existed == null) {
            AccountRoom accountRoom = new AccountRoom();
            accountRoom.setAccountId(loginUser.accountId());
            accountRoom.setRoomId(room.getId());
            accountRoom.setStatus("ACTIVE");
            accountRoom.setBindSource("SELF");
            accountRoom.setConfirmedAt(now);
            accountRoomMapper.insert(accountRoom);
        } else if (!"ACTIVE".equals(existed.getStatus())) {
            existed.setStatus("ACTIVE");
            existed.setBindSource("SELF");
            existed.setConfirmedAt(now);
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

    public List<ResidentCommunityOptionVO> listCommunityOptions(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        return communityMapper.listActiveCommunityOptions();
    }

    public List<ResidentBuildingOptionVO> listBuildingOptions(LoginUser loginUser, Long communityId) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        requireCommunity(communityId);
        return roomMapper.listBuildingsByCommunity(communityId).stream().map(value -> {
            ResidentBuildingOptionVO vo = new ResidentBuildingOptionVO();
            vo.setBuildingNo(value);
            return vo;
        }).toList();
    }

    public List<ResidentUnitOptionVO> listUnitOptions(LoginUser loginUser, Long communityId, String buildingNo) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        requireCommunity(communityId);
        if (buildingNo == null || buildingNo.trim().isEmpty()) {
            throw new BusinessException("INVALID_REQUEST", "buildingNo 不能为空", HttpStatus.BAD_REQUEST);
        }
        return roomMapper.listUnitsByCommunityAndBuilding(communityId, buildingNo.trim()).stream().map(value -> {
            ResidentUnitOptionVO vo = new ResidentUnitOptionVO();
            vo.setUnitNo(value);
            return vo;
        }).toList();
    }

    public List<ResidentRoomOptionVO> listRoomOptions(LoginUser loginUser, Long communityId, String buildingNo, String unitNo) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        requireCommunity(communityId);
        if (buildingNo == null || buildingNo.trim().isEmpty() || unitNo == null || unitNo.trim().isEmpty()) {
            throw new BusinessException("INVALID_REQUEST", "楼栋和单元不能为空", HttpStatus.BAD_REQUEST);
        }
        return roomMapper.listRoomOptions(communityId, buildingNo.trim(), unitNo.trim());
    }

    public boolean hasActiveBinding(Long accountId, Long roomId) {
        return accountRoomMapper.countActiveBinding(accountId, roomId) > 0;
    }

    private void requireCommunity(Long communityId) {
        if (communityId == null || communityMapper.findById(communityId) == null) {
            throw new BusinessException("NOT_FOUND", "小区不存在", HttpStatus.NOT_FOUND);
        }
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
