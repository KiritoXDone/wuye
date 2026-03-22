package com.wuye.room.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.room.dto.AdminRoomBatchCreateDTO;
import com.wuye.room.dto.AdminRoomBatchDeleteDTO;
import com.wuye.room.dto.AdminRoomBatchUpdateDTO;
import com.wuye.room.dto.AdminRoomCreateDTO;
import com.wuye.room.dto.AdminRoomListQuery;
import com.wuye.room.dto.AdminRoomUpdateDTO;
import com.wuye.room.service.AdminRoomService;
import com.wuye.room.vo.AdminRoomVO;
import com.wuye.room.vo.BatchOperationResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
                                               AdminRoomListQuery query) {
        return ApiResponse.success(adminRoomService.list(loginUser, query));
    }

    @PostMapping
    public ApiResponse<AdminRoomVO> create(@CurrentUser LoginUser loginUser,
                                           @Valid @RequestBody AdminRoomCreateDTO dto) {
        return ApiResponse.success(adminRoomService.create(loginUser, dto));
    }

    @PutMapping("/{roomId}")
    public ApiResponse<AdminRoomVO> update(@CurrentUser LoginUser loginUser,
                                           @PathVariable Long roomId,
                                           @Valid @RequestBody AdminRoomUpdateDTO dto) {
        return ApiResponse.success(adminRoomService.update(loginUser, roomId, dto));
    }

    @DeleteMapping("/{roomId}")
    public ApiResponse<Void> delete(@CurrentUser LoginUser loginUser,
                                    @PathVariable Long roomId) {
        adminRoomService.hardDelete(loginUser, roomId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{roomId}/hard-delete")
    public ApiResponse<Void> hardDelete(@CurrentUser LoginUser loginUser,
                                        @PathVariable Long roomId) {
        adminRoomService.hardDelete(loginUser, roomId);
        return ApiResponse.success();
    }

    @PostMapping("/batch-create")
    public ApiResponse<BatchOperationResultVO> batchCreate(@CurrentUser LoginUser loginUser,
                                                           @Valid @RequestBody AdminRoomBatchCreateDTO dto) {
        return ApiResponse.success(adminRoomService.batchCreate(loginUser, dto));
    }

    @PostMapping("/batch-update")
    public ApiResponse<BatchOperationResultVO> batchUpdate(@CurrentUser LoginUser loginUser,
                                                           @Valid @RequestBody AdminRoomBatchUpdateDTO dto) {
        return ApiResponse.success(adminRoomService.batchUpdate(loginUser, dto));
    }

    @PostMapping("/batch-delete")
    public ApiResponse<BatchOperationResultVO> batchDelete(@CurrentUser LoginUser loginUser,
                                                           @Valid @RequestBody AdminRoomBatchDeleteDTO dto) {
        return ApiResponse.success(adminRoomService.batchDelete(loginUser, dto));
    }
}
