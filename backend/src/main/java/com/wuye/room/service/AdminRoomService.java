package com.wuye.room.service;

import com.wuye.bill.entity.WaterMeter;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.bill.mapper.WaterMeterMapper;
import com.wuye.bill.mapper.WaterReadingMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.room.dto.AdminRoomBatchCreateDTO;
import com.wuye.room.dto.AdminRoomBatchDeleteDTO;
import com.wuye.room.dto.AdminRoomBatchUpdateDTO;
import com.wuye.room.dto.AdminRoomCreateDTO;
import com.wuye.room.dto.AdminRoomListQuery;
import com.wuye.room.dto.AdminRoomUpdateDTO;
import com.wuye.room.entity.Community;
import com.wuye.room.entity.Room;
import com.wuye.room.entity.RoomType;
import com.wuye.room.mapper.AccountRoomMapper;
import com.wuye.room.mapper.CommunityMapper;
import com.wuye.room.mapper.RoomMapper;
import com.wuye.room.mapper.RoomTypeMapper;
import com.wuye.room.vo.AdminRoomVO;
import com.wuye.room.vo.BatchOperationResultVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminRoomService {

    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final CommunityMapper communityMapper;
    private final AccountRoomMapper accountRoomMapper;
    private final BillMapper billMapper;
    private final WaterMeterMapper waterMeterMapper;
    private final WaterReadingMapper waterReadingMapper;
    private final AccessGuard accessGuard;

    public AdminRoomService(RoomMapper roomMapper,
                            RoomTypeMapper roomTypeMapper,
                            CommunityMapper communityMapper,
                            AccountRoomMapper accountRoomMapper,
                            BillMapper billMapper,
                            WaterMeterMapper waterMeterMapper,
                            WaterReadingMapper waterReadingMapper,
                            AccessGuard accessGuard) {
        this.roomMapper = roomMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.communityMapper = communityMapper;
        this.accountRoomMapper = accountRoomMapper;
        this.billMapper = billMapper;
        this.waterMeterMapper = waterMeterMapper;
        this.waterReadingMapper = waterReadingMapper;
        this.accessGuard = accessGuard;
    }

    public List<AdminRoomVO> list(LoginUser loginUser, AdminRoomListQuery query) {
        accessGuard.requireRole(loginUser, "ADMIN");
        requireCommunity(query.getCommunityId());
        return roomMapper.listByAdminQuery(normalizeQuery(query)).stream().map(this::toAdminVO).collect(Collectors.toList());
    }

    @Transactional
    public AdminRoomVO create(LoginUser loginUser, AdminRoomCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Room room = buildRoom(dto);
        ensureLocationUnique(room.getCommunityId(), room.getBuildingNo(), room.getUnitNo(), room.getRoomNo(), null);
        roomMapper.insert(room);
        ensureWaterMeter(room.getId());
        return toAdminVO(requireRoom(room.getId()));
    }

    @Transactional
    public AdminRoomVO update(LoginUser loginUser, Long roomId, AdminRoomUpdateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Room room = requireRoom(roomId);
        RoomType roomType = requireRoomTypeIfPresent(dto.getRoomTypeId(), room.getCommunityId());
        room.setRoomTypeId(dto.getRoomTypeId());
        if (dto.getAreaM2() != null) {
            room.setAreaM2(normalizeArea(dto.getAreaM2()));
        } else if (roomType != null && room.getRoomTypeId() != null) {
            room.setAreaM2(roomType.getAreaM2());
        }
        roomMapper.updateAdminRoom(room);
        return toAdminVO(requireRoom(roomId));
    }

    @Transactional
    public AdminRoomVO disable(LoginUser loginUser, Long roomId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Room room = requireRoom(roomId);
        room.setStatus(0);
        roomMapper.updateAdminRoom(room);
        return toAdminVO(requireRoom(roomId));
    }

    @Transactional
    public AdminRoomVO enable(LoginUser loginUser, Long roomId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Room room = requireRoom(roomId);
        room.setStatus(1);
        roomMapper.updateAdminRoom(room);
        return toAdminVO(requireRoom(roomId));
    }

    @Transactional
    public BatchOperationResultVO batchCreate(LoginUser loginUser, AdminRoomBatchCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        requireCommunity(dto.getCommunityId());
        requireRoomTypeIfPresent(dto.getRoomTypeId(), dto.getCommunityId());
        BatchOperationResultVO result = new BatchOperationResultVO();
        List<String> roomNos = dto.getRoomNos() == null ? List.of() : dto.getRoomNos();
        result.setRequestedCount(roomNos.size());
        Set<String> deduplicated = new LinkedHashSet<>();
        for (String rawRoomNo : roomNos) {
            String roomNo = normalizeText(rawRoomNo);
            if (roomNo == null) {
                addSkipped(result, "存在空房号，已跳过");
                continue;
            }
            if (!deduplicated.add(roomNo)) {
                addSkipped(result, "房号 " + roomNo + " 重复提交，已跳过");
                continue;
            }
            if (roomMapper.findByLocation(dto.getCommunityId(), normalizeText(dto.getBuildingNo()), normalizeText(dto.getUnitNo()), roomNo) != null) {
                addSkipped(result, "房号 " + roomNo + " 已存在，未重复创建");
                continue;
            }
            Room room = new Room();
            room.setCommunityId(dto.getCommunityId());
            room.setBuildingNo(normalizeText(dto.getBuildingNo()));
            room.setUnitNo(normalizeText(dto.getUnitNo()));
            room.setRoomNo(roomNo);
            room.setRoomTypeId(dto.getRoomTypeId());
            room.setAreaM2(normalizeArea(dto.getAreaM2()));
            room.setStatus(1);
            roomMapper.insert(room);
            ensureWaterMeter(room.getId());
            result.setSuccessCount(result.getSuccessCount() + 1);
        }
        result.setSkippedCount(result.getSkippedReasons().size());
        return result;
    }

    @Transactional
    public BatchOperationResultVO batchUpdate(LoginUser loginUser, AdminRoomBatchUpdateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        requireCommunity(dto.getCommunityId());
        if (dto.getTargetRoomTypeId() == null && dto.getTargetAreaM2() == null) {
            throw new BusinessException("INVALID_REQUEST", "请至少提供新的户型或面积", HttpStatus.BAD_REQUEST);
        }
        requireRoomTypeIfPresent(dto.getTargetRoomTypeId(), dto.getCommunityId());
        List<Room> rooms = resolveTargetRooms(buildQuery(dto), dto.getSelectionRoomIds(), dto.getApplyToFiltered());
        if (rooms.isEmpty()) {
            throw new BusinessException("INVALID_REQUEST", "没有匹配到可更新的房间", HttpStatus.BAD_REQUEST);
        }
        BatchOperationResultVO result = new BatchOperationResultVO();
        result.setRequestedCount(rooms.size());
        for (Room room : rooms) {
            if (dto.getTargetRoomTypeId() != null) {
                room.setRoomTypeId(dto.getTargetRoomTypeId());
            }
            if (dto.getTargetAreaM2() != null) {
                room.setAreaM2(normalizeArea(dto.getTargetAreaM2()));
            }
            roomMapper.updateAdminRoom(room);
            result.setSuccessCount(result.getSuccessCount() + 1);
        }
        return result;
    }

    @Transactional
    public BatchOperationResultVO batchDelete(LoginUser loginUser, AdminRoomBatchDeleteDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        requireCommunity(dto.getCommunityId());
        List<Room> rooms = resolveTargetRooms(buildQuery(dto), dto.getSelectionRoomIds(), dto.getApplyToFiltered());
        if (rooms.isEmpty()) {
            throw new BusinessException("INVALID_REQUEST", "没有匹配到可停用的房间", HttpStatus.BAD_REQUEST);
        }
        BatchOperationResultVO result = new BatchOperationResultVO();
        result.setRequestedCount(rooms.size());
        List<String> skippedReasons = new ArrayList<>();
        for (Room room : rooms) {
            if (Integer.valueOf(0).equals(room.getStatus())) {
                skippedReasons.add(formatRoomLabel(room) + " 已是停用状态");
                continue;
            }
            room.setStatus(0);
            roomMapper.updateAdminRoom(room);
            result.setSuccessCount(result.getSuccessCount() + 1);
            if (hasHistory(room.getId())) {
                skippedReasons.add(formatRoomLabel(room) + " 已存在历史绑定/账单/水表/抄表数据，本次按停用处理");
            }
        }
        result.setSkippedReasons(skippedReasons);
        result.setSkippedCount(skippedReasons.size());
        return result;
    }

    private Room buildRoom(AdminRoomCreateDTO dto) {
        requireCommunity(dto.getCommunityId());
        requireRoomTypeIfPresent(dto.getRoomTypeId(), dto.getCommunityId());
        Room room = new Room();
        room.setCommunityId(dto.getCommunityId());
        room.setBuildingNo(normalizeText(dto.getBuildingNo()));
        room.setUnitNo(normalizeText(dto.getUnitNo()));
        room.setRoomNo(normalizeText(dto.getRoomNo()));
        room.setRoomTypeId(dto.getRoomTypeId());
        room.setAreaM2(normalizeArea(dto.getAreaM2()));
        room.setStatus(1);
        return room;
    }

    private AdminRoomListQuery buildQuery(AdminRoomBatchUpdateDTO dto) {
        AdminRoomListQuery query = new AdminRoomListQuery();
        query.setCommunityId(dto.getCommunityId());
        query.setBuildingNo(dto.getBuildingNo());
        query.setUnitNo(dto.getUnitNo());
        query.setRoomNoKeyword(dto.getRoomNoKeyword());
        query.setRoomSuffix(dto.getRoomSuffix());
        query.setRoomTypeId(dto.getRoomTypeId());
        query.setStatus(dto.getStatus());
        return normalizeQuery(query);
    }

    private AdminRoomListQuery buildQuery(AdminRoomBatchDeleteDTO dto) {
        AdminRoomListQuery query = new AdminRoomListQuery();
        query.setCommunityId(dto.getCommunityId());
        query.setBuildingNo(dto.getBuildingNo());
        query.setUnitNo(dto.getUnitNo());
        query.setRoomNoKeyword(dto.getRoomNoKeyword());
        query.setRoomSuffix(dto.getRoomSuffix());
        query.setRoomTypeId(dto.getRoomTypeId());
        query.setStatus(dto.getStatus());
        return normalizeQuery(query);
    }

    private List<Room> resolveTargetRooms(AdminRoomListQuery query, List<Long> selectionRoomIds, Boolean applyToFiltered) {
        boolean useFiltered = Boolean.TRUE.equals(applyToFiltered);
        if (useFiltered) {
            return roomMapper.listByAdminQuery(query);
        }
        if (selectionRoomIds == null || selectionRoomIds.isEmpty()) {
            throw new BusinessException("INVALID_REQUEST", "请先勾选房间或选择按筛选结果执行", HttpStatus.BAD_REQUEST);
        }
        Set<Long> ids = new LinkedHashSet<>(selectionRoomIds);
        return roomMapper.listByIds(query.getCommunityId(), ids);
    }

    private void ensureLocationUnique(Long communityId, String buildingNo, String unitNo, String roomNo, Long roomId) {
        Room duplicated = roomMapper.findByLocation(communityId, buildingNo, unitNo, roomNo);
        if (duplicated != null && (roomId == null || !duplicated.getId().equals(roomId))) {
            throw new BusinessException("ROOM_DUPLICATED", "同小区楼栋单元房号已存在", HttpStatus.BAD_REQUEST);
        }
    }

    private RoomType requireRoomTypeIfPresent(Long roomTypeId, Long communityId) {
        if (roomTypeId == null) {
            return null;
        }
        RoomType roomType = roomTypeMapper.findById(roomTypeId);
        if (roomType == null || !roomType.getCommunityId().equals(communityId)) {
            throw new BusinessException("NOT_FOUND", "户型不存在", HttpStatus.NOT_FOUND);
        }
        return roomType;
    }

    private Community requireCommunity(Long communityId) {
        if (communityId == null) {
            throw new BusinessException("INVALID_REQUEST", "communityId 不能为空", HttpStatus.BAD_REQUEST);
        }
        Community community = communityMapper.findById(communityId);
        if (community == null) {
            throw new BusinessException("NOT_FOUND", "小区不存在", HttpStatus.NOT_FOUND);
        }
        return community;
    }

    private Room requireRoom(Long roomId) {
        Room room = roomMapper.findById(roomId);
        if (room == null) {
            throw new BusinessException("NOT_FOUND", "房间不存在", HttpStatus.NOT_FOUND);
        }
        return room;
    }

    private boolean hasHistory(Long roomId) {
        return accountRoomMapper.countByRoomId(roomId) > 0
                || billMapper.countByRoomId(roomId) > 0
                || roomMapper.countWaterMeters(roomId) > 0
                || waterReadingMapper.countByRoomId(roomId) > 0;
    }

    private AdminRoomListQuery normalizeQuery(AdminRoomListQuery query) {
        query.setBuildingNo(normalizeText(query.getBuildingNo()));
        query.setUnitNo(normalizeText(query.getUnitNo()));
        query.setRoomNoKeyword(normalizeText(query.getRoomNoKeyword()));
        query.setRoomSuffix(normalizeText(query.getRoomSuffix()));
        return query;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal normalizeArea(BigDecimal area) {
        return area == null ? null : area.stripTrailingZeros().scale() < 0 ? area.setScale(0) : area;
    }

    private void addSkipped(BatchOperationResultVO result, String reason) {
        List<String> reasons = new ArrayList<>(result.getSkippedReasons());
        reasons.add(reason);
        result.setSkippedReasons(reasons);
        result.setSkippedCount(reasons.size());
    }

    private String formatRoomLabel(Room room) {
        return room.getBuildingNo() + "-" + room.getUnitNo() + "-" + room.getRoomNo();
    }

    private void ensureWaterMeter(Long roomId) {
        if (waterMeterMapper.findByRoomId(roomId) != null) {
            return;
        }
        WaterMeter waterMeter = new WaterMeter();
        waterMeter.setRoomId(roomId);
        waterMeter.setMeterNo("WM-ROOM-" + roomId);
        waterMeter.setStatus(1);
        waterMeterMapper.insert(waterMeter);
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
