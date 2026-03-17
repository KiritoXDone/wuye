package com.wuye.importexport.entity;

import java.time.LocalDateTime;

public class ExportJob {
    private Long id;
    private String exportType;
    private String requestJson;
    private String fileUrl;
    private String status;
    private LocalDateTime expiredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getExportType() { return exportType; }
    public void setExportType(String exportType) { this.exportType = exportType; }
    public String getRequestJson() { return requestJson; }
    public void setRequestJson(String requestJson) { this.requestJson = requestJson; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }
}
