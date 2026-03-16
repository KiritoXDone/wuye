package com.wuye.bill.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class WaterBillGenerateDTO {

    @NotNull(message = "communityId 不能为空")
    private Long communityId;
    @NotNull(message = "year 不能为空")
    private Integer year;
    @NotNull(message = "month 不能为空")
    @Min(value = 1, message = "month 必须在 1 到 12 之间")
    @Max(value = 12, message = "month 必须在 1 到 12 之间")
    private Integer month;
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

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public String getOverwriteStrategy() {
        return overwriteStrategy;
    }

    public void setOverwriteStrategy(String overwriteStrategy) {
        this.overwriteStrategy = overwriteStrategy;
    }
}
