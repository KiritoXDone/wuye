package com.wuye.room.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.room.dto.AdminRoomUpdateDTO;
import com.wuye.room.service.AdminRoomService;
import com.wuye.room.vo.AdminRoomVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/rooms")
public class AdminRoomController {

    private final AdminRoomService adminRoomService;

    public AdminRoomController(AdminRoomService adminRoomService) {
        this.adminRoomService = adminRoomService;
    }

    @GetMapping
    public ApiResponse<List<AdminRoomVO>> list(@CurrentUser LoginUser loginUser,
                                               @RequestParam Long communityId) {
        return ApiResponse.success(adminRoomService.list(loginUser, communityId));
    }

    @PutMapping("/{roomId}")
    public ApiResponse<AdminRoomVO> update(@CurrentUser LoginUser loginUser,
                                           @PathVariable Long roomId,
                                           @Valid @RequestBody AdminRoomUpdateDTO dto) {
        return ApiResponse.success(adminRoomService.update(loginUser, roomId, dto));
    }
}
