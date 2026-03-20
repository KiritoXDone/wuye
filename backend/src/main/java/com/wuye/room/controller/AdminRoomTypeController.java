package com.wuye.room.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.room.dto.RoomTypeUpsertDTO;
import com.wuye.room.service.RoomTypeService;
import com.wuye.room.vo.RoomTypeVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/room-types")
public class AdminRoomTypeController {

    private final RoomTypeService roomTypeService;

    public AdminRoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public ApiResponse<List<RoomTypeVO>> list(@CurrentUser LoginUser loginUser,
                                              @RequestParam Long communityId) {
        return ApiResponse.success(roomTypeService.list(loginUser, communityId));
    }

    @PostMapping
    public ApiResponse<RoomTypeVO> create(@CurrentUser LoginUser loginUser,
                                          @Valid @RequestBody RoomTypeUpsertDTO dto) {
        return ApiResponse.success(roomTypeService.create(loginUser, dto));
    }

    @PutMapping("/{roomTypeId}")
    public ApiResponse<RoomTypeVO> update(@CurrentUser LoginUser loginUser,
                                          @PathVariable Long roomTypeId,
                                          @Valid @RequestBody RoomTypeUpsertDTO dto) {
        return ApiResponse.success(roomTypeService.update(loginUser, roomTypeId, dto));
    }
}
