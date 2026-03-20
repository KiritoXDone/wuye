package com.wuye.room.vo;

import java.util.ArrayList;
import java.util.List;

public class BatchOperationResultVO {

    private int requestedCount;
    private int successCount;
    private int skippedCount;
    private List<String> skippedReasons = new ArrayList<>();

    public int getRequestedCount() {
        return requestedCount;
    }

    public void setRequestedCount(int requestedCount) {
        this.requestedCount = requestedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<String> getSkippedReasons() {
        return skippedReasons;
    }

    public void setSkippedReasons(List<String> skippedReasons) {
        this.skippedReasons = skippedReasons;
    }
}
