package com.wuye.importexport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.audit.service.AuditLogService;
import com.wuye.agent.mapper.UserGroupMapper;
import com.wuye.agent.entity.UserGroup;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.entity.BillLine;
import com.wuye.bill.mapper.BillLineMapper;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.common.util.FileNoGenerator;
import com.wuye.common.util.MoneyUtils;
import com.wuye.importexport.dto.BillExportCreateDTO;
import com.wuye.importexport.dto.BillImportCreateDTO;
import com.wuye.importexport.entity.ExportJob;
import com.wuye.importexport.entity.ImportBatch;
import com.wuye.importexport.entity.ImportRowError;
import com.wuye.importexport.mapper.ExportJobMapper;
import com.wuye.importexport.mapper.ImportBatchMapper;
import com.wuye.importexport.mapper.ImportRowErrorMapper;
import com.wuye.room.entity.Room;
import com.wuye.room.mapper.CommunityMapper;
import com.wuye.room.mapper.RoomMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ImportExportService {

    private final ImportBatchMapper importBatchMapper;
    private final ImportRowErrorMapper importRowErrorMapper;
    private final ExportJobMapper exportJobMapper;
    private final BillMapper billMapper;
    private final BillLineMapper billLineMapper;
    private final RoomMapper roomMapper;
    private final CommunityMapper communityMapper;
    private final UserGroupMapper userGroupMapper;
    private final ImportExportFileService importExportFileService;
    private final AccessGuard accessGuard;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final String exportRootDir;

    public ImportExportService(ImportBatchMapper importBatchMapper,
                               ImportRowErrorMapper importRowErrorMapper,
                               ExportJobMapper exportJobMapper,
                               BillMapper billMapper,
                               BillLineMapper billLineMapper,
                               RoomMapper roomMapper,
                               CommunityMapper communityMapper,
                               UserGroupMapper userGroupMapper,
                               ImportExportFileService importExportFileService,
                               AccessGuard accessGuard,
                               ObjectMapper objectMapper,
                               AuditLogService auditLogService,
                               @Value("${app.import-export.export-dir:exports/bills}") String exportRootDir) {
        this.importBatchMapper = importBatchMapper;
        this.importRowErrorMapper = importRowErrorMapper;
        this.exportJobMapper = exportJobMapper;
        this.billMapper = billMapper;
        this.billLineMapper = billLineMapper;
        this.roomMapper = roomMapper;
        this.communityMapper = communityMapper;
        this.userGroupMapper = userGroupMapper;
        this.importExportFileService = importExportFileService;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.exportRootDir = exportRootDir;
    }

    @Transactional
    public ImportBatch createBillImport(LoginUser loginUser, BillImportCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        ImportBatch batch = new ImportBatch();
        batch.setBatchNo(FileNoGenerator.importBatchNo());
        batch.setImportType("BILL");
        batch.setFileUrl(dto.getFileUrl());
        batch.setStatus("PROCESSING");
        batch.setTotalCount(0);
        batch.setSuccessCount(0);
        batch.setFailCount(0);
        importBatchMapper.insert(batch);

        try {
            List<Map<String, String>> rows = importExportFileService.readRows(dto.getFileUrl());
            int successCount = 0;
            int failCount = 0;
            int rowNo = 1;
            for (Map<String, String> row : rows) {
                rowNo++;
                try {
                    importBillRow(row);
                    successCount++;
                } catch (BusinessException ex) {
                    failCount++;
                    saveRowError(batch.getId(), rowNo, ex.getCode(), ex.getMessage(), writeJson(row));
                }
            }
            batch.setStatus(failCount > 0 ? "PARTIAL_SUCCESS" : "SUCCESS");
            batch.setTotalCount(rows.size());
            batch.setSuccessCount(successCount);
            batch.setFailCount(failCount);
            importBatchMapper.updateResult(batch);
        } catch (IOException ex) {
            batch.setStatus("FAILED");
            batch.setTotalCount(0);
            batch.setSuccessCount(0);
            batch.setFailCount(1);
            importBatchMapper.updateResult(batch);
            saveRowError(batch.getId(), 0, "INVALID_ARGUMENT", ex.getMessage(), dto.getFileUrl());
        }
        ImportBatch savedBatch = importBatchMapper.findById(batch.getId());
        auditLogService.record(loginUser, "IMPORT", savedBatch.getBatchNo(), "IMPORT", buildImportAuditDetail(savedBatch));
        return savedBatch;
    }

    public ImportBatch getImportBatch(LoginUser loginUser, Long batchId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        ImportBatch batch = importBatchMapper.findById(batchId);
        if (batch == null) {
            throw new BusinessException("NOT_FOUND", "导入批次不存在", HttpStatus.NOT_FOUND);
        }
        return batch;
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
        exportJob.setStatus("PROCESSING");
        exportJob.setExpiredAt(LocalDateTime.now().plusDays(7));
        exportJobMapper.insert(exportJob);

        try {
            List<List<String>> rows = buildExportRows(dto);
            Path exportDirectory = Path.of(exportRootDir);
            String extension = "xlsx";
            Path filePath = exportDirectory.resolve(FileNoGenerator.exportFileName(extension));
            importExportFileService.writeXlsx(filePath, "账单导出", exportHeaders(), rows);
            exportJob.setFileUrl(filePath.toAbsolutePath().toString());
            exportJob.setStatus("SUCCESS");
            exportJobMapper.updateResult(exportJob);
        } catch (IOException ex) {
            exportJob.setStatus("FAILED");
            exportJob.setFileUrl(null);
            exportJobMapper.updateResult(exportJob);
        }
        ExportJob savedJob = exportJobMapper.findById(exportJob.getId());
        auditLogService.record(loginUser, "EXPORT", String.valueOf(savedJob.getId()), "EXPORT", buildExportAuditDetail(savedJob, dto));
        return savedJob;
    }

    public ExportJob getExportJob(LoginUser loginUser, Long jobId) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "FINANCE");
        ExportJob exportJob = exportJobMapper.findById(jobId);
        if (exportJob == null) {
            throw new BusinessException("NOT_FOUND", "导出任务不存在", HttpStatus.NOT_FOUND);
        }
        return exportJob;
    }

    private void importBillRow(Map<String, String> row) {
        String billNo = required(row, "bill_no");
        String feeType = required(row, "fee_type");
        Integer periodYear = parseInt(required(row, "period_year"), "period_year");
        String communityCode = required(row, "community_code");
        String buildingNo = required(row, "building_no");
        String unitNo = required(row, "unit_no");
        String roomNo = required(row, "room_no");
        String groupCode = required(row, "group_code");
        BigDecimal amountDue = parseMoney(required(row, "amount_due"), "amount_due");
        LocalDate dueDate = parseDate(required(row, "due_date"), "due_date");
        boolean propertyBill = "PROPERTY".equals(feeType);
        Integer periodMonth = propertyBill
                ? parseOptionalInt(row.get("period_month"), "period_month")
                : parseInt(required(row, "period_month"), "period_month");

        Long communityId = communityMapper.findIdByCommunityCode(communityCode);
        if (communityId == null) {
            throw new BusinessException("VALIDATION_FAILED", "community_code 不存在", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        UserGroup userGroup = userGroupMapper.findByGroupCode(groupCode);
        Long groupId = userGroup == null ? null : userGroup.getId();
        if (groupId == null) {
            throw new BusinessException("VALIDATION_FAILED", "group_code 不存在", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Room room = roomMapper.findByLocation(communityId, buildingNo, unitNo, roomNo);
        if (room == null) {
            throw new BusinessException("VALIDATION_FAILED", "room 不存在", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Integer uniqueMonth = propertyBill ? null : periodMonth;
        if (billMapper.findByUniqueKey(room.getId(), feeType, periodYear, uniqueMonth) != null) {
            throw new BusinessException("VALIDATION_FAILED", "同房间同账期账单已存在", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Bill bill = new Bill();
        bill.setBillNo(billNo);
        bill.setRoomId(room.getId());
        bill.setGroupId(groupId);
        bill.setFeeType(feeType);
        bill.setCycleType(propertyBill ? "YEAR" : "MONTH");
        bill.setPeriodYear(periodYear);
        bill.setPeriodMonth(propertyBill ? null : periodMonth);
        bill.setServicePeriodStart(propertyBill
                ? LocalDate.of(periodYear, 1, 1)
                : LocalDate.of(periodYear, periodMonth, 1));
        bill.setServicePeriodEnd(propertyBill
                ? LocalDate.of(periodYear, 12, 31)
                : LocalDate.of(periodYear, periodMonth, 1).with(TemporalAdjusters.lastDayOfMonth()));
        bill.setAmountDue(MoneyUtils.scaleMoney(amountDue));
        bill.setDiscountAmountTotal(BigDecimal.ZERO.setScale(2));
        bill.setAmountPaid(BigDecimal.ZERO.setScale(2));
        bill.setDueDate(dueDate);
        bill.setStatus("ISSUED");
        bill.setSourceType("IMPORTED");
        bill.setRemark(row.getOrDefault("remark", "账单导入"));
        billMapper.insert(bill);

        BillLine line = new BillLine();
        line.setBillId(bill.getId());
        line.setLineNo(1);
        line.setLineType(feeType);
        line.setItemName(billNo + " 导入账单");
        line.setUnitPrice(MoneyUtils.scaleMoney(amountDue));
        line.setQuantity(BigDecimal.ONE.setScale(3));
        line.setLineAmount(MoneyUtils.scaleMoney(amountDue));
        line.setExtJson(writeJson(row));
        billLineMapper.insert(line);
    }

    private List<List<String>> buildExportRows(BillExportCreateDTO dto) {
        List<List<String>> rows = new ArrayList<>();
        List<com.wuye.bill.vo.BillListItemVO> bills = billMapper.listAdminBills(dto.getPeriodYear(), dto.getPeriodMonth(), dto.getFeeType(), dto.getStatus(), 0, 1000);
        for (com.wuye.bill.vo.BillListItemVO bill : bills) {
            rows.add(List.of(
                    String.valueOf(bill.getBillId()),
                    bill.getBillNo(),
                    bill.getRoomLabel(),
                    bill.getFeeType(),
                    bill.getPeriod(),
                    bill.getAmountDue().toPlainString(),
                    bill.getAmountPaid().toPlainString(),
                    bill.getStatus(),
                    String.valueOf(bill.getDueDate())
            ));
        }
        return rows;
    }

    private List<String> exportHeaders() {
        return List.of("bill_id", "bill_no", "room_label", "fee_type", "period", "amount_due", "amount_paid", "status", "due_date");
    }

    private void saveRowError(Long batchId, int rowNo, String code, String message, String rawData) {
        ImportRowError error = new ImportRowError();
        error.setBatchId(batchId);
        error.setRowNo(rowNo);
        error.setErrorCode(code);
        error.setErrorMessage(message);
        error.setRawData(rawData);
        importRowErrorMapper.insert(error);
    }

    private String required(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new BusinessException("VALIDATION_FAILED", key + " 不能为空", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return value.trim();
    }

    private Integer parseInt(String value, String field) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException("VALIDATION_FAILED", field + " 格式错误", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private Integer parseOptionalInt(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseInt(value.trim(), field);
    }

    private BigDecimal parseMoney(String value, String field) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException("VALIDATION_FAILED", field + " 格式错误", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new BusinessException("VALIDATION_FAILED", field + " 格式错误", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize import/export json", ex);
        }
    }

    private Map<String, Object> buildImportAuditDetail(ImportBatch batch) {
        Map<String, Object> detail = new java.util.LinkedHashMap<>();
        detail.put("importBatchId", batch.getId());
        detail.put("batchNo", batch.getBatchNo());
        detail.put("importType", batch.getImportType());
        detail.put("fileUrl", batch.getFileUrl());
        detail.put("status", batch.getStatus());
        detail.put("totalCount", batch.getTotalCount());
        detail.put("successCount", batch.getSuccessCount());
        detail.put("failCount", batch.getFailCount());
        return detail;
    }

    private Map<String, Object> buildExportAuditDetail(ExportJob exportJob, BillExportCreateDTO dto) {
        Map<String, Object> detail = new java.util.LinkedHashMap<>();
        detail.put("exportJobId", exportJob.getId());
        detail.put("exportType", exportJob.getExportType());
        detail.put("status", exportJob.getStatus());
        detail.put("periodYear", dto.getPeriodYear());
        detail.put("periodMonth", dto.getPeriodMonth());
        detail.put("feeType", dto.getFeeType());
        detail.put("billStatus", dto.getStatus());
        detail.put("fileUrl", exportJob.getFileUrl());
        detail.put("expiredAt", exportJob.getExpiredAt());
        return detail;
    }
}
