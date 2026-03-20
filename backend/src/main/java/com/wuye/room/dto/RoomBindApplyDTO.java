package com.wuye.room.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RoomBindApplyDTO {

    @NotNull(message = "roomId 不能为空")
    @Min(value = 1, message = "roomId 非法")
    private Long roomId;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}
