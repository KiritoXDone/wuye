package com.wuye.importexport.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ImportExportFileService {

    private final Path importRootPath;

    public ImportExportFileService(@Value("${app.import-export.import-dir:imports/bills}") String importRootDir) {
        this.importRootPath = Path.of(importRootDir).toAbsolutePath().normalize();
    }

    public List<Map<String, String>> readRows(String fileUrl) throws IOException {
        Path filePath = resolveImportPath(fileUrl);
        String lower = filePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) {
            return readCsv(filePath);
        }
        if (lower.endsWith(".xlsx")) {
            return readXlsx(filePath);
        }
        throw new IOException("仅支持 csv / xlsx 文件");
    }

    public void writeCsv(Path path, List<String> headers, List<List<String>> rows) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            printer.printRecord(headers);
            for (List<String> row : rows) {
                printer.printRecord(row);
            }
        }
    }

    public void writeXlsx(Path path, String sheetName, List<String> headers, List<List<String>> rows) throws IOException {
        Files.createDirectories(path.getParent());
        try (Workbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(path)) {
            Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(sheetName));
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }
            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(i + 1);
                List<String> values = rows.get(i);
                for (int j = 0; j < values.size(); j++) {
                    row.createCell(j).setCellValue(values.get(j));
                }
            }
            workbook.write(outputStream);
        }
    }

    private List<Map<String, String>> readCsv(Path filePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath);
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .get()
                     .parse(reader)) {
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                parser.getHeaderMap().forEach((header, index) -> row.put(cleanHeader(header), record.get(header)));
                rows.add(row);
            }
            return rows;
        }
    }

    private List<Map<String, String>> readXlsx(Path filePath) throws IOException {
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(filePath.toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            int cellCount = headerRow == null ? 0 : headerRow.getLastCellNum();
            for (int i = 0; i < cellCount; i++) {
                Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                headers.add(cell == null ? "" : formatter.formatCellValue(cell));
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                boolean hasValue = false;
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String value = cell == null ? "" : formatter.formatCellValue(cell);
                    if (!value.isBlank()) {
                        hasValue = true;
                    }
                    values.put(headers.get(j), value);
                }
                if (hasValue) {
                    rows.add(values);
                }
            }
            return rows;
        }
    }

    private Path resolveImportPath(String fileUrl) throws IOException {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IOException("导入文件不能为空");
        }
        String normalized = fileUrl.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file:")) {
            throw new IOException("导入文件必须位于服务端导入目录内");
        }
        Path candidate = Path.of(normalized);
        if (!candidate.isAbsolute()) {
            candidate = importRootPath.resolve(candidate);
        }
        Path resolved = candidate.toAbsolutePath().normalize();
        if (!resolved.startsWith(importRootPath)) {
            throw new IOException("导入文件必须位于服务端导入目录内");
        }
        if (!Files.isRegularFile(resolved)) {
            throw new IOException("导入文件不存在");
        }
        return resolved;
    }

    private String cleanHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.replace("\uFEFF", "").trim();
    }
}
