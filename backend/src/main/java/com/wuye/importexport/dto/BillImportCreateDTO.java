package com.wuye.importexport.dto;

import jakarta.validation.constraints.NotBlank;

public class BillImportCreateDTO {
    @NotBlank(message = "fileUrl 不能为空")
    private String fileUrl;

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}
