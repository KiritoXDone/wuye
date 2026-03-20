package com.wuye.room.controller;

import com.wuye.bill.service.BillQueryService;
import com.wuye.bill.vo.BillListItemVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.api.PageResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.room.dto.RoomBindApplyDTO;
import com.wuye.room.service.RoomBindingService;
import com.wuye.room.vo.ResidentBuildingOptionVO;
import com.wuye.room.vo.ResidentCommunityOptionVO;
import com.wuye.room.vo.ResidentRoomOptionVO;
import com.wuye.room.vo.ResidentUnitOptionVO;
import com.wuye.room.vo.RoomVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/me/rooms")
public class RoomController {

    private final RoomBindingService roomBindingService;
    private final BillQueryService billQueryService;

    public RoomController(RoomBindingService roomBindingService, BillQueryService billQueryService) {
        this.roomBindingService = roomBindingService;
        this.billQueryService = billQueryService;
    }

    @GetMapping
    public ApiResponse<List<RoomVO>> myRooms(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(roomBindingService.myRooms(loginUser));
    }

    @GetMapping("/{roomId}")
    public ApiResponse<RoomVO> myRoom(@CurrentUser LoginUser loginUser, @PathVariable Long roomId) {
        return ApiResponse.success(roomBindingService.myRoom(loginUser, roomId));
    }

    @PostMapping
    public ApiResponse<RoomVO> applyBinding(@CurrentUser LoginUser loginUser,
                                            @Valid @RequestBody RoomBindApplyDTO dto) {
        return ApiResponse.success(roomBindingService.applyBinding(loginUser, dto));
    }

    @GetMapping("/options/communities")
    public ApiResponse<List<ResidentCommunityOptionVO>> communityOptions(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(roomBindingService.listCommunityOptions(loginUser));
    }

    @GetMapping("/options/buildings")
    public ApiResponse<List<ResidentBuildingOptionVO>> buildingOptions(@CurrentUser LoginUser loginUser,
                                                                       @RequestParam Long communityId) {
        return ApiResponse.success(roomBindingService.listBuildingOptions(loginUser, communityId));
    }

    @GetMapping("/options/units")
    public ApiResponse<List<ResidentUnitOptionVO>> unitOptions(@CurrentUser LoginUser loginUser,
                                                               @RequestParam Long communityId,
                                                               @RequestParam String buildingNo) {
        return ApiResponse.success(roomBindingService.listUnitOptions(loginUser, communityId, buildingNo));
    }

    @GetMapping("/options/room-numbers")
    public ApiResponse<List<ResidentRoomOptionVO>> roomOptions(@CurrentUser LoginUser loginUser,
                                                               @RequestParam Long communityId,
                                                               @RequestParam String buildingNo,
                                                               @RequestParam String unitNo) {
        return ApiResponse.success(roomBindingService.listRoomOptions(loginUser, communityId, buildingNo, unitNo));
    }

    @PostMapping("/{roomId}/confirm")
    public ApiResponse<RoomVO> confirmBinding(@CurrentUser LoginUser loginUser, @PathVariable Long roomId) {
        return ApiResponse.success(roomBindingService.confirmBinding(loginUser, roomId));
    }

    @PostMapping("/{roomId}/unbind")
    public ApiResponse<Void> unbind(@CurrentUser LoginUser loginUser, @PathVariable Long roomId) {
        roomBindingService.unbind(loginUser, roomId);
        return ApiResponse.success();
    }

    @GetMapping("/{roomId}/bills")
    public ApiResponse<PageResponse<BillListItemVO>> roomBills(@CurrentUser LoginUser loginUser, @PathVariable Long roomId) {
        return ApiResponse.success(billQueryService.listRoomBills(loginUser, roomId));
    }
}
