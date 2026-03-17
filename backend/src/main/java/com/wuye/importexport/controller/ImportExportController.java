package com.wuye.importexport.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.importexport.dto.BillExportCreateDTO;
import com.wuye.importexport.dto.BillImportCreateDTO;
import com.wuye.importexport.entity.ExportJob;
import com.wuye.importexport.entity.ImportBatch;
import com.wuye.importexport.entity.ImportRowError;
import com.wuye.importexport.service.ImportExportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class ImportExportController {

    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @PostMapping("/imports/bills")
    public ApiResponse<ImportBatch> createBillImport(@CurrentUser LoginUser loginUser,
                                                     @Valid @RequestBody BillImportCreateDTO dto) {
        return ApiResponse.success(importExportService.createBillImport(loginUser, dto));
    }

    @GetMapping("/imports/{batchId}")
    public ApiResponse<ImportBatch> getImportBatch(@CurrentUser LoginUser loginUser,
                                                   @PathVariable Long batchId) {
        return ApiResponse.success(importExportService.getImportBatch(loginUser, batchId));
    }

    @GetMapping("/imports/{batchId}/errors")
    public ApiResponse<List<ImportRowError>> listImportErrors(@CurrentUser LoginUser loginUser,
                                                              @PathVariable Long batchId) {
        return ApiResponse.success(importExportService.listImportErrors(loginUser, batchId));
    }

    @PostMapping("/exports/bills")
    public ApiResponse<ExportJob> createBillExport(@CurrentUser LoginUser loginUser,
                                                   @RequestBody BillExportCreateDTO dto) {
        return ApiResponse.success(importExportService.createBillExport(loginUser, dto));
    }

    @GetMapping("/exports/{jobId}")
    public ApiResponse<ExportJob> getExportJob(@CurrentUser LoginUser loginUser,
                                               @PathVariable Long jobId) {
        return ApiResponse.success(importExportService.getExportJob(loginUser, jobId));
    }
}
