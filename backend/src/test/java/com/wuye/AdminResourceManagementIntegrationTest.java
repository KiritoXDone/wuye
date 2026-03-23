package com.wuye;

import com.fasterxml.jackson.databind.JsonNode;
import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminResourceManagementIntegrationTest extends AbstractIntegrationTest {

    @Test
    void communityRoomTypeAndRoomEndpointsSupportCrudAndBatchOperations() throws Exception {
        mockMvc.perform(get("/api/v1/admin/communities")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(100));

        long removableCommunityId = createCommunity("COMM-RM-001", "可删除小区");
        mockMvc.perform(put("/api/v1/admin/communities/" + removableCommunityId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityCode": "COMM-RM-002",
                                  "name": "可删除小区-更新"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.communityCode").value("COMM-RM-002"))
                .andExpect(jsonPath("$.data.name").value("可删除小区-更新"));

        mockMvc.perform(delete("/api/v1/admin/communities/" + removableCommunityId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM community WHERE id = ?",
                Integer.class,
                removableCommunityId)).isEqualTo(0);

        long roomTypeId = createRoomType(100L, "RT-A", "高层三房", "118.80");
        mockMvc.perform(get("/api/v1/admin/room-types")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("communityId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(roomTypeId));

        mockMvc.perform(put("/api/v1/admin/room-types/" + roomTypeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "typeCode": "RT-B",
                                  "typeName": "高层四房",
                                  "areaM2": 128.60
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.typeCode").value("RT-B"))
                .andExpect(jsonPath("$.data.typeName").value("高层四房"));

        long room901Id = createRoom(100L, "9", "1", "901", roomTypeId, "128.60");
        mockMvc.perform(get("/api/v1/admin/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("communityId", "100")
                        .param("buildingNo", "9")
                        .param("unitNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(room901Id));

        mockMvc.perform(put("/api/v1/admin/rooms/" + room901Id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomTypeId": %s,
                                  "areaM2": 130.10
                                }
                                """.formatted(roomTypeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(room901Id))
                .andExpect(jsonPath("$.data.areaM2").value(130.10));

        mockMvc.perform(post("/api/v1/admin/rooms/batch-create")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "buildingNo": "9",
                                  "unitNo": "1",
                                  "roomNos": ["902", "903"],
                                  "roomTypeId": %s,
                                  "areaM2": 99.90
                                }
                                """.formatted(roomTypeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedCount").value(2))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.skippedCount").value(0));

        mockMvc.perform(post("/api/v1/admin/rooms/batch-update")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "applyToFiltered": true,
                                  "buildingNo": "9",
                                  "unitNo": "1",
                                  "roomSuffix": "3",
                                  "targetAreaM2": 101.10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedCount").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1));

        long room902Id = findRoomId("9", "1", "902");
        long room903Id = findRoomId("9", "1", "903");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT area_m2 FROM room WHERE id = ?",
                String.class,
                room903Id)).isEqualTo("101.10");

        mockMvc.perform(post("/api/v1/admin/rooms/batch-delete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "selectionRoomIds": [%s]
                                }
                                """.formatted(room903Id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedCount").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1));

        mockMvc.perform(delete("/api/v1/admin/rooms/" + room902Id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/admin/rooms/" + room901Id + "/hard-delete")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM room WHERE id = ?",
                Integer.class,
                room901Id)).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM room WHERE id = ?",
                Integer.class,
                room902Id)).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM room WHERE id = ?",
                Integer.class,
                room903Id)).isEqualTo(0);

        mockMvc.perform(delete("/api/v1/admin/room-types/" + roomTypeId + "/hard-delete")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM room_type WHERE id = ?",
                Integer.class,
                roomTypeId)).isEqualTo(0);
    }

    @Test
    void adminAccountAndAgentEndpointsSupportManagementQueries() throws Exception {
        mockMvc.perform(get("/api/v1/admin/accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("accountType", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].accountType").value("ADMIN"));

        long createdAdminId = createAdminAccount("ops_admin_case", "NewPass123", "运维管理员");
        mockMvc.perform(post("/api/v1/admin/accounts/" + createdAdminId + "/reset-password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newPassword": "ResetPass123"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ops_admin_case",
                                  "password": "ResetPass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(createdAdminId));

        mockMvc.perform(put("/api/v1/admin/accounts/" + createdAdminId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "0"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ops_admin_case",
                                  "password": "ResetPass123"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/admin/accounts/" + createdAdminId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/accounts/10002/rooms")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roomId").value(1001))
                .andExpect(jsonPath("$.data[0].bindingStatus").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/admin/accounts/10002/rooms/1001/unbind")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM account_room WHERE account_id = 10002 AND room_id = 1001",
                String.class)).isEqualTo("INACTIVE");

        MvcResult orgUnitsResult = mockMvc.perform(get("/api/v1/admin/org-units")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        boolean containsRootOrg = false;
        for (JsonNode item : read(orgUnitsResult).path("data")) {
            if ("ROOT".equals(item.path("orgCode").asText())) {
                containsRootOrg = true;
                break;
            }
        }
        assertThat(containsRootOrg).isTrue();

        mockMvc.perform(get("/api/v1/admin/agent-groups")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groupCode").value("G-COMM001-1-2"));

        mockMvc.perform(post("/api/v1/admin/agent-groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentCode": "AGENT-A",
                                  "groupCode": "G-COMM001-1-2",
                                  "permission": "MANAGE",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("MANAGE"));

        mockMvc.perform(get("/api/v1/agent/groups")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groupCode").value("G-COMM001-1-2"))
                .andExpect(jsonPath("$.data[0].permission").value("MANAGE"));
    }

    private long createCommunity(String code, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/communities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityCode": "%s",
                                  "name": "%s"
                                }
                                """.formatted(code, name)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }

    private long createRoomType(long communityId, String typeCode, String typeName, String areaM2) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/room-types")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": %s,
                                  "typeCode": "%s",
                                  "typeName": "%s",
                                  "areaM2": %s
                                }
                                """.formatted(communityId, typeCode, typeName, areaM2)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }

    private long createRoom(long communityId, String buildingNo, String unitNo, String roomNo, long roomTypeId, String areaM2) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": %s,
                                  "buildingNo": "%s",
                                  "unitNo": "%s",
                                  "roomNo": "%s",
                                  "roomTypeId": %s,
                                  "areaM2": %s
                                }
                                """.formatted(communityId, buildingNo, unitNo, roomNo, roomTypeId, areaM2)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }

    private long findRoomId(String buildingNo, String unitNo, String roomNo) {
        return jdbcTemplate.queryForObject("""
                SELECT id
                FROM room
                WHERE community_id = 100
                  AND building_no = ?
                  AND unit_no = ?
                  AND room_no = ?
                ORDER BY id DESC
                LIMIT 1
                """, Long.class, buildingNo, unitNo, roomNo);
    }

    private long createAdminAccount(String username, String password, String realName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/accounts/admins")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "realName": "%s",
                                  "mobile": "13912345678"
                                }
                                """.formatted(username, password, realName)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }
}
