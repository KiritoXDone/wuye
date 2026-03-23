package com.wuye;

import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.import-export.export-dir=${java.io.tmpdir}/wuye-export-blocked")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ImportExportClosureIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private org.springframework.core.env.Environment environment;

    @Test
    void importAndExportTasksExposeNotFoundAndRetainFailedExportResult() throws Exception {
        mockMvc.perform(get("/api/v1/admin/imports/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(get("/api/v1/admin/exports/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        Path blockedRoot = Path.of(environment.getProperty("app.import-export.export-dir"));
        Files.deleteIfExists(blockedRoot);
        Files.writeString(blockedRoot, "blocked export root");
        try {
            MvcResult exportResult = mockMvc.perform(post("/api/v1/admin/exports/bills")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "periodYear": 2026,
                                      "periodMonth": 6,
                                      "feeType": "PROPERTY",
                                      "status": "ISSUED"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("FAILED"))
                    .andExpect(jsonPath("$.data.fileUrl").isEmpty())
                    .andReturn();

            long jobId = read(exportResult).path("data").path("id").asLong();

            mockMvc.perform(get("/api/v1/admin/exports/" + jobId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(jobId))
                    .andExpect(jsonPath("$.data.status").value("FAILED"))
                    .andExpect(jsonPath("$.data.fileUrl").isEmpty());
        } finally {
            Files.deleteIfExists(blockedRoot);
        }
    }

    @Test
    void importRejectsRemoteAndOutOfDirectorySources() throws Exception {
        MvcResult remoteImportResult = mockMvc.perform(post("/api/v1/admin/imports/bills")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileUrl": "http://127.0.0.1:8080/evil.csv"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andReturn();

        long remoteBatchId = read(remoteImportResult).path("data").path("id").asLong();
        mockMvc.perform(get("/api/v1/admin/imports/" + remoteBatchId + "/errors")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].errorCode").value("INVALID_ARGUMENT"));

        Path outsideFile = Files.createTempFile("wuye-import-outside-", ".csv");
        Files.writeString(outsideFile, "bill_no\n", java.nio.charset.StandardCharsets.UTF_8);
        try {
            MvcResult outsideImportResult = mockMvc.perform(post("/api/v1/admin/imports/bills")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "fileUrl": "%s"
                                    }
                                    """.formatted(outsideFile.toString().replace("\\", "\\\\"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("FAILED"))
                    .andReturn();

            long outsideBatchId = read(outsideImportResult).path("data").path("id").asLong();
            mockMvc.perform(get("/api/v1/admin/imports/" + outsideBatchId + "/errors")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].errorCode").value("INVALID_ARGUMENT"));
        } finally {
            Files.deleteIfExists(outsideFile);
        }
    }
}
