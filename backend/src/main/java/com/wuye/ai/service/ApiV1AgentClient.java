package com.wuye.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.bill.dto.WaterReadingCreateDTO;
import com.wuye.bill.vo.BillDetailVO;
import com.wuye.bill.vo.BillListItemVO;
import com.wuye.common.api.PageResponse;
import com.wuye.common.config.AppAiProperties;
import com.wuye.common.exception.BusinessException;
import com.wuye.payment.dto.PaymentCreateDTO;
import com.wuye.payment.vo.PaymentCreateVO;
import com.wuye.payment.vo.PaymentStatusVO;
import com.wuye.room.dto.AdminRoomCreateDTO;
import com.wuye.room.dto.AdminRoomListQuery;
import com.wuye.room.vo.AdminCommunityVO;
import com.wuye.room.vo.AdminRoomVO;
import com.wuye.room.vo.RoomTypeVO;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ApiV1AgentClient {

    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;
    private final AppAiProperties appAiProperties;

    public ApiV1AgentClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper, AppAiProperties appAiProperties) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.objectMapper = objectMapper;
        this.appAiProperties = appAiProperties;
    }

    public List<AdminCommunityVO> listAdminCommunities() {
        return getList("/api/v1/admin/communities", new ParameterizedTypeReference<>() {});
    }

    public List<RoomTypeVO> listRoomTypes(Long communityId) {
        return getList(withQuery("/api/v1/admin/room-types", Map.of("communityId", communityId)), new ParameterizedTypeReference<>() {});
    }

    public List<AdminRoomVO> listAdminRooms(AdminRoomListQuery query) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("communityId", query.getCommunityId());
        params.put("buildingNo", query.getBuildingNo());
        params.put("unitNo", query.getUnitNo());
        params.put("roomNo", query.getRoomNo());
        params.put("roomNoKeyword", query.getRoomNoKeyword());
        params.put("roomSuffix", query.getRoomSuffix());
        params.put("roomTypeId", query.getRoomTypeId());
        return getList(withQuery("/api/v1/admin/rooms", params), new ParameterizedTypeReference<>() {});
    }

    public AdminRoomVO createRoom(AdminRoomCreateDTO dto) {
        return post("/api/v1/admin/rooms", dto, new ParameterizedTypeReference<>() {});
    }

    public void disableRoom(Long roomId) {
        exchange("/api/v1/admin/rooms/" + roomId, HttpMethod.DELETE, null, new ParameterizedTypeReference<Void>() {});
    }

    public BillDetailVO getBillDetail(Long billId, boolean admin) {
        String path = admin ? "/api/v1/admin/bills/" + billId : "/api/v1/bills/" + billId;
        return exchange(path, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    }

    public PageResponse<BillListItemVO> listRoomBills(Long roomId, String status, boolean admin) {
        String path;
        if (admin) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("roomId", roomId);
            params.put("status", status);
            params.put("pageNo", 1);
            params.put("pageSize", 20);
            path = withQuery("/api/v1/admin/bills", params);
        } else {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("status", status);
            path = withQuery("/api/v1/me/rooms/" + roomId + "/bills", params);
        }
        return exchange(path, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    }

    public PaymentCreateVO createPayment(PaymentCreateDTO dto) {
        return post("/api/v1/payments", dto, new ParameterizedTypeReference<>() {});
    }

    public PaymentStatusVO queryPayment(String payOrderNo) {
        return exchange("/api/v1/payments/" + payOrderNo, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    }

    public Map<String, Object> createWaterReading(Long roomId,
                                                  Integer year,
                                                  Integer month,
                                                  java.math.BigDecimal prevReading,
                                                  java.math.BigDecimal currReading,
                                                  String readAt,
                                                  String remark) {
        WaterReadingCreateDTO dto = new WaterReadingCreateDTO();
        dto.setRoomId(roomId);
        dto.setYear(year);
        dto.setMonth(month);
        dto.setPrevReading(prevReading);
        dto.setCurrReading(currReading);
        dto.setReadAt(java.time.LocalDateTime.parse(readAt));
        dto.setRemark(remark);
        return post("/api/v1/admin/water-readings", dto, new ParameterizedTypeReference<>() {});
    }

    private <T> T post(String path, Object body, ParameterizedTypeReference<T> type) {
        return exchange(path, HttpMethod.POST, body, type);
    }

    private <T> List<T> getList(String path, ParameterizedTypeReference<List<T>> type) {
        return exchange(path, HttpMethod.GET, null, type);
    }

    private <T> T exchange(String path, HttpMethod method, Object body, ParameterizedTypeReference<T> type) {
        String authorization = RequestAuthHolder.getAuthorization();
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException("UNAUTHORIZED", "缺少当前用户授权信息，无法调用后端接口", HttpStatus.UNAUTHORIZED);
        }
        RestTemplate restTemplate = restTemplateBuilder
                .rootUri(resolveBackendBaseUrl())
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, authorization);

        try {
            ResponseEntity<String> response = restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"0".equals(root.path("code").asText())) {
                throw new BusinessException(root.path("code").asText("REQUEST_FAILED"), root.path("message").asText("请求失败"), HttpStatus.BAD_REQUEST);
            }
            JsonNode data = root.path("data");
            return objectMapper.convertValue(data, objectMapper.getTypeFactory().constructType(type.getType()));
        } catch (HttpStatusCodeException ex) {
            throw resolveHttpException(ex);
        } catch (ResourceAccessException ex) {
            throw new BusinessException("API_REQUEST_FAILED", "调用后端接口失败：无法连接本地后端服务", HttpStatus.BAD_GATEWAY);
        } catch (RestClientException ex) {
            throw new BusinessException("API_REQUEST_FAILED", "调用后端接口失败", HttpStatus.BAD_GATEWAY);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("API_RESPONSE_INVALID", "后端接口响应解析失败", HttpStatus.BAD_GATEWAY);
        }
    }

    private String withQuery(String path, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        params.forEach((key, value) -> {
            if (value != null && !value.toString().isBlank()) {
                builder.queryParam(key, value);
            }
        });
        return builder.build().toUriString();
    }

    private String resolveBackendBaseUrl() {
        String configured = appAiProperties.getRuntime().getBackendBaseUrl();
        if (configured == null || configured.isBlank()) {
            return "http://127.0.0.1:8081";
        }
        return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
    }

    private BusinessException resolveHttpException(HttpStatusCodeException ex) {
        try {
            JsonNode root = objectMapper.readTree(ex.getResponseBodyAsString());
            String code = root.path("code").asText("API_REQUEST_FAILED");
            String message = root.path("message").asText();
            if (message == null || message.isBlank()) {
                message = "调用后端接口失败";
            }
            return new BusinessException(code, message, HttpStatus.valueOf(ex.getStatusCode().value()));
        } catch (Exception ignored) {
            return new BusinessException("API_REQUEST_FAILED", "调用后端接口失败", HttpStatus.valueOf(ex.getStatusCode().value()));
        }
    }
}
