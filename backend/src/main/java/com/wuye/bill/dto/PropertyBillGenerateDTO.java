package com.wuye.bill.dto;

import jakarta.validation.constraints.NotNull;

public class PropertyBillGenerateDTO {

    @NotNull(message = "communityId 不能为空")
    private Long communityId;
    @NotNull(message = "year 不能为空")
    private Integer year;
    private String overwriteStrategy;

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getOverwriteStrategy() {
        return overwriteStrategy;
    }

    public void setOverwriteStrategy(String overwriteStrategy) {
        this.overwriteStrategy = overwriteStrategy;
    }
}
