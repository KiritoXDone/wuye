package com.wuye;

import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HouseholdPaymentOverviewIntegrationTest extends AbstractIntegrationTest {

    @Test
    void adminCanViewHouseholdOverviewAndMarkBillPaidManually() throws Exception {
        createFeeRule("PROPERTY", "YEAR", 2032, "2.5000");
        createFeeRule("WATER", "MONTH", 2032, "3.2500");

        mockMvc.perform(post("/api/v1/admin/bills/generate/property-yearly")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": 2032,
                                  "overwriteStrategy": "SKIP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(2));

        mockMvc.perform(post("/api/v1/admin/water-meters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1001,
                                  "meterNo": "WM-1001-HOUSEHOLD",
                                  "installAt": "2032-01-01"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/water-readings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1001,
                                  "year": 2032,
                                  "month": 8,
                                  "prevReading": 10.000,
                                  "currReading": 18.000,
                                  "readAt": "2032-08-15T10:00:00",
                                  "remark": "household-overview"
                                }
                                """))
                .andExpect(status().isOk());

        long propertyBillId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM bill
                WHERE room_id = 1001
                  AND fee_type = 'PROPERTY'
                  AND period_year = 2032
                """, Long.class);

        mockMvc.perform(get("/api/v1/admin/billing/households")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("communityId", "100")
                        .param("periodYear", "2032")
                        .param("periodMonth", "8")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].roomId").value(1001))
                .andExpect(jsonPath("$.data.list[0].propertyBillId").value(propertyBillId))
                .andExpect(jsonPath("$.data.list[0].propertyStatus").value("ISSUED"))
                .andExpect(jsonPath("$.data.list[0].waterStatus").value("ISSUED"));

        mockMvc.perform(post("/api/v1/admin/bills/" + propertyBillId + "/mark-paid")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paidAt": "2032-09-01T09:30:00",
                                  "remark": "线下现金已收"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM bill WHERE id = ?",
                String.class,
                propertyBillId)).isEqualTo("PAID");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT amount_paid FROM bill WHERE id = ?",
                java.math.BigDecimal.class,
                propertyBillId)).isEqualByComparingTo("246.25");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM audit_log WHERE biz_type = 'BILL' AND biz_id = ? AND action = 'MANUAL_MARK_PAID'",
                Integer.class,
                String.valueOf(propertyBillId))).isEqualTo(1);

        mockMvc.perform(get("/api/v1/admin/billing/households")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("communityId", "100")
                        .param("periodYear", "2032")
                        .param("periodMonth", "8")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].propertyStatus").value("PAID"));
    }

    private void createFeeRule(String feeType, String cycleType, int year, String unitPrice) throws Exception {
        mockMvc.perform(post("/api/v1/admin/fee-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "feeType": "%s",
                                  "unitPrice": %s,
                                  "cycleType": "%s",
                                  "effectiveFrom": "%s-01-01",
                                  "effectiveTo": "%s-12-31",
                                  "remark": "household overview"
                                }
                                """.formatted(feeType, unitPrice, cycleType, year, year)))
                .andExpect(status().isOk());
    }
}
