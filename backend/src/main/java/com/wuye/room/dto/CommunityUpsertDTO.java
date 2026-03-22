package com.wuye.room.dto;

import jakarta.validation.constraints.NotBlank;

public class CommunityUpsertDTO {

    @NotBlank
    private String communityCode;

    @NotBlank
    private String name;

    public String getCommunityCode() {
        return communityCode;
    }

    public void setCommunityCode(String communityCode) {
        this.communityCode = communityCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
