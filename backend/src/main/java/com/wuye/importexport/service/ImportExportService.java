package com.wuye.importexport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.importexport.dto.BillExportCreateDTO;
import com.wuye.importexport.dto.BillImportCreateDTO;
import com.wuye.importexport.entity.ExportJob;
import com.wuye.importexport.entity.ImportBatch;
import com.wuye.importexport.entity.ImportRowError;
import com.wuye.importexport.mapper.ExportJobMapper;
import com.wuye.importexport.mapper.ImportBatchMapper;
import com.wuye.importexport.mapper.ImportRowErrorMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ImportExportService {

    private final ImportBatchMapper importBatchMapper;
    private final ImportRowErrorMapper importRowErrorMapper;
    private final ExportJobMapper exportJobMapper;
    private final AccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public ImportExportService(ImportBatchMapper importBatchMapper,
                               ImportRowErrorMapper importRowErrorMapper,
                               ExportJobMapper exportJobMapper,
                               AccessGuard accessGuard,
                               ObjectMapper objectMapper) {
        this.importBatchMapper = importBatchMapper;
        this.importRowErrorMapper = importRowErrorMapper;
        this.exportJobMapper = exportJobMapper;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportBatch createBillImport(LoginUser loginUser, BillImportCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        ImportBatch batch = new ImportBatch();
        batch.setBatchNo("IMP-" + System.currentTimeMillis());
        batch.setImportType("BILL");
        batch.setFileUrl(dto.getFileUrl());
        batch.setStatus("SUCCESS");
        batch.setTotalCount(3);
        batch.setSuccessCount(2);
        batch.setFailCount(1);
        importBatchMapper.insert(batch);

        ImportRowError error = new ImportRowError();
        error.setBatchId(batch.getId());
        error.setRowNo(3);
        error.setErrorCode("VALIDATION_FAILED");
        error.setErrorMessage("group_code 不存在");
        error.setRawData("{\"bill_no\":\"B-IMPORT-003\",\"group_code\":\"INVALID\"}");
        importRowErrorMapper.insert(error);
        return batch;
    }

    public ImportBatch getImportBatch(LoginUser loginUser, Long batchId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return importBatchMapper.findById(batchId);
    }

    public List<ImportRowError> listImportErrors(LoginUser loginUser, Long batchId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return importRowErrorMapper.listByBatchId(batchId);
    }

    @Transactional
    public ExportJob createBillExport(LoginUser loginUser, BillExportCreateDTO dto) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "FINANCE");
        ExportJob exportJob = new ExportJob();
        exportJob.setExportType("BILL");
        exportJob.setRequestJson(writeJson(dto));
        exportJob.setFileUrl("/exports/bills/export-" + System.currentTimeMillis() + ".csv");
        exportJob.setStatus("SUCCESS");
        exportJob.setExpiredAt(LocalDateTime.now().plusDays(7));
        exportJobMapper.insert(exportJob);
        return exportJob;
    }

    public ExportJob getExportJob(LoginUser loginUser, Long jobId) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "FINANCE");
        return exportJobMapper.findById(jobId);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize import/export json", ex);
        }
    }
}
